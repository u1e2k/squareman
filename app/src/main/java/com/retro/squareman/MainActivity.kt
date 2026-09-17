package com.retro.squareman

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.retro.squareman.ui.AppScreen
import com.retro.squareman.ui.MainViewModel
import com.retro.squareman.ui.PlayerStyle
import com.retro.squareman.ui.components.CassetteDeckView
import com.retro.squareman.ui.components.DigitalPlayerView
import com.retro.squareman.ui.components.LibraryView
import com.retro.squareman.ui.components.RetroIndexCassetteView
import com.retro.squareman.ui.components.SpectrumAnalyzerView
import com.retro.squareman.ui.theme.PSPAccentBlue
import com.retro.squareman.ui.theme.RetroBorder
import com.retro.squareman.ui.theme.RetroDarkBg
import com.retro.squareman.ui.theme.RetroFocusAmber
import com.retro.squareman.ui.theme.RetroPanelBg
import com.retro.squareman.ui.theme.RetroSurface
import com.retro.squareman.ui.theme.RetroTextDim
import com.retro.squareman.ui.theme.RetroTextSecondary
import com.retro.squareman.ui.theme.SquaremanTheme

@UnstableApi
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (storageGranted) {
            viewModel.scanTracks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        setContent {
            SquaremanTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    /**
     * ポータブルゲーム機型端末の物理キーコードを最優先で捕捉
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.onDpadUp()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.onDpadDown()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (event.repeatCount > 0) {
                        viewModel.onRewind()
                    } else {
                        viewModel.onDpadLeft()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (event.repeatCount > 0) {
                        viewModel.onFastForward()
                    } else {
                        viewModel.onDpadRight()
                    }
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_SPACE -> {
                    viewModel.onButtonA()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE -> {
                    if (viewModel.currentScreen.value != AppScreen.LIBRARY) {
                        viewModel.onButtonB()
                        return true
                    }
                }
                KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_X -> {
                    viewModel.onButtonX()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_Y,
                KeyEvent.KEYCODE_Y -> {
                    viewModel.onButtonY()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_PAGE_UP -> {
                    viewModel.onButtonL1()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    viewModel.onButtonR1()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L2 -> {
                    viewModel.onButtonL2()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R2 -> {
                    viewModel.onButtonR2()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@UnstableApi
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playerStyle by viewModel.playerStyle.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val selectedIndex by viewModel.selectedTrackIndex.collectAsState()
    val currentTrack by viewModel.currentPlayingTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { false },
        color = RetroDarkBg
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. トップ・システムヘッダー
            TopStatusBar(
                currentScreen = currentScreen,
                playerStyle = playerStyle,
                onToggleStyle = { viewModel.togglePlayerStyle() }
            )

            // 2. メインコンテンツ領域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentScreen) {
                    AppScreen.LIBRARY -> {
                        LibraryView(
                            tracks = tracks,
                            selectedIndex = selectedIndex,
                            currentPlayingTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackSelected = { index ->
                                val track = tracks.getOrNull(index) ?: return@LibraryView
                                viewModel.playTrack(track)
                            }
                        )
                    }
                    AppScreen.CASSETTE -> {
                        val activeTrack = currentTrack ?: tracks.getOrNull(selectedIndex)

                        // 選択されたスタイルに応じて3種類のUIを描画
                        when (playerStyle) {
                            PlayerStyle.DIGITAL -> {
                                DigitalPlayerView(
                                    track = activeTrack,
                                    isPlaying = isPlaying,
                                    currentPositionMs = currentPosition,
                                    durationMs = duration,
                                    isShuffle = isShuffle,
                                    repeatMode = repeatMode,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSkipNext = { viewModel.playNextTrack() },
                                    onSkipPrevious = { viewModel.playPreviousTrack() },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onToggleRepeat = { viewModel.toggleRepeat() },
                                    onSeekTo = { pos -> viewModel.seekTo(pos) }
                                )
                            }
                            PlayerStyle.RETRO_CASSETTE -> {
                                RetroIndexCassetteView(
                                    track = activeTrack,
                                    isPlaying = isPlaying,
                                    currentPositionMs = currentPosition,
                                    durationMs = duration
                                )
                            }
                            PlayerStyle.SKELETON_CASSETTE -> {
                                CassetteDeckView(
                                    track = activeTrack,
                                    isPlaying = isPlaying,
                                    currentPositionMs = currentPosition,
                                    durationMs = duration
                                )
                            }
                        }
                    }
                    AppScreen.SPECTRUM_FULL -> {
                        SpectrumAnalyzerView(
                            visualizer = viewModel.visualizer,
                            isFullScreen = true
                        )
                    }
                }
            }

            // 3. 物理キーガイド・フッターバー
            KeyGuideFooter(
                currentScreen = currentScreen,
                onToggleStyle = { viewModel.togglePlayerStyle() }
            )
        }
    }
}

@Composable
private fun TopStatusBar(
    currentScreen: AppScreen,
    playerStyle: PlayerStyle,
    onToggleStyle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RetroPanelBg)
            .border(1.dp, RetroBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ロゴ & モデルインジケータ
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PSPMAN",
                color = PSPAccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))

            // スタイル切替バッジ（タップまたはXボタン）
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF252A38))
                    .border(1.dp, Color(0xFF3B4359), RoundedCornerShape(3.dp))
                    .clickable { onToggleStyle() }
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "[X] ${playerStyle.label}",
                    color = RetroFocusAmber,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // L1 / R1 タブ表示
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabBadge("L1", isButton = true)
            TabBadge("LIB", isActive = currentScreen == AppScreen.LIBRARY)
            TabBadge("PLAYER", isActive = currentScreen == AppScreen.CASSETTE)
            TabBadge("EQ", isActive = currentScreen == AppScreen.SPECTRUM_FULL)
            TabBadge("R1", isButton = true)
        }
    }
}

@Composable
private fun TabBadge(text: String, isActive: Boolean = false, isButton: Boolean = false) {
    val bg = when {
        isButton -> Color(0xFF222733)
        isActive -> PSPAccentBlue
        else -> Color.Transparent
    }
    val textColor = when {
        isActive -> Color(0xFF0A0C10)
        isButton -> RetroFocusAmber
        else -> RetroTextDim
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KeyGuideFooter(currentScreen: AppScreen, onToggleStyle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RetroSurface)
            .border(1.dp, RetroBorder)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeyGuideItem(key = "DPAD", desc = if (currentScreen == AppScreen.LIBRARY) "SELECT" else "NAVI/SEEK")
        KeyGuideItem(key = "A", desc = "OK")
        KeyGuideItem(key = "B", desc = "BACK")
        KeyGuideItem(key = "L2/R2", desc = "TRACK")
        KeyGuideItem(key = "L1/R1", desc = "VIEW")
        KeyGuideItem(key = "X", desc = "STYLE", onClick = onToggleStyle)
    }
}

@Composable
private fun KeyGuideItem(key: String, desc: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF2C313F))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = key,
                color = RetroFocusAmber,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = desc,
            color = RetroTextSecondary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
