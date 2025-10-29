package com.nihongo.conversation.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.difficulty.DifficultyLevel
import com.nihongo.conversation.core.difficulty.DifficultyManager
import com.nihongo.conversation.core.memory.MemoryManager
import com.nihongo.conversation.core.util.ImmutableList
import com.nihongo.conversation.core.util.ImmutableMap
import com.nihongo.conversation.core.util.ImmutableSet
import com.nihongo.conversation.core.util.Result
import com.nihongo.conversation.core.util.toImmutableList
import com.nihongo.conversation.core.util.toImmutableMap
import com.nihongo.conversation.core.util.toImmutableSet
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Optimized UI state with ImmutableList to prevent unnecessary recompositions
 * Using immutable wrappers ensures Compose treats the state as stable
 */
data class ChatUiState(
    val messages: ImmutableList<Message> = ImmutableList.empty(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val scenario: Scenario? = null,
    val user: User? = null,
    val autoSpeak: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val hints: ImmutableList<Hint> = ImmutableList.empty(),
    val isLoadingHints: Boolean = false,
    val showHintDialog: Boolean = false,
    val grammarExplanation: GrammarExplanation? = null,
    val isLoadingGrammar: Boolean = false,
    val showGrammarSheet: Boolean = false,
    val translations: ImmutableMap<Long, String> = ImmutableMap.empty(), // messageId -> Korean translation
    val expandedTranslations: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds with translation expanded
    val grammarCache: ImmutableMap<String, GrammarExplanation> = ImmutableMap.empty(), // text -> cached grammar
    val showEndChatDialog: Boolean = false, // Show confirmation dialog for ending chat
    val showNewChatToast: Boolean = false, // Show toast when new chat starts
    val showPronunciationSheet: Boolean = false, // Show pronunciation practice sheet
    val pronunciationTargetText: String? = null, // Text to practice
    val pronunciationResult: PronunciationResult? = null, // Result of pronunciation attempt
    val isPronunciationRecording: Boolean = false // Whether currently recording pronunciation
) {
    /**
     * Computed property using derivedStateOf pattern
     * Only recomputes when messages change
     */
    val hasMessages: Boolean get() = messages.isNotEmpty()

    /**
     * Computed property for message count
     */
    val messageCount: Int get() = messages.size
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val voiceManager: VoiceManager,
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val difficultyManager: DifficultyManager,
    private val memoryManager: MemoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val voiceState: StateFlow<VoiceState> = voiceManager.state

    private var currentConversationId: Long? = null
    private var currentUserId: Long = 0
    private var currentScenarioId: Long = 0

    // Job references for proper cancellation in onCleared()
    private var settingsFlowJob: Job? = null
    private var profileFlowJob: Job? = null
    private var voiceEventsJob: Job? = null
    private var messagesFlowJob: Job? = null

    // Memory config based on device capabilities
    private val memoryConfig = memoryManager.getMemoryConfig()

    init {
        observeVoiceEvents()
        observeSettings()
        observeUserProfile()
    }

    private fun observeSettings() {
        settingsFlowJob = viewModelScope.launch {
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
        profileFlowJob = viewModelScope.launch {
            profileRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun initConversation(userId: Long, scenarioId: Long) {
        viewModelScope.launch {
            // Cancel previous message flow if exists
            messagesFlowJob?.cancel()

            // Clear caches when switching scenarios to free memory
            val isScenarioSwitch = currentScenarioId != 0L && currentScenarioId != scenarioId
            if (isScenarioSwitch) {
                _uiState.update {
                    it.copy(
                        grammarCache = ImmutableMap.empty(),
                        translations = ImmutableMap.empty(),
                        expandedTranslations = ImmutableSet.empty(),
                        hints = ImmutableList.empty()
                    )
                }
            }

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

                    // Load messages with memory limit
                    messagesFlowJob = viewModelScope.launch {
                        repository.getMessages(existingConversationId)
                            .collect { messages ->
                                // Limit message history based on device memory
                                val limitedMessages = if (messages.size > memoryConfig.maxMessageHistory) {
                                    messages.takeLast(memoryConfig.maxMessageHistory)
                                } else {
                                    messages
                                }
                                _uiState.update { it.copy(messages = limitedMessages.toImmutableList()) }
                            }
                    }
                } else {
                    // No existing conversation - will be created on first message
                    _uiState.update { it.copy(messages = ImmutableList.empty()) }
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

            // Combine all prompts (optimized by API service)
            val enhancedPrompt = scenario.systemPrompt + personalizedPrefix + difficultyPrompt

            // Use streaming API for instant response feel
            var autoSpeakTriggered = false
            repository.sendMessageStream(
                conversationId = conversationId,
                userMessage = message,
                conversationHistory = _uiState.value.messages.items,
                systemPrompt = enhancedPrompt
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Result.Success -> {
                        // Auto-speak once when first chunk arrives
                        if (!autoSpeakTriggered && _uiState.value.autoSpeak && result.data.content.length > 10) {
                            voiceManager.speak(result.data.content, speed = _uiState.value.speechSpeed)
                            autoSpeakTriggered = true
                        }

                        // Check if this is the final chunk (by checking if loading should stop)
                        // We keep loading state true during streaming
                        _uiState.update { it.copy(isLoading = false) }
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
        voiceEventsJob = viewModelScope.launch {
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
                    conversationHistory = _uiState.value.messages.items,
                    userLevel = 1 // TODO: Get from user profile
                )
                _uiState.update {
                    it.copy(
                        hints = hints.toImmutableList(),
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
                    conversationHistory = _uiState.value.messages.items,
                    userLevel = user?.level ?: 1
                )

                _uiState.update {
                    // Limit grammar cache size based on memory config
                    val currentCache = it.grammarCache.items
                    val newCache = if (currentCache.size >= memoryConfig.maxCacheSize) {
                        // Remove oldest entry (first entry) when cache is full
                        currentCache.entries.drop(1).associate { entry -> entry.key to entry.value } +
                                (sentence to grammarExplanation)
                    } else {
                        currentCache + (sentence to grammarExplanation)
                    }

                    it.copy(
                        grammarExplanation = grammarExplanation,
                        isLoadingGrammar = false,
                        grammarCache = newCache.toImmutableMap()
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
                    it.copy(translations = (it.translations.items + (messageId to translation)).toImmutableMap())
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
            val expanded = state.expandedTranslations.items
            state.copy(
                expandedTranslations = if (messageId in expanded) {
                    (expanded - messageId).toImmutableSet()
                } else {
                    (expanded + messageId).toImmutableSet()
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
                        messages = ImmutableList.empty(),
                        inputText = "",
                        error = null,
                        translations = ImmutableMap.empty(),
                        expandedTranslations = ImmutableSet.empty(),
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
                    messages = ImmutableList.empty(),
                    inputText = "",
                    error = null,
                    translations = ImmutableMap.empty(),
                    expandedTranslations = ImmutableSet.empty(),
                    grammarCache = ImmutableMap.empty(), // Clear grammar cache too
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

        // Cancel all active coroutine jobs to prevent memory leaks
        settingsFlowJob?.cancel()
        profileFlowJob?.cancel()
        voiceEventsJob?.cancel()
        messagesFlowJob?.cancel()

        // Clear all caches to free memory
        _uiState.update {
            it.copy(
                messages = ImmutableList.empty(),
                grammarCache = ImmutableMap.empty(),
                translations = ImmutableMap.empty(),
                expandedTranslations = ImmutableSet.empty(),
                hints = ImmutableList.empty()
            )
        }

        // Release voice manager resources
        voiceManager.release()
    }
}
