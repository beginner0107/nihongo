package com.nihongo.conversation.core.voice

import android.media.MediaPlayer
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class PlaybackState {
    object Idle : PlaybackState()
    object Loading : PlaybackState()
    data class Playing(val position: Long, val duration: Long, val speed: Float = 1.0f) : PlaybackState()
    data class Paused(val position: Long, val duration: Long) : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

@Singleton
class VoicePlaybackManager @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state

    private var mediaPlayer: MediaPlayer? = null
    private var speed: Float = 1.0f

    fun playFile(path: String) {
        stop()
        _state.value = PlaybackState.Loading
        try {
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setDataSource(path)
            mp.setOnPreparedListener {
                applySpeed(mp)
                mp.start()
                tick()
            }
            mp.setOnCompletionListener {
                _state.value = PlaybackState.Completed
                stop()
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            _state.value = PlaybackState.Error("再生エラー: ${e.message}")
            stop()
        }
    }

    private fun tick() {
        val mp = mediaPlayer ?: return
        scope.launch {
            while (mp.isPlaying) {
                _state.value = PlaybackState.Playing(mp.currentPosition.toLong(), mp.duration.toLong(), speed)
                kotlinx.coroutines.delay(200)
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _state.value = PlaybackState.Paused(it.currentPosition.toLong(), it.duration.toLong())
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            tick()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = PlaybackState.Idle
    }

    fun setSpeed(value: Float) {
        speed = value.coerceIn(0.5f, 1.5f)
        mediaPlayer?.let { applySpeed(it) }
    }

    private fun applySpeed(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams
                params.speed = speed
                mp.playbackParams = params
            } catch (_: Exception) {}
        }
    }
}

