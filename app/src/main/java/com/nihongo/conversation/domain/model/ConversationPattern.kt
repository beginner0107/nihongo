package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a common user input pattern for caching AI responses
 * Reduces API calls by matching similar user inputs to pre-generated responses
 */
@Entity(tableName = "conversation_patterns")
data class ConversationPattern(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Pattern metadata
    val pattern: String, // Normalized user input pattern (e.g., "注文したい", "おすすめは")
    val scenarioId: Long, // Which scenario this pattern belongs to
    val difficultyLevel: Int, // 1=Beginner, 2=Intermediate, 3=Advanced
    val category: String, // e.g., "greeting", "ordering", "asking_question"

    // Context information
    val conversationTurn: Int = 0, // Which turn in conversation (0=any, 1=first, 2=second, etc)
    val keywords: String = "", // Comma-separated keywords for matching (e.g., "注文,order,ラーメン")

    // Usage statistics
    val usageCount: Long = 0, // How many times this pattern has been matched
    val lastUsedTimestamp: Long = System.currentTimeMillis(),

    // Quality metrics
    val successRate: Float = 1.0f, // How often users continue conversation after this response
    val averageSimilarity: Float = 0.0f, // Average similarity score when matched

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
