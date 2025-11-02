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
}
