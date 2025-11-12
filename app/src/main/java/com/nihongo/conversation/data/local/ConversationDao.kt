package com.nihongo.conversation.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.ConversationStats
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationById(id: Long): Flow<Conversation?>

    /**
     * Get all conversations for user with pagination support
     * Optimized with composite index on (userId, updatedAt)
     */
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationsByUser(userId: Long): Flow<List<Conversation>>

    /**
     * Get recent N conversations for quick preview
     */
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentConversations(userId: Long, limit: Int = 10): List<Conversation>

    /**
     * Get completed conversations with pagination
     * Optimized with composite index on (userId, scenarioId, isCompleted)
     */
    @Query("SELECT * FROM conversations WHERE userId = :userId AND isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedConversationsByUser(userId: Long): Flow<List<Conversation>>

    /**
     * Get active conversations (for quick access)
     * Leverages composite index for fast filtering
     */
    @Query("SELECT * FROM conversations WHERE userId = :userId AND isCompleted = 0 ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getActiveConversations(userId: Long, limit: Int = 5): List<Conversation>

    /**
     * Get all active conversations for dashboard statistics
     */
    @Query("SELECT * FROM conversations WHERE userId = :userId AND isCompleted = 0 ORDER BY updatedAt DESC")
    fun getActiveConversationsByUser(userId: Long): Flow<List<Conversation>>

    /**
     * Get latest active conversation by user and scenario
     * Uses composite index (userId, scenarioId, isCompleted) for optimal performance
     */
    @Query("SELECT * FROM conversations WHERE userId = :userId AND scenarioId = :scenarioId AND isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestActiveConversationByUserAndScenario(userId: Long, scenarioId: Long): Conversation?

    @Query("SELECT * FROM conversations WHERE userId = :userId AND scenarioId = :scenarioId AND isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedConversationsByUserAndScenario(userId: Long, scenarioId: Long): Flow<List<Conversation>>

    /**
     * Get conversation count by status (for statistics)
     * Fast count using index
     */
    @Query("SELECT COUNT(*) FROM conversations WHERE userId = :userId AND isCompleted = :isCompleted")
    suspend fun getConversationCount(userId: Long, isCompleted: Boolean): Int

    /**
     * Batch insert conversations (for data seeding/import)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<Conversation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation): Long

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: Long)

    // ========== Conversation Statistics View ==========

    /**
     * Get pre-calculated conversation statistics for a user
     * Uses database view for optimal performance
     */
    @Query("SELECT * FROM conversation_stats WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationStats(userId: Long): Flow<List<ConversationStats>>

    /**
     * Get statistics for a specific conversation
     * Instant lookup from pre-calculated view
     */
    @Query("SELECT * FROM conversation_stats WHERE conversationId = :conversationId")
    suspend fun getConversationStatsById(conversationId: Long): ConversationStats?

    /**
     * Get statistics for completed conversations only
     */
    @Query("SELECT * FROM conversation_stats WHERE userId = :userId AND isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedConversationStats(userId: Long): Flow<List<ConversationStats>>
}
