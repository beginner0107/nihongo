package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.PronunciationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PronunciationHistoryDao {

    /**
     * Save a pronunciation practice attempt
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPractice(practice: PronunciationHistory): Long

    /**
     * Get all pronunciation practices for a user
     */
    @Query("SELECT * FROM pronunciation_history WHERE userId = :userId ORDER BY practicedAt DESC")
    fun getAllPractices(userId: Long): Flow<List<PronunciationHistory>>

    /**
     * Get pronunciation practices for a specific message
     */
    @Query("SELECT * FROM pronunciation_history WHERE messageId = :messageId ORDER BY practicedAt DESC")
    fun getPracticesByMessage(messageId: Long): Flow<List<PronunciationHistory>>

    /**
     * Get pronunciation practices for a specific vocabulary word
     */
    @Query("SELECT * FROM pronunciation_history WHERE vocabularyId = :vocabularyId ORDER BY practicedAt DESC")
    fun getPracticesByVocabulary(vocabularyId: Long): Flow<List<PronunciationHistory>>

    /**
     * Get all practices for a specific text
     */
    @Query("""
        SELECT * FROM pronunciation_history
        WHERE userId = :userId AND expectedText = :expectedText
        ORDER BY practicedAt DESC
    """)
    fun getPracticesByText(userId: Long, expectedText: String): Flow<List<PronunciationHistory>>

    /**
     * Get best score for a specific text
     */
    @Query("""
        SELECT MAX(accuracyScore) FROM pronunciation_history
        WHERE userId = :userId AND expectedText = :expectedText
    """)
    suspend fun getBestScore(userId: Long, expectedText: String): Int?

    /**
     * Get latest practice for a specific text
     */
    @Query("""
        SELECT * FROM pronunciation_history
        WHERE userId = :userId AND expectedText = :expectedText
        ORDER BY practicedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestPractice(userId: Long, expectedText: String): PronunciationHistory?

    /**
     * Get total number of practices for user
     */
    @Query("SELECT COUNT(*) FROM pronunciation_history WHERE userId = :userId")
    suspend fun getTotalPracticeCount(userId: Long): Int

    /**
     * Get number of unique phrases practiced
     */
    @Query("SELECT COUNT(DISTINCT expectedText) FROM pronunciation_history WHERE userId = :userId")
    suspend fun getUniquePhraseCount(userId: Long): Int

    /**
     * Get average accuracy score for user
     */
    @Query("SELECT AVG(accuracyScore) FROM pronunciation_history WHERE userId = :userId")
    suspend fun getAverageAccuracy(userId: Long): Double?

    /**
     * Get best accuracy score for user
     */
    @Query("SELECT MAX(accuracyScore) FROM pronunciation_history WHERE userId = :userId")
    suspend fun getBestAccuracy(userId: Long): Int?

    /**
     * Get total practice time for user
     */
    @Query("SELECT SUM(durationMs) FROM pronunciation_history WHERE userId = :userId")
    suspend fun getTotalPracticeTime(userId: Long): Long?

    /**
     * Get practices within a date range
     */
    @Query("""
        SELECT * FROM pronunciation_history
        WHERE userId = :userId AND practicedAt BETWEEN :startTime AND :endTime
        ORDER BY practicedAt DESC
    """)
    fun getPracticesInRange(userId: Long, startTime: Long, endTime: Long): Flow<List<PronunciationHistory>>

    /**
     * Get average accuracy by date for the last N days
     */
    @Query("""
        SELECT DATE(practicedAt / 1000, 'unixepoch') as date, AVG(accuracyScore) as avgScore
        FROM pronunciation_history
        WHERE userId = :userId AND practicedAt >= :startTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getAccuracyTrendRaw(userId: Long, startTime: Long): List<AccuracyByDate>

    /**
     * Get all unique phrases with their statistics
     */
    @Query("""
        SELECT
            expectedText,
            COUNT(*) as attemptCount,
            MAX(accuracyScore) as bestScore,
            AVG(accuracyScore) as avgScore,
            MAX(practicedAt) as latestAttempt
        FROM pronunciation_history
        WHERE userId = :userId
        GROUP BY expectedText
        ORDER BY latestAttempt DESC
    """)
    suspend fun getPhrasesWithStats(userId: Long): List<PhraseStatsRaw>

    /**
     * Get weak phrases (average score < 70%)
     */
    @Query("""
        SELECT
            expectedText,
            COUNT(*) as attemptCount,
            MAX(accuracyScore) as bestScore,
            AVG(accuracyScore) as avgScore,
            MAX(practicedAt) as latestAttempt
        FROM pronunciation_history
        WHERE userId = :userId
        GROUP BY expectedText
        HAVING AVG(accuracyScore) < 70
        ORDER BY avgScore ASC
        LIMIT :limit
    """)
    suspend fun getWeakPhrases(userId: Long, limit: Int = 10): List<PhraseStatsRaw>

    /**
     * Get mastered phrases (average score >= 90%)
     */
    @Query("""
        SELECT
            expectedText,
            COUNT(*) as attemptCount,
            MAX(accuracyScore) as bestScore,
            AVG(accuracyScore) as avgScore,
            MAX(practicedAt) as latestAttempt
        FROM pronunciation_history
        WHERE userId = :userId
        GROUP BY expectedText
        HAVING AVG(accuracyScore) >= 90
        ORDER BY avgScore DESC
        LIMIT :limit
    """)
    suspend fun getMasteredPhrases(userId: Long, limit: Int = 10): List<PhraseStatsRaw>

    /**
     * Get recent practices
     */
    @Query("""
        SELECT * FROM pronunciation_history
        WHERE userId = :userId
        ORDER BY practicedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentPractices(userId: Long, limit: Int = 20): List<PronunciationHistory>

    /**
     * Delete all practices for a user
     */
    @Query("DELETE FROM pronunciation_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    /**
     * Delete practices older than a specific date
     */
    @Query("DELETE FROM pronunciation_history WHERE userId = :userId AND practicedAt < :beforeTime")
    suspend fun deleteOldPractices(userId: Long, beforeTime: Long)
}

/**
 * Data class for raw query results
 */
data class PhraseStatsRaw(
    val expectedText: String,
    val attemptCount: Int,
    val bestScore: Int,
    val avgScore: Double,
    val latestAttempt: Long
)

data class AccuracyByDate(
    val date: String,
    val avgScore: Double
)
