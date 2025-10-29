package com.nihongo.conversation.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimePeriod {
    WEEK, MONTH
}

data class StatsUiState(
    val timePeriod: TimePeriod = TimePeriod.WEEK,
    val weeklyStats: WeeklyStats? = null,
    val monthlyStats: WeeklyStats? = null,
    val scenarioProgress: List<ScenarioProgress> = emptyList(),
    val studyStreak: StudyStreak? = null,
    val totalStats: Triple<Int, Int, Int>? = null, // (conversations, messages, minutes)
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadAllStats()
    }

    private fun loadAllStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load weekly stats
                val weeklyStats = statsRepository.getWeeklyStats()

                // Load monthly stats
                val monthlyStats = statsRepository.getMonthlyStats()

                // Load scenario progress
                val scenarioProgress = statsRepository.getScenarioProgress()

                // Load study streak
                val studyStreak = statsRepository.getStudyStreak()

                // Load total stats
                val totalStats = statsRepository.getTotalStats()

                _uiState.update {
                    it.copy(
                        weeklyStats = weeklyStats,
                        monthlyStats = monthlyStats,
                        scenarioProgress = scenarioProgress,
                        studyStreak = studyStreak,
                        totalStats = totalStats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "統計データの読み込みに失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    fun setTimePeriod(period: TimePeriod) {
        _uiState.update { it.copy(timePeriod = period) }
    }

    fun refresh() {
        loadAllStats()
    }

    fun getCurrentPeriodStats(): WeeklyStats? {
        return when (_uiState.value.timePeriod) {
            TimePeriod.WEEK -> _uiState.value.weeklyStats
            TimePeriod.MONTH -> _uiState.value.monthlyStats
        }
    }
}
