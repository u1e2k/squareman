package com.retro.squareman.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.squareman.data.model.Track
import com.retro.squareman.ui.theme.PSPAccentBlue
import com.retro.squareman.ui.theme.RetroBorder
import com.retro.squareman.ui.theme.RetroDarkBg
import com.retro.squareman.ui.theme.RetroFocusAmber
import com.retro.squareman.ui.theme.RetroPanelBg
import com.retro.squareman.ui.theme.RetroSurface
import com.retro.squareman.ui.theme.RetroTextDim
import com.retro.squareman.ui.theme.RetroTextPrimary
import com.retro.squareman.ui.theme.RetroTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 物理キー特化・1:1 & 4:3 最適化カセットデッキコンポーネント
 */
@Composable
fun CassetteDeckView(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    // 進捗率 (0.0 .. 1.0)
    val progress = remember(currentPositionMs, durationMs) {
        if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // リール回転角の状態
    var leftReelAngle by remember { mutableFloatStateOf(0f) }
    var rightReelAngle by remember { mutableFloatStateOf(0f) }

    // リール回転のアニメーションループ
    LaunchedEffect(isPlaying, progress) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameTime = 0L

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f // 秒換算

                    // 最小・最大半径比率
                    val rMin = 1.0f
                    val rMax = 2.5f
                    val rLeft = sqrt(rMin * rMin + (rMax * rMax - rMin * rMin) * (1f - progress))
                    val rRight = sqrt(rMin * rMin + (rMax * rMax - rMin * rMin) * progress)

                    // 基準角速度 (度/秒)
                    val baseSpeed = 120f
                    val leftOmega = (baseSpeed * (rMin / rLeft))
                    val rightOmega = (baseSpeed * (rMin / rRight))

                    leftReelAngle = (leftReelAngle + leftOmega * dt) % 360f
                    rightReelAngle = (rightReelAngle + rightOmega * dt) % 360f
                }
                lastFrameTime = frameTimeNanos
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroDarkBg)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. カセット本体 (アスペクト比 1.55 : 1 のクラシックカセット比率) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(12.dp))
                .background(RetroPanelBg)
                .border(2.dp, RetroBorder, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            // カセットシェル & リール Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCassetteShell(size)
                drawCassetteWindow(size, progress, leftReelAngle, rightReelAngle)
            }

            // ラベル中央上部: カセットラベル風テキスト
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SIDE A  TYPE-II [CrO2]",
                    color = RetroFocusAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPlaying) "PLAY ▶" else "PAUSE ❚❚",
                    color = if (isPlaying) Color(0xFF00E676) else RetroTextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. メタデータ & アートワーク表示領域 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(RetroSurface)
                .border(1.dp, RetroBorder, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 埋め込みアートワーク（存在時）またはレトロなアイコンプレースホルダー
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(RetroPanelBg)
                    .border(1.dp, PSPAccentBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (track?.artwork != null) {
                    androidx.compose.foundation.Image(
                        bitmap = track.artwork.asImageBitmap(),
                        contentDescription = "Artwork",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "♪",
                        color = PSPAccentBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 曲情報テキスト
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track?.title ?: "No Track Selected",
                    color = RetroTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track?.artist ?: "--",
                    color = RetroTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track?.album ?: "--",
                    color = RetroTextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // タイムコード表示
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    color = PSPAccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "/ " + formatTime(durationMs),
                    color = RetroTextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * カセット外枠・ネジ穴・テープスロットの描画
 */
private fun DrawScope.drawCassetteShell(size: Size) {
    val w = size.width
    val h = size.height

    // カセット外装の陰影
    drawRoundRect(
        color = Color(0xFF222530),
        size = Size(w, h),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 3f)
    )

    // 四隅のネジ (レトロディテール)
    val screwRadius = 3.5f
    val screwColor = Color(0xFF6C7688)
    val margin = 12f
    val screws = listOf(
        Offset(margin, margin),
        Offset(w - margin, margin),
        Offset(margin, h - margin),
        Offset(w - margin, h - margin),
        Offset(w / 2, margin)
    )
    screws.forEach { pt ->
        drawCircle(screwColor, screwRadius, pt)
        drawLine(Color(0xFF16181F), Offset(pt.x - screwRadius, pt.y), Offset(pt.x + screwRadius, pt.y), 1.5f)
    }

    // 下部台形磁気ヘッドエリア
    val trapTop = h * 0.72f
    val trapBottom = h
    val trapLeftTop = w * 0.22f
    val trapRightTop = w * 0.78f
    val trapLeftBottom = w * 0.15f
    val trapRightBottom = w * 0.85f

    val trapPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(trapLeftTop, trapTop)
        lineTo(trapRightTop, trapTop)
        lineTo(trapRightBottom, trapBottom)
        lineTo(trapLeftBottom, trapBottom)
        close()
    }
    drawPath(trapPath, color = Color(0xFF14161D))
    drawPath(trapPath, color = Color(0xFF2C3240), style = Stroke(width = 2f))
}

/**
 * 透明テープ窓と左右リール、動的テープ巻き厚みの描画
 */
private fun DrawScope.drawCassetteWindow(
    size: Size,
    progress: Float,
    leftAngle: Float,
    rightAngle: Float
) {
    val w = size.width
    val h = size.height

    // 中央の透明窓矩形
    val windowWidth = w * 0.64f
    val windowHeight = h * 0.38f
    val windowLeft = (w - windowWidth) / 2f
    val windowTop = h * 0.26f

    // 窓の背景（ダークスモークプラスチック）
    drawRoundRect(
        color = Color(0xFF0F1117),
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color(0xFF383E4E),
        topLeft = Offset(windowLeft, windowTop),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2f)
    )

    // リール中心座標
    val leftReelCenter = Offset(w * 0.32f, windowTop + windowHeight / 2f)
    val rightReelCenter = Offset(w * 0.68f, windowTop + windowHeight / 2f)

    // リール芯（ハブ）の固定最小半径
    val hubRadius = windowHeight * 0.22f
    val maxTapeRadius = windowHeight * 0.44f

    // テープ巻きの動的半径: 面積保存則 R = sqrt(R_min^2 + (R_max^2 - R_min^2) * ratio)
    val leftTapeRadius = sqrt(hubRadius * hubRadius + (maxTapeRadius * maxTapeRadius - hubRadius * hubRadius) * (1f - progress))
    val rightTapeRadius = sqrt(hubRadius * hubRadius + (maxTapeRadius * maxTapeRadius - hubRadius * hubRadius) * progress)

    // 1. テープ巻線の描画（濃いブラウン磁気テープ色）
    val tapeColor = Color(0xFF3B2A22)
    val tapeTextureColor = Color(0xFF4D372D)

    // 左テープ巻き
    drawCircle(tapeColor, leftTapeRadius, leftReelCenter)
    drawCircle(tapeTextureColor, leftTapeRadius * 0.85f, leftReelCenter, style = Stroke(1.5f))

    // 右テープ巻き
    drawCircle(tapeColor, rightTapeRadius, rightReelCenter)
    drawCircle(tapeTextureColor, rightTapeRadius * 0.85f, rightReelCenter, style = Stroke(1.5f))

    // テープの走行ライン（下部ガイドをつなぐ直線）
    drawLine(
        color = tapeColor,
        start = Offset(leftReelCenter.x, leftReelCenter.y + leftTapeRadius),
        end = Offset(rightReelCenter.x, rightReelCenter.y + rightTapeRadius),
        strokeWidth = 3f
    )

    // 2. リールハブ（白いプラスチック歯車）の描画
    drawReelHub(leftReelCenter, hubRadius, leftAngle)
    drawReelHub(rightReelCenter, hubRadius, rightAngle)

    // 中央の目盛り線 (テープ残量ゲージ)
    val scaleCenterX = w * 0.5f
    for (i in -3..3) {
        val y = leftReelCenter.y + i * 6f
        val lineLen = if (i == 0) 14f else 8f
        drawLine(
            color = Color(0xFF5A6273),
            start = Offset(scaleCenterX - lineLen / 2f, y),
            end = Offset(scaleCenterX + lineLen / 2f, y),
            strokeWidth = 1.5f
        )
    }
}

/**
 * 6つの突起を持つクラシックなカセットハブ（スプロケット歯車）
 */
private fun DrawScope.drawReelHub(center: Offset, radius: Float, angleDegrees: Float) {
    rotate(angleDegrees, pivot = center) {
        // 外輪
        drawCircle(
            color = Color(0xFFE2E6ED),
            radius = radius,
            center = center
        )
        // 軸穴
        drawCircle(
            color = Color(0xFF0D0E12),
            radius = radius * 0.48f,
            center = center
        )

        // 6つのギア歯 (突起)
        val teethCount = 6
        val toothLength = radius * 0.35f
        for (i in 0 until teethCount) {
            val rad = (i * 60f) * (PI / 180f).toFloat()
            val startX = center.x + cos(rad) * (radius * 0.45f)
            val startY = center.y + sin(rad) * (radius * 0.45f)
            val endX = center.x + cos(rad) * (radius * 0.45f + toothLength)
            val endY = center.y + sin(rad) * (radius * 0.45f + toothLength)

            drawLine(
                color = Color(0xFFB0B7C6),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.5f
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
