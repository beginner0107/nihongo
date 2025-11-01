package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.ConversationPattern
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationPatternDao {
    @Query("SELECT * FROM conversation_patterns WHERE scenarioId = :scenarioId AND difficultyLevel = :difficultyLevel ORDER BY usageCount DESC")
    suspend fun getPatternsForScenario(scenarioId: Long, difficultyLevel: Int): List<ConversationPattern>

    @Query("SELECT * FROM conversation_patterns WHERE scenarioId = :scenarioId ORDER BY usageCount DESC")
    suspend fun getAllPatternsForScenario(scenarioId: Long): List<ConversationPattern>

    @Query("SELECT * FROM conversation_patterns WHERE id = :patternId")
    suspend fun getPattern(patternId: Long): ConversationPattern?

    @Query("SELECT * FROM conversation_patterns WHERE category = :category AND scenarioId = :scenarioId")
    suspend fun getPatternsByCategory(category: String, scenarioId: Long): List<ConversationPattern>

    @Query("""
        SELECT * FROM conversation_patterns
        WHERE scenarioId = :scenarioId
        AND difficultyLevel = :difficultyLevel
        AND (conversationTurn = 0 OR conversationTurn = :turn)
        ORDER BY usageCount DESC
        LIMIT :limit
    """)
    suspend fun getTopPatterns(
        scenarioId: Long,
        difficultyLevel: Int,
        turn: Int,
        limit: Int = 50
    ): List<ConversationPattern>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: ConversationPattern): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatterns(patterns: List<ConversationPattern>): List<Long>

    @Update
    suspend fun updatePattern(pattern: ConversationPattern)

    @Query("""
        UPDATE conversation_patterns
        SET usageCount = usageCount + 1,
            lastUsedTimestamp = :timestamp,
            averageSimilarity = (averageSimilarity * usageCount + :similarity) / (usageCount + 1)
        WHERE id = :patternId
    """)
    suspend fun incrementUsage(patternId: Long, similarity: Float, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversation_patterns WHERE id = :patternId")
    suspend fun deletePattern(patternId: Long)

    @Query("DELETE FROM conversation_patterns WHERE scenarioId = :scenarioId")
    suspend fun deletePatternsForScenario(scenarioId: Long)

    @Query("SELECT COUNT(*) FROM conversation_patterns")
    suspend fun getPatternCount(): Int

    @Query("SELECT COUNT(*) FROM conversation_patterns WHERE scenarioId = :scenarioId")
    suspend fun getPatternCountForScenario(scenarioId: Long): Int

    // Analytics queries
    @Query("""
        SELECT * FROM conversation_patterns
        WHERE scenarioId = :scenarioId
        ORDER BY usageCount DESC
        LIMIT :limit
    """)
    suspend fun getMostUsedPatterns(scenarioId: Long, limit: Int = 20): List<ConversationPattern>

    @Query("""
        SELECT AVG(usageCount) FROM conversation_patterns
        WHERE scenarioId = :scenarioId
    """)
    suspend fun getAverageUsageCount(scenarioId: Long): Float
}
