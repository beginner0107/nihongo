package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.CachedResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedResponseDao {
    @Query("SELECT * FROM cached_responses WHERE patternId = :patternId")
    suspend fun getResponsesForPattern(patternId: Long): List<CachedResponse>

    @Query("""
        SELECT * FROM cached_responses
        WHERE patternId = :patternId
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getRandomResponse(patternId: Long): CachedResponse?

    @Query("""
        SELECT * FROM cached_responses
        WHERE patternId = :patternId
        AND usageCount < (
            SELECT AVG(usageCount) FROM cached_responses WHERE patternId = :patternId
        )
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getLeastUsedResponse(patternId: Long): CachedResponse?

    @Query("SELECT * FROM cached_responses WHERE id = :responseId")
    suspend fun getResponse(responseId: Long): CachedResponse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: CachedResponse): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponses(responses: List<CachedResponse>)

    @Update
    suspend fun updateResponse(response: CachedResponse)

    @Query("""
        UPDATE cached_responses
        SET usageCount = usageCount + 1,
            lastUsedTimestamp = :timestamp
        WHERE id = :responseId
    """)
    suspend fun incrementUsage(responseId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE cached_responses
        SET userSatisfactionScore = (userSatisfactionScore * usageCount + :score) / (usageCount + 1)
        WHERE id = :responseId
    """)
    suspend fun updateSatisfactionScore(responseId: Long, score: Float)

    @Query("DELETE FROM cached_responses WHERE id = :responseId")
    suspend fun deleteResponse(responseId: Long)

    @Query("DELETE FROM cached_responses WHERE patternId = :patternId")
    suspend fun deleteResponsesForPattern(patternId: Long)

    @Query("SELECT COUNT(*) FROM cached_responses")
    suspend fun getResponseCount(): Int

    @Query("SELECT COUNT(*) FROM cached_responses WHERE patternId = :patternId")
    suspend fun getResponseCountForPattern(patternId: Long): Int

    @Query("""
        SELECT * FROM cached_responses
        WHERE patternId = :patternId
        ORDER BY userSatisfactionScore DESC
        LIMIT :limit
    """)
    suspend fun getTopRatedResponses(patternId: Long, limit: Int = 10): List<CachedResponse>
}
