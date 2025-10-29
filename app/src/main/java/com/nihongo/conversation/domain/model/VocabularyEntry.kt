package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a vocabulary word or phrase extracted from conversations
 */
@Entity(
    tableName = "vocabulary_entries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("word")]
)
data class VocabularyEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val word: String, // Japanese word/phrase
    val reading: String? = null, // Hiragana/Katakana reading
    val meaning: String, // Korean translation
    val exampleSentence: String? = null, // Example from conversation
    val sourceConversationId: Long? = null, // Where it was learned
    val difficulty: Int = 1, // 1-5 difficulty level
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long = System.currentTimeMillis(), // When to review next
    val reviewCount: Int = 0, // Total times reviewed
    val correctCount: Int = 0, // Times answered correctly
    val easeFactor: Float = 2.5f, // SM-2 ease factor (starts at 2.5)
    val interval: Int = 0, // Days until next review
    val isMastered: Boolean = false // Marked as mastered by user
)

/**
 * Represents a single review session for a vocabulary entry
 */
@Entity(
    tableName = "review_history",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntry::class,
            parentColumns = ["id"],
            childColumns = ["vocabularyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vocabularyId"), Index("reviewedAt")]
)
data class ReviewHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vocabularyId: Long,
    val reviewedAt: Long = System.currentTimeMillis(),
    val quality: Int, // 0-5 rating (SM-2 quality)
    val timeSpentMs: Long = 0 // Time spent on this review
)

/**
 * Rating quality for spaced repetition
 */
enum class ReviewQuality(val value: Int, val label: String) {
    BLACKOUT(0, "全く覚えていない"),        // Complete blackout
    INCORRECT(1, "間違えた"),               // Incorrect, but familiar
    DIFFICULT(2, "難しかった"),             // Correct with difficulty
    HESITANT(3, "少し迷った"),              // Correct with hesitation
    EASY(4, "簡単だった"),                  // Easy to recall
    PERFECT(5, "完璧！");                   // Perfect, instant recall

    companion object {
        fun fromValue(value: Int): ReviewQuality {
            return entries.find { it.value == value } ?: BLACKOUT
        }
    }
}

/**
 * Statistics for vocabulary learning
 */
data class VocabularyStats(
    val totalWords: Int,
    val masteredWords: Int,
    val dueForReview: Int,
    val newWords: Int,
    val reviewedToday: Int,
    val accuracyRate: Float // 0.0 - 1.0
)
