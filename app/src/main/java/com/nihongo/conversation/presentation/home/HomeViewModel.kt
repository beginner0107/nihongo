package com.nihongo.conversation.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.recommendation.ScenarioRecommendationEngine
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.data.repository.QuestRepository
import com.nihongo.conversation.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for simplified HomeScreen (Phase 11)
 * Phase 1 Completion: Added streak data support
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val questRepository: QuestRepository,
    private val statsRepository: StatsRepository,
    private val recommendationEngine: ScenarioRecommendationEngine
) : ViewModel() {

    private val currentUserId = 1L

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            // Load today's message count
            viewModelScope.launch {
                try {
                    conversationRepository.getTodayMessageCount(currentUserId)
                        .collect { count ->
                            _uiState.update { it.copy(todayMessageCount = count) }
                        }
                } catch (e: Exception) {
                    // Ignore - not critical
                }
            }

            // Load user points
            viewModelScope.launch {
                try {
                    questRepository.getUserPoints(currentUserId)
                        .collect { points ->
                            if (points != null) {
                                _uiState.update { it.copy(
                                    userLevel = points.level,
                                    totalPoints = points.totalPoints
                                ) }
                            }
                        }
                } catch (e: Exception) {
                    // Ignore - not critical
                }
            }

            // Phase 1 Completion: Load streak data
            viewModelScope.launch {
                try {
                    val streak = statsRepository.getStudyStreak()
                    _uiState.update { it.copy(
                        currentStreak = streak.currentStreak,
                        longestStreak = streak.longestStreak
                    ) }
                } catch (e: Exception) {
                    // Ignore - not critical
                }
            }

            // Load recommendation
            refreshRecommendation()

            // Set loading to false immediately
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshRecommendation() {
        viewModelScope.launch {
            try {
                // Get all scenarios from ConversationRepository
                val allScenarios = conversationRepository.getAllScenarios().first()

                // Get completed scenario IDs
                val completedIds = conversationRepository.getCompletedConversations(currentUserId)
                    .first()
                    .map { it.scenarioId }

                // Generate recommendation
                val recommendation = recommendationEngine.getRecommendation(
                    allScenarios = allScenarios,
                    user = null, // User profile not needed for now
                    completedScenarioIds = completedIds,
                    userLevel = _uiState.value.userLevel
                )

                _uiState.update { it.copy(todayRecommendation = recommendation) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "추천을 불러올 수 없습니다: ${e.message}") }
            }
        }
    }

    fun resumeLastConversation() {
        // TODO: Navigate to last conversation
        // This will be implemented when navigation is added
    }

    fun startRandomScenario() {
        viewModelScope.launch {
            try {
                val allScenarios = conversationRepository.getAllScenarios().first()
                val random = allScenarios.randomOrNull()
                // TODO: Navigate to random scenario
                // This will be implemented when navigation is added
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "랜덤 시나리오를 불러올 수 없습니다") }
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,  // Phase 1 Completion: Added longest streak
    val todayMessageCount: Int = 0,
    val dailyGoal: Int = 10,
    val userLevel: Int = 1,
    val totalPoints: Int = 0,
    val todayRecommendation: ScenarioRecommendation? = null,
    val error: String? = null
)
