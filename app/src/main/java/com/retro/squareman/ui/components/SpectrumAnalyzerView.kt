package com.retro.squareman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.squareman.audio.SpectrumVisualizer
import com.retro.squareman.ui.theme.PSPAccentBlue
import com.retro.squareman.ui.theme.RetroBorder
import com.retro.squareman.ui.theme.RetroDarkBg
import com.retro.squareman.ui.theme.RetroPanelBg
import com.retro.squareman.ui.theme.RetroTextDim
import com.retro.squareman.ui.theme.RetroTextPrimary
import com.retro.squareman.ui.theme.SpectrumPalettes

enum class VisualizerPalette(val label: String, val colors: List<Color>, val peakColor: Color) {
    VFD_CYAN("VFD CYAN", SpectrumPalettes.VfdCyan, Color(0xFFE0F7FA)),
    AMBER("RETRO AMBER", SpectrumPalettes.Amber, Color(0xFFFFF9C4)),
    EMERALD("EMERALD GREEN", SpectrumPalettes.EmeraldGreen, Color(0xFFE8F5E9)),
    MULTICOLOR("HI-FI MULTI", SpectrumPalettes.Multicolor, Color(0xFFFF5252))
}

/**
 * 10バンド・レトロスペクトラムアナライザ (VFD / LCD風)
 */
@Composable
fun SpectrumAnalyzerView(
    visualizer: SpectrumVisualizer,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false
) {
    val bands by visualizer.bands.collectAsState()
    val peaks by visualizer.peaks.collectAsState()

    var currentPaletteIndex by remember { mutableIntStateOf(0) }
    val palettes = remember { VisualizerPalette.values() }
    val selectedPalette = palettes[currentPaletteIndex]

    val frequencyLabels = remember {
        listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroDarkBg)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ヘッダー情報（パレット切り替え可能）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-BAND GRAPHIC EQUALIZER / ANALYZER",
                color = PSPAccentBlue,
                fontSize = if (isFullScreen) 12.sp else 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // パレット切り替えインジケータ
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(RetroPanelBg)
                    .border(1.dp, RetroBorder, RoundedCornerShape(4.dp))
                    .clickable {
                        currentPaletteIndex = (currentPaletteIndex + 1) % palettes.size
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PALETTE: ${selectedPalette.label}",
                    color = selectedPalette.peakColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // VFD / 液晶風スペアナ描画領域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090A0E))
                .border(2.dp, RetroBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSpectrumGrid(size)
                drawSpectrumBars(
                    size = size,
                    bands = bands,
                    peaks = peaks,
                    palette = selectedPalette
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 周波数軸ラベル表示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            frequencyLabels.forEach { label ->
                Text(
                    text = label,
                    color = RetroTextDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 背景のレトロ目盛りグリッド描画 (+6dB, 0dB, -12dB など)
 */
private fun DrawScope.drawSpectrumGrid(size: Size) {
    val w = size.width
    val h = size.height

    // 縦グリッドライン
    val bandWidth = w / 10f
    for (i in 1..9) {
        val x = i * bandWidth
        drawLine(
            color = Color(0xFF141722),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
    }

    // 横グリッドライン (レベルゲージ)
    val dbSteps = 5
    for (step in 1 until dbSteps) {
        val y = h * (step.toFloat() / dbSteps)
        drawLine(
            color = Color(0xFF141722),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
    }
}

/**
 * セグメントLED / 蛍光管風のバーとピークドットの描画
 */
private fun DrawScope.drawSpectrumBars(
    size: Size,
    bands: FloatArray,
    peaks: FloatArray,
    palette: VisualizerPalette
) {
    val w = size.width
    val h = size.height
    val count = 10
    val totalBarSlotWidth = w / count
    val barGap = totalBarSlotWidth * 0.22f
    val barWidth = totalBarSlotWidth - barGap

    // 縦方向のセグメント分割数（例: 20セグメントでLEDバー感を強調）
    val totalSegments = 24
    val segmentGap = 2.5f
    val totalSegmentGaps = (totalSegments - 1) * segmentGap
    val segmentHeight = (h - totalSegmentGaps) / totalSegments

    for (i in 0 until count) {
        val slotX = i * totalBarSlotWidth
        val barLeft = slotX + barGap / 2f

        val mag = bands.getOrElse(i) { 0f }.coerceIn(0f, 1f)
        val peak = peaks.getOrElse(i) { 0f }.coerceIn(0f, 1f)

        val litSegments = (mag * totalSegments).toInt()
        val peakSegment = (peak * (totalSegments - 1)).toInt()

        // セグメントごとの描画
        for (seg in 0 until totalSegments) {
            // 下から数えて seg 番目 (0 = 最下段, totalSegments - 1 = 最上段)
            val segY = h - (seg + 1) * segmentHeight - seg * segmentGap

            val isLit = seg < litSegments
            val isPeak = seg == peakSegment

            // グラデーション色決定
            val ratio = seg.toFloat() / (totalSegments - 1)
            val color = when {
                isPeak -> palette.peakColor
                isLit -> interpolateColor(palette.colors, ratio)
                else -> Color(0xFF12141C) // 消灯セグメント（レトロ液晶の薄っすら見える影）
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(barLeft, segY),
                size = Size(barWidth, segmentHeight),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }
    }
}

private fun interpolateColor(colors: List<Color>, ratio: Float): Color {
    if (colors.isEmpty()) return Color.White
    if (colors.size == 1) return colors[0]

    val scaled = ratio.coerceIn(0f, 1f) * (colors.size - 1)
    val index = scaled.toInt().coerceIn(0, colors.size - 2)
    val fraction = scaled - index

    val c1 = colors[index]
    val c2 = colors[index + 1]

    return Color(
        red = c1.red + (c2.red - c1.red) * fraction,
        green = c1.green + (c2.green - c1.green) * fraction,
        blue = c1.blue + (c2.blue - c1.blue) * fraction,
        alpha = 1f
    )
}
