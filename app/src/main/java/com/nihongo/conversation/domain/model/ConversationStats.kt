package com.nihongo.conversation.domain.model

import androidx.room.DatabaseView

/**
 * Database view for pre-calculated conversation statistics
 * This view joins conversations with messages and aggregates data for better performance
 */
@DatabaseView(
    viewName = "conversation_stats",
    value = """
        SELECT
            c.id as conversationId,
            c.userId,
            c.scenarioId,
            c.createdAt,
            c.updatedAt,
            c.isCompleted,
            COUNT(m.id) as messageCount,
            SUM(CASE WHEN m.isUser = 1 THEN 1 ELSE 0 END) as userMessageCount,
            SUM(CASE WHEN m.isUser = 0 THEN 1 ELSE 0 END) as aiMessageCount,
            MAX(m.timestamp) as lastMessageTime,
            AVG(m.complexityScore) as avgComplexity,
            (c.updatedAt - c.createdAt) as duration
        FROM conversations c
        LEFT JOIN messages m ON c.id = m.conversationId
        GROUP BY c.id
    """
)
data class ConversationStats(
    val conversationId: Long,
    val userId: Long,
    val scenarioId: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isCompleted: Boolean,
    val messageCount: Int,
    val userMessageCount: Int,
    val aiMessageCount: Int,
    val lastMessageTime: Long?,
    val avgComplexity: Float?,
    val duration: Long
)
