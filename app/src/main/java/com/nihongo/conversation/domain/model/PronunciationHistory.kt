package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single pronunciation practice attempt
 * Tracks each practice session with scores and metadata
 */
@Entity(
    tableName = "pronunciation_history",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = VocabularyEntry::class,
            parentColumns = ["id"],
            childColumns = ["vocabularyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["messageId"]),
        Index(value = ["vocabularyId"]),
        Index(value = ["practicedAt"]),
        Index(value = ["userId", "practicedAt"]),
        Index(value = ["userId", "accuracyScore"])
    ]
)
data class PronunciationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // User who practiced
    val userId: Long,

    // Optional: Link to specific message (if practicing from chat)
    val messageId: Long? = null,

    // Optional: Link to vocabulary word (if practicing vocabulary)
    val vocabularyId: Long? = null,

    // Text that was supposed to be pronounced
    val expectedText: String,

    // What the speech recognition heard
    val recognizedText: String,

    // Accuracy score (0-100)
    val accuracyScore: Int,

    // Detailed word-by-word comparison (JSON serialized)
    val wordComparisonJson: String,

    // When this practice occurred
    val practicedAt: Long = System.currentTimeMillis(),

    // Duration of the practice attempt in milliseconds
    val durationMs: Long = 0,

    // Number of attempts for this specific text
    val attemptNumber: Int = 1,

    // Source of practice (CHAT, VOCABULARY, CUSTOM)
    val source: PracticeSource = PracticeSource.CHAT
)

enum class PracticeSource {
    CHAT,           // Practicing from a chat message
    VOCABULARY,     // Practicing vocabulary
    CUSTOM          // Custom text practice
}

/**
 * Aggregated statistics for a specific phrase across multiple attempts
 */
data class PhraseStats(
    val expectedText: String,
    val attemptCount: Int,
    val bestScore: Int,
    val averageScore: Double,
    val latestScore: Int,
    val latestAttemptDate: Long,
    val improvementRate: Double, // Percentage improvement from first to latest
    val messageId: Long? = null,
    val vocabularyId: Long? = null
)

/**
 * User's overall pronunciation statistics
 */
data class PronunciationStats(
    val totalAttempts: Int = 0,
    val uniquePhrases: Int = 0,
    val averageAccuracy: Double = 0.0,
    val bestAccuracy: Int = 0,
    val totalPracticeTimeMs: Long = 0,
    val improvementTrend: List<Pair<String, Int>> = emptyList(), // (date, avg score)
    val weakPhrases: List<PhraseStats> = emptyList(), // Phrases needing more practice
    val masteredPhrases: List<PhraseStats> = emptyList(), // Phrases with >90% accuracy
    val recentPractices: List<PronunciationHistory> = emptyList()
)
