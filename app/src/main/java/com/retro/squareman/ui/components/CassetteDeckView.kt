package com.retro.squareman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.squareman.R
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
import kotlin.math.sqrt

/**
 * 透過PNG素材を重ね合わせたスプライトレイヤー構造のカセットデッキ
 */
@Composable
fun CassetteDeckView(
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

    // リール回転物理計算
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
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. カセット本体 (スプライトレイヤー構造) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            SpriteCassetteLayers(
                progress = progress,
                leftReelAngle = leftReelAngle,
                rightReelAngle = rightReelAngle,
                trackTitle = track?.title ?: "PSPMAN",
                artistName = track?.artist ?: "OBSOLETESONY"
            )
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
                    Image(
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

            // 曲情報テキスト
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "No Track Selected",
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

            // タイムコード
            Column(horizontalAlignment = Alignment.End) {
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
 * 透過PNG素材を多層に重ねてリールを回転させるスプライトコア
 */
@Composable
private fun SpriteCassetteLayers(
    progress: Float,
    leftReelAngle: Float,
    rightReelAngle: Float,
    trackTitle: String,
    artistName: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // 基準画像サイズ 800 x 516 に対するスケーリング
        // 窓座標: winX = 136, winY = 200, winW = 528, winH = 227
        // 左リール中心: x = 270 (33.75%), y = 313 (60.65%)
        // 右リール中心: x = 530 (66.25%), y = 313 (60.65%)
        // リール直径: 約 130px (16.25% of width)

        val reelSize = totalWidth * 0.19f
        val reelCenterY = totalHeight * 0.605f
        val leftReelCenterX = totalWidth * 0.335f
        val rightReelCenterX = totalWidth * 0.665f

        // --- LAYER 1 (最背面): 背面プレート (cassette_body_back.png) ---
        Image(
            painter = painterResource(id = R.drawable.cassette_body_back),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // --- LAYER 1.5: 窓内の動的テープ巻き (進捗率に応じた残量変化) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTapeWinding(
                progress = progress,
                leftCenter = Offset(size.width * 0.335f, size.height * 0.605f),
                rightCenter = Offset(size.width * 0.665f, size.height * 0.605f),
                hubR = size.width * 0.085f,
                maxR = size.width * 0.155f
            )
        }

        // --- LAYER 2: 独立回転する左右の透過リールスプライト (cassette_reel.png) ---
        // 左リール
        Image(
            painter = painterResource(id = R.drawable.cassette_reel),
            contentDescription = "Left Reel",
            modifier = Modifier
                .size(reelSize)
                .offset(
                    x = leftReelCenterX - reelSize / 2f,
                    y = reelCenterY - reelSize / 2f
                )
                .rotate(leftReelAngle)
        )

        // 右リール
        Image(
            painter = painterResource(id = R.drawable.cassette_reel),
            contentDescription = "Right Reel",
            modifier = Modifier
                .size(reelSize)
                .offset(
                    x = rightReelCenterX - reelSize / 2f,
                    y = reelCenterY - reelSize / 2f
                )
                .rotate(rightReelAngle)
        )

        // --- LAYER 3: 前面シェル (cassette_body_front.png - 窓部完全透過) ---
        Image(
            painter = painterResource(id = R.drawable.cassette_body_front),
            contentDescription = "Cassette Front",
            modifier = Modifier.fillMaxSize()
        )

        // --- LAYER 4: ラベルテキストオーバーレイ (曲名・アーティスト名) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = totalHeight * 0.05f, start = totalWidth * 0.12f, end = totalWidth * 0.12f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trackTitle.uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = artistName.uppercase(),
                    color = RetroFocusAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- LAYER 5: 窓ガラス表面の斜め反射光 (Glass Glare) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val winW = size.width * 0.66f
            val winH = size.height * 0.44f
            val winX = (size.width - winW) / 2f
            val winY = size.height * (200f / 516f)

            val glareBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.00f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.02f),
                    Color.White.copy(alpha = 0.00f)
                ),
                start = Offset(winX + winW * 0.15f, winY),
                end = Offset(winX + winW * 0.65f, winY + winH)
            )

            drawRoundRect(
                brush = glareBrush,
                topLeft = Offset(winX, winY),
                size = Size(winW, winH),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}

/**
 * 窓の奥で見える磁気テープの動的巻き量（左右リール間移行）
 */
private fun DrawScope.drawTapeWinding(
    progress: Float,
    leftCenter: Offset,
    rightCenter: Offset,
    hubR: Float,
    maxR: Float
) {
    val rLeft = sqrt(hubR * hubR + (maxR * maxR - hubR * hubR) * (1f - progress))
    val rRight = sqrt(hubR * hubR + (maxR * maxR - hubR * hubR) * progress)

    val tapeColor = Color(0xFF2C1E17)
    val tapeRimColor = Color(0xFF4A3428)

    // 左テープ巻き
    drawCircle(tapeColor, rLeft, leftCenter)
    drawCircle(tapeRimColor, rLeft, leftCenter, style = Stroke(2f))

    // 右テープ巻き
    drawCircle(tapeColor, rRight, rightCenter)
    drawCircle(tapeRimColor, rRight, rightCenter, style = Stroke(2f))

    // 下部テープ走行ライン
    val tapeY = leftCenter.y + maxR * 0.95f
    drawLine(
        color = tapeColor,
        start = Offset(leftCenter.x - rLeft * 0.7f, tapeY),
        end = Offset(rightCenter.x + rRight * 0.7f, tapeY),
        strokeWidth = 3f
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
