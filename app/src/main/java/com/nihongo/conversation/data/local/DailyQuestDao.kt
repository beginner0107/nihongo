package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.data.local.entity.DailyQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestDao {
    /**
     * Get active quests for a user (not expired, ordered by completion status and reward)
     */
    @Query("""
        SELECT * FROM daily_quests
        WHERE userId = :userId AND expiresAt > :now
        ORDER BY isCompleted ASC, rewardPoints DESC
    """)
    fun getActiveQuests(userId: Long, now: Long = System.currentTimeMillis()): Flow<List<DailyQuestEntity>>

    /**
     * Get completed quests for a user (last 10)
     */
    @Query("""
        SELECT * FROM daily_quests
        WHERE userId = :userId AND isCompleted = 1
        ORDER BY completedAt DESC
        LIMIT 10
    """)
    fun getCompletedQuests(userId: Long): Flow<List<DailyQuestEntity>>

    /**
     * Get a specific quest by ID
     */
    @Query("SELECT * FROM daily_quests WHERE id = :questId")
    suspend fun getQuestById(questId: Long): DailyQuestEntity?

    /**
     * Insert a new quest
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: DailyQuestEntity): Long

    /**
     * Insert multiple quests (for daily generation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<DailyQuestEntity>)

    /**
     * Update quest progress value
     */
    @Query("UPDATE daily_quests SET currentValue = :value WHERE id = :questId")
    suspend fun updateQuestProgress(questId: Long, value: Int)

    /**
     * Mark quest as completed
     */
    @Query("""
        UPDATE daily_quests
        SET isCompleted = 1, completedAt = :completedAt
        WHERE id = :questId
    """)
    suspend fun completeQuest(questId: Long, completedAt: Long = System.currentTimeMillis())

    /**
     * Update entire quest entity
     */
    @Update
    suspend fun updateQuest(quest: DailyQuestEntity)

    /**
     * Delete expired quests (cleanup)
     */
    @Query("DELETE FROM daily_quests WHERE expiresAt < :now")
    suspend fun deleteExpiredQuests(now: Long = System.currentTimeMillis())

    /**
     * Delete all quests for a user (for testing/reset)
     */
    @Query("DELETE FROM daily_quests WHERE userId = :userId")
    suspend fun deleteUserQuests(userId: Long)
}
