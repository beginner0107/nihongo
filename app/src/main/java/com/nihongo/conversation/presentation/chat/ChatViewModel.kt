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
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.Hint
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.PronunciationResult
import com.nihongo.conversation.domain.model.PronunciationScorer
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
    val showGrammarSheet: Boolean = false,
    val translations: Map<Long, String> = emptyMap(), // messageId -> Korean translation
    val expandedTranslations: Set<Long> = emptySet(), // messageIds with translation expanded
    val grammarCache: Map<String, GrammarExplanation> = emptyMap(), // text -> cached grammar
    val showEndChatDialog: Boolean = false, // Show confirmation dialog for ending chat
    val showNewChatToast: Boolean = false, // Show toast when new chat starts
    val showPronunciationSheet: Boolean = false, // Show pronunciation practice sheet
    val pronunciationTargetText: String? = null, // Text to practice
    val pronunciationResult: PronunciationResult? = null, // Result of pronunciation attempt
    val isPronunciationRecording: Boolean = false // Whether currently recording pronunciation
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

    private var currentConversationId: Long? = null
    private var currentUserId: Long = 0
    private var currentScenarioId: Long = 0

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
            // Store user and scenario IDs
            currentUserId = userId
            currentScenarioId = scenarioId

            // Load scenario
            repository.getScenario(scenarioId).first()?.let { scenario ->
                _uiState.update { it.copy(scenario = scenario) }

                // Try to get existing conversation (don't create yet - wait for first message)
                val existingConversationId = repository.getExistingConversation(userId, scenarioId)

                if (existingConversationId != null) {
                    // Resume existing conversation
                    currentConversationId = existingConversationId

                    // Load messages
                    repository.getMessages(existingConversationId)
                        .collect { messages ->
                            _uiState.update { it.copy(messages = messages) }
                        }
                } else {
                    // No existing conversation - will be created on first message
                    _uiState.update { it.copy(messages = emptyList()) }
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

            // Create conversation on first message if it doesn't exist
            if (currentConversationId == null) {
                currentConversationId = repository.getOrCreateConversation(currentUserId, currentScenarioId)
            }

            val conversationId = currentConversationId ?: run {
                _uiState.update { it.copy(isLoading = false, error = "会話を作成できませんでした") }
                return@launch
            }

            // Get personalized prompt prefix
            val personalizedPrefix = profileRepository.getPersonalizedPromptPrefix()

            // Get difficulty-specific guidelines
            val user = _uiState.value.user
            val difficultyLevel = DifficultyLevel.fromInt(user?.level ?: 1)
            val difficultyPrompt = difficultyManager.getDifficultyPrompt(difficultyLevel)

            // Combine all prompts
            val enhancedPrompt = scenario.systemPrompt + personalizedPrefix + difficultyPrompt

            repository.sendMessage(
                conversationId = conversationId,
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
                        // Check if in pronunciation practice mode
                        if (_uiState.value.isPronunciationRecording) {
                            checkPronunciation(event.text)
                        } else {
                            _uiState.update { it.copy(inputText = event.text) }
                        }
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
            // Check cache first
            val cached = _uiState.value.grammarCache[sentence]
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        grammarExplanation = cached,
                        showGrammarSheet = true,
                        isLoadingGrammar = false
                    )
                }
                return@launch
            }

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
                        isLoadingGrammar = false,
                        grammarCache = it.grammarCache + (sentence to grammarExplanation)
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

    fun requestTranslation(messageId: Long, japaneseText: String) {
        viewModelScope.launch {
            // Check if already translated
            if (_uiState.value.translations.containsKey(messageId)) return@launch

            try {
                val translation = repository.translateToKorean(japaneseText)
                _uiState.update {
                    it.copy(translations = it.translations + (messageId to translation))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "번역 실패: ${e.message}")
                }
            }
        }
    }

    fun toggleMessageTranslation(messageId: Long) {
        _uiState.update { state ->
            val expanded = state.expandedTranslations
            state.copy(
                expandedTranslations = if (messageId in expanded) {
                    expanded - messageId
                } else {
                    expanded + messageId
                }
            )
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

    fun showEndChatDialog() {
        _uiState.update { it.copy(showEndChatDialog = true) }
    }

    fun dismissEndChatDialog() {
        _uiState.update { it.copy(showEndChatDialog = false) }
    }

    fun confirmEndChat() {
        viewModelScope.launch {
            currentConversationId?.let { conversationId ->
                // Mark conversation as completed
                repository.completeConversation(conversationId)

                // Reset state for new conversation
                currentConversationId = null
                _uiState.update {
                    it.copy(
                        messages = emptyList(),
                        inputText = "",
                        error = null,
                        translations = emptyMap(),
                        expandedTranslations = emptySet(),
                        showEndChatDialog = false
                    )
                }
            }
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            // Complete current conversation if exists
            currentConversationId?.let { conversationId ->
                repository.completeConversation(conversationId)
            }

            // Create new conversation immediately
            currentConversationId = repository.getOrCreateConversation(currentUserId, currentScenarioId)

            // Clear state and show toast
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    inputText = "",
                    error = null,
                    translations = emptyMap(),
                    expandedTranslations = emptySet(),
                    grammarCache = emptyMap(), // Clear grammar cache too
                    showNewChatToast = true
                )
            }
        }
    }

    fun dismissNewChatToast() {
        _uiState.update { it.copy(showNewChatToast = false) }
    }

    // Pronunciation Practice Functions
    fun startPronunciationPractice(text: String) {
        _uiState.update {
            it.copy(
                showPronunciationSheet = true,
                pronunciationTargetText = text,
                pronunciationResult = null,
                isPronunciationRecording = false
            )
        }
    }

    fun startPronunciationRecording() {
        _uiState.update { it.copy(isPronunciationRecording = true) }
        voiceManager.startListening()
    }

    fun stopPronunciationRecording() {
        voiceManager.stopListening()
        _uiState.update { it.copy(isPronunciationRecording = false) }
    }

    fun checkPronunciation(recognizedText: String) {
        val targetText = _uiState.value.pronunciationTargetText ?: return

        val result = PronunciationScorer.calculateScore(
            expected = targetText,
            recognized = recognizedText
        )

        _uiState.update {
            it.copy(
                pronunciationResult = result,
                isPronunciationRecording = false
            )
        }
    }

    fun retryPronunciation() {
        _uiState.update {
            it.copy(
                pronunciationResult = null,
                isPronunciationRecording = false
            )
        }
    }

    fun dismissPronunciationSheet() {
        _uiState.update {
            it.copy(
                showPronunciationSheet = false,
                pronunciationTargetText = null,
                pronunciationResult = null,
                isPronunciationRecording = false
            )
        }
        voiceManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
