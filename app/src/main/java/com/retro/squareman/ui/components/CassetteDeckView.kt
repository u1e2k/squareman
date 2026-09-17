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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.sqrt

/**
 * 80s ヴィンテージ・オーディオ カセットデッキ UI
 * (本物の1980年代カセットテープ・スプライトレイヤー＆機械式カウンター搭載)
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

    // リール回転物理計算 (テープ残量に反比例したリアルな角速度)
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

    // レトロウォークマン風カラーパレット
    val deckBg = Color(0xFF14161B)
    val deckPocketBg = Color(0xFF0D0F13)
    val deckBorder = Color(0xFF2E3340)
    val amberAccent = Color(0xFFFF9F1C)
    val textRetroBeige = Color(0xFFEDE8DD)
    val textRetroDim = Color(0xFF8A909E)
    val ledGreen = if (isPlaying) Color(0xFF38EF7D) else Color(0xFF164724)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(deckBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. デッキ上部: レトロオーディオ・インジケーター & 3桁アナログカウンター ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側: モデル銘板 & PLAY LEDランプ
            Row(verticalAlignment = Alignment.CenterVertically) {
                // PLAY LEDランプ
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .shadow(elevation = if (isPlaying) 6.dp else 0.dp, shape = CircleShape)
                        .background(ledGreen, CircleShape)
                        .border(1.5.dp, Color(0xFF2A303C), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PLAY",
                    color = if (isPlaying) textRetroBeige else textRetroDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(14.dp))

                // ステレオバッジ
                Text(
                    text = "STEREO CASSETTE DECK",
                    color = textRetroDim,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 右側: 3桁機械式テープカウンター [ 0 | 4 | 2 ]
            val counterVal = ((currentPositionMs / 1000) % 1000).toInt()
            val digit1 = (counterVal / 100) % 10
            val digit2 = (counterVal / 10) % 10
            val digit3 = counterVal % 10

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF0A0C0E))
                    .border(1.dp, Color(0xFF383F50), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "TAPE",
                    color = amberAccent,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E222A))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$digit1 $digit2 $digit3",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 2. 中央: カセット挿入ポケット (カセットウェル) ＆ 本物のヴィンテージカセット ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(1.53f)
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(deckPocketBg)
                .border(2.dp, deckBorder, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            VintageCassetteLayers(
                progress = progress,
                leftReelAngle = leftReelAngle,
                rightReelAngle = rightReelAngle,
                trackTitle = track?.title ?: "PSPMAN RETRO",
                artistName = track?.artist ?: "1980s VINTAGE"
            )
        }

        // --- 3. デッキ下部: アナログ風プログレススケール & タイムコード ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // アナログチューナー風プログレスバー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0E1015))
                    .border(1.dp, Color(0xFF2A2F3D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 背景目盛り
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = size.width / 20f
                    for (i in 0..20) {
                        val x = i * step
                        val h = if (i % 5 == 0) size.height * 0.7f else size.height * 0.4f
                        val y = (size.height - h) / 2f
                        drawLine(
                            color = Color(0xFF282E3D),
                            start = Offset(x, y),
                            end = Offset(x, y + h),
                            strokeWidth = 1f
                        )
                    }
                }

                // 進行バー
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(amberAccent.copy(alpha = 0.6f), amberAccent)
                            )
                        )
                )

                // アナログ指針 (オレンジのインジケーターピン)
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val indicatorX = maxWidth * progress
                    Box(
                        modifier = Modifier
                            .offset(x = (indicatorX - 1.5.dp).coerceAtLeast(0.dp))
                            .width(3.dp)
                            .height(14.dp)
                            .background(amberAccent, RoundedCornerShape(1.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // タイムコード ＆ フォーマット表示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TYPE I [NORMAL BIAS]",
                        color = textRetroDim,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DOLBY B NR",
                        color = amberAccent.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = amberAccent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / " + formatTime(durationMs),
                        color = textRetroDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * 本物の80sヴィンテージカセットテープ 多層スプライトレンダリング
 */
