package com.nihongo.conversation.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.difficulty.DifficultyLevel
import com.nihongo.conversation.core.difficulty.DifficultyManager
import com.nihongo.conversation.core.memory.MemoryManager
import com.nihongo.conversation.core.session.UserSessionManager
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
import com.nihongo.conversation.data.repository.GrammarFeedbackRepository
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarFeedback
import com.nihongo.conversation.domain.model.Hint
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.PronunciationResult
import com.nihongo.conversation.domain.model.PronunciationScorer
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.TranscriptEntry
import com.nihongo.conversation.domain.model.User
import com.nihongo.conversation.domain.model.VoiceOnlySession
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
    val grammarError: String? = null, // Error message for grammar analysis
    val grammarRetryCount: Int = 0, // Number of retry attempts
    val currentGrammarSentence: String? = null, // Current sentence being analyzed
    val translations: ImmutableMap<Long, String> = ImmutableMap.empty(), // messageId -> Korean translation
    val expandedTranslations: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds with translation expanded
    val translationErrors: ImmutableMap<Long, String> = ImmutableMap.empty(), // messageId -> error message
    val translationRetryCount: ImmutableMap<Long, Int> = ImmutableMap.empty(), // messageId -> retry count
    val grammarCache: ImmutableMap<String, GrammarExplanation> = ImmutableMap.empty(), // text -> cached grammar
    val showEndChatDialog: Boolean = false, // Show confirmation dialog for ending chat
    val showNewChatToast: Boolean = false, // Show toast when new chat starts
    val showPronunciationSheet: Boolean = false, // Show pronunciation practice sheet
    val pronunciationTargetText: String? = null, // Text to practice
    val pronunciationResult: PronunciationResult? = null, // Result of pronunciation attempt
    val isPronunciationRecording: Boolean = false, // Whether currently recording pronunciation
    val grammarFeedback: ImmutableMap<Long, ImmutableList<GrammarFeedback>> = ImmutableMap.empty(), // messageId -> feedback list
    val isAnalyzingFeedback: Boolean = false, // Whether analyzing current message
    val unacknowledgedFeedbackCount: Int = 0, // Badge count for feedback tab
    val feedbackEnabled: Boolean = true, // Toggle for real-time feedback analysis
    val voiceOnlySession: VoiceOnlySession? = null, // Voice-only mode session state
    val showTranscriptDialog: Boolean = false, // Show post-conversation transcript
    val lastAiComplexityScore: Int = 0, // Last AI message complexity score for adaptive difficulty
    val adaptiveNudge: String = "" // Adaptive difficulty nudge (very short, 8 chars max)
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

    /**
     * Whether in voice-only mode
     */
    val isVoiceOnlyMode: Boolean get() = voiceOnlySession?.isActive == true
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val voiceManager: VoiceManager,
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val difficultyManager: DifficultyManager,
    private val memoryManager: MemoryManager,
    private val userSessionManager: UserSessionManager,
    private val grammarFeedbackRepository: GrammarFeedbackRepository,
    private val mlKitTranslator: com.nihongo.conversation.core.translation.MLKitTranslator
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
    private var memoryConfigJob: Job? = null  // Phase 6A
    private var memoryLevelJob: Job? = null    // Phase 6A

    // Phase 6A: Use reactive memory config (deprecated static config)
    @Suppress("DEPRECATION")
    private val memoryConfig = memoryManager.getMemoryConfig()

    init {
        observeVoiceEvents()
        observeSettings()
        observeUserProfile()
        observeMemoryPressure()  // Phase 6A
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

                // Set up message flow for the newly created conversation
                val newConversationId = currentConversationId
                if (newConversationId != null) {
                    messagesFlowJob?.cancel()
                    messagesFlowJob = viewModelScope.launch {
                        repository.getMessages(newConversationId)
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

                    // Wait a brief moment for the flow to be ready to collect
                    kotlinx.coroutines.delay(50)
                }
            }

            val conversationId = currentConversationId ?: run {
                _uiState.update { it.copy(isLoading = false, error = "会話を作成できませんでした") }
                return@launch
            }

            // Get personalized prompt prefix
            val personalizedPrefix = profileRepository.getPersonalizedPromptPrefix()

            // Get difficulty-specific guidelines (using compact version for token efficiency)
            val user = _uiState.value.user
            val difficultyLevel = DifficultyLevel.fromInt(user?.level ?: 1)
            val difficultyPrompt = difficultyManager.getCompactDifficultyPrompt(difficultyLevel)

            // Add adaptive nudge if last AI response was off-target (Phase 2)
            val lastComplexity = _uiState.value.lastAiComplexityScore
            val adaptiveNudge = if (lastComplexity > 0) {
                difficultyManager.getAdaptiveNudge(lastComplexity, difficultyLevel)
            } else {
                ""
            }

            // Combine all prompts (optimized by API service to ~500 chars)
            // Adaptive nudge is very short (8 chars max), so minimal token impact
            val enhancedPrompt = scenario.systemPrompt + personalizedPrefix + difficultyPrompt + adaptiveNudge

            // Use streaming API for instant response feel
            var userMessageId: Long? = null
            var finalAiMessage: String? = null

            // Set voice state to Thinking when starting AI generation
            voiceManager.setThinking()

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
                        // Store the latest AI message content
                        finalAiMessage = result.data.content

                        // Store user message ID for feedback analysis
                        // User message is typically the second-to-last message
                        val messages = _uiState.value.messages.items
                        if (messages.size >= 2) {
                            val userMessage = messages[messages.size - 2]
                            if (userMessage.isUser && userMessageId == null) {
                                userMessageId = userMessage.id
                            }
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
                        // Reset voice state on error
                        voiceManager.setIdle()
                    }
                }
            }

            // After streaming is complete, analyze complexity and prepare adaptive nudge (Phase 2)
            finalAiMessage?.let { aiMsg ->
                // Calculate complexity of AI response
                val complexity = difficultyManager.analyzeVocabularyComplexity(aiMsg)
                val complexityScore = difficultyManager.getComplexityScore(complexity)

                // Update state with complexity score for next message
                _uiState.update {
                    it.copy(
                        lastAiComplexityScore = complexityScore,
                        adaptiveNudge = difficultyManager.getAdaptiveNudge(complexityScore, difficultyLevel)
                    )
                }

                // Speak the AI message
                if (_uiState.value.autoSpeak && aiMsg.isNotEmpty()) {
                    voiceManager.speak(aiMsg, speed = _uiState.value.speechSpeed)
                }
            }

            // After streaming is complete, analyze the user message for feedback
            userMessageId?.let { messageId ->
                analyzeMessageForFeedback(messageId, message)
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
                // Get user level from session
                val userLevel = userSessionManager.getCurrentUserLevelSync()

                val hints = repository.getHints(
                    conversationHistory = _uiState.value.messages.items,
                    userLevel = userLevel
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

    fun requestGrammarExplanation(sentence: String, retryAttempt: Int = 0) {
        android.util.Log.d("GrammarDebug", "=== requestGrammarExplanation START ===")
        android.util.Log.d("GrammarDebug", "Sentence: '$sentence'")
        android.util.Log.d("GrammarDebug", "Retry attempt: $retryAttempt")

        viewModelScope.launch {
            // Check cache first
            val cached = _uiState.value.grammarCache[sentence]
            if (cached != null) {
                android.util.Log.d("GrammarDebug", "✅ Found in cache, returning cached result")
                _uiState.update {
                    it.copy(
                        grammarExplanation = cached,
                        showGrammarSheet = true,
                        isLoadingGrammar = false,
                        grammarError = null,
                        grammarRetryCount = 0
                    )
                }
                return@launch
            }

            val user = _uiState.value.user
            val userLevel = user?.level ?: 1
            android.util.Log.d("GrammarDebug", "User level: $userLevel")

            // Check if we can analyze locally (simple patterns) to avoid API call
            val canAnalyzeLocally = com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.canAnalyzeLocally(sentence)
            android.util.Log.d("GrammarDebug", "Can analyze locally: $canAnalyzeLocally")

            if (canAnalyzeLocally) {
                android.util.Log.d("GrammarDebug", "📱 Using LOCAL analyzer for simple sentence")
                // Use local analyzer for simple sentences
                val localExplanation = com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.analyzeSentence(
                    sentence = sentence,
                    userLevel = userLevel
                )
                android.util.Log.d("GrammarDebug", "Local analysis completed: ${localExplanation.components.size} components found")

                _uiState.update {
                    // Cache the local result
                    val currentCache = it.grammarCache.items
                    val newCache = if (currentCache.size >= memoryConfig.maxCacheSize) {
                        currentCache.entries.drop(1).associate { entry -> entry.key to entry.value } +
                                (sentence to localExplanation)
                    } else {
                        currentCache + (sentence to localExplanation)
                    }

                    it.copy(
                        grammarExplanation = localExplanation,
                        showGrammarSheet = true,
                        isLoadingGrammar = false,
                        grammarCache = newCache.toImmutableMap(),
                        grammarError = null,
                        grammarRetryCount = 0
                    )
                }
                return@launch
            }

            android.util.Log.d("GrammarDebug", "🌐 Using API for complex sentence")
            // For complex sentences, proceed with API call
            _uiState.update {
                it.copy(
                    isLoadingGrammar = true,
                    showGrammarSheet = true,
                    grammarExplanation = null,
                    grammarError = null,
                    grammarRetryCount = retryAttempt,
                    currentGrammarSentence = sentence
                )
            }

            try {
                android.util.Log.d("GrammarDebug", "Calling repository.explainGrammar()...")
                val grammarExplanation = repository.explainGrammar(
                    sentence = sentence,
                    conversationHistory = _uiState.value.messages.items,
                    userLevel = userLevel
                )
                android.util.Log.d("GrammarDebug", "API response received: ${grammarExplanation.overallExplanation}")

                // No retries - API service already handles fallback
                // The GeminiApiService now automatically returns local analysis on any error
                android.util.Log.d("GrammarDebug", "Received analysis result (may be local fallback)")

                // Even if it's an error message, we already have local analysis from the API service

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
                        grammarCache = newCache.toImmutableMap(),
                        grammarError = null,
                        grammarRetryCount = 0
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("GrammarDebug", "❌ Exception in grammar analysis: ${e.message}", e)
                android.util.Log.e("GrammarDebug", "Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("GrammarDebug", "Stack trace:", e)

                // Immediately fallback to local analyzer on exception (no retries)
                android.util.Log.d("GrammarDebug", "Falling back to LOCAL analyzer due to exception")
                val fallbackExplanation = com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.analyzeSentence(
                    sentence = sentence,
                    userLevel = userLevel
                )

                _uiState.update {
                    // Cache the fallback result
                    val currentCache = it.grammarCache.items
                    val newCache = if (currentCache.size >= memoryConfig.maxCacheSize) {
                        currentCache.entries.drop(1).associate { entry -> entry.key to entry.value } +
                                (sentence to fallbackExplanation)
                    } else {
                        currentCache + (sentence to fallbackExplanation)
                    }

                    it.copy(
                        grammarExplanation = fallbackExplanation,
                        isLoadingGrammar = false,
                        grammarCache = newCache.toImmutableMap(),
                        grammarError = "로컬 분석 모드 (에러: ${e.message})",
                        grammarRetryCount = 0
                    )
                }
            }
        }
    }

    fun retryGrammarAnalysis() {
        // Reset and retry with the current sentence
        val sentence = _uiState.value.currentGrammarSentence
        if (sentence != null) {
            requestGrammarExplanation(sentence, retryAttempt = 0)
        }
    }

    fun requestTranslation(messageId: Long, japaneseText: String, retryAttempt: Int = 0) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "=== Translation Request ===")
            android.util.Log.d("ChatViewModel", "MessageId: $messageId")
            android.util.Log.d("ChatViewModel", "Text: '$japaneseText'")
            android.util.Log.d("ChatViewModel", "Retry attempt: $retryAttempt")

            // Check if already translated successfully
            if (_uiState.value.translations.containsKey(messageId)) {
                android.util.Log.d("ChatViewModel", "Already translated, skipping")
                return@launch
            }

            // Update retry count in UI state
            _uiState.update {
                it.copy(
                    translationRetryCount = (it.translationRetryCount.items + (messageId to retryAttempt)).toImmutableMap(),
                    translationErrors = (it.translationErrors.items - messageId).toImmutableMap() // Clear error
                )
            }

            try {
                // Step 1: Try local dictionary first (instant, no API call)
                val localTranslation = com.nihongo.conversation.core.translation.LocalTranslationDictionary.translate(japaneseText)
                if (localTranslation != null) {
                    android.util.Log.d("ChatViewModel", "Local dictionary hit: '$localTranslation'")
                    _uiState.update {
                        it.copy(
                            translations = (it.translations.items + (messageId to localTranslation)).toImmutableMap(),
                            translationRetryCount = (it.translationRetryCount.items - messageId).toImmutableMap()
                        )
                    }
                    return@launch
                }

                // Step 2: Try ML Kit translation (fast, on-device)
                try {
                    android.util.Log.d("ChatViewModel", "No local match, trying ML Kit...")
                    val mlTranslation = mlKitTranslator.translate(japaneseText)
                    android.util.Log.d("ChatViewModel", "ML Kit translation success: '$mlTranslation'")

                    _uiState.update {
                        it.copy(
                            translations = (it.translations.items + (messageId to mlTranslation)).toImmutableMap(),
                            translationRetryCount = (it.translationRetryCount.items - messageId).toImmutableMap(),
                            translationErrors = (it.translationErrors.items - messageId).toImmutableMap()
                        )
                    }
                    return@launch
                } catch (mlError: Exception) {
                    android.util.Log.w("ChatViewModel", "ML Kit failed, falling back to API: ${mlError.message}")
                }

                // Step 3: Fallback to Gemini API translation (slow but reliable)
                android.util.Log.d("ChatViewModel", "ML Kit failed, calling Gemini API...")
                val translation = repository.translateToKorean(japaneseText)
                android.util.Log.d("ChatViewModel", "API translation success: '$translation'")

                _uiState.update {
                    it.copy(
                        translations = (it.translations.items + (messageId to translation)).toImmutableMap(),
                        translationRetryCount = (it.translationRetryCount.items - messageId).toImmutableMap(),
                        translationErrors = (it.translationErrors.items - messageId).toImmutableMap()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Translation failed", e)
                android.util.Log.e("ChatViewModel", "Error type: ${e.javaClass.simpleName}")
                android.util.Log.e("ChatViewModel", "Error message: ${e.message}")

                // Retry logic with exponential backoff
                if (retryAttempt < 3) {
                    val delayMs = 1000L * (retryAttempt + 1) // 1s, 2s, 3s
                    android.util.Log.d("ChatViewModel", "Retrying after ${delayMs}ms (attempt ${retryAttempt + 1}/3)")

                    kotlinx.coroutines.delay(delayMs)
                    requestTranslation(messageId, japaneseText, retryAttempt + 1)
                } else {
                    // Max retries reached - show error
                    android.util.Log.e("ChatViewModel", "Max retries reached, showing error")

                    val errorMessage = when {
                        e.message?.contains("한도", ignoreCase = true) == true -> "API 한도 초과"
                        e.message?.contains("시간 초과", ignoreCase = true) == true -> "시간 초과 - 다시 시도하세요"
                        e.message?.contains("네트워크", ignoreCase = true) == true -> "네트워크 오류"
                        else -> "번역 실패 - 다시 시도하세요"
                    }

                    _uiState.update {
                        it.copy(
                            translationErrors = (it.translationErrors.items + (messageId to errorMessage)).toImmutableMap(),
                            translationRetryCount = (it.translationRetryCount.items - messageId).toImmutableMap()
                        )
                    }
                }
            }
        }
    }

    fun retryTranslation(messageId: Long, japaneseText: String) {
        android.util.Log.d("ChatViewModel", "Manual retry requested for message $messageId")
        // Clear error and retry from scratch
        _uiState.update {
            it.copy(
                translationErrors = (it.translationErrors.items - messageId).toImmutableMap(),
                translationRetryCount = (it.translationRetryCount.items - messageId).toImmutableMap()
            )
        }
        requestTranslation(messageId, japaneseText, retryAttempt = 0)
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
                // Stop collecting old conversation messages to prevent repopulation
                messagesFlowJob?.cancel()
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

            // Stop collecting from the previous conversation
            messagesFlowJob?.cancel()

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

            // Start collecting messages for the new conversation so UI updates immediately
            val newConversationId = currentConversationId
            if (newConversationId != null) {
                messagesFlowJob = viewModelScope.launch {
                    repository.getMessages(newConversationId)
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

    /**
     * Analyze user message for grammar and style feedback
     * Called automatically after user sends a message
     */
    private fun analyzeMessageForFeedback(messageId: Long, userMessage: String) {
        // Skip if feedback is disabled
        if (!_uiState.value.feedbackEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingFeedback = true) }

            try {
                // Get conversation context for better analysis
                val conversationContext = _uiState.value.messages.items
                    .takeLast(5)
                    .map { it.content }

                val userLevel = _uiState.value.user?.level ?: 1

                // Analyze message using AI
                val feedbackList = grammarFeedbackRepository.analyzeMessage(
                    userId = currentUserId,
                    messageId = messageId,
                    userMessage = userMessage,
                    conversationContext = conversationContext,
                    userLevel = userLevel
                )

                // Update state with feedback
                if (feedbackList.isNotEmpty()) {
                    val currentFeedbackMap = _uiState.value.grammarFeedback.items.toMutableMap()
                    currentFeedbackMap[messageId] = feedbackList.toImmutableList()

                    _uiState.update {
                        it.copy(
                            grammarFeedback = currentFeedbackMap.toImmutableMap(),
                            isAnalyzingFeedback = false,
                            unacknowledgedFeedbackCount = it.unacknowledgedFeedbackCount + feedbackList.size
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently fail - feedback is optional
                _uiState.update { it.copy(isAnalyzingFeedback = false) }
            }
        }
    }

    /**
     * Get feedback for a specific message
     */
    fun getFeedbackForMessage(messageId: Long): List<GrammarFeedback> {
        return _uiState.value.grammarFeedback.items[messageId]?.items ?: emptyList()
    }

    /**
     * Acknowledge feedback (mark as seen)
     */
    fun acknowledgeFeedback(feedbackId: Long) {
        viewModelScope.launch {
            grammarFeedbackRepository.acknowledgeFeedback(feedbackId)

            _uiState.update {
                it.copy(
                    unacknowledgedFeedbackCount = maxOf(0, it.unacknowledgedFeedbackCount - 1)
                )
            }
        }
    }

    /**
     * Apply a correction from feedback
     */
    fun applyCorrectionFromFeedback(feedbackId: Long, correctedText: String) {
        viewModelScope.launch {
            grammarFeedbackRepository.markCorrectionApplied(feedbackId)

            // Update input text with correction
            _uiState.update { it.copy(inputText = correctedText) }
        }
    }

    /**
     * Toggle real-time feedback analysis
     */
    fun toggleFeedback() {
        _uiState.update { it.copy(feedbackEnabled = !it.feedbackEnabled) }

        viewModelScope.launch {
            settingsDataStore.updateFeedbackEnabled(_uiState.value.feedbackEnabled)
        }
    }

    /**
     * Load unacknowledged feedback count
     */
    private fun loadUnacknowledgedFeedbackCount() {
        viewModelScope.launch {
            grammarFeedbackRepository.getUnacknowledgedFeedback(currentUserId)
                .collect { feedbackList ->
                    _uiState.update { it.copy(unacknowledgedFeedbackCount = feedbackList.size) }
                }
        }
    }

    /**
     * Start voice-only conversation mode
     */
    fun startVoiceOnlyMode(targetDuration: Int = 5) {
        val conversationId = currentConversationId

        val session = VoiceOnlySession(
            isActive = true,
            startTime = System.currentTimeMillis(),
            conversationId = conversationId,
            targetDuration = targetDuration,
            messageCount = _uiState.value.messages.size
        )

        _uiState.update {
            it.copy(voiceOnlySession = session)
        }
        voiceManager.setIdle()

        // Auto-enable auto-speak for voice-only mode
        if (!_uiState.value.autoSpeak) {
            viewModelScope.launch {
                settingsDataStore.updateAutoSpeak(true)
            }
        }
    }

    /**
     * End voice-only conversation mode
     */
    fun endVoiceOnlyMode() {
        val session = _uiState.value.voiceOnlySession ?: return

        // Build transcript from current messages
        val transcript = _uiState.value.messages.items.map { message ->
            TranscriptEntry(
                messageId = message.id,
                text = message.content,
                isUser = message.isUser,
                timestamp = message.timestamp,
                wasSpoken = true, // Assume all were spoken in voice mode
                wasHeard = true
            )
        }

        _uiState.update {
            it.copy(
                voiceOnlySession = session.copy(
                    isActive = false,
                    transcript = transcript
                ),
                showTranscriptDialog = true
            )
        }
        voiceManager.setIdle()

        // Stop any ongoing voice activity
        voiceManager.stopListening()
    }


    /**
     * Dismiss transcript dialog
     */
    fun dismissTranscript() {
        _uiState.update {
            it.copy(
                showTranscriptDialog = false,
                voiceOnlySession = null
            )
        }
    }

    /**
     * Check if voice-only session should auto-end
     */
    private fun checkVoiceOnlyTimeout() {
        val session = _uiState.value.voiceOnlySession
        if (session != null && session.isActive && session.isComplete) {
            endVoiceOnlyMode()
        }
    }

    /**
     * Phase 6A: Observe memory pressure and react to it
     */
    private fun observeMemoryPressure() {
        // Observe memory config changes
        memoryConfigJob = viewModelScope.launch {
            memoryManager.memoryConfigFlow.collect { config ->
                // Trim messages if current count exceeds new limit
                _uiState.update { state ->
                    if (state.messages.size > config.maxMessageHistory) {
                        android.util.Log.w("ChatViewModel",
                            "Memory config changed - trimming messages: ${state.messages.size} → ${config.maxMessageHistory}")
                        state.copy(
                            messages = state.messages.items.takeLast(config.maxMessageHistory).toImmutableList()
                        )
                    } else {
                        state
                    }
                }
            }
        }

        // Observe memory level for cache trimming
        memoryLevelJob = viewModelScope.launch {
            memoryManager.memoryLevel.collect { level ->
                when (level) {
                    MemoryManager.MemoryLevel.CRITICAL -> {
                        android.util.Log.w("ChatViewModel", "CRITICAL memory pressure - clearing all caches")
                        // Clear grammar cache
                        _uiState.update {
                            it.copy(
                                grammarCache = ImmutableMap.empty(),
                                translations = ImmutableMap.empty(),
                                translationErrors = ImmutableMap.empty()
                            )
                        }
                        // Clear LocalGrammarAnalyzer cache
                        com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.clearCache()
                    }
                    MemoryManager.MemoryLevel.LOW -> {
                        android.util.Log.w("ChatViewModel", "LOW memory pressure - trimming caches")
                        val currentConfig = memoryManager.memoryConfigFlow.value

                        // Trim grammar cache to 50%
                        _uiState.update { state ->
                            val grammarCacheSize = state.grammarCache.items.size
                            val targetSize = (currentConfig.maxCacheSize * 0.5).toInt()

                            if (grammarCacheSize > targetSize) {
                                val trimmedGrammar = state.grammarCache.items.entries
                                    .drop(grammarCacheSize - targetSize)
                                    .associate { it.key to it.value }

                                state.copy(
                                    grammarCache = trimmedGrammar.toImmutableMap()
                                )
                            } else {
                                state
                            }
                        }

                        // Trim LocalGrammarAnalyzer cache
                        val targetSize = (currentConfig.maxCacheSize * 0.5).toInt()
                        com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.trimCache(targetSize)

                        // Clear oldest translations (keep 50%)
                        _uiState.update { state ->
                            val translationCount = state.translations.items.size
                            if (translationCount > 10) {
                                val toKeep = translationCount / 2
                                val keptTranslations = state.translations.items.entries
                                    .sortedByDescending { it.key }  // Keep most recent
                                    .take(toKeep)
                                    .associate { it.key to it.value }

                                state.copy(
                                    translations = keptTranslations.toImmutableMap()
                                )
                            } else {
                                state
                            }
                        }
                    }
                    MemoryManager.MemoryLevel.NORMAL -> {
                        // Normal operation - no action needed
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Cancel all active coroutine jobs to prevent memory leaks
        settingsFlowJob?.cancel()
        profileFlowJob?.cancel()
        voiceEventsJob?.cancel()
        messagesFlowJob?.cancel()
        memoryConfigJob?.cancel()  // Phase 6A
        memoryLevelJob?.cancel()    // Phase 6A

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
