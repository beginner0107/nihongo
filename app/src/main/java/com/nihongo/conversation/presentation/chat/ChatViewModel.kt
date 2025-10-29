package com.nihongo.conversation.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.difficulty.DifficultyLevel
import com.nihongo.conversation.core.difficulty.DifficultyManager
import com.nihongo.conversation.core.util.Result
import com.nihongo.conversation.core.voice.VoiceEvent
import com.nihongo.conversation.core.voice.VoiceManager
import com.nihongo.conversation.core.voice.VoiceState
import com.nihongo.conversation.data.local.SettingsDataStore
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.Hint
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val scenario: Scenario? = null,
    val user: User? = null,
    val autoSpeak: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val hints: List<Hint> = emptyList(),
    val isLoadingHints: Boolean = false,
    val showHintDialog: Boolean = false,
    val grammarExplanation: GrammarExplanation? = null,
    val isLoadingGrammar: Boolean = false,
    val showGrammarSheet: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val voiceManager: VoiceManager,
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val difficultyManager: DifficultyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val voiceState: StateFlow<VoiceState> = voiceManager.state

    private var currentConversationId: Long = 0

    init {
        observeVoiceEvents()
        observeSettings()
        observeUserProfile()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.userSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        autoSpeak = settings.autoSpeak,
                        speechSpeed = settings.speechSpeed
                    )
                }
                voiceManager.setSpeechSpeed(settings.speechSpeed)
            }
        }
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            profileRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun initConversation(userId: Long, scenarioId: Long) {
        viewModelScope.launch {
            // Load scenario
            repository.getScenario(scenarioId).first()?.let { scenario ->
                _uiState.update { it.copy(scenario = scenario) }

                // Create or get conversation
                val conversation = Conversation(
                    userId = userId,
                    scenarioId = scenarioId
                )
                currentConversationId = repository.createConversation(conversation)

                // Load messages
                repository.getMessages(currentConversationId)
                    .collect { messages ->
                        _uiState.update { it.copy(messages = messages) }
                    }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val message = _uiState.value.inputText.trim()
        if (message.isEmpty() || _uiState.value.isLoading) return

        val scenario = _uiState.value.scenario ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

            // Get personalized prompt prefix
            val personalizedPrefix = profileRepository.getPersonalizedPromptPrefix()

            // Get difficulty-specific guidelines
            val user = _uiState.value.user
            val difficultyLevel = DifficultyLevel.fromInt(user?.level ?: 1)
            val difficultyPrompt = difficultyManager.getDifficultyPrompt(difficultyLevel)

            // Combine all prompts
            val enhancedPrompt = scenario.systemPrompt + personalizedPrefix + difficultyPrompt

            repository.sendMessage(
                conversationId = currentConversationId,
                userMessage = message,
                conversationHistory = _uiState.value.messages,
                systemPrompt = enhancedPrompt
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        // Auto-speak AI response
                        if (_uiState.value.autoSpeak) {
                            voiceManager.speak(result.data.content, speed = _uiState.value.speechSpeed)
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeVoiceEvents() {
        viewModelScope.launch {
            voiceManager.events.collect { event ->
                when (event) {
                    is VoiceEvent.RecognitionResult -> {
                        _uiState.update { it.copy(inputText = event.text) }
                    }
                    is VoiceEvent.Error -> {
                        _uiState.update { it.copy(error = event.message) }
                    }
                    is VoiceEvent.SpeakingComplete -> {
                        // Optional: handle speaking completion
                    }
                }
            }
        }
    }

    fun startVoiceRecording() {
        voiceManager.startListening()
    }

    fun stopVoiceRecording() {
        voiceManager.stopListening()
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text, speed = _uiState.value.speechSpeed)
    }

    fun toggleAutoSpeak() {
        viewModelScope.launch {
            settingsDataStore.updateAutoSpeak(!_uiState.value.autoSpeak)
        }
    }

    fun requestHints() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHints = true, showHintDialog = true) }

            try {
                val hints = repository.getHints(
                    conversationHistory = _uiState.value.messages,
                    userLevel = 1 // TODO: Get from user profile
                )
                _uiState.update {
                    it.copy(
                        hints = hints,
                        isLoadingHints = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHints = false,
                        error = "힌트를 가져오는데 실패했습니다"
                    )
                }
            }
        }
    }

    fun dismissHintDialog() {
        _uiState.update { it.copy(showHintDialog = false) }
    }

    fun useHint(hint: Hint) {
        _uiState.update {
            it.copy(
                inputText = hint.japanese,
                showHintDialog = false
            )
        }
    }

    fun requestGrammarExplanation(sentence: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingGrammar = true,
                    showGrammarSheet = true,
                    grammarExplanation = null
                )
            }

            try {
                val user = _uiState.value.user
                val grammarExplanation = repository.explainGrammar(
                    sentence = sentence,
                    conversationHistory = _uiState.value.messages,
                    userLevel = user?.level ?: 1
                )

                _uiState.update {
                    it.copy(
                        grammarExplanation = grammarExplanation,
                        isLoadingGrammar = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingGrammar = false,
                        error = "문법 분석을 가져오는데 실패했습니다"
                    )
                }
            }
        }
    }

    fun dismissGrammarSheet() {
        _uiState.update {
            it.copy(
                showGrammarSheet = false,
                grammarExplanation = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
