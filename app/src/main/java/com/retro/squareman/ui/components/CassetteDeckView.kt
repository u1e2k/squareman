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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * クリアスケルトン＆リアルメカニカル構造カセットデッキコンポーネント
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
    var rollerAngle by remember { mutableFloatStateOf(0f) }

    // リール回転のアニメーションループ
    LaunchedEffect(isPlaying, progress) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameTime = 0L

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f

                    val rMin = 1.0f
                    val rMax = 2.5f
                    val rLeft = sqrt(rMin * rMin + (rMax * rMax - rMin * rMin) * (1f - progress))
                    val rRight = sqrt(rMin * rMin + (rMax * rMax - rMin * rMin) * progress)

                    val baseSpeed = 120f
                    val leftOmega = (baseSpeed * (rMin / rLeft))
                    val rightOmega = (baseSpeed * (rMin / rRight))
                    val rollerOmega = baseSpeed * 2.8f // 小型ローラーは高速回転

                    leftReelAngle = (leftReelAngle + leftOmega * dt) % 360f
                    rightReelAngle = (rightReelAngle + rightOmega * dt) % 360f
                    rollerAngle = (rollerAngle + rollerOmega * dt) % 360f
                }
                lastFrameTime = frameTimeNanos
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroDarkBg)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. カセット本体 (1.55 : 1 比率) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F1218))
                .border(2.dp, Color(0xFF2C3345), RoundedCornerShape(10.dp))
                .padding(6.dp)
        ) {
            // カセットスケルトン & メカニカル Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSkeletonShell(size)
                drawMechanicalDeck(size, progress, leftReelAngle, rightReelAngle, rollerAngle)
                drawWindowReflections(size)
            }

            // 上部レトロラベル
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(RetroFocusAmber)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "A",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TYPE-II [CrO2] 70μs EQ",
                        color = RetroFocusAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NR [B/C]",
                        color = RetroTextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "RUN ▶" else "STOP ❚❚",
                        color = if (isPlaying) Color(0xFF00E676) else RetroTextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 2. メタデータ & アートワーク表示領域 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(RetroSurface)
                .border(1.dp, RetroBorder, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アートワーク
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RetroPanelBg)
                    .border(1.dp, PSPAccentBlue.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
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
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 曲名 & アーティスト
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track?.title ?: "No Track Loaded",
                    color = RetroTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = track?.artist ?: "--",
                    color = RetroTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = track?.album ?: "--",
                    color = RetroTextDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // タイムカウンター
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
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * クリアスケルトンシェルの内部リブ・構造線・ネジの描画
 */
private fun DrawScope.drawSkeletonShell(size: Size) {
    val w = size.width
    val h = size.height

    // 内部のスケルトン補強リブ（Xトラス構造と外周リブ）
    val ribColor = Color(0xFF1B202D)
    drawLine(ribColor, Offset(w * 0.1f, h * 0.15f), Offset(w * 0.9f, h * 0.85f), strokeWidth = 1.2f)
    drawLine(ribColor, Offset(w * 0.9f, h * 0.15f), Offset(w * 0.1f, h * 0.85f), strokeWidth = 1.2f)
    drawLine(ribColor, Offset(w * 0.05f, h * 0.5f), Offset(w * 0.95f, h * 0.5f), strokeWidth = 1f)

    // 上下シェルの合わせ目（モールドパーティングライン）
    drawLine(Color(0xFF262C3D), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1.5f)

    // 5箇所の金属ネジ（シルバー光沢＋十字/マイナス溝）
    val screwColor = Color(0xFFA6AFBD)
    val screwMargin = 10f
    val screws = listOf(
        Offset(screwMargin, screwMargin),
        Offset(w - screwMargin, screwMargin),
        Offset(screwMargin, h - screwMargin),
        Offset(w - screwMargin, h - screwMargin),
        Offset(w / 2f, screwMargin * 0.9f)
    )
    screws.forEach { pt ->
        drawCircle(Color(0xFF10131A), 4.5f, pt)
        drawCircle(screwColor, 3.2f, pt)
        drawCircle(Color(0xFFD4D9E2), 2f, pt)
        // プラスネジ溝
        drawLine(Color(0xFF1E222D), Offset(pt.x - 2f, pt.y), Offset(pt.x + 2f, pt.y), 1.2f)
        drawLine(Color(0xFF1E222D), Offset(pt.x, pt.y - 2f), Offset(pt.x, pt.y + 2f), 1.2f)
    }

    // 下部台形磁気ヘッドエリアの外形
    val trapTop = h * 0.70f
    val trapBottom = h
    val trapLeftTop = w * 0.20f
    val trapRightTop = w * 0.80f
    val trapLeftBottom = w * 0.12f
    val trapRightBottom = w * 0.88f

    val trapPath = Path().apply {
        moveTo(trapLeftTop, trapTop)
        lineTo(trapRightTop, trapTop)
        lineTo(trapRightBottom, trapBottom)
        lineTo(trapLeftBottom, trapBottom)
        close()
    }
    drawPath(trapPath, color = Color(0xFF0C0E14))
    drawPath(trapPath, color = Color(0xFF333B4F), style = Stroke(width = 2f))
}

/**
 * メカニカルデッキ（テープパス、ガイドローラー、磁気ヘッド、銅スプリング、リール）の精密描画
 */
private fun DrawScope.drawMechanicalDeck(
    size: Size,
    progress: Float,
    leftAngle: Float,
    rightAngle: Float,
    rollerAngle: Float
) {
    val w = size.width
    val h = size.height

    // 中央の透明窓
    val winW = w * 0.68f
    val winH = h * 0.40f
    val winLeft = (w - winW) / 2f
    val winTop = h * 0.24f

    // 窓背景（ダークスモークアクリル）
    drawRoundRect(
        color = Color(0xFF090B10),
        topLeft = Offset(winLeft, winTop),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF3D465C),
        topLeft = Offset(winLeft, winTop),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.8f)
    )

    // リール中心座標
    val leftReelCenter = Offset(w * 0.31f, winTop + winH / 2f)
    val rightReelCenter = Offset(w * 0.69f, winTop + winH / 2f)

    // ガイドローラー座標（左右下部）
    val leftRollerCenter = Offset(w * 0.16f, h * 0.82f)
    val rightRollerCenter = Offset(w * 0.84f, h * 0.82f)
    val rollerRadius = w * 0.035f

    // リール半径
    val hubRadius = winH * 0.24f
    val maxTapeRadius = winH * 0.46f
    val leftTapeRadius = sqrt(hubRadius * hubRadius + (maxTapeRadius * maxTapeRadius - hubRadius * hubRadius) * (1f - progress))
    val rightTapeRadius = sqrt(hubRadius * hubRadius + (maxTapeRadius * maxTapeRadius - hubRadius * hubRadius) * progress)

    // 1. テープ走行ライン（左ローラー -> 中央ヘッド -> 右ローラー -> 各リール）
    val tapeColor = Color(0xFF35251D) // 磁気テープ色 (酸化鉄ブラウン)
    val tapeGuideY = h * 0.84f

    // 下部水平テープパス
    drawLine(
        color = tapeColor,
        start = Offset(leftRollerCenter.x, tapeGuideY),
        end = Offset(rightRollerCenter.x, tapeGuideY),
        strokeWidth = 4.5f
    )
    // 左リールから左ローラーへの斜め引き込み線
    drawLine(
        color = tapeColor,
        start = Offset(leftReelCenter.x - leftTapeRadius * 0.9f, leftReelCenter.y),
        end = Offset(leftRollerCenter.x, leftRollerCenter.y),
        strokeWidth = 3.5f
    )
    // 右リールから右ローラーへの斜め引き込み線
    drawLine(
        color = tapeColor,
        start = Offset(rightReelCenter.x + rightTapeRadius * 0.9f, rightReelCenter.y),
        end = Offset(rightRollerCenter.x, rightRollerCenter.y),
        strokeWidth = 3.5f
    )

    // 2. 磁気ヘッド & 銅製リーフスプリング & フェルトパッド描画
    val headCenterX = w / 2f
    val headTop = h * 0.74f

    // パーマロイ磁気シールドプレート
    val shieldW = w * 0.18f
    val shieldH = h * 0.10f
    drawRoundRect(
        color = Color(0xFF2E3342),
        topLeft = Offset(headCenterX - shieldW / 2f, headTop),
        size = Size(shieldW, shieldH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF535E79),
        topLeft = Offset(headCenterX - shieldW / 2f, headTop),
        size = Size(shieldW, shieldH),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(1.2f)
    )

    // 銅製リーフスプリング（ブラスゴールド色）
    val springW = shieldW * 0.75f
    val springY = headTop + shieldH * 0.5f
    drawLine(
        color = Color(0xFFD49A3D),
        start = Offset(headCenterX - springW / 2f, springY),
        end = Offset(headCenterX + springW / 2f, springY),
        strokeWidth = 2.5f
    )

    // フェルト製プレッシャーパッド（中央の四角いパッド）
    val padW = 10f
    val padH = 6f
    drawRect(
        color = Color(0xFF8C7D73),
        topLeft = Offset(headCenterX - padW / 2f, springY - padH / 2f),
        size = Size(padW, padH)
    )

    // 左右キャプスタン軸穴 (円形ホール)
    drawCircle(Color(0xFF0B0D12), 4.5f, Offset(headCenterX - shieldW * 0.7f, headTop + shieldH * 0.5f))
    drawCircle(Color(0xFF384055), 4.5f, Offset(headCenterX - shieldW * 0.7f, headTop + shieldH * 0.5f), style = Stroke(1f))
    drawCircle(Color(0xFF0B0D12), 4.5f, Offset(headCenterX + shieldW * 0.7f, headTop + shieldH * 0.5f))
    drawCircle(Color(0xFF384055), 4.5f, Offset(headCenterX + shieldW * 0.7f, headTop + shieldH * 0.5f), style = Stroke(1f))

    // 3. 左右の回転ガイドローラー描画
    drawGuideRoller(leftRollerCenter, rollerRadius, rollerAngle)
    drawGuideRoller(rightRollerCenter, rollerRadius, -rollerAngle)

    // 4. テープ巻き（リール巻線）の描画（ヘアラインテクスチャ同心円）
    drawTapePack(leftReelCenter, hubRadius, leftTapeRadius)
    drawTapePack(rightReelCenter, hubRadius, rightTapeRadius)

    // 5. リールハブ（白いプラスチック歯車 & 赤いクランプピン）
    drawClassicReelHub(leftReelCenter, hubRadius, leftAngle)
    drawClassicReelHub(rightReelCenter, hubRadius, rightAngle)

    // 中央テープ目盛り (0 .. 100 ゲージ)
    val meterX = w / 2f
    for (i in -4..4) {
        val y = leftReelCenter.y + i * 5f
        val len = if (i == 0) 16f else if (i % 2 == 0) 10f else 6f
        val color = if (i == 0) RetroFocusAmber else Color(0xFF4C556B)
        drawLine(color, Offset(meterX - len / 2f, y), Offset(meterX + len / 2f, y), 1.2f)
    }
}

