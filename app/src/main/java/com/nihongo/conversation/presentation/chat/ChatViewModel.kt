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
import com.nihongo.conversation.core.voice.VoiceLanguage
import com.nihongo.conversation.core.voice.VoiceManager
import com.nihongo.conversation.core.voice.VoiceState
import com.nihongo.conversation.data.local.SettingsDataStore
import com.nihongo.conversation.data.local.entity.QuestType
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.data.repository.GrammarFeedbackRepository
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.data.repository.QuestRepository
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
 * Result of Korean to Japanese translation
 */
data class KoreanToJapaneseResult(
    val japanese: String,           // "ありがとうございました"
    val romanization: String,       // "아리가토우 고자이마시타"
    val korean: String              // 원본 "정말 감사합니다"
)

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
    val scenarioCategory: String? = null, // e.g., "🏠 일상 생활"
    val scenarioDifficulty: String? = null, // e.g., "초급"
    val isFavoriteScenario: Boolean = false, // Is current scenario favorited
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
    val messagesWithFurigana: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds with furigana enabled
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
    val adaptiveNudge: String = "", // Adaptive difficulty nudge (very short, 8 chars max)
    val showKoreanToJapaneseDialog: Boolean = false, // Show Korean→Japanese conversion dialog
    val koreanToJapaneseResult: KoreanToJapaneseResult? = null, // Conversion result
    val isTranslatingKorToJpn: Boolean = false, // Loading state for conversion
    val showFurigana: Boolean = false, // Show furigana on AI messages
    val furiganaType: com.nihongo.conversation.domain.model.FuriganaType = com.nihongo.conversation.domain.model.FuriganaType.HIRAGANA, // Furigana display type
    val selectedVoiceLanguage: VoiceLanguage = VoiceLanguage.JAPANESE, // Selected voice input language

    // User message translation (Japanese → Korean)
    val userTranslations: ImmutableMap<Long, String> = ImmutableMap.empty(), // messageId -> Korean translation
    val expandedUserTranslations: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds with translation expanded
    val userTranslationErrors: ImmutableMap<Long, String> = ImmutableMap.empty(), // messageId -> error message

    // User message furigana
    val userMessagesWithFurigana: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds with furigana enabled

    // User message grammar feedback
    val userGrammarFeedback: ImmutableMap<Long, ImmutableList<GrammarFeedback>> = ImmutableMap.empty(), // messageId -> feedback list
    val expandedUserGrammarFeedback: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds with feedback expanded
    val userGrammarAnalyzing: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds being analyzed
    val userGrammarErrors: ImmutableMap<Long, String> = ImmutableMap.empty(), // messageId -> error message

    // Phase 5: Message bookmarking & sharing
    val savedMessages: ImmutableSet<Long> = ImmutableSet.empty(), // messageIds that are bookmarked

    // Snackbar/feedback messages
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
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
    private val mlKitTranslator: com.nihongo.conversation.core.translation.MLKitTranslator,
    private val translationRepository: com.nihongo.conversation.data.repository.TranslationRepository,
    private val vocabularyRepository: com.nihongo.conversation.data.repository.VocabularyRepository,
    private val questRepository: QuestRepository,
    private val savedMessageRepository: com.nihongo.conversation.data.repository.SavedMessageRepository  // Phase 5
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
        observeSavedMessages()  // Phase 5
        observeMemoryPressure()  // Phase 6A
    }

    private fun observeSettings() {
        settingsFlowJob = viewModelScope.launch {
            settingsDataStore.userSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        autoSpeak = settings.autoSpeak,
                        speechSpeed = settings.speechSpeed,
                        showFurigana = settings.showFurigana,
                        furiganaType = settings.furiganaType
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

    // Phase 5: Observe saved messages for current conversation
    private fun observeSavedMessages() {
        viewModelScope.launch {
            // Collect saved messages and update UI state
            savedMessageRepository.getSavedMessages(currentUserId)
                .collect { savedMessagesList ->
                    val savedMessageIds = savedMessagesList.map { it.messageId }.toSet()
                    _uiState.update {
                        it.copy(savedMessages = savedMessageIds.toImmutableSet())
                    }
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

            // Load scenario and user for favorite check
            val scenario = repository.getScenario(scenarioId).first()
            val user = repository.getUser(userId).first()

            scenario?.let {
                // Check if scenario is favorited
                val favoriteIds = user?.favoriteScenarios?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
                val isFavorite = favoriteIds.contains(scenario.id)

                // Get category label
                val categoryLabel = getCategoryLabel(scenario.category)

                // Get difficulty label
                val difficultyLabel = when (scenario.difficulty) {
                    1 -> "초급"
                    2 -> "중급"
                    3 -> "고급"
                    else -> "초급"
                }

                _uiState.update {
                    it.copy(
                        scenario = scenario,
                        scenarioCategory = categoryLabel,
                        scenarioDifficulty = difficultyLabel,
                        isFavoriteScenario = isFavorite,
                        user = user
                    )
                }

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

        // Check if input is Korean
        if (isKoreanText(message)) {
            // Show Korean→Japanese conversion dialog
            translateKoreanToJapanese(message)
            return
        }

        // Send Japanese message directly
        sendJapaneseMessage(message)
    }

    /**
     * Check if text contains Korean characters
     */
    private fun isKoreanText(text: String): Boolean {
        return text.matches(Regex(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*"))
    }

    /**
     * Send Japanese message (extracted from original sendMessage)
     * Made public to be called from Korean→Japanese dialog
     */
    fun sendJapaneseMessage(message: String) {
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

            // Get difficulty-specific guidelines from scenario (using compact version for token efficiency)
            val difficultyLevel = DifficultyLevel.fromInt(scenario.difficulty)
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

            // Update quest progress: MESSAGE_COUNT (increment by 1)
            viewModelScope.launch {
                questRepository.incrementQuestProgressByType(
                    userId = currentUserId,
                    questType = QuestType.MESSAGE_COUNT,
                    amount = 1
                )
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
                            // Handle based on selected language
                            when (_uiState.value.selectedVoiceLanguage) {
                                VoiceLanguage.JAPANESE -> {
                                    // Japanese: set input text directly
                                    _uiState.update { it.copy(inputText = event.text) }
                                }
                                VoiceLanguage.KOREAN -> {
                                    // Korean: auto-translate to Japanese
                                    translateKoreanToJapanese(event.text)
                                }
                            }
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
        voiceManager.startListening(_uiState.value.selectedVoiceLanguage)
    }

    fun toggleVoiceLanguage() {
        _uiState.update {
            it.copy(
                selectedVoiceLanguage = when (it.selectedVoiceLanguage) {
                    VoiceLanguage.JAPANESE -> VoiceLanguage.KOREAN
                    VoiceLanguage.KOREAN -> VoiceLanguage.JAPANESE
                }
            )
        }
    }

    fun stopVoiceRecording() {
        voiceManager.stopListening()
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text, speed = _uiState.value.speechSpeed)
    }

    fun speakMessageSlowly(text: String) {
        voiceManager.speak(text, speed = 0.7f)
    }

    fun toggleAutoSpeak() {
        viewModelScope.launch {
            settingsDataStore.updateAutoSpeak(!_uiState.value.autoSpeak)
        }
    }

    /**
     * Toggle furigana display for a specific message
     *
     * @param messageId The ID of the message to toggle furigana for
     */
    fun toggleMessageFurigana(messageId: Long) {
        _uiState.update { currentState ->
            val currentFuriganaMessages = currentState.messagesWithFurigana.toMutableSet()
            if (messageId in currentFuriganaMessages) {
                // Remove from set (turn OFF)
                currentFuriganaMessages.remove(messageId)
            } else {
                // Add to set (turn ON)
                currentFuriganaMessages.add(messageId)
            }
            currentState.copy(messagesWithFurigana = currentFuriganaMessages.toImmutableSet())
        }
    }

    /**
     * Translate Korean text to Japanese and show dialog
     */
    fun translateKoreanToJapanese(korean: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTranslatingKorToJpn = true, error = null) }

            try {
                // Use TranslationRepository for Korean→Japanese translation
                val result = translationRepository.translate(
                    text = korean,
                    sourceLang = "ko",
                    targetLang = "ja",
                    provider = com.nihongo.conversation.data.remote.deepl.TranslationProvider.MICROSOFT,
                    useCache = true,
                    fallbackChain = listOf(com.nihongo.conversation.data.remote.deepl.TranslationProvider.DEEP_L)  // ML Kit doesn't support ko→ja yet
                )

                when (result) {
                    is com.nihongo.conversation.data.repository.TranslationResult.Success -> {
                        // Convert Japanese to Korean pronunciation
                        val romanization = japaneseToKoreanPronunciation(result.translatedText)

                        _uiState.update {
                            it.copy(
                                showKoreanToJapaneseDialog = true,
                                koreanToJapaneseResult = KoreanToJapaneseResult(
                                    japanese = result.translatedText,
                                    romanization = romanization,
                                    korean = korean
                                ),
                                isTranslatingKorToJpn = false
                            )
                        }
                    }
                    is com.nihongo.conversation.data.repository.TranslationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                error = "번역 실패: ${result.message}",
                                isTranslatingKorToJpn = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "번역 중 오류: ${e.message}",
                        isTranslatingKorToJpn = false
                    )
                }
            }
        }
    }

    /**
     * Dismiss Korean→Japanese dialog
     */
    fun dismissKorToJpnDialog() {
        _uiState.update {
            it.copy(
                showKoreanToJapaneseDialog = false,
                koreanToJapaneseResult = null
            )
        }
    }

    /**
     * Convert Japanese (including kanji) to Korean pronunciation approximation
     *
     * Uses Kuromoji to convert kanji to hiragana first, then maps to Korean.
     * This ensures accurate pronunciation even for kanji characters.
     */
    private fun japaneseToKoreanPronunciation(japanese: String): String {
        // Step 1: Convert kanji to hiragana using Kuromoji
        val readings = try {
            com.nihongo.conversation.core.grammar.KuromojiGrammarAnalyzer.getReadings(japanese)
        } catch (e: Exception) {
            // Fallback to original text if Kuromoji fails
            listOf(japanese)
        }

        // Step 2: Convert hiragana/katakana to Korean
        val hiraganaMap = mapOf(
            "あ" to "아", "い" to "이", "う" to "우", "え" to "에", "お" to "오",
            "か" to "카", "き" to "키", "く" to "쿠", "け" to "케", "こ" to "코",
            "が" to "가", "ぎ" to "기", "ぐ" to "구", "げ" to "게", "ご" to "고",
            "さ" to "사", "し" to "시", "す" to "스", "せ" to "세", "そ" to "소",
            "ざ" to "자", "じ" to "지", "ず" to "즈", "ぜ" to "제", "ぞ" to "조",
            "た" to "타", "ち" to "치", "つ" to "츠", "て" to "테", "と" to "토",
            "だ" to "다", "ぢ" to "지", "づ" to "즈", "で" to "데", "ど" to "도",
            "な" to "나", "に" to "니", "ぬ" to "누", "ね" to "네", "の" to "노",
            "は" to "하", "ひ" to "히", "ふ" to "후", "へ" to "헤", "ほ" to "호",
            "ば" to "바", "び" to "비", "ぶ" to "부", "べ" to "베", "ぼ" to "보",
            "ぱ" to "파", "ぴ" to "피", "ぷ" to "푸", "ぺ" to "페", "ぽ" to "포",
            "ま" to "마", "み" to "미", "む" to "무", "め" to "메", "も" to "모",
            "や" to "야", "ゆ" to "유", "よ" to "요",
            "ら" to "라", "り" to "리", "る" to "루", "れ" to "레", "ろ" to "로",
            "わ" to "와", "を" to "오", "ん" to "ㅇ",
            // Katakana
            "ア" to "아", "イ" to "이", "ウ" to "우", "エ" to "에", "オ" to "오",
            "カ" to "카", "キ" to "키", "ク" to "쿠", "ケ" to "케", "コ" to "코",
            "ガ" to "가", "ギ" to "기", "グ" to "구", "ゲ" to "게", "ゴ" to "고",
            "サ" to "사", "シ" to "시", "ス" to "스", "セ" to "세", "ソ" to "소",
            "ザ" to "자", "ジ" to "지", "ズ" to "즈", "ゼ" to "제", "ゾ" to "조",
            "タ" to "타", "チ" to "치", "ツ" to "츠", "テ" to "테", "ト" to "토",
            "ダ" to "다", "ヂ" to "지", "ヅ" to "즈", "デ" to "데", "ド" to "도",
            "ナ" to "나", "ニ" to "니", "ヌ" to "누", "ネ" to "네", "ノ" to "노",
            "ハ" to "하", "ヒ" to "히", "フ" to "후", "ヘ" to "헤", "ホ" to "호",
            "バ" to "바", "ビ" to "비", "ブ" to "부", "ベ" to "베", "ボ" to "보",
            "パ" to "파", "ピ" to "피", "プ" to "푸", "ペ" to "페", "ポ" to "포",
            "マ" to "마", "ミ" to "미", "ム" to "무", "メ" to "메", "モ" to "모",
            "ヤ" to "야", "ユ" to "유", "ヨ" to "요",
            "ラ" to "라", "リ" to "리", "ル" to "루", "レ" to "레", "ロ" to "로",
            "ワ" to "와", "ヲ" to "오", "ン" to "ㅇ",
            // Special characters (장음 기호 및 특수 문자)
            "ー" to "-",   // Long vowel mark (장음 기호)
            "～" to "~",
            "、" to ",",
            "。" to "."
        )

        // Step 3: Map each reading (hiragana) to Korean
        return readings.joinToString(" ") { reading ->
            reading.map { char ->
                hiraganaMap[char.toString()] ?: char.toString()
            }.joinToString("")
        }
    }

    fun requestHints() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHints = true, showHintDialog = true) }

            try {
                // Get scenario difficulty and context
                val scenario = _uiState.value.scenario
                val scenarioDifficulty = scenario?.difficulty ?: 1

                val hints = repository.getHints(
                    conversationHistory = _uiState.value.messages.items,
                    userLevel = scenarioDifficulty,
                    scenarioSystemPrompt = scenario?.systemPrompt ?: ""
                )
                _uiState.update {
                    it.copy(
                        hints = hints.toImmutableList(),
                        isLoadingHints = false
                    )
                }
            } catch (e: Exception) {
                // Fallback to scenario-based hints
                val fallbackHints = generateScenarioBasedFallback(_uiState.value.scenario)
                _uiState.update {
                    it.copy(
                        hints = fallbackHints.toImmutableList(),
                        isLoadingHints = false,
                        error = "힌트 생성 실패 (기본 힌트 표시)"
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

            val scenario = _uiState.value.scenario
            val scenarioDifficulty = scenario?.difficulty ?: 1
            android.util.Log.d("GrammarDebug", "Scenario difficulty: $scenarioDifficulty")

            // Check if we can analyze locally (simple patterns) to avoid API call
            val canAnalyzeLocally = com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.canAnalyzeLocally(sentence)
            android.util.Log.d("GrammarDebug", "Can analyze locally: $canAnalyzeLocally")

            if (canAnalyzeLocally) {
                android.util.Log.d("GrammarDebug", "📱 Using LOCAL analyzer for simple sentence")
                // Use local analyzer for simple sentences
                val localExplanation = com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer.analyzeSentence(
                    sentence = sentence,
                    userLevel = scenarioDifficulty
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
                    userLevel = scenarioDifficulty
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
                    userLevel = scenarioDifficulty
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
            android.util.Log.d("ChatViewModel", "=== Translation Request (TranslationRepository) ===")
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
                // Use TranslationRepository with automatic fallback chain
                // Priority: Cache → Microsoft (2M/month) → DeepL (500k/month) → ML Kit (offline)
                val result = translationRepository.translate(
                    text = japaneseText,
                    provider = com.nihongo.conversation.data.remote.deepl.TranslationProvider.MICROSOFT,
                    useCache = true,
                    fallbackChain = listOf(
                        com.nihongo.conversation.data.remote.deepl.TranslationProvider.DEEP_L,
                        com.nihongo.conversation.data.remote.deepl.TranslationProvider.ML_KIT
                    )
                )

                when (result) {
                    is com.nihongo.conversation.data.repository.TranslationResult.Success -> {
                        android.util.Log.d("ChatViewModel", "Translation success from ${result.provider}")
                        android.util.Log.d("ChatViewModel", "From cache: ${result.fromCache}, Elapsed: ${result.elapsed}ms")
                        android.util.Log.d("ChatViewModel", "Translation: '${result.translatedText}'")

                        _uiState.update {
                            it.copy(
                                translations = (it.translations.items + (messageId to result.translatedText)).toImmutableMap(),
                                translationRetryCount = (it.translationRetryCount.items - messageId).toImmutableMap(),
                                translationErrors = (it.translationErrors.items - messageId).toImmutableMap()
                            )
                        }
                    }

                    is com.nihongo.conversation.data.repository.TranslationResult.Error -> {
                        android.util.Log.e("ChatViewModel", "Translation error: ${result.message}")

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
                                result.message.contains("한도", ignoreCase = true) -> "API 한도 초과"
                                result.message.contains("시간 초과", ignoreCase = true) -> "시간 초과 - 다시 시도하세요"
                                result.message.contains("네트워크", ignoreCase = true) -> "네트워크 오류"
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
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Unexpected translation error", e)

                // Retry logic with exponential backoff
                if (retryAttempt < 3) {
                    val delayMs = 1000L * (retryAttempt + 1)
                    android.util.Log.d("ChatViewModel", "Retrying after ${delayMs}ms (attempt ${retryAttempt + 1}/3)")

                    kotlinx.coroutines.delay(delayMs)
                    requestTranslation(messageId, japaneseText, retryAttempt + 1)
                } else {
                    _uiState.update {
                        it.copy(
                            translationErrors = (it.translationErrors.items + (messageId to "번역 실패 - 다시 시도하세요")).toImmutableMap(),
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

                val scenario = _uiState.value.scenario
                val scenarioDifficulty = scenario?.difficulty ?: 1

                // Analyze message using AI
                val feedbackList = grammarFeedbackRepository.analyzeMessage(
                    userId = currentUserId,
                    messageId = messageId,
                    userMessage = userMessage,
                    conversationContext = conversationContext,
                    userLevel = scenarioDifficulty
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

        // Update quest progress: VOICE_ONLY_SESSION (completed 1 session)
        viewModelScope.launch {
            questRepository.incrementQuestProgressByType(
                userId = currentUserId,
                questType = QuestType.VOICE_ONLY_SESSION,
                amount = 1
            )
        }
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

    /**
     * Generate scenario-based fallback hints when AI hint generation fails
     */
    private fun generateScenarioBasedFallback(scenario: Scenario?): List<Hint> {
        return when (scenario?.category) {
            "DAILY_LIFE" -> listOf(
                Hint("分かりました", "알겠습니다", "wakarimashita", "동의/이해 표현"),
                Hint("お願いします", "부탁합니다", "onegaishimasu", "요청할 때"),
                Hint("すみません", "죄송합니다", "sumimasen", "사과/호칭")
            )

            "TRAVEL" -> listOf(
                Hint("いくらですか", "얼마입니까", "ikura desu ka", "가격 물을 때"),
                Hint("ここまでお願いします", "여기까지 부탁합니다", "koko made onegaishimasu", "택시/이동"),
                Hint("写真を撮っていただけますか", "사진 찍어주시겠어요", "shashin wo totte itadakemasu ka", "사진 부탁")
            )

            "WORK", "BUSINESS" -> listOf(
                Hint("承知しました", "알겠습니다", "shouchi shimashita", "비즈니스 동의"),
                Hint("確認いたします", "확인하겠습니다", "kakunin itashimasu", "확인 응답"),
                Hint("よろしくお願いいたします", "잘 부탁드립니다", "yoroshiku onegai itashimasu", "비즈니스 인사")
            )

            "JLPT_PRACTICE" -> when (scenario.difficulty) {
                1 -> listOf(  // N5/N4
                    Hint("はい、そうです", "네, 그렇습니다", "hai, sou desu", "긍정 답변"),
                    Hint("いいえ、違います", "아니요, 다릅니다", "iie, chigaimasu", "부정 답변"),
                    Hint("もう一度お願いします", "다시 한 번 부탁합니다", "mou ichido onegaishimasu", "재요청")
                )
                2 -> listOf(  // N3/N2
                    Hint("そうですね", "그렇네요", "sou desu ne", "동의/공감"),
                    Hint("どうしたらいいですか", "어떻게 하면 좋을까요", "dou shitara ii desu ka", "조언 요청"),
                    Hint("教えていただけますか", "가르쳐주시겠어요", "oshiete itadakemasu ka", "설명 요청")
                )
                else -> listOf(  // N1
                    Hint("おっしゃる通りです", "말씀하신 대로입니다", "ossharu toori desu", "정중한 동의"),
                    Hint("恐れ入りますが", "송구합니다만", "osoreirimasu ga", "정중한 전치사"),
                    Hint("承りました", "알겠습니다(최고 경어)", "uketamawarimashita", "최고 경어")
                )
            }

            "ENTERTAINMENT", "ESPORTS", "CULTURE" -> listOf(
                Hint("いいですね", "좋네요", "ii desu ne", "긍정적 반응"),
                Hint("私も好きです", "저도 좋아합니다", "watashi mo suki desu", "공감"),
                Hint("どう思いますか", "어떻게 생각하세요", "dou omoimasu ka", "의견 물어보기")
            )

            "TECH" -> listOf(
                Hint("確認します", "확인합니다", "kakunin shimasu", "확인"),
                Hint("修正します", "수정합니다", "shuusei shimasu", "수정"),
                Hint("テストしてみます", "테스트해봅니다", "tesuto shite mimasu", "시도")
            )

            else -> listOf(  // Default fallback for all other scenarios
                Hint("そうですね", "그렇네요", "sou desu ne", "동의"),
                Hint("分かりました", "알겠습니다", "wakarimashita", "이해"),
                Hint("ありがとうございます", "감사합니다", "arigatou gozaimasu", "감사")
            )
        }
    }

    /**
     * Get category label with emoji for display
     */
    private fun getCategoryLabel(category: String): String {
        return when (category) {
            "DAILY_LIFE" -> "🏠 일상 생활"
            "WORK" -> "💼 직장/업무"
            "TRAVEL" -> "✈️ 여행"
            "ENTERTAINMENT" -> "🎵 엔터테인먼트"
            "ESPORTS" -> "🎮 e스포츠"
            "TECH" -> "💻 기술/개발"
            "FINANCE" -> "💰 금융/재테크"
            "CULTURE" -> "🎭 문화"
            "HOUSING" -> "🏢 부동산/주거"
            "HEALTH" -> "🏥 건강/의료"
            "STUDY" -> "📚 학습/교육"
            "DAILY_CONVERSATION" -> "💬 일상 회화"
            "JLPT_PRACTICE" -> "📖 JLPT 연습"
            "BUSINESS" -> "🤝 비즈니스"
            "ROMANCE" -> "💕 연애/관계"
            "EMERGENCY" -> "🚨 긴급 상황"
            else -> "📚 기타"
        }
    }

    /**
     * Update an existing message content
     */
    fun updateMessage(messageId: Long, newContent: String) {
        viewModelScope.launch {
            val message = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val updatedMessage = message.copy(content = newContent)
            repository.updateMessage(updatedMessage)
        }
    }

    /**
     * Delete a message
     */
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            val message = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            repository.deleteMessage(message)
        }
    }

    /**
     * Add message content to vocabulary
     */
    fun addToVocabulary(messageId: Long) {
        viewModelScope.launch {
            try {
                val message = _uiState.value.messages.find { it.id == messageId } ?: return@launch
                if (message.isUser) return@launch // Only AI messages can be added

                // Extract Japanese text without furigana
                val japaneseText = message.content.replace(Regex("（[^）]*）|\\([^)]*\\)"), "").trim()

                // Get Korean translation
                val translation = _uiState.value.translations[messageId]
                    ?: ""

                // Add to vocabulary
                vocabularyRepository.addCustomVocabulary(
                    userId = currentUserId,
                    word = japaneseText,
                    reading = null,
                    meaning = translation,
                    exampleSentence = japaneseText,
                    difficulty = 1,
                    addToReviewQueue = true
                )
            } catch (e: IllegalArgumentException) {
                // Word already exists - silently ignore
            } catch (e: Exception) {
                // Failed to add - silently ignore
            }
        }
    }

    // ========== User Message Features ==========

    /**
     * Request translation for user message (Japanese → Korean)
     */
    fun requestUserTranslation(messageId: Long, japaneseText: String, retryAttempt: Int = 0) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "=== User Translation Request ===")
            android.util.Log.d("ChatViewModel", "MessageId: $messageId")
            android.util.Log.d("ChatViewModel", "Text: '$japaneseText'")
            android.util.Log.d("ChatViewModel", "Retry attempt: $retryAttempt")

            try {
                val result = translationRepository.translate(
                    text = japaneseText,
                    sourceLang = "ja",
                    targetLang = "ko",
                    provider = com.nihongo.conversation.data.remote.deepl.TranslationProvider.MICROSOFT,
                    useCache = true,
                    fallbackChain = listOf(
                        com.nihongo.conversation.data.remote.deepl.TranslationProvider.DEEP_L,
                        com.nihongo.conversation.data.remote.deepl.TranslationProvider.ML_KIT
                    )
                )

                when (result) {
                    is com.nihongo.conversation.data.repository.TranslationResult.Success -> {
                        android.util.Log.d("ChatViewModel", "✓ Translation success (${result.provider}, ${result.elapsed}ms, cache: ${result.fromCache})")
                        _uiState.update {
                            it.copy(
                                userTranslations = (it.userTranslations.items + (messageId to result.translatedText)).toImmutableMap(),
                                userTranslationErrors = (it.userTranslationErrors.items - messageId).toImmutableMap()
                            )
                        }
                    }
                    is com.nihongo.conversation.data.repository.TranslationResult.Error -> {
                        android.util.Log.e("ChatViewModel", "✗ Translation failed: ${result.message}")

                        // Retry logic (max 3 attempts)
                        if (retryAttempt < 3) {
                            val delayMs = 1000L * (retryAttempt + 1)
                            android.util.Log.d("ChatViewModel", "Retrying after ${delayMs}ms (attempt ${retryAttempt + 1}/3)")
                            kotlinx.coroutines.delay(delayMs)
                            requestUserTranslation(messageId, japaneseText, retryAttempt + 1)
                        } else {
                            // Max retries reached
                            android.util.Log.e("ChatViewModel", "Max retries reached, showing error")
                            val errorMessage = when {
                                result.message.contains("quota", ignoreCase = true) -> "번역 한도 초과"
                                result.message.contains("network", ignoreCase = true) -> "네트워크 오류"
                                else -> "번역 실패"
                            }

                            _uiState.update {
                                it.copy(
                                    userTranslationErrors = (it.userTranslationErrors.items + (messageId to errorMessage)).toImmutableMap()
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Exception during user translation", e)

                if (retryAttempt < 3) {
                    val delayMs = 1000L * (retryAttempt + 1)
                    kotlinx.coroutines.delay(delayMs)
                    requestUserTranslation(messageId, japaneseText, retryAttempt + 1)
                } else {
                    _uiState.update {
                        it.copy(
                            userTranslationErrors = (it.userTranslationErrors.items + (messageId to "번역 실패 - 다시 시도하세요")).toImmutableMap()
                        )
                    }
                }
            }
        }
    }

    /**
     * Toggle user message translation expansion
     */
    fun toggleUserTranslation(messageId: Long) {
        _uiState.update { state ->
            val expanded = state.expandedUserTranslations.items
            state.copy(
                expandedUserTranslations = if (messageId in expanded) {
                    (expanded - messageId).toImmutableSet()
                } else {
                    (expanded + messageId).toImmutableSet()
                }
            )
        }
    }

    /**
     * Retry user message translation
     */
    fun retryUserTranslation(messageId: Long, japaneseText: String) {
        _uiState.update {
            it.copy(
                userTranslationErrors = (it.userTranslationErrors.items - messageId).toImmutableMap()
            )
        }
        requestUserTranslation(messageId, japaneseText, retryAttempt = 0)
    }

    /**
     * Toggle furigana for user message
     */
    fun toggleUserMessageFurigana(messageId: Long) {
        _uiState.update { state ->
            val currentSet = state.userMessagesWithFurigana.items
            state.copy(
                userMessagesWithFurigana = if (messageId in currentSet) {
                    (currentSet - messageId).toImmutableSet()
                } else {
                    (currentSet + messageId).toImmutableSet()
                }
            )
        }
    }

    /**
     * Request grammar feedback for user message
     */
    fun requestUserGrammarFeedback(messageId: Long, userText: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "=== User Grammar Feedback Request ===")
            android.util.Log.d("ChatViewModel", "MessageId: $messageId")
            android.util.Log.d("ChatViewModel", "Text: '$userText'")

            // Mark as analyzing
            _uiState.update {
                it.copy(
                    userGrammarAnalyzing = (it.userGrammarAnalyzing.items + messageId).toImmutableSet(),
                    userGrammarErrors = (it.userGrammarErrors.items - messageId).toImmutableMap()
                )
            }

            try {
                // Collect conversation context (last 6 messages)
                val context = _uiState.value.messages.items
                    .takeLast(6)
                    .map { it.content }

                val userLevel = 1 // Default beginner level (User model doesn't have level field)

                // Call grammar feedback repository
                val feedbackList = grammarFeedbackRepository.analyzeMessage(
                    userId = _uiState.value.user?.id ?: 0,
                    messageId = messageId,
                    userMessage = userText,
                    conversationContext = context,
                    userLevel = userLevel
                )

                android.util.Log.d("ChatViewModel", "✓ Grammar analysis complete: ${feedbackList.size} feedback items")

                _uiState.update {
                    it.copy(
                        userGrammarFeedback = (it.userGrammarFeedback.items + (messageId to feedbackList.toImmutableList())).toImmutableMap(),
                        userGrammarAnalyzing = (it.userGrammarAnalyzing.items - messageId).toImmutableSet()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "✗ Grammar analysis failed", e)

                _uiState.update {
                    it.copy(
                        userGrammarErrors = (it.userGrammarErrors.items + (messageId to "분석 실패: ${e.message}")).toImmutableMap(),
                        userGrammarAnalyzing = (it.userGrammarAnalyzing.items - messageId).toImmutableSet()
                    )
                }
            }
        }
    }

    /**
     * Toggle user message grammar feedback expansion
     */
    fun toggleUserGrammarFeedback(messageId: Long) {
        _uiState.update { state ->
            val expanded = state.expandedUserGrammarFeedback.items
            state.copy(
                expandedUserGrammarFeedback = if (messageId in expanded) {
                    (expanded - messageId).toImmutableSet()
                } else {
                    (expanded + messageId).toImmutableSet()
                }
            )
        }
    }

    /**
     * Retry user message grammar analysis
     */
    fun retryUserGrammarAnalysis(messageId: Long, userText: String) {
        _uiState.update {
            it.copy(
                userGrammarErrors = (it.userGrammarErrors.items - messageId).toImmutableMap()
            )
        }
        requestUserGrammarFeedback(messageId, userText)
    }

    // ========== Phase 5: Message Bookmarking & Sharing ==========

    /**
     * Check if a message is bookmarked
     */
    fun isMessageSaved(messageId: Long): Flow<Boolean> {
        return savedMessageRepository.isMessageSaved(currentUserId, messageId)
    }

    /**
     * Bookmark a message for later review
     */
    fun saveMessage(messageId: Long, userNote: String? = null) {
        viewModelScope.launch {
            // Find the message in current conversation
            val message = _uiState.value.messages.firstOrNull { it.id == messageId }
            if (message == null) {
                _uiState.update {
                    it.copy(errorMessage = "メッセージが見つかりません")
                }
                return@launch
            }

            val scenarioTitle = _uiState.value.scenario?.title ?: "不明なシナリオ"

            val result = savedMessageRepository.saveMessage(
                userId = currentUserId,
                message = message,
                scenarioTitle = scenarioTitle,
                userNote = userNote
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        errorMessage = null,
                        snackbarMessage = "メッセージを保存しました"  // "Message saved"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = "保存に失敗しました: ${error.message}",
                        snackbarMessage = null
                    )
                }
            }
        }
    }

    /**
     * Remove bookmark from a message
     */
    fun unsaveMessage(messageId: Long) {
        viewModelScope.launch {
            val result = savedMessageRepository.unsaveMessage(currentUserId, messageId)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        errorMessage = null,
                        snackbarMessage = "保存を解除しました"  // "Bookmark removed"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = "解除に失敗しました: ${error.message}",
                        snackbarMessage = null
                    )
                }
            }
        }
    }

    /**
     * Share message text via Android Share API
     * Returns the text to be shared
     */
    fun getShareText(message: Message): String {
        val scenario = _uiState.value.scenario
        val prefix = if (message.isUser) "🗣️ 私: " else "🤖 AI: "
        val scenarioInfo = scenario?.let { "\n\n📚 シナリオ: ${it.title}" } ?: ""

        return """
            $prefix${message.content}
            $scenarioInfo

            📱 日本語会話アプリで学習中
        """.trimIndent()
    }
}
