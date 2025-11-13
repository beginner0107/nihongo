package com.nihongo.conversation.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.voice.VoiceManager
import com.nihongo.conversation.core.voice.VoicePlaybackManager
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.data.repository.VoiceRecordingRepository
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.VoiceRecording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 2: 대화 복습 모드 ViewModel
 */
@HiltViewModel
class ConversationReviewViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val voiceRecordingRepository: VoiceRecordingRepository,
    private val voicePlaybackManager: VoicePlaybackManager,
    private val voiceManager: VoiceManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConversationReviewUiState())
    val uiState: StateFlow<ConversationReviewUiState> = _uiState.asStateFlow()
    
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private var playbackJob: Job? = null
    
    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                // Load messages
                val messages = conversationRepository.getMessagesByConversation(conversationId)
                
                // Load voice recordings for user messages
                val messagesWithRecordings = messages.map { message ->
                    val recording = if (message.isUser && message.voiceRecordingId != null) {
                        voiceRecordingRepository.getById(message.voiceRecordingId)
                    } else {
                        null
                    }
                    MessageWithRecording(message, recording)
                }
                
                // Calculate statistics
                val voiceCount = messagesWithRecordings.count { it.recording != null }
                val totalDurationMs = messagesWithRecordings
                    .mapNotNull { it.recording?.durationMs }
                    .sum()
                
                _uiState.update {
                    it.copy(
                        messages = messagesWithRecordings,
                        voiceMessageCount = voiceCount,
                        totalDuration = formatDuration(totalDurationMs),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "대화를 불러올 수 없습니다: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun playAll() {
        playSequence(PlaybackMode.ALL)
    }
    
    fun playUserOnly() {
        playSequence(PlaybackMode.USER_ONLY)
    }
    
    fun playAiOnly() {
        playSequence(PlaybackMode.AI_ONLY)
    }
    
    private fun playSequence(mode: PlaybackMode) {
        playbackJob?.cancel()
        _uiState.update { it.copy(playbackMode = mode) }
        
        val messages = when (mode) {
            PlaybackMode.ALL -> _uiState.value.messages
            PlaybackMode.USER_ONLY -> _uiState.value.messages.filter { it.message.isUser }
            PlaybackMode.AI_ONLY -> _uiState.value.messages.filter { !it.message.isUser }
        }
        
        playbackJob = viewModelScope.launch {
            _playbackState.value = PlaybackState.Playing(0, messages.size)
            
            for ((index, item) in messages.withIndex()) {
                // Check if paused
                while (_playbackState.value is PlaybackState.Paused) {
                    delay(100)
                }
                
                // Check if stopped
                if (_playbackState.value is PlaybackState.Idle) {
                    break
                }
                
                _playbackState.value = PlaybackState.Playing(index, messages.size)
                _uiState.update { it.copy(currentPlayingMessageId = item.message.id) }
                
                // Play message
                if (item.message.isUser && item.recording != null) {
                    // Play recorded voice
                    playVoiceRecording(item.recording)
                } else {
                    // Play TTS
                    playTTS(item.message.content)
                }
                
                // Wait for completion + gap
                delay(1000) // 1 second gap between messages
            }
            
            _playbackState.value = PlaybackState.Idle
            _uiState.update { it.copy(currentPlayingMessageId = null) }
        }
    }
    
    fun playMessage(messageId: Long) {
        val item = _uiState.value.messages.find { it.message.id == messageId } ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(currentPlayingMessageId = messageId) }
            
            if (item.message.isUser && item.recording != null) {
                playVoiceRecording(item.recording)
            } else {
                playTTS(item.message.content)
            }
            
            delay(500)
            _uiState.update { it.copy(currentPlayingMessageId = null) }
        }
    }
    
    fun pause() {
        val current = _playbackState.value
        if (current is PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Paused(current.currentIndex, current.totalCount)
            voicePlaybackManager.pause()
            voiceManager.stopSpeaking()
        }
    }
    
    fun resume() {
        val current = _playbackState.value
        if (current is PlaybackState.Paused) {
            _playbackState.value = PlaybackState.Playing(current.currentIndex, current.totalCount)
            voicePlaybackManager.resume()
        }
    }
    
    fun stop() {
        playbackJob?.cancel()
        _playbackState.value = PlaybackState.Idle
        _uiState.update { it.copy(currentPlayingMessageId = null) }
        voicePlaybackManager.stop()
        voiceManager.stopSpeaking()
    }
    
    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
        voicePlaybackManager.setSpeed(speed)
    }
    
    fun toggleSpeedMenu() {
        _uiState.update { it.copy(showSpeedMenu = !it.showSpeedMenu) }
    }
    
    private suspend fun playVoiceRecording(recording: VoiceRecording) {
        voicePlaybackManager.playFile(recording.filePath)
        // Wait for playback to complete
        voicePlaybackManager.state
            .filter { it is com.nihongo.conversation.core.voice.PlaybackState.Completed || it is com.nihongo.conversation.core.voice.PlaybackState.Error }
            .first()
    }
    
    private suspend fun playTTS(text: String) {
        voiceManager.speak(text, speed = _uiState.value.playbackSpeed)
        // Wait for TTS to complete (approximate based on text length)
        val estimatedDuration = (text.length * 100).toLong() // ~100ms per character
        delay(estimatedDuration)
    }
    
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}분 ${seconds}초"
    }
    
    override fun onCleared() {
        super.onCleared()
        stop()
    }
}

// Data classes
data class ConversationReviewUiState(
    val messages: List<MessageWithRecording> = emptyList(),
    val voiceMessageCount: Int = 0,
    val totalDuration: String = "0분 0초",
    val playbackMode: PlaybackMode = PlaybackMode.ALL,
    val playbackSpeed: Float = 1.0f,
    val currentPlayingMessageId: Long? = null,
    val showSpeedMenu: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class MessageWithRecording(
    val message: Message,
    val recording: VoiceRecording?
)

enum class PlaybackMode {
    ALL,
    USER_ONLY,
    AI_ONLY
}

sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Playing(val currentIndex: Int, val totalCount: Int) : PlaybackState()
    data class Paused(val currentIndex: Int, val totalCount: Int) : PlaybackState()
}