/**
 * 磁気テープ巻きの質感（光沢リングと同心円）
 */
private fun DrawScope.drawTapePack(center: Offset, minR: Float, currentR: Float) {
    if (currentR <= minR) return

    val tapeBaseColor = Color(0xFF2C1E17)
    val tapeHighlightColor = Color(0xFF422E23)

    // ベース巻線
    drawCircle(tapeBaseColor, currentR, center)

    // 同心円のヘアラインハイライト（巻かれたテープの光沢）
    val step = (currentR - minR) / 4f
    for (j in 1..3) {
        val r = minR + step * j
        drawCircle(tapeHighlightColor, r, center, style = Stroke(1.2f))
    }
    // 外周エッジライン
    drawCircle(Color(0xFF50382B), currentR, center, style = Stroke(1.5f))
}

/**
 * 3本爪スプロケット歯車 ＆ 赤いテープクランプピンを備えたハブ
 */
private fun DrawScope.drawClassicReelHub(center: Offset, radius: Float, angleDegrees: Float) {
    rotate(angleDegrees, pivot = center) {
        // ハブ外周リング (オフホワイト樹脂)
        drawCircle(Color(0xFFE5E9F0), radius, center)
        drawCircle(Color(0xFFB8C0D0), radius, center, style = Stroke(1.5f))

        // テープ固定の赤いクランプピン (本物のカセットにある赤/青ピン)
        val pinRad = 40f * (PI / 180f).toFloat()
        val pinDist = radius * 0.72f
        val pinOffset = Offset(center.x + cos(pinRad) * pinDist, center.y + sin(pinRad) * pinDist)
        drawCircle(Color(0xFFE53935), radius * 0.16f, pinOffset)

        // ドライブ軸穴 (六角形または円形スター)
        drawCircle(Color(0xFF0F1218), radius * 0.50f, center)

        // 3つの大きなスプロケット爪
        val teethCount = 3
        for (i in 0 until teethCount) {
            val rad = (i * 120f) * (PI / 180f).toFloat()
            val innerX = center.x + cos(rad) * (radius * 0.35f)
            val innerY = center.y + sin(rad) * (radius * 0.35f)
            val outerX = center.x + cos(rad) * (radius * 0.52f)
            val outerY = center.y + sin(rad) * (radius * 0.52f)

            drawLine(
                color = Color(0xFFECEFF4),
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = 4f
            )
        }
    }
}

