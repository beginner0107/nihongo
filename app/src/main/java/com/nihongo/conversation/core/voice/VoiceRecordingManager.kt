package com.nihongo.conversation.core.voice

import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class RecordingState {
    object Idle : RecordingState()
    object Preparing : RecordingState()
    data class Recording(val durationMs: Long) : RecordingState()
    object Paused : RecordingState()
    data class Error(val message: String) : RecordingState()
    data class Completed(
        val filePath: String,
        val durationMs: Long,
        val fileSizeBytes: Long,
        val recordedAt: Long,
        val language: String
    ) : RecordingState()
}

data class RecordingResult(
    val file: File,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val recordedAt: Long,
    val language: String
)

@Singleton
class VoiceRecordingManager @Inject constructor(
    private val fileManager: VoiceFileManager
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime: Long = 0
    private var tickerJob: Job? = null
    private var currentLanguage: String = "ja-JP"

    fun startRecording(conversationId: Long, language: String): StateFlow<RecordingState> {
        stopInternal(release = true)
        currentLanguage = language
        _state.value = RecordingState.Preparing
        val ts = System.currentTimeMillis()
        val outFile = fileManager.createTempFile(conversationId, ts)
        currentFile = outFile

        try {
            val r = MediaRecorder()
            recorder = r
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100)
            r.setAudioChannels(1)
            r.setAudioEncodingBitRate(96000)
            r.setOutputFile(outFile.absolutePath)
            r.prepare()
            r.start()

            startTime = ts
            _state.value = RecordingState.Recording(0)
            tickerJob?.cancel()
            tickerJob = scope.launch {
                while (true) {
                    val dur = System.currentTimeMillis() - startTime
                    _state.value = RecordingState.Recording(dur)
                    if (dur >= 60_000) {
                        stopRecording()
                        break
                    }
                    kotlinx.coroutines.delay(200)
                }
            }
        } catch (e: Exception) {
            _state.value = RecordingState.Error("録音エラー: ${e.message}")
            stopInternal(release = true)
        }

        return state
    }

    fun stopRecording(): RecordingResult {
        tickerJob?.cancel()
        val r = recorder
        val f = currentFile
        val startedAt = startTime
        var duration = 0L
        try {
            if (r != null) {
                r.stop()
                duration = System.currentTimeMillis() - startedAt
            }
        } catch (_: Exception) {
        } finally {
            stopInternal(release = true)
        }
        val fileSize = f?.length() ?: 0L
        val result = RecordingResult(
            file = f ?: File(""),
            durationMs = duration,
            fileSizeBytes = fileSize,
            recordedAt = startedAt,
            language = currentLanguage
        )
        _state.value = RecordingState.Completed(
            filePath = result.file.absolutePath,
            durationMs = result.durationMs,
            fileSizeBytes = result.fileSizeBytes,
            recordedAt = result.recordedAt,
            language = result.language
        )
        return result
    }

    fun cancelRecording() {
        tickerJob?.cancel()
        currentFile?.delete()
        stopInternal(release = true)
        _state.value = RecordingState.Idle
    }

    fun isRecording(): Boolean = recorder != null

    fun getRecordingDuration(): Long = if (startTime > 0) System.currentTimeMillis() - startTime else 0

    private fun stopInternal(release: Boolean) {
        try {
            recorder?.reset()
            if (release) {
                recorder?.release()
            }
        } catch (_: Exception) {
        }
        recorder = null
        startTime = 0
    }
}
