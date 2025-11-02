package com.nihongo.conversation.presentation.scenario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.domain.model.Scenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScenarioUiState(
    val allScenarios: List<Scenario> = emptyList(),
    val scenarios: List<Scenario> = emptyList(),
    val selectedCategory: String? = null, // null = "전체"
    val searchQuery: String = "",  // Search query
    val selectedDifficulties: Set<Int> = emptySet(),  // Selected difficulty filters (1, 2, 3)
    val isLoading: Boolean = true,
    val favoriteScenarioIds: List<Long> = emptyList() // Track favorite scenario IDs
)

@HiltViewModel
class ScenarioViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScenarioUiState())
    val uiState: StateFlow<ScenarioUiState> = _uiState.asStateFlow()

    init {
        loadScenarios()
        loadFavorites()
    }

    private fun loadScenarios() {
        viewModelScope.launch {
            repository.getAllScenarios().collect { scenarios ->
                _uiState.value = _uiState.value.copy(
                    allScenarios = scenarios,
                    isLoading = false
                )
                applyFilters()  // Apply filters after scenarios are loaded
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val favoriteIds = profileRepository.getFavoriteScenarioIds()
            _uiState.value = _uiState.value.copy(favoriteScenarioIds = favoriteIds)
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    /**
     * Update search query with debounce
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    /**
     * Toggle difficulty filter
     */
    fun toggleDifficulty(difficulty: Int) {
        val newDifficulties = if (difficulty in _uiState.value.selectedDifficulties) {
            _uiState.value.selectedDifficulties - difficulty
        } else {
            _uiState.value.selectedDifficulties + difficulty
        }
        _uiState.value = _uiState.value.copy(selectedDifficulties = newDifficulties)
        applyFilters()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedDifficulties = emptySet()
        )
        applyFilters()
    }

    /**
     * Apply all filters (category, search, difficulty)
     */
    private fun applyFilters() {
        val allScenarios = _uiState.value.allScenarios
        val category = _uiState.value.selectedCategory
        val searchQuery = _uiState.value.searchQuery
        val difficulties = _uiState.value.selectedDifficulties

        val filtered = allScenarios
            .filter { scenario ->
                // Category filter
                filterByCategory(scenario, category)
            }
            .filter { scenario ->
                // Search filter
                if (searchQuery.isBlank()) {
                    true
                } else {
                    scenario.title.contains(searchQuery, ignoreCase = true) ||
                    scenario.description.contains(searchQuery, ignoreCase = true) ||
                    getCategoryLabel(scenario.category).contains(searchQuery, ignoreCase = true)
                }
            }
            .filter { scenario ->
                // Difficulty filter
                if (difficulties.isEmpty()) {
                    true
                } else {
                    scenario.difficulty in difficulties
                }
            }

        _uiState.value = _uiState.value.copy(scenarios = filtered)
    }

    private fun filterByCategory(scenario: Scenario, category: String?): Boolean {
        // Define main tab categories
        val mainCategories = setOf(
            "ENTERTAINMENT", "WORK", "DAILY_LIFE", "TRAVEL", "TECH", "ESPORTS", "JLPT_PRACTICE"
        )

        return when (category) {
            null -> true // "전체" - show all
            "FAVORITE" -> scenario.id in _uiState.value.favoriteScenarioIds // Favorites only
            "OTHER" -> scenario.category !in mainCategories // "기타" - all categories NOT in main tabs
            else -> scenario.category == category // Specific category
        }
    }

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
     * Toggle favorite status for a scenario
     */
    fun toggleFavorite(scenarioId: Long) {
        viewModelScope.launch {
            val isFavorite = profileRepository.toggleFavoriteScenario(scenarioId)

            // Reload favorites list
            loadFavorites()

            // Reapply filters (especially if showing favorites)
            applyFilters()
        }
    }

    fun deleteCustomScenario(scenarioId: Long) {
        viewModelScope.launch {
            repository.deleteScenario(scenarioId)
        }
    }

    /**
     * Create a custom scenario
     */
    fun createCustomScenario(
        title: String,
        description: String,
        category: String,
        difficulty: Int,
        emoji: String,
        systemPrompt: String
    ) {
        viewModelScope.launch {
            val slug = generateSlug(title)
            val scenario = Scenario(
                title = title,
                description = description,
                difficulty = difficulty,
                systemPrompt = systemPrompt,
                slug = slug,
                category = category,
                thumbnailEmoji = emoji,
                isCustom = true,
                promptVersion = 1
            )
            repository.createScenario(scenario)
        }
    }

    /**
     * Generate AI-powered system prompt
     */
    fun generateSystemPrompt(
        title: String,
        description: String,
        difficulty: Int,
        onGenerated: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val difficultyText = when (difficulty) {
                    1 -> "初級レベル（JLPT N5-N4相当）の簡単な表現"
                    2 -> "中級レベル（JLPT N3-N2相当）の自然な表現"
                    3 -> "上級レベル（JLPT N1相当）の丁寧で正確な表現"
                    else -> "適切なレベルの表現"
                }

                val generationPrompt = """
                    以下の情報を元に、日本語会話練習アプリのシステムプロンプトを生成してください。

                    シナリオタイトル: $title
                    シナリオ説明: $description
                    難易度: $difficultyText

                    システムプロンプトの要件:
                    1. AIの役割を明確に定義する
                    2. ユーザーとの会話スタイルを指定する
                    3. 使用する言葉遣いのレベルを指定する
                    4. 【重要】以下の制約を必ず含める:
                       「【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。」

                    200文字程度の簡潔なシステムプロンプトを日本語で作成してください。
                """.trimIndent()

                // Call Gemini API to generate prompt
                val generated = repository.generateSimpleText(generationPrompt)
                onGenerated(generated)
            } catch (e: Exception) {
                // Fallback to default prompt
                val defaultPrompt = generateFallbackPrompt(title, description, difficulty)
                onGenerated(defaultPrompt)
            }
        }
    }

    /**
     * Generate slug from title (for unique scenario identification)
     */
    private fun generateSlug(title: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitized = title
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣ぁ-んァ-ヶ一-龯]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(30)
        return "custom_${sanitized}_$timestamp"
    }

    /**
     * Generate fallback prompt when AI generation fails
     */
    private fun generateFallbackPrompt(title: String, description: String, difficulty: Int): String {
        val difficultyText = when (difficulty) {
            1 -> "初級レベルの簡単な表現を使って"
            2 -> "中級レベルの自然な表現を使って"
            3 -> "上級レベルの丁寧で正確な表現を使って"
            else -> ""
        }

        return """
            あなたは「$title」のシナリオでユーザーと会話します。

            状況: $description

            ${difficultyText}、自然な日本語で応答してください。

            【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
        """.trimIndent()
    }
}
