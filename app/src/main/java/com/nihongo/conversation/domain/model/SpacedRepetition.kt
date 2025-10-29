package com.nihongo.conversation.domain.model

import kotlin.math.roundToInt

/**
 * Implements the SuperMemo 2 (SM-2) spaced repetition algorithm
 *
 * The SM-2 algorithm calculates optimal review intervals based on:
 * - Quality of recall (0-5 rating)
 * - Current ease factor (difficulty multiplier)
 * - Current interval (days since last review)
 */
object SpacedRepetitionAlgorithm {

    /**
     * Calculate the next review date and updated parameters based on review quality
     *
     * @param entry Current vocabulary entry
     * @param quality Review quality rating (0-5)
     * @return Updated vocabulary entry with new scheduling parameters
     */
    fun calculateNextReview(entry: VocabularyEntry, quality: ReviewQuality): VocabularyEntry {
        val q = quality.value

        // Calculate new ease factor
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        var newEaseFactor = entry.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))

        // Ease factor minimum is 1.3
        if (newEaseFactor < 1.3f) {
            newEaseFactor = 1.3f
        }

        val newInterval: Int
        val newNextReviewAt: Long

        when {
            // If quality < 3, reset interval and review again soon
            q < 3 -> {
                newInterval = 0
                newNextReviewAt = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes
            }
            // First review: 1 day
            entry.reviewCount == 0 -> {
                newInterval = 1
                newNextReviewAt = System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000L)
            }
            // Second review: 6 days
            entry.reviewCount == 1 -> {
                newInterval = 6
                newNextReviewAt = System.currentTimeMillis() + (6 * 24 * 60 * 60 * 1000L)
            }
            // Subsequent reviews: interval * ease factor
            else -> {
                newInterval = (entry.interval * newEaseFactor).roundToInt()
                newNextReviewAt = System.currentTimeMillis() + (newInterval * 24 * 60 * 60 * 1000L)
            }
        }

        val isCorrect = q >= 3

        return entry.copy(
            lastReviewedAt = System.currentTimeMillis(),
            nextReviewAt = newNextReviewAt,
            reviewCount = entry.reviewCount + 1,
            correctCount = if (isCorrect) entry.correctCount + 1 else entry.correctCount,
            easeFactor = newEaseFactor,
            interval = newInterval
        )
    }

    /**
     * Check if a vocabulary entry is due for review
     */
    fun isDueForReview(entry: VocabularyEntry): Boolean {
        return entry.nextReviewAt <= System.currentTimeMillis()
    }

    /**
     * Get the number of days until next review
     */
    fun getDaysUntilReview(entry: VocabularyEntry): Int {
        val millisUntilReview = entry.nextReviewAt - System.currentTimeMillis()
        val daysUntilReview = (millisUntilReview / (24 * 60 * 60 * 1000)).toInt()
        return maxOf(0, daysUntilReview)
    }

    /**
     * Calculate accuracy rate from review counts
     */
    fun calculateAccuracy(correctCount: Int, totalCount: Int): Float {
        if (totalCount == 0) return 0f
        return correctCount.toFloat() / totalCount
    }

    /**
     * Determine if a word should be considered "mastered"
     * Criteria: Reviewed at least 5 times with 90%+ accuracy and interval > 30 days
     */
    fun isMastered(entry: VocabularyEntry): Boolean {
        if (entry.reviewCount < 5) return false
        val accuracy = calculateAccuracy(entry.correctCount, entry.reviewCount)
        return accuracy >= 0.9f && entry.interval >= 30
    }
}

/**
 * Configuration for vocabulary review sessions
 */
data class ReviewSessionConfig(
    val maxNewWords: Int = 10,      // Maximum new words per session
    val maxReviewWords: Int = 20,   // Maximum review words per session
    val includeNew: Boolean = true,  // Include new words
    val includeDue: Boolean = true   // Include due reviews
)

/**
 * Result of a review session
 */
data class ReviewSessionResult(
    val wordsReviewed: Int,
    val correctAnswers: Int,
    val averageQuality: Float,
    val timeSpentMs: Long,
    val newWordsMastered: Int = 0
)
