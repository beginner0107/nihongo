package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.CardDifficulty
import com.nihongo.conversation.domain.model.SentenceCard
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceCardDao {

    // ========== CREATE ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: SentenceCard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<SentenceCard>)

    // ========== READ ==========

    @Query("SELECT * FROM sentence_cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): SentenceCard?

    @Query("SELECT * FROM sentence_cards WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllCards(userId: Long): Flow<List<SentenceCard>>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId AND nextReviewDate <= :currentTime
        ORDER BY nextReviewDate ASC
        LIMIT :limit
    """)
    suspend fun getDueCards(userId: Long, currentTime: Long, limit: Int = 20): List<SentenceCard>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId AND repetitions = 0
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getNewCards(userId: Long, limit: Int = 10): List<SentenceCard>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId AND conversationId = :conversationId
        ORDER BY createdAt DESC
    """)
    fun getCardsFromConversation(userId: Long, conversationId: Long): Flow<List<SentenceCard>>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId AND pattern = :pattern
        ORDER BY createdAt DESC
    """)
    fun getCardsByPattern(userId: Long, pattern: String): Flow<List<SentenceCard>>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId AND difficulty = :difficulty
        ORDER BY nextReviewDate ASC
    """)
    fun getCardsByDifficulty(userId: Long, difficulty: CardDifficulty): Flow<List<SentenceCard>>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId AND tags LIKE '%' || :tag || '%'
        ORDER BY createdAt DESC
    """)
    fun getCardsByTag(userId: Long, tag: String): Flow<List<SentenceCard>>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId
        AND (sentence LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchCards(userId: Long, query: String): Flow<List<SentenceCard>>

    // ========== UPDATE ==========

    @Update
    suspend fun update(card: SentenceCard)

    @Query("""
        UPDATE sentence_cards
        SET repetitions = :repetitions,
            easeFactor = :easeFactor,
            interval = :interval,
            nextReviewDate = :nextReviewDate,
            correctCount = :correctCount,
            incorrectCount = :incorrectCount,
            lastReviewedAt = :reviewedAt
        WHERE id = :cardId
    """)
    suspend fun updateReviewData(
        cardId: Long,
        repetitions: Int,
        easeFactor: Float,
        interval: Int,
        nextReviewDate: Long,
        correctCount: Int,
        incorrectCount: Int,
        reviewedAt: Long
    )

    @Query("""
        UPDATE sentence_cards
        SET hasCompletedReading = :completed
        WHERE id = :cardId
    """)
    suspend fun markReadingCompleted(cardId: Long, completed: Boolean)

    @Query("""
        UPDATE sentence_cards
        SET hasCompletedListening = :completed
        WHERE id = :cardId
    """)
    suspend fun markListeningCompleted(cardId: Long, completed: Boolean)

    @Query("""
        UPDATE sentence_cards
        SET hasCompletedFillInBlank = :completed
        WHERE id = :cardId
    """)
    suspend fun markFillInBlankCompleted(cardId: Long, completed: Boolean)

    @Query("""
        UPDATE sentence_cards
        SET hasCompletedSpeaking = :completed
        WHERE id = :cardId
    """)
    suspend fun markSpeakingCompleted(cardId: Long, completed: Boolean)

    @Query("""
        UPDATE sentence_cards
        SET audioUrl = :audioUrl, hasAudio = 1
        WHERE id = :cardId
    """)
    suspend fun updateAudioUrl(cardId: Long, audioUrl: String)

    // ========== DELETE ==========

    @Delete
    suspend fun delete(card: SentenceCard)

    @Query("DELETE FROM sentence_cards WHERE id = :cardId")
    suspend fun deleteById(cardId: Long)

    @Query("DELETE FROM sentence_cards WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    // ========== STATISTICS ==========

    @Query("SELECT COUNT(*) FROM sentence_cards WHERE userId = :userId")
    suspend fun getTotalCardCount(userId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM sentence_cards
        WHERE userId = :userId AND nextReviewDate <= :currentTime
    """)
    suspend fun getDueCardCount(userId: Long, currentTime: Long): Int

    @Query("""
        SELECT COUNT(*) FROM sentence_cards
        WHERE userId = :userId AND repetitions = 0
    """)
    suspend fun getNewCardCount(userId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM sentence_cards
        WHERE userId = :userId
        AND lastReviewedAt >= :startOfDay
        AND lastReviewedAt < :endOfDay
    """)
    suspend fun getReviewedTodayCount(userId: Long, startOfDay: Long, endOfDay: Long): Int

    @Query("""
        SELECT AVG(CAST(correctCount AS FLOAT) / NULLIF(correctCount + incorrectCount, 0) * 100)
        FROM sentence_cards
        WHERE userId = :userId AND (correctCount + incorrectCount) > 0
    """)
    suspend fun getAverageAccuracy(userId: Long): Float?

    @Query("""
        SELECT difficulty, COUNT(*) as count
        FROM sentence_cards
        WHERE userId = :userId
        GROUP BY difficulty
    """)
    suspend fun getCardCountByDifficultyRaw(userId: Long): List<DifficultyCount>

    @Query("""
        SELECT pattern, COUNT(*) as count
        FROM sentence_cards
        WHERE userId = :userId AND pattern IS NOT NULL
        GROUP BY pattern
        ORDER BY count DESC
    """)
    suspend fun getCardCountByPattern(userId: Long): List<PatternCount>

    @Query("""
        SELECT pattern,
               AVG(CAST(correctCount AS FLOAT) / NULLIF(correctCount + incorrectCount, 0) * 100) as accuracy
        FROM sentence_cards
        WHERE userId = :userId
          AND pattern IS NOT NULL
          AND (correctCount + incorrectCount) > 0
        GROUP BY pattern
        HAVING COUNT(*) >= 3
        ORDER BY accuracy DESC
        LIMIT :limit
    """)
    suspend fun getStrongestPatterns(userId: Long, limit: Int = 5): List<PatternAccuracy>

    @Query("""
        SELECT pattern,
               AVG(CAST(correctCount AS FLOAT) / NULLIF(correctCount + incorrectCount, 0) * 100) as accuracy
        FROM sentence_cards
        WHERE userId = :userId
          AND pattern IS NOT NULL
          AND (correctCount + incorrectCount) > 0
        GROUP BY pattern
        HAVING COUNT(*) >= 3
        ORDER BY accuracy ASC
        LIMIT :limit
    """)
    suspend fun getWeakestPatterns(userId: Long, limit: Int = 5): List<PatternAccuracy>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId
          AND (correctCount + incorrectCount) > 0
        ORDER BY (CAST(incorrectCount AS FLOAT) / (correctCount + incorrectCount)) DESC
        LIMIT :limit
    """)
    suspend fun getMostDifficultCards(userId: Long, limit: Int = 10): List<SentenceCard>

    @Query("""
        SELECT * FROM sentence_cards
        WHERE userId = :userId
        ORDER BY lastReviewedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentlyReviewedCards(userId: Long, limit: Int = 10): List<SentenceCard>

    @Query("""
        SELECT DISTINCT pattern FROM sentence_cards
        WHERE userId = :userId AND pattern IS NOT NULL
        ORDER BY pattern
    """)
    suspend fun getAllPatterns(userId: Long): List<String>

    @Query("""
        SELECT DISTINCT tags FROM sentence_cards
        WHERE userId = :userId AND tags != ''
    """)
    suspend fun getAllTags(userId: Long): List<String>
}

/**
 * Data class for difficulty count queries
 */
data class DifficultyCount(
    val difficulty: CardDifficulty,
    val count: Int
)

/**
 * Data class for pattern count queries
 */
data class PatternCount(
    val pattern: String,
    val count: Int
)

/**
 * Data class for pattern accuracy queries
 */
data class PatternAccuracy(
    val pattern: String,
    val accuracy: Float
)
