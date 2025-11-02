package com.nihongo.conversation.presentation.scenario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.repository.ConversationRepository
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
    val isLoading: Boolean = true
)

@HiltViewModel
class ScenarioViewModel @Inject constructor(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScenarioUiState())
    val uiState: StateFlow<ScenarioUiState> = _uiState.asStateFlow()

    init {
        loadScenarios()
    }

    private fun loadScenarios() {
        viewModelScope.launch {
            repository.getAllScenarios().collect { scenarios ->
                _uiState.value = _uiState.value.copy(
                    allScenarios = scenarios,
                    scenarios = filterScenarios(scenarios, _uiState.value.selectedCategory),
                    isLoading = false
                )
            }
        }
    }

    fun selectCategory(category: String?) {
        val filtered = filterScenarios(_uiState.value.allScenarios, category)
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            scenarios = filtered
        )
    }

    private fun filterScenarios(scenarios: List<Scenario>, category: String?): List<Scenario> {
        // Define main tab categories
        val mainCategories = setOf(
            "ENTERTAINMENT", "WORK", "DAILY_LIFE", "TRAVEL", "TECH", "ESPORTS", "JLPT_PRACTICE"
        )

        return when (category) {
            null -> scenarios // "전체" - show all
            "OTHER" -> scenarios.filter {
                // "기타" - all categories NOT in main tabs
                it.category !in mainCategories
            }
            else -> scenarios.filter { it.category == category } // Specific category
        }
    }

    fun deleteCustomScenario(scenarioId: Long) {
        viewModelScope.launch {
            repository.deleteScenario(scenarioId)
        }
    }
}
