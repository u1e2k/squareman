package com.retro.squareman.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.retro.squareman.audio.SpectrumVisualizer
import com.retro.squareman.data.MusicScanner
import com.retro.squareman.data.model.Track
import com.retro.squareman.service.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    LIBRARY,
    CASSETTE,
    SPECTRUM_FULL
}

@UnstableApi
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val musicScanner = MusicScanner(context)
    val visualizer = SpectrumVisualizer()

    // 画面状態
    private val _currentScreen = MutableStateFlow(AppScreen.CASSETTE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // 楽曲リスト & 選択状態
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _selectedTrackIndex = MutableStateFlow(0)
    val selectedTrackIndex: StateFlow<Int> = _selectedTrackIndex.asStateFlow()

    private val _currentPlayingTrack = MutableStateFlow<Track?>(null)
    val currentPlayingTrack: StateFlow<Track?> = _currentPlayingTrack.asStateFlow()

    // 再生ステータス
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressPollJob: Job? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        initMediaController()
        scanTracks()
        observePlaybackServiceSession()
    }

    private fun observePlaybackServiceSession() {
        viewModelScope.launch {
            PlaybackService.audioSessionId.collect { sessionId ->
                if (sessionId > 0) {
                    visualizer.start(sessionId)
                } else {
                    visualizer.stop()
                }
            }
        }
    }

    fun scanTracks() {
        viewModelScope.launch {
            val list = musicScanner.scanMusicDirectory()
            _tracks.value = list
            if (list.isNotEmpty() && _currentPlayingTrack.value == null) {
                _selectedTrackIndex.value = 0
            }
        }
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                startProgressPolling()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { r -> r.run() })
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackTimes()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val matched = _tracks.value.find { it.id.toString() == mediaId }
                _currentPlayingTrack.value = matched
                updatePlaybackTimes()
            }
        })
    }

    private fun startProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackTimes()
                delay(200L)
            }
        }
    }

    private fun updatePlaybackTimes() {
        mediaController?.let { controller ->
            _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
            _duration.value = if (controller.duration > 0) controller.duration else 0L
            _isPlaying.value = controller.isPlaying
        }
    }

    // --- 物理キー入力ルーティング ---

    fun onDpadUp() {
        when (_currentScreen.value) {
            AppScreen.LIBRARY -> {
                val current = _selectedTrackIndex.value
                if (current > 0) {
                    _selectedTrackIndex.value = current - 1
                }
            }
            AppScreen.CASSETTE, AppScreen.SPECTRUM_FULL -> {
                // 音量アップ
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
        }
    }

    fun onDpadDown() {
        when (_currentScreen.value) {
            AppScreen.LIBRARY -> {
                val current = _selectedTrackIndex.value
                val max = _tracks.value.size - 1
                if (current < max) {
                    _selectedTrackIndex.value = current + 1
                }
            }
            AppScreen.CASSETTE, AppScreen.SPECTRUM_FULL -> {
                // 音量ダウン
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
        }
    }

    fun onDpadLeft() {
        // 前の曲へスキップ
        playPreviousTrack()
    }

    fun onDpadRight() {
        // 次の曲へスキップ
        playNextTrack()
    }

    fun onFastForward() {
        mediaController?.let { controller ->
            val target = (controller.currentPosition + 5000L).coerceAtMost(controller.duration)
            controller.seekTo(target)
            _currentPosition.value = target
        }
    }

    fun onRewind() {
        mediaController?.let { controller ->
            val target = (controller.currentPosition - 5000L).coerceAtLeast(0L)
            controller.seekTo(target)
            _currentPosition.value = target
        }
    }

    fun onButtonA() {
        when (_currentScreen.value) {
            AppScreen.LIBRARY -> {
                // リスト選択中の曲を再生し、カセット画面へ移行
                val list = _tracks.value
                val index = _selectedTrackIndex.value
                if (index in list.indices) {
                    playTrack(list[index])
                    _currentScreen.value = AppScreen.CASSETTE
                }
            }
            AppScreen.CASSETTE, AppScreen.SPECTRUM_FULL -> {
                togglePlayPause()
            }
        }
    }

    fun onButtonB() {
        when (_currentScreen.value) {
            AppScreen.SPECTRUM_FULL -> _currentScreen.value = AppScreen.CASSETTE
            AppScreen.CASSETTE -> _currentScreen.value = AppScreen.LIBRARY
            AppScreen.LIBRARY -> {
                // 既にライブラリの場合はそのまま
            }
        }
    }

    fun onButtonL1() {
        // 画面を左循環: SPECTRUM_FULL -> CASSETTE -> LIBRARY
        _currentScreen.value = when (_currentScreen.value) {
            AppScreen.SPECTRUM_FULL -> AppScreen.CASSETTE
            AppScreen.CASSETTE -> AppScreen.LIBRARY
            AppScreen.LIBRARY -> AppScreen.SPECTRUM_FULL
        }
    }

    fun onButtonR1() {
        // 画面を右循環: LIBRARY -> CASSETTE -> SPECTRUM_FULL
        _currentScreen.value = when (_currentScreen.value) {
            AppScreen.LIBRARY -> AppScreen.CASSETTE
            AppScreen.CASSETTE -> AppScreen.SPECTRUM_FULL
            AppScreen.SPECTRUM_FULL -> AppScreen.LIBRARY
        }
    }

    fun playTrack(track: Track) {
        val controller = mediaController ?: return
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
        _currentPlayingTrack.value = track
        _selectedTrackIndex.value = _tracks.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.currentMediaItem == null && _tracks.value.isNotEmpty()) {
                val index = _selectedTrackIndex.value.coerceIn(_tracks.value.indices)
                playTrack(_tracks.value[index])
            } else {
                controller.play()
            }
        }
    }

    fun playNextTrack() {
        val list = _tracks.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentPlayingTrack.value?.id }
        val nextIndex = if (currentIndex in list.indices) (currentIndex + 1) % list.size else 0
        playTrack(list[nextIndex])
    }

    fun playPreviousTrack() {
        val list = _tracks.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentPlayingTrack.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playTrack(list[prevIndex])
    }

    override fun onCleared() {
        progressPollJob?.cancel()
        visualizer.stop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
        super.onCleared()
    }
}