/**
 * 回転する白いガイドローラー
 */
private fun DrawScope.drawGuideRoller(center: Offset, radius: Float, angle: Float) {
    // ローラー本体 (白樹脂)
    drawCircle(Color(0xFFDDE2EC), radius, center)
    drawCircle(Color(0xFF636D82), radius, center, style = Stroke(1.2f))

    // 回転インジケータ (十字スリット)
    rotate(angle, pivot = center) {
        drawLine(
            color = Color(0xFF8893A8),
            start = Offset(center.x - radius * 0.7f, center.y),
            end = Offset(center.x + radius * 0.7f, center.y),
            strokeWidth = 1.2f
        )
    }

    // 中心金属ピン
    drawCircle(Color(0xFF232733), radius * 0.35f, center)
    drawCircle(Color(0xFFAEB7C7), radius * 0.18f, center)
}

/**
 * 透明アクリル窓の斜め反射光（Glass Glare Reflection）
 */
private fun DrawScope.drawWindowReflections(size: Size) {
    val w = size.width
    val h = size.height

    val winW = w * 0.68f
    val winH = h * 0.40f
    val winLeft = (w - winW) / 2f
    val winTop = h * 0.24f

    // 45度のシャープな斜めグレア光沢（左上から右下）
    val reflectionBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.00f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.00f)
        ),
        start = Offset(winLeft + winW * 0.1f, winTop),
        end = Offset(winLeft + winW * 0.6f, winTop + winH)
    )

    drawRoundRect(
        brush = reflectionBrush,
        topLeft = Offset(winLeft, winTop),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(6f, 6f)
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
