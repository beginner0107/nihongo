package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationById(id: Long): Flow<Conversation?>

    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationsByUser(userId: Long): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE userId = :userId AND isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedConversationsByUser(userId: Long): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE userId = :userId AND scenarioId = :scenarioId AND isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestActiveConversationByUserAndScenario(userId: Long, scenarioId: Long): Conversation?

    @Query("SELECT * FROM conversations WHERE userId = :userId AND scenarioId = :scenarioId AND isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedConversationsByUserAndScenario(userId: Long, scenarioId: Long): Flow<List<Conversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation): Long

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)
}
