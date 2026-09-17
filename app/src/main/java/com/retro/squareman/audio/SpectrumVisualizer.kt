package com.retro.squareman.audio

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Visualizer API を利用して 10バンドのスペクトラム解析を行うコントローラー
 */
class SpectrumVisualizer {

    companion object {
        private const val TAG = "SpectrumVisualizer"
        const val BAND_COUNT = 10

        // 10バンドの中心周波数 (Hz)
        val CENTER_FREQUENCIES = floatArrayOf(
            31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
        )
    }

    // 0.0f .. 1.0f に正規化された 10バンドの振幅データ
    private val _bands = MutableStateFlow(FloatArray(BAND_COUNT) { 0f })
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    // ピークホールド値（レトロなピークメーター表示用）
    private val _peaks = MutableStateFlow(FloatArray(BAND_COUNT) { 0f })
    val peaks: StateFlow<FloatArray> = _peaks.asStateFlow()

    private var visualizer: Visualizer? = null
    private var currentSessionId = 0

    // スムージング用バッファ
    private val smoothedBands = FloatArray(BAND_COUNT) { 0f }
    private val currentPeaks = FloatArray(BAND_COUNT) { 0f }
    private val peakHoldFrames = IntArray(BAND_COUNT) { 0 }

    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * 指定された AudioSessionId で Visualizer を初期化・開始
     */
    @Synchronized
    fun start(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (visualizer != null && currentSessionId == audioSessionId) return

        stop()
        currentSessionId = audioSessionId

        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // 通常 1024
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                enabled = true
            }
            startPolling()
            Log.d(TAG, "Visualizer started on session: $audioSessionId, captureSize: ${visualizer?.captureSize}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Visualizer: ${e.message}")
            stop()
        }
    }

    /**
     * キャプチャの停止とリソース開放
     */
    @Synchronized
    fun stop() {
        updateJob?.cancel()
        updateJob = null

        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing visualizer: ${e.message}")
        } finally {
            visualizer = null
            currentSessionId = 0
            resetBands()
        }
    }

    private fun resetBands() {
        smoothedBands.fill(0f)
        currentPeaks.fill(0f)
        _bands.value = FloatArray(BAND_COUNT) { 0f }
        _peaks.value = FloatArray(BAND_COUNT) { 0f }
    }

    /**
     * 一定間隔（約 30〜60fps）で FFT を安全に取得して帯域集約
     */
    private fun startPolling() {
        updateJob?.cancel()
        updateJob = scope.launch {
            val fftBytes = ByteArray(1024)
            while (isActive) {
                val v = visualizer
                if (v != null && v.enabled) {
                    try {
                        val status = v.getFft(fftBytes)
                        if (status == Visualizer.SUCCESS) {
                            processFft(fftBytes, v.samplingRate / 1000)
                        }
                    } catch (e: Exception) {
                        // セッション切断など
                    }
                }
                // 約 33ms (30fps) 間隔で省電力かつ滑らかに更新
                delay(33L)
            }
        }
    }

    private fun processFft(fft: ByteArray, samplingRateHz: Int) {
        val n = fft.size
        val halfN = n / 2
        val bandMagnitudes = FloatArray(BAND_COUNT) { 0f }
        val bandBinCounts = IntArray(BAND_COUNT) { 0 }

        // 各ビンの周波数間隔
        val binWidth = samplingRateHz.toFloat() / n.toFloat()

        // FFT 振幅計算 (k = 1 .. halfN - 1)
        for (k in 1 until halfN) {
            val r = fft[2 * k].toFloat()
            val i = fft[2 * k + 1].toFloat()
            val mag = hypot(r, i)
            val freq = k * binWidth

            // 最も近いバンドインデックスに割り当て
            val bandIdx = findNearestBand(freq)
            if (bandIdx in 0 until BAND_COUNT) {
                bandMagnitudes[bandIdx] = max(bandMagnitudes[bandIdx], mag)
                bandBinCounts[bandIdx]++
            }
        }

        // 正規化とスムージング (Attack & Decay)
        val outputBands = FloatArray(BAND_COUNT)
        val outputPeaks = FloatArray(BAND_COUNT)

        for (i in 0 until BAND_COUNT) {
            // 各バンドのゲイン補正（低域・超高域の感度バランスを調整）
            val gain = when (i) {
                0 -> 1.8f  // 31Hz
                1 -> 1.5f  // 62Hz
                2 -> 1.3f  // 125Hz
                7 -> 1.4f  // 4kHz
                8 -> 1.8f  // 8kHz
                9 -> 2.2f  // 16kHz
                else -> 1.0f
            }

            // 0..128 程度のマグニチュードを 0.0 .. 1.0 にマッピング (対数的な感度)
            val rawValue = (bandMagnitudes[i] / 64f) * gain
            val clamped = min(1.0f, max(0.0f, rawValue))

            // Attack は高速、Decay は滑らかに
            if (clamped > smoothedBands[i]) {
                smoothedBands[i] = clamped // 急峻に上昇
            } else {
                smoothedBands[i] = max(0f, smoothedBands[i] - 0.08f) // 滑らかに落下
            }
            outputBands[i] = smoothedBands[i]

            // ピークホールド更新
            if (smoothedBands[i] >= currentPeaks[i]) {
                currentPeaks[i] = smoothedBands[i]
                peakHoldFrames[i] = 10 // 約10フレーム保持
            } else {
                if (peakHoldFrames[i] > 0) {
                    peakHoldFrames[i]--
                } else {
                    currentPeaks[i] = max(0f, currentPeaks[i] - 0.03f)
                }
            }
            outputPeaks[i] = currentPeaks[i]
        }

        _bands.value = outputBands
        _peaks.value = outputPeaks
    }

    private fun findNearestBand(freq: Float): Int {
        var minDiff = Float.MAX_VALUE
        var bestIdx = 0
        for (i in CENTER_FREQUENCIES.indices) {
            val center = CENTER_FREQUENCIES[i]
            // オクターブ帯域の境界比較
            val diff = kotlin.math.abs(freq - center)
            if (diff < minDiff) {
                minDiff = diff
                bestIdx = i
            }
        }
        return bestIdx
    }
}
