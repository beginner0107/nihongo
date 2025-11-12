package com.nihongo.conversation.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.nihongo.conversation.domain.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    /**
     * Get all messages for a conversation (for streaming updates)
     * Use this for real-time message updates in active chat
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<Message>>

    /**
     * Get messages with pagination (for review/history screens)
     * Use this for large conversations to avoid loading all messages at once
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationPaged(conversationId: Long): PagingSource<Int, Message>

    /**
     * Get recent N messages for quick preview
     * Optimized with LIMIT for fast loading
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: Long, limit: Int = 20): List<Message>

    /**
     * Get message count for a conversation (cached by index)
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int

    /**
     * Get last message for a conversation (for preview)
     * Optimized with LIMIT 1
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: Long): Message?

    /**
     * Batch insert messages (optimized for bulk operations)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)

    /**
     * Get message count since a specific timestamp (for daily statistics)
     */
    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE conversationId IN (
            SELECT id FROM conversations WHERE userId = :userId
        ) AND timestamp >= :startTimestamp
    """)
    fun getMessageCountSince(userId: Long, startTimestamp: Long): Flow<Int>
}
