package com.nihongo.conversation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihongo.conversation.data.local.entity.GrammarFeedbackCacheEntity

/**
 * DAO for grammar feedback cache
 * Provides fast lookup for previously analyzed messages
 */
@Dao
interface GrammarFeedbackCacheDao {

    /**
     * Get cached feedback for a specific message text and user level
     * Returns null if not found or expired
     */
    @Query("SELECT * FROM grammar_feedback_cache WHERE messageText = :text AND userLevel = :level")
    suspend fun getCachedFeedback(text: String, level: Int): GrammarFeedbackCacheEntity?

    /**
     * Cache grammar feedback result
     * Replaces existing cache if messageText already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheFeedback(cache: GrammarFeedbackCacheEntity)

    /**
     * Delete expired cache entries
     * @param expiryTime Timestamp before which all entries should be deleted
     */
    @Query("DELETE FROM grammar_feedback_cache WHERE timestamp < :expiryTime")
    suspend fun deleteExpiredCache(expiryTime: Long): Int

    /**
     * Get total cache count (for statistics)
     */
    @Query("SELECT COUNT(*) FROM grammar_feedback_cache")
    suspend fun getCacheCount(): Int

    /**
     * Clear all cache (for settings)
     */
    @Query("DELETE FROM grammar_feedback_cache")
    suspend fun clearAllCache(): Int
}
