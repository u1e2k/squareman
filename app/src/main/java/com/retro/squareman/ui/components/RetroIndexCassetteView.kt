package com.retro.squareman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.squareman.data.model.Track
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PSPMAN 実機スタイル 2: レトロ・インデックス カセット UI (画像2を精密再現)
 */
@Composable
fun RetroIndexCassetteView(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val progress = remember(currentPositionMs, durationMs) {
        if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    var leftReelAngle by remember { mutableFloatStateOf(0f) }
    var rightReelAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, progress) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameTime = 0L

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f

                    val rMin = 1.0f
                    val rMax = 2.4f
                    val rLeft = sqrt(rMin * rMin + (rMax * rMax - rMin * rMin) * (1f - progress))
                    val rRight = sqrt(rMin * rMin + (rMax * rMax - rMin * rMin) * progress)

                    val baseSpeed = 110f
                    val leftOmega = (baseSpeed * (rMin / rLeft))
                    val rightOmega = (baseSpeed * (rMin / rRight))

                    leftReelAngle = (leftReelAngle + leftOmega * dt) % 360f
                    rightReelAngle = (rightReelAngle + rightOmega * dt) % 360f
                }
                lastFrameTime = frameTimeNanos
            }
        }
    }

    val vintageRed = Color(0xFFC92A2A)
    val paperBg = Color(0xFFFAF7F0)
    val paperLine = Color(0xFFDDD5C7)
    val textInk = Color(0xFF1C2028)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121418))
    ) {
        // --- 1. 上部: ヴィンテージ・インデックスカード領域 ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f)
                .background(paperBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1行目: INDEX + 曲名
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INDEX",
                    color = Color(0xFF7A7060),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(68.dp)
                )
                Text(
                    text = track?.title ?: "PSPMAN",
                    color = textInk,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = paperLine, thickness = 1.5.dp)

            // 2行目: アーティスト名
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARTIST",
                    color = Color(0xFF8A8070),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(68.dp)
                )
                Text(
                    text = track?.artist ?: "OBSOLETESONY",
                    color = Color(0xFF323846),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = paperLine, thickness = 1.dp)

            // 3行目: アルバム名
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALBUM",
                    color = Color(0xFF8A8070),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(68.dp)
                )
                Text(
                    text = (track?.album ?: "CASSETTE COLLECTION").uppercase(),
                    color = Color(0xFF5A6070),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- 2. 中央: レトロレッドバンド ＆ カセット窓 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .background(vintageRed)
                .padding(vertical = 10.dp)
        ) {
            // 左端: A / JAPAN
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A",
                    color = Color(0xFF141820),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "JAPAN",
                    color = Color(0xFF141820),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // 中央: 黒カセットブロック
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.76f)
                    .fillMaxSize()
                    .background(Color(0xFF0F1115))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRetroIndexCassette(
                        size = size,
                        progress = progress,
                        leftAngle = leftReelAngle,
                        rightAngle = rightReelAngle
                    )
                }
            }
        }

        // --- 3. 下部: レトロデッキコンソール & 進行バー & タイムコード ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.24f)
                .background(Color(0xFF121418))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // プログレスバー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF222630), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxSize()
                        .background(Color(0xFFFF9F1C), RoundedCornerShape(2.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    color = Color(0xFFFF9F1C),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPlaying) "PLAY ▶" else "PAUSE ❚❚",
                    color = if (isPlaying) Color(0xFF38EF7D) else Color(0xFF6B7280),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTime(durationMs),
                    color = Color(0xFF8E95A5),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * 窓領域外へのテープはみ出しを clipRect で完全に防止し、白リールを端正に描画
 */
private fun DrawScope.drawRetroIndexCassette(
    size: Size,
    progress: Float,
    leftAngle: Float,
    rightAngle: Float
) {
    val w = size.width
    val h = size.height

    val reelCenterY = h / 2f
    val leftReelX = w * 0.22f
    val rightReelX = w * 0.78f
    val reelRadius = h * 0.32f // 黒枠内に上品に収まるサイズ

    // 1. 中央テープ窓 (ダークスレートグレー)
    val windowW = w * 0.36f
    val windowH = h * 0.80f
    val windowLeft = (w - windowW) / 2f
    val windowTop = (h - windowH) / 2f

    // 窓の背景
    drawRect(
        color = Color(0xFF2A2E3B),
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowW, windowH)
    )

    // ★重要: テープ残量の左右円弧は窓の範囲内だけにクリップする！
    clipRect(
        left = windowLeft,
        top = windowTop,
        right = windowLeft + windowW,
        bottom = windowTop + windowH
    ) {
        // 左側テープ巻き
        val leftTapeRadius = (windowW * 0.62f) * (1f - progress * 0.65f)
        drawCircle(
            color = Color(0xFF1E212A),
            radius = leftTapeRadius,
            center = Offset(windowLeft, reelCenterY)
        )
        // 右側テープ巻き
        val rightTapeRadius = (windowW * 0.62f) * (0.35f + progress * 0.65f)
        drawCircle(
            color = Color(0xFF1E212A),
            radius = rightTapeRadius,
            center = Offset(windowLeft + windowW, reelCenterY)
        )

        // 中央十字目盛り線
        val meterColor = Color(0xFF161820)
        drawLine(
            color = meterColor,
            start = Offset(windowLeft + windowW * 0.10f, reelCenterY),
            end = Offset(windowLeft + windowW * 0.90f, reelCenterY),
            strokeWidth = 2.5f
        )
        val tickH = windowH * 0.52f
        // 中央縦線
        drawLine(
            color = meterColor,
            start = Offset(w / 2f, reelCenterY - tickH / 2f),
            end = Offset(w / 2f, reelCenterY + tickH / 2f),
            strokeWidth = 2.5f
        )
        // 左右縦目盛り
        drawLine(
            color = meterColor,
            start = Offset(w / 2f - windowW * 0.24f, reelCenterY - tickH * 0.30f),
            end = Offset(w / 2f - windowW * 0.24f, reelCenterY + tickH * 0.30f),
            strokeWidth = 2f
        )
        drawLine(
            color = meterColor,
            start = Offset(w / 2f + windowW * 0.24f, reelCenterY - tickH * 0.30f),
            end = Offset(w / 2f + windowW * 0.24f, reelCenterY + tickH * 0.30f),
            strokeWidth = 2f
        )
    }

    // 2. 左右の白抜き極太スプロケットリール
    drawBoldWhiteReel(Offset(leftReelX, reelCenterY), reelRadius, leftAngle)
    drawBoldWhiteReel(Offset(rightReelX, reelCenterY), reelRadius, rightAngle)
}

/**
 * 白抜き極太リールの描画
 */
private fun DrawScope.drawBoldWhiteReel(center: Offset, radius: Float, angle: Float) {
    rotate(angle, pivot = center) {
        // 白い太いリング
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.28f)
        )

        // 6つの内向き四角いギア爪
        val teeth = 6
        val toothW = radius * 0.22f
        val toothH = radius * 0.18f
        for (i in 0 until teeth) {
            val rad = (i * 60f) * (PI / 180f).toFloat()
            val tx = center.x + cos(rad) * (radius * 0.72f)
            val ty = center.y + sin(rad) * (radius * 0.72f)

            rotate(i * 60f, pivot = Offset(tx, ty)) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(tx - toothW / 2f, ty - toothH / 2f),
                    size = Size(toothW, toothH)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
