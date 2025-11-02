package com.nihongo.conversation.presentation.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.data.repository.VocabularyRepository
import com.nihongo.conversation.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardReviewUiState(
    val cards: List<VocabularyEntry> = emptyList(),
    val currentCardIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionStats: SessionStats = SessionStats(),
    val isSessionComplete: Boolean = false,
    val reviewStartTime: Long = System.currentTimeMillis()
)

data class SessionStats(
    val totalCards: Int = 0,
    val reviewedCards: Int = 0,
    val correctCount: Int = 0,
    val qualitySum: Int = 0,
    val timeSpentMs: Long = 0
) {
    val averageQuality: Float
        get() = if (reviewedCards > 0) qualitySum.toFloat() / reviewedCards else 0f

    val accuracy: Float
        get() = if (reviewedCards > 0) correctCount.toFloat() / reviewedCards else 0f

    val progress: Float
        get() = if (totalCards > 0) reviewedCards.toFloat() / totalCards else 0f
}

@HiltViewModel
class FlashcardReviewViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardReviewUiState())
    val uiState: StateFlow<FlashcardReviewUiState> = _uiState.asStateFlow()

    private var cardStartTime: Long = System.currentTimeMillis()

    init {
        loadReviewSession()
    }

    /**
     * Load vocabulary entries for review session
     */
    fun loadReviewSession(config: ReviewSessionConfig = ReviewSessionConfig()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val userId = userSessionManager.getCurrentUserIdSync() ?: 1L
                val cards = vocabularyRepository.getReviewSession(userId, config)

                if (cards.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSessionComplete = true,
                            error = "복습할 카드가 없습니다"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            cards = cards,
                            currentCardIndex = 0,
                            isLoading = false,
                            isCardFlipped = false,
                            sessionStats = SessionStats(totalCards = cards.size),
                            reviewStartTime = System.currentTimeMillis()
                        )
                    }
                    cardStartTime = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "カードの読み込みに失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Flip the current card to reveal the answer
     */
    fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    /**
     * Submit a review for the current card and move to next
     */
    fun submitReview(quality: ReviewQuality) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.currentCardIndex >= state.cards.size) return@launch

            val currentCard = state.cards[state.currentCardIndex]
            val timeSpent = System.currentTimeMillis() - cardStartTime

            try {
                // Submit review to repository (SM-2 algorithm is applied)
                vocabularyRepository.submitReview(
                    vocabularyId = currentCard.id,
                    quality = quality,
                    timeSpentMs = timeSpent
                )

                // Update session stats
                val isCorrect = quality.value >= 3
                val updatedStats = state.sessionStats.copy(
                    reviewedCards = state.sessionStats.reviewedCards + 1,
                    correctCount = state.sessionStats.correctCount + if (isCorrect) 1 else 0,
                    qualitySum = state.sessionStats.qualitySum + quality.value,
                    timeSpentMs = state.sessionStats.timeSpentMs + timeSpent
                )

                // Check if session is complete
                val isComplete = state.currentCardIndex >= state.cards.size - 1

                _uiState.update {
                    it.copy(
                        currentCardIndex = if (!isComplete) it.currentCardIndex + 1 else it.currentCardIndex,
                        isCardFlipped = false,
                        sessionStats = updatedStats,
                        isSessionComplete = isComplete
                    )
                }

                // Reset card timer for next card
                if (!isComplete) {
                    cardStartTime = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "レビューの送信に失敗しました: ${e.message}")
                }
            }
        }
    }

    /**
     * Skip current card (marks as DIFFICULT)
     */
    fun skipCard() {
        submitReview(ReviewQuality.DIFFICULT)
    }

    /**
     * Go back to previous card (if possible)
     */
    fun previousCard() {
        _uiState.update {
            if (it.currentCardIndex > 0) {
                it.copy(
                    currentCardIndex = it.currentCardIndex - 1,
                    isCardFlipped = false
                )
            } else {
                it
            }
        }
    }

    /**
     * Restart the review session
     */
    fun restartSession() {
        loadReviewSession()
    }

    /**
     * Get current card
     */
    fun getCurrentCard(): VocabularyEntry? {
        val state = _uiState.value
        return if (state.currentCardIndex < state.cards.size) {
            state.cards[state.currentCardIndex]
        } else {
            null
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Format time duration to human readable string
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return when {
            minutes == 0 -> "${seconds}초"
            minutes < 60 -> "${minutes}분${remainingSeconds}초"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                "${hours}시간${remainingMinutes}분"
            }
        }
    }
}
