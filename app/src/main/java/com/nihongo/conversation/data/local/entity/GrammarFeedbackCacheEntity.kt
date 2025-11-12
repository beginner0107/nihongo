package com.nihongo.conversation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cache entity for grammar feedback to avoid redundant Gemini API calls
 * Caches analysis results for 30 days
 */
@Entity(
    tableName = "grammar_feedback_cache",
    indices = [
        Index(value = ["messageText"]),
        Index(value = ["timestamp"])
    ]
)
data class GrammarFeedbackCacheEntity(
    @PrimaryKey
    val messageText: String, // The actual message text as primary key
    val feedbackJson: String, // JSON array of feedback items
    val userLevel: Int, // User level when analyzed (1=beginner, 2=intermediate, 3=advanced)
    val timestamp: Long = System.currentTimeMillis() // For cache expiry (30 days)
)
