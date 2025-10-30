package com.nihongo.conversation.presentation.pronunciation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.data.repository.PronunciationHistoryRepository
import com.nihongo.conversation.domain.model.PhraseStats
import com.nihongo.conversation.domain.model.PronunciationHistory
import com.nihongo.conversation.domain.model.PronunciationStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PronunciationHistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Overall statistics
    val stats: PronunciationStats = PronunciationStats(),

    // All practiced phrases grouped by text
    val allPhrases: List<PhraseStats> = emptyList(),

    // Sorting and filtering
    val sortBy: SortOption = SortOption.LATEST,
    val filterBy: FilterOption = FilterOption.ALL
)

enum class SortOption {
    LATEST,         // Most recently practiced
    ACCURACY_HIGH,  // Highest average accuracy
    ACCURACY_LOW,   // Lowest average accuracy (needs more practice)
    ATTEMPTS       // Most attempts
}

enum class FilterOption {
    ALL,           // All phrases
    WEAK,          // Average < 70%
    LEARNING,      // Average 70-89%
    MASTERED       // Average >= 90%
}

@HiltViewModel
class PronunciationHistoryViewModel @Inject constructor(
    private val pronunciationHistoryRepository: PronunciationHistoryRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PronunciationHistoryUiState())
    val uiState: StateFlow<PronunciationHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val userId = userSessionManager.getCurrentUserIdSync() ?: 1L

                // Load overall statistics
                val stats = pronunciationHistoryRepository.getStatistics(userId)

                // Load all phrases
                val allPhrases = pronunciationHistoryRepository.getPhrasesWithStats(userId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        allPhrases = sortAndFilterPhrases(allPhrases, it.sortBy, it.filterBy)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "履歴の読み込みに失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update {
            it.copy(
                sortBy = sortOption,
                allPhrases = sortAndFilterPhrases(it.allPhrases, sortOption, it.filterBy)
            )
        }
    }

    fun setFilterOption(filterOption: FilterOption) {
        _uiState.update {
            it.copy(
                filterBy = filterOption,
                allPhrases = sortAndFilterPhrases(it.allPhrases, it.sortBy, filterOption)
            )
        }
    }

    private fun sortAndFilterPhrases(
        phrases: List<PhraseStats>,
        sortBy: SortOption,
        filterBy: FilterOption
    ): List<PhraseStats> {
        // Filter
        val filtered = when (filterBy) {
            FilterOption.ALL -> phrases
            FilterOption.WEAK -> phrases.filter { it.averageScore < 70 }
            FilterOption.LEARNING -> phrases.filter { it.averageScore in 70.0..89.9 }
            FilterOption.MASTERED -> phrases.filter { it.averageScore >= 90 }
        }

        // Sort
        return when (sortBy) {
            SortOption.LATEST -> filtered.sortedByDescending { it.latestAttemptDate }
            SortOption.ACCURACY_HIGH -> filtered.sortedByDescending { it.averageScore }
            SortOption.ACCURACY_LOW -> filtered.sortedBy { it.averageScore }
            SortOption.ATTEMPTS -> filtered.sortedByDescending { it.attemptCount }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Format duration in ms to readable string
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)

        return when {
            hours > 0 -> "${hours}時間${minutes}分"
            minutes > 0 -> "${minutes}分${seconds}秒"
            else -> "${seconds}秒"
        }
    }

    /**
     * Get color for accuracy score
     */
    fun getAccuracyColor(score: Double): androidx.compose.ui.graphics.Color {
        return when {
            score >= 90 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
            score >= 70 -> androidx.compose.ui.graphics.Color(0xFFFFC107)
            else -> androidx.compose.ui.graphics.Color(0xFFF44336)
        }
    }
}
