package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.CacheAnalytics
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheAnalyticsDao {
    @Query("""
        SELECT * FROM cache_analytics
        WHERE userId = :userId
        AND date = :date
        AND scenarioId = :scenarioId
        LIMIT 1
    """)
    suspend fun getAnalyticsForDay(userId: Long, scenarioId: Long, date: String): CacheAnalytics?

    @Query("""
        SELECT * FROM cache_analytics
        WHERE userId = :userId
        AND scenarioId = :scenarioId
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun getRecentAnalytics(userId: Long, scenarioId: Long, limit: Int = 30): List<CacheAnalytics>

    @Query("""
        SELECT * FROM cache_analytics
        WHERE userId = :userId
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun getAllUserAnalytics(userId: Long, limit: Int = 30): List<CacheAnalytics>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: CacheAnalytics): Long

    @Update
    suspend fun updateAnalytics(analytics: CacheAnalytics)

    @Query("""
        INSERT OR REPLACE INTO cache_analytics (
            id, date, userId, scenarioId, cacheHits, cacheMisses, apiCalls,
            averageSimilarityScore, averageResponseTime, userContinuationRate,
            tokensSaved, estimatedCostSaved, createdAt
        )
        SELECT
            COALESCE(id, 0),
            :date,
            :userId,
            :scenarioId,
            COALESCE(cacheHits, 0) + :cacheHits,
            COALESCE(cacheMisses, 0) + :cacheMisses,
            COALESCE(apiCalls, 0) + :apiCalls,
            :avgSimilarity,
            :avgResponseTime,
            :continuationRate,
            COALESCE(tokensSaved, 0) + :tokensSaved,
            COALESCE(estimatedCostSaved, 0) + :costSaved,
            COALESCE(createdAt, :timestamp)
        FROM (
            SELECT * FROM cache_analytics
            WHERE date = :date AND userId = :userId AND scenarioId = :scenarioId
            LIMIT 1
        )
    """)
    suspend fun incrementAnalytics(
        date: String,
        userId: Long,
        scenarioId: Long,
        cacheHits: Int = 0,
        cacheMisses: Int = 0,
        apiCalls: Int = 0,
        avgSimilarity: Float = 0f,
        avgResponseTime: Long = 0L,
        continuationRate: Float = 0f,
        tokensSaved: Long = 0L,
        costSaved: Float = 0f,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM cache_analytics WHERE date < :beforeDate")
    suspend fun deleteOldAnalytics(beforeDate: String)

    @Query("""
        SELECT SUM(cacheHits) as totalHits,
               SUM(cacheMisses) as totalMisses,
               SUM(apiCalls) as totalApiCalls
        FROM cache_analytics
        WHERE userId = :userId
    """)
    suspend fun getTotalStats(userId: Long): Map<String, Int>

    @Query("""
        SELECT AVG(CAST(cacheHits AS FLOAT) / (cacheHits + cacheMisses)) as hitRate
        FROM cache_analytics
        WHERE userId = :userId
        AND (cacheHits + cacheMisses) > 0
    """)
    suspend fun getAverageHitRate(userId: Long): Float
}
