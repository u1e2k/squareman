package com.retro.squareman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

/**
 * 物理キー・D-Pad 最適化のトラック一覧ビュー
 */
@Composable
fun LibraryView(
    tracks: List<Track>,
    selectedIndex: Int,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    onTrackSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    // 選択インデックスが変わったら自動でスクロール追従
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in tracks.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroDarkBg)
            .padding(8.dp)
    ) {
        // リストヘッダー情報
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRACK LIST [TOTAL: ${tracks.size}]",
                color = RetroFocusAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (tracks.isNotEmpty()) "${selectedIndex + 1} / ${tracks.size}" else "0 / 0",
                color = RetroTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (tracks.isEmpty()) {
            // トラックが見つからない場合のレトロガイダンス
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RetroPanelBg)
                    .border(1.dp, RetroBorder, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO TRACKS FOUND",
                        color = RetroTextSecondary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please place MP3/FLAC files in\n/storage/emulated/0/Music\nand grant storage permission.",
                        color = RetroTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RetroPanelBg)
                    .border(1.dp, RetroBorder, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(tracks) { index, track ->
                    val isSelected = index == selectedIndex
                    val isCurrent = track.id == currentPlayingTrack?.id

                    TrackItemRow(
                        track = track,
                        index = index + 1,
                        isSelected = isSelected,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        onClick = { onTrackSelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackItemRow(
    track: Track,
    index: Int,
    isSelected: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    // 選択中行のレトロハイライトデザイン（反転色＋アクセント枠線）
    val rowBg = if (isSelected) RetroSurface else Color.Transparent
    val borderColor = if (isSelected) PSPAccentBlue else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(rowBg)
            .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // カーソルまたはトラック番号インジケータ
        Text(
            text = if (isSelected) ">" else String.format("%02d", index),
            color = if (isSelected) PSPAccentBlue else RetroTextDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )

        // ミニサムネイル / アイコン
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(RetroDarkBg),
            contentAlignment = Alignment.Center
        ) {
            if (track.artwork != null) {
                androidx.compose.foundation.Image(
                    bitmap = track.artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = if (isCurrent) (if (isPlaying) "▶" else "❚❚") else "♪",
                    color = if (isCurrent) PSPAccentBlue else RetroTextDim,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // タイトル & アーティスト
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) PSPAccentBlue else if (isSelected) RetroTextPrimary else RetroTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} - ${track.album}",
                color = RetroTextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 再生時間
        Text(
            text = track.formattedDuration,
            color = if (isSelected) RetroTextPrimary else RetroTextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
