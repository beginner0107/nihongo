package com.nihongo.conversation.presentation.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.repository.QuestRepository
import com.nihongo.conversation.domain.model.Quest
import com.nihongo.conversation.domain.model.UserPoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Quest/Mission system
 */
@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questRepository: QuestRepository
) : ViewModel() {

    private val currentUserId = 1L // TODO: Get from UserManager

    private val _uiState = MutableStateFlow(QuestUiState())
    val uiState: StateFlow<QuestUiState> = _uiState.asStateFlow()

    init {
        loadQuests()
        loadUserPoints()
        generateDailyQuestsIfNeeded()
    }

    private fun loadQuests() {
        viewModelScope.launch {
            questRepository.getActiveQuests(currentUserId)
                .catch { exception ->
                    _uiState.update { it.copy(error = exception.message) }
                }
                .collect { quests ->
                    _uiState.update { it.copy(
                        quests = quests,
                        isLoading = false
                    ) }
                }
        }
    }

    private fun loadUserPoints() {
        viewModelScope.launch {
            questRepository.getUserPoints(currentUserId)
                .catch { exception ->
                    _uiState.update { it.copy(error = exception.message) }
                }
                .collect { points ->
                    _uiState.update { it.copy(userPoints = points) }
                }
        }
    }

    private fun generateDailyQuestsIfNeeded() {
        viewModelScope.launch {
            try {
                questRepository.generateDailyQuests(currentUserId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateQuestProgress(questId: Long, value: Int) {
        viewModelScope.launch {
            try {
                questRepository.updateQuestProgress(questId, value)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun completeQuest(questId: Long) {
        viewModelScope.launch {
            try {
                val rewardPoints = questRepository.completeQuest(questId)
                _uiState.update { it.copy(
                    showQuestCompletedDialog = true,
                    lastCompletedQuestReward = rewardPoints
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissQuestCompletedDialog() {
        _uiState.update { it.copy(
            showQuestCompletedDialog = false,
            lastCompletedQuestReward = 0
        ) }
    }

    fun refreshQuests() {
        _uiState.update { it.copy(isLoading = true) }
        loadQuests()
        loadUserPoints()
    }
}

data class QuestUiState(
    val quests: List<Quest> = emptyList(),
    val userPoints: UserPoints? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showQuestCompletedDialog: Boolean = false,
    val lastCompletedQuestReward: Int = 0
)
