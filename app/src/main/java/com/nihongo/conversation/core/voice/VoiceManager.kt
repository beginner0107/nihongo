package com.nihongo.conversation.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class VoiceState {
    data object Idle : VoiceState()
    data object Listening : VoiceState()
    data object Speaking : VoiceState()
    data class Error(val message: String) : VoiceState()
}

sealed class VoiceEvent {
    data class RecognitionResult(val text: String) : VoiceEvent()
    data class SpeakingComplete(val utteranceId: String) : VoiceEvent()
    data class Error(val message: String) : VoiceEvent()
}

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _events = Channel<VoiceEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val pendingSpeechQueue = mutableListOf<PendingSpeech>()
    private var isInitializing = false

    private data class PendingSpeech(val text: String, val utteranceId: String, val speed: Float)

    init {
        initializeTts()
    }

    private fun initializeTts() {
        // Don't start a new initialization if one is already in progress
        if (isInitializing) return

        // If already initialized successfully, don't re-initialize
        if (isTtsInitialized && textToSpeech != null) return

        isInitializing = true

        try {
            // Clean up any previous failed instance
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsInitialized = false

            textToSpeech = TextToSpeech(context) { status ->
                when (status) {
                    TextToSpeech.SUCCESS -> {
                        val tts = textToSpeech
                        if (tts == null) {
                            isInitializing = false
                            _events.trySend(VoiceEvent.Error("TTS初期化エラー"))
                            return@TextToSpeech
                        }

                        val langResult = tts.setLanguage(Locale.JAPANESE)
                        when (langResult) {
                            TextToSpeech.LANG_MISSING_DATA -> {
                                isInitializing = false
                                _events.trySend(VoiceEvent.Error("日本語音声データがありません。デバイス設定でダウンロードしてください。"))
                                return@TextToSpeech
                            }
                            TextToSpeech.LANG_NOT_SUPPORTED -> {
                                isInitializing = false
                                _events.trySend(VoiceEvent.Error("日本語音声がサポートされていません"))
                                return@TextToSpeech
                            }
                        }

                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _state.value = VoiceState.Speaking
                            }

                            override fun onDone(utteranceId: String?) {
                                _state.value = VoiceState.Idle
                                utteranceId?.let {
                                    _events.trySend(VoiceEvent.SpeakingComplete(it))
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                _state.value = VoiceState.Idle
                                _events.trySend(VoiceEvent.Error("音声出力エラー"))
                            }
                        })

                        // Mark as initialized - SUCCESS!
                        isTtsInitialized = true
                        isInitializing = false

                        // Process pending queue
                        synchronized(pendingSpeechQueue) {
                            if (pendingSpeechQueue.isNotEmpty()) {
                                pendingSpeechQueue.forEachIndexed { index, pending ->
                                    tts.setSpeechRate(pending.speed)
                                    val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                                    tts.speak(pending.text, queueMode, null, pending.utteranceId)
                                }
                                pendingSpeechQueue.clear()
                            }
                        }
                    }
                    else -> {
                        isInitializing = false
                        _events.trySend(VoiceEvent.Error("TTS初期化失敗。デバイスの音声設定を確認してください。"))
                    }
                }
            }
        } catch (e: Exception) {
            isInitializing = false
            _events.trySend(VoiceEvent.Error("TTS初期化エラー: ${e.message}"))
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _events.trySend(VoiceEvent.Error("音声認識が利用できません"))
            return
        }

        stopListening() // Stop any existing recognition

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceState.Listening
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _state.value = VoiceState.Idle
                }

                override fun onError(error: Int) {
                    _state.value = VoiceState.Idle
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音声エラー"
                        SpeechRecognizer.ERROR_CLIENT -> "クライアントエラー"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "権限がありません"
                        SpeechRecognizer.ERROR_NETWORK -> "ネットワークエラー"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "タイムアウト"
                        SpeechRecognizer.ERROR_NO_MATCH -> "音声が認識できませんでした"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "音声認識ビジー"
                        SpeechRecognizer.ERROR_SERVER -> "サーバーエラー"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "音声タイムアウト"
                        else -> "不明なエラー"
                    }
                    _events.trySend(VoiceEvent.Error(message))
                }

                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        if (matches.isNotEmpty()) {
                            _events.trySend(VoiceEvent.RecognitionResult(matches[0]))
                        }
                    }
                    _state.value = VoiceState.Idle
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = VoiceState.Idle
    }

    fun speak(text: String, utteranceId: String = "tts_${System.currentTimeMillis()}", speed: Float = 1.0f) {
        if (text.isBlank()) return

        // Remove pronunciation guides in parentheses (e.g., "お席（せき）" -> "お席")
        val cleanText = text.replace(Regex("（[^）]*）|\\([^)]*\\)"), "").trim()
        if (cleanText.isEmpty()) {
            _events.trySend(VoiceEvent.Error("テキストが空です"))
            return
        }

        val tts = textToSpeech
        if (tts == null || !isTtsInitialized) {
            // Queue speech request for later
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.add(PendingSpeech(cleanText, utteranceId, speed))
            }

            // Try to initialize if not already in progress
            if (!isInitializing) {
                initializeTts()
            }
            return
        }

        // Speak immediately if initialized
        try {
            tts.setSpeechRate(speed.coerceIn(0.5f, 2.0f))

            // Android TTS has a character limit (~4000 chars)
            // Split long text into sentences and queue them
            val sentences = splitIntoSentences(cleanText)

            sentences.forEachIndexed { index, sentence ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val sentenceId = "${utteranceId}_$index"
                val result = tts.speak(sentence, queueMode, null, sentenceId)

                if (result == TextToSpeech.ERROR) {
                    _events.trySend(VoiceEvent.Error("音声再生エラー。音量を確認してください。"))
                    return
                }
            }
        } catch (e: Exception) {
            _events.trySend(VoiceEvent.Error("TTSエラー: ${e.message}"))
        }
    }

    /**
     * Split text into sentences for TTS processing
     * Android TTS has a character limit (~4000 chars), so we need to split long texts
     */
    private fun splitIntoSentences(text: String): List<String> {
        val maxChunkSize = 3500
        val result = mutableListOf<String>()

        // First, split by newlines to handle paragraph breaks
        val paragraphs = text.split("\n").filter { it.isNotBlank() }

        for (paragraph in paragraphs) {
            // If paragraph is short enough, add it directly
            if (paragraph.length <= maxChunkSize) {
                result.add(paragraph)
                continue
            }

            // Split long paragraphs by sentence delimiters (。！？)
            val sentencePattern = Regex("[。！？]")
            val sentences = mutableListOf<String>()
            var currentPos = 0

            sentencePattern.findAll(paragraph).forEach { match ->
                val sentence = paragraph.substring(currentPos, match.range.last + 1)
                if (sentence.isNotBlank()) {
                    sentences.add(sentence)
                }
                currentPos = match.range.last + 1
            }

            // Add remaining text if any
            if (currentPos < paragraph.length) {
                val remaining = paragraph.substring(currentPos)
                if (remaining.isNotBlank()) {
                    sentences.add(remaining)
                }
            }

            // If still no sentences found, force split by character limit
            if (sentences.isEmpty()) {
                var remaining = paragraph
                while (remaining.length > maxChunkSize) {
                    result.add(remaining.substring(0, maxChunkSize))
                    remaining = remaining.substring(maxChunkSize)
                }
                if (remaining.isNotEmpty()) {
                    result.add(remaining)
                }
            } else {
                // Add sentences, combining short ones if possible
                var currentChunk = ""
                for (sentence in sentences) {
                    if (sentence.length > maxChunkSize) {
                        // Single sentence too long - force split
                        if (currentChunk.isNotEmpty()) {
                            result.add(currentChunk)
                            currentChunk = ""
                        }
                        var remaining = sentence
                        while (remaining.length > maxChunkSize) {
                            result.add(remaining.substring(0, maxChunkSize))
                            remaining = remaining.substring(maxChunkSize)
                        }
                        if (remaining.isNotEmpty()) {
                            currentChunk = remaining
                        }
                    } else if (currentChunk.length + sentence.length > maxChunkSize) {
                        // Adding this sentence would exceed limit
                        result.add(currentChunk)
                        currentChunk = sentence
                    } else {
                        // Add to current chunk
                        currentChunk += sentence
                    }
                }
                if (currentChunk.isNotEmpty()) {
                    result.add(currentChunk)
                }
            }
        }

        // Return at least one item (original text if all else fails)
        return if (result.isEmpty()) listOf(text) else result
    }

    fun setSpeechSpeed(speed: Float) {
        textToSpeech?.setSpeechRate(speed)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _state.value = VoiceState.Idle
    }

    fun retryTtsInitialization() {
        // Force a fresh initialization attempt
        isTtsInitialized = false
        isInitializing = false
        initializeTts()
    }

    fun release() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsInitialized = false
        isInitializing = false
        synchronized(pendingSpeechQueue) {
            pendingSpeechQueue.clear()
        }
    }
}
