package com.nihongo.conversation.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nihongo.conversation.domain.model.FeedbackType
import com.nihongo.conversation.domain.model.GrammarFeedback
import com.nihongo.conversation.domain.model.MistakePattern
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarFeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: GrammarFeedback): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbackList(feedbackList: List<GrammarFeedback>): List<Long>

    @Update
    suspend fun updateFeedback(feedback: GrammarFeedback)

    @Query("SELECT * FROM grammar_feedback WHERE id = :feedbackId")
    suspend fun getFeedbackById(feedbackId: Long): GrammarFeedback?

    @Query("SELECT * FROM grammar_feedback WHERE messageId = :messageId ORDER BY createdAt ASC")
    fun getFeedbackForMessage(messageId: Long): Flow<List<GrammarFeedback>>

    @Query("SELECT * FROM grammar_feedback WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentFeedback(userId: Long, limit: Int = 50): Flow<List<GrammarFeedback>>

    @Query("""
        SELECT * FROM grammar_feedback
        WHERE userId = :userId
        AND feedbackType = :feedbackType
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun getFeedbackByType(userId: Long, feedbackType: FeedbackType, limit: Int = 50): Flow<List<GrammarFeedback>>

    @Query("""
        SELECT * FROM grammar_feedback
        WHERE userId = :userId
        AND createdAt >= :startTime
        ORDER BY createdAt DESC
    """)
    fun getFeedbackSince(userId: Long, startTime: Long): Flow<List<GrammarFeedback>>

    @Query("""
        SELECT COUNT(*) FROM grammar_feedback
        WHERE userId = :userId
        AND createdAt >= :startTime
    """)
    suspend fun getFeedbackCount(userId: Long, startTime: Long): Int

    @Query("""
        SELECT COUNT(*) FROM grammar_feedback
        WHERE userId = :userId
        AND feedbackType = :feedbackType
        AND createdAt >= :startTime
    """)
    suspend fun getFeedbackCountByType(userId: Long, feedbackType: FeedbackType, startTime: Long): Int

    @Query("""
        SELECT grammarPattern, COUNT(*) as count, MAX(createdAt) as lastOccurrence
        FROM grammar_feedback
        WHERE userId = :userId
        AND grammarPattern IS NOT NULL
        AND createdAt >= :startTime
        GROUP BY grammarPattern
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getCommonMistakes(userId: Long, startTime: Long, limit: Int = 10): List<MistakePatternRaw>

    @Query("""
        SELECT * FROM grammar_feedback
        WHERE userId = :userId
        AND userAcknowledged = 0
        ORDER BY createdAt DESC
    """)
    fun getUnacknowledgedFeedback(userId: Long): Flow<List<GrammarFeedback>>

    @Query("""
        UPDATE grammar_feedback
        SET userAcknowledged = 1
        WHERE id = :feedbackId
    """)
    suspend fun markAsAcknowledged(feedbackId: Long)

    @Query("""
        UPDATE grammar_feedback
        SET userAppliedCorrection = 1
        WHERE id = :feedbackId
    """)
    suspend fun markAsApplied(feedbackId: Long)

    @Query("""
        SELECT COUNT(*) FROM grammar_feedback
        WHERE userId = :userId
        AND userAppliedCorrection = 1
        AND createdAt >= :startTime
    """)
    suspend fun getAppliedCorrectionsCount(userId: Long, startTime: Long): Int

    @Query("""
        SELECT
            feedbackType,
            COUNT(*) as count,
            AVG(CASE WHEN userAppliedCorrection = 1 THEN 1.0 ELSE 0.0 END) as applicationRate
        FROM grammar_feedback
        WHERE userId = :userId
        AND createdAt >= :startTime
        GROUP BY feedbackType
    """)
    suspend fun getFeedbackStatsByType(userId: Long, startTime: Long): List<FeedbackTypeStats>

    @Query("""
        SELECT DATE(createdAt / 1000, 'unixepoch') as date,
               COUNT(*) as count
        FROM grammar_feedback
        WHERE userId = :userId
        AND createdAt >= :startTime
        GROUP BY DATE(createdAt / 1000, 'unixepoch')
        ORDER BY date ASC
    """)
    suspend fun getDailyFeedbackCounts(userId: Long, startTime: Long): List<DailyFeedbackCount>

    @Query("DELETE FROM grammar_feedback WHERE userId = :userId")
    suspend fun deleteAllFeedbackForUser(userId: Long)

    @Query("DELETE FROM grammar_feedback WHERE messageId = :messageId")
    suspend fun deleteFeedbackForMessage(messageId: Long)
}

/**
 * Raw data class for common mistakes query result
 */
data class MistakePatternRaw(
    val grammarPattern: String,
    val count: Int,
    val lastOccurrence: Long
)

/**
 * Statistics by feedback type
 */
data class FeedbackTypeStats(
    val feedbackType: FeedbackType,
    val count: Int,
    val applicationRate: Double
)

/**
 * Daily feedback count for trend analysis
 */
data class DailyFeedbackCount(
    val date: String,
    val count: Int
)