@Composable
private fun VintageCassetteLayers(
    progress: Float,
    leftReelAngle: Float,
    rightReelAngle: Float,
    trackTitle: String,
    artistName: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // 基準画像サイズ 1019 x 665
        // 左リール中心: x = 297/1019 = 0.2915, y = 337/665 = 0.5068
        // 右リール中心: x = 721/1019 = 0.7075, y = 337/665 = 0.5068
        val leftReelCenterX = totalWidth * 0.2915f
        val rightReelCenterX = totalWidth * 0.7075f
        val reelCenterY = totalHeight * 0.5068f
        val reelSize = totalWidth * 0.18f

        // --- LAYER 1 (最背面): カセット背面プレート (cassette_vintage_back.png) ---
        Image(
            painter = painterResource(id = R.drawable.cassette_vintage_back),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // --- LAYER 2: 窓の奥のリアルな磁気テープ巻き取り (進捗率 0%〜100% で動的移行) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawVintageTapeWinding(
                progress = progress,
                leftCenter = Offset(size.width * 0.2915f, size.height * 0.5068f),
                rightCenter = Offset(size.width * 0.7075f, size.height * 0.5068f),
                hubR = size.width * 0.088f,
                maxR = size.width * 0.178f
            )
        }

        // --- LAYER 3: 独立回転する左右のヴィンテージスプロケットハブ (cassette_vintage_reel.png) ---
        // 左リール
        Image(
            painter = painterResource(id = R.drawable.cassette_vintage_reel),
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
            painter = painterResource(id = R.drawable.cassette_vintage_reel),
            contentDescription = "Right Reel",
            modifier = Modifier
                .size(reelSize)
                .offset(
                    x = rightReelCenterX - reelSize / 2f,
                    y = reelCenterY - reelSize / 2f
                )
                .rotate(rightReelAngle)
        )

        // --- LAYER 4: カセット前面シェル (cassette_vintage_front.png) ---
        Image(
            painter = painterResource(id = R.drawable.cassette_vintage_front),
            contentDescription = "Vintage Cassette Front",
            modifier = Modifier.fillMaxSize()
        )

        // --- LAYER 5: 手書き/テプラ風インデックスラベル文字 (曲名・アーティスト名) ---
        // ラベル枠: X = 0.426 * width, Y = 0.125 * height, 幅 0.48 * width, 高 0.168 * height
        val labelOffsetX = totalWidth * 0.44f
        val labelOffsetY = totalHeight * 0.13f
        val labelWidth = totalWidth * 0.46f

        Column(
            modifier = Modifier
                .offset(x = labelOffsetX, y = labelOffsetY)
                .width(labelWidth)
                .padding(start = 28.dp, top = 2.dp)
        ) {
            // 曲名 (TITLE行)
            Text(
                text = trackTitle.uppercase(),
                color = Color(0xFF1E2330), // ヴィンテージ濃紺インク
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // アーティスト名 (ARTIST行)
            Text(
                text = artistName.uppercase(),
                color = Color(0xFF353C4D),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- LAYER 6: アクリル窓の微かな反射ハイライト (Glass Glare) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val winW = size.width * 0.212f
            val winH = size.height * 0.230f
            val winX = size.width * 0.3935f
            val winY = size.height * 0.370f

            val glareBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.00f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.02f),
                    Color.White.copy(alpha = 0.00f)
                ),
                start = Offset(winX + winW * 0.1f, winY),
                end = Offset(winX + winW * 0.8f, winY + winH)
            )

            drawRoundRect(
                brush = glareBrush,
                topLeft = Offset(winX, winY),
                size = Size(winW, winH),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

/**
 * リアルな酸化鉄磁気テープの動的巻き取り描画
 */
private fun DrawScope.drawVintageTapeWinding(
    progress: Float,
    leftCenter: Offset,
    rightCenter: Offset,
    hubR: Float,
    maxR: Float
) {
    val rLeft = sqrt(hubR * hubR + (maxR * maxR - hubR * hubR) * (1f - progress))
    val rRight = sqrt(hubR * hubR + (maxR * maxR - hubR * hubR) * progress)

    val tapeColor = Color(0xFF281C15)       // リアルな酸化鉄ダークブラウン
    val tapeRimColor = Color(0xFF453023)    // 外周の光沢リム

    // 左テープ巻き
    drawCircle(tapeColor, rLeft, leftCenter)
    drawCircle(tapeRimColor, rLeft, leftCenter, style = Stroke(1.5f))

    // 右テープ巻き
    drawCircle(tapeColor, rRight, rightCenter)
    drawCircle(tapeRimColor, rRight, rightCenter, style = Stroke(1.5f))

    // 下部テープ走行ライン
    val tapeY = leftCenter.y + maxR * 0.96f
    drawLine(
        color = tapeColor,
        start = Offset(leftCenter.x - rLeft * 0.65f, tapeY),
        end = Offset(rightCenter.x + rRight * 0.65f, tapeY),
        strokeWidth = 2.5f
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
