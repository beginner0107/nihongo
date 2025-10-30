package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Types of grammar feedback
 */
enum class FeedbackType {
    GRAMMAR_ERROR,      // 文法間違い
    UNNATURAL,          // 不自然な表現
    BETTER_EXPRESSION,  // より良い表現
    CONVERSATION_FLOW,  // 会話の流れ
    POLITENESS_LEVEL    // 敬語レベル
}

/**
 * Severity level of the issue
 */
enum class FeedbackSeverity {
    INFO,      // 情報 (optional improvement)
    WARNING,   // 注意 (unnatural but understandable)
    ERROR      // 間違い (grammatically wrong)
}

/**
 * Entity for storing AI grammar feedback and corrections
 */
@Entity(
    tableName = "grammar_feedback",
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["messageId"]),
        Index(value = ["createdAt"]),
        Index(value = ["feedbackType"]),
        Index(value = ["severity"]),
        Index(value = ["userId", "feedbackType"]),
        Index(value = ["userId", "createdAt"])
    ]
)
data class GrammarFeedback(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Long,
    val messageId: Long,

    // Original text that user sent
    val originalText: String,

    // Corrected version (if applicable)
    val correctedText: String? = null,

    // Type of feedback
    val feedbackType: FeedbackType,

    // Severity level
    val severity: FeedbackSeverity,

    // Explanation in Korean for why this is wrong/unnatural
    val explanation: String,

    // Better alternative expression (if applicable)
    val betterExpression: String? = null,

    // Additional context or usage notes
    val additionalNotes: String? = null,

    // Pattern/rule being violated (e.g., "助詞の使い方", "敬語の使い方")
    val grammarPattern: String? = null,

    // User's reaction to feedback
    val userAcknowledged: Boolean = false,
    val userAppliedCorrection: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Statistics for tracking user's common mistakes
 */
data class MistakePattern(
    val grammarPattern: String,
    val count: Int,
    val lastOccurrence: Long,
    val improvementRate: Double // Percentage of times user didn't repeat the mistake
)

/**
 * Weekly progress summary
 */
data class FeedbackProgress(
    val weekStart: Long,
    val totalMessages: Int,
    val totalFeedback: Int,
    val grammarErrors: Int,
    val unnaturalExpressions: Int,
    val improvementCount: Int,
    val feedbackRate: Double // feedback per message
)
