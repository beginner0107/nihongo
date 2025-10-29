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
    val scenarios: List<Scenario> = emptyList(),
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
                _uiState.value = ScenarioUiState(
                    scenarios = scenarios,
                    isLoading = false
                )
            }
        }
    }
}
