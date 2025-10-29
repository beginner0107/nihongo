package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.ReviewHistory
import com.nihongo.conversation.domain.model.VocabularyEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    // ========== Vocabulary Entry Queries ==========

    @Query("SELECT * FROM vocabulary_entries WHERE id = :id")
    fun getVocabularyById(id: Long): Flow<VocabularyEntry?>

    @Query("SELECT * FROM vocabulary_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllVocabulary(userId: Long): Flow<List<VocabularyEntry>>

    @Query("""
        SELECT * FROM vocabulary_entries
        WHERE userId = :userId
        AND nextReviewAt <= :currentTime
        AND isMastered = 0
        ORDER BY nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getDueForReview(userId: Long, currentTime: Long, limit: Int): List<VocabularyEntry>

    @Query("""
        SELECT * FROM vocabulary_entries
        WHERE userId = :userId
        AND reviewCount = 0
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getNewWords(userId: Long, limit: Int): List<VocabularyEntry>

    @Query("""
        SELECT * FROM vocabulary_entries
        WHERE userId = :userId
        AND isMastered = 1
        ORDER BY lastReviewedAt DESC
    """)
    fun getMasteredWords(userId: Long): Flow<List<VocabularyEntry>>

    @Query("SELECT * FROM vocabulary_entries WHERE userId = :userId AND word = :word LIMIT 1")
    suspend fun findByWord(userId: Long, word: String): VocabularyEntry?

    @Query("SELECT COUNT(*) FROM vocabulary_entries WHERE userId = :userId")
    fun getTotalWordCount(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_entries WHERE userId = :userId AND isMastered = 1")
    fun getMasteredWordCount(userId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM vocabulary_entries
        WHERE userId = :userId
        AND nextReviewAt <= :currentTime
        AND isMastered = 0
    """)
    fun getDueWordCount(userId: Long, currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_entries WHERE userId = :userId AND reviewCount = 0")
    fun getNewWordCount(userId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM vocabulary_entries
        WHERE userId = :userId
        AND lastReviewedAt >= :todayStart
    """)
    fun getReviewedTodayCount(userId: Long, todayStart: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(entry: VocabularyEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabularyList(entries: List<VocabularyEntry>): List<Long>

    @Update
    suspend fun updateVocabulary(entry: VocabularyEntry)

    @Delete
    suspend fun deleteVocabulary(entry: VocabularyEntry)

    @Query("DELETE FROM vocabulary_entries WHERE userId = :userId")
    suspend fun deleteAllVocabulary(userId: Long)

    // ========== Review History Queries ==========

    @Query("SELECT * FROM review_history WHERE vocabularyId = :vocabularyId ORDER BY reviewedAt DESC")
    fun getReviewHistory(vocabularyId: Long): Flow<List<ReviewHistory>>

    @Query("""
        SELECT * FROM review_history
        WHERE vocabularyId = :vocabularyId
        ORDER BY reviewedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentReviews(vocabularyId: Long, limit: Int): List<ReviewHistory>

    @Query("""
        SELECT AVG(quality) FROM review_history
        WHERE vocabularyId = :vocabularyId
    """)
    suspend fun getAverageQuality(vocabularyId: Long): Float?

    @Insert
    suspend fun insertReviewHistory(history: ReviewHistory): Long

    @Delete
    suspend fun deleteReviewHistory(history: ReviewHistory)

    // ========== Search Queries ==========

    @Query("""
        SELECT * FROM vocabulary_entries
        WHERE userId = :userId
        AND (word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        LIMIT 50
    """)
    fun searchVocabulary(userId: Long, query: String): Flow<List<VocabularyEntry>>
}
