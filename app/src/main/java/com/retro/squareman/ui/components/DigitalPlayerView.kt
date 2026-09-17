package com.retro.squareman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.retro.squareman.data.model.Track

/**
 * Walkman / PSPMAN 風デジタル・プレイヤー UI (シャッフル・リピート対応)
 */
@Composable
fun DigitalPlayerView(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: Int,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(currentPositionMs, durationMs) {
        if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val orangeRed = Color(0xFFE83A14)
    val darkBg = Color(0xFF101114)
    val textMuted = Color(0xFF8E95A5)
    val textSub = Color(0xFF5B6170)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. 上部〜中央: 正方形アートワーク領域 ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181B22))
                    .border(1.dp, Color(0xFF282C38), RoundedCornerShape(10.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (track?.artwork != null) {
                    Image(
                        bitmap = track.artwork.asImageBitmap(),
                        contentDescription = "Artwork",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    PspmanDotMatrixArt(
                        color = orangeRed,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // オーディオコーデック表記
            val codecText = if (track?.filePath?.lowercase()?.endsWith(".flac") == true) {
                "FLAC 44.1 kHz / 16 bit"
            } else if (track?.filePath?.lowercase()?.endsWith(".wav") == true) {
                "WAV 44.1 kHz / 16 bit"
            } else {
                "MP3 320 kbps / 44.1 kHz"
            }
            Text(
                text = codecText,
                color = textMuted,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        // --- 2. メタデータ (曲名・アーティスト・アルバム) ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track?.title ?: "PSPMAN",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track?.artist ?: "OBSOLETESONY",
                color = Color(0xFFCFD4DC),
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = track?.album ?: "Digital Music Player",
                color = textSub,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // --- 3. シークバー ＆ タイムコード ---
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = formatTime(durationMs),
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    if (durationMs > 0) {
                        onSeekTo((newProgress * durationMs).toLong())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFE2E6EE),
                    inactiveTrackColor = Color(0xFF2E323E)
                )
            )
        }

        // --- 4. コントロールボタン列 (シャッフル, |<<, 再生[赤丸], >>|, リピート) ---
        Row(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // シャッフル (クリックでON/OFF切り替え)
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) orangeRed else textMuted,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onToggleShuffle() }
            )

            // PREV
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onSkipPrevious() }
            )

            // PLAY / PAUSE (赤丸アクセントリング)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, orangeRed, CircleShape)
                .clickable { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // NEXT
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onSkipNext() }
            )

            // リピート (クリックで OFF -> ALL -> ONE -> OFF 切り替え)
            val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                Icons.Default.RepeatOne
            } else {
                Icons.Default.Repeat
            }
            val repeatColor = if (repeatMode != Player.REPEAT_MODE_OFF) orangeRed else textMuted

            Icon(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = repeatColor,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onToggleRepeat() }
            )
        }
    }
}

@Composable
private fun PspmanDotMatrixArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(8.dp)) {
        val w = size.width
        val h = size.height

        val cols = 7
        val rows = 7
        val stepX = w / (cols - 1)
        val stepY = h / (rows - 1)

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val x = c * stepX
                val y = r * stepY

                val baseRadius = 2.4f + (c * 2.0f)

                val shouldDraw = when {
                    c == 6 && (r in 2..5) -> true
                    c == 5 && (r in 1..6) -> true
                    c < 5 -> true
                    else -> false
                }

                if (shouldDraw) {
                    val finalRadius = if (c >= 5 && r in 2..4) baseRadius * 1.15f else baseRadius
                    drawCircle(color = color, radius = finalRadius, center = Offset(x, y))
                }
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
