package com.nihongo.conversation.data.repository

import com.nihongo.conversation.core.util.VocabularyExtractor
import com.nihongo.conversation.data.local.MessageDao
import com.nihongo.conversation.data.local.VocabularyDao
import com.nihongo.conversation.data.remote.GeminiApiService
import com.nihongo.conversation.domain.model.*
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao,
    private val messageDao: MessageDao,
    private val vocabularyExtractor: VocabularyExtractor,
    private val geminiApi: GeminiApiService
) {

    // ========== Vocabulary Management ==========

    fun getAllVocabulary(userId: Long): Flow<List<VocabularyEntry>> {
        return vocabularyDao.getAllVocabulary(userId)
    }

    fun getVocabularyById(id: Long): Flow<VocabularyEntry?> {
        return vocabularyDao.getVocabularyById(id)
    }

    fun getMasteredWords(userId: Long): Flow<List<VocabularyEntry>> {
        return vocabularyDao.getMasteredWords(userId)
    }

    suspend fun addVocabulary(entry: VocabularyEntry): Long {
        return vocabularyDao.insertVocabulary(entry)
    }

    suspend fun updateVocabulary(entry: VocabularyEntry) {
        vocabularyDao.updateVocabulary(entry)
    }

    suspend fun deleteVocabulary(entry: VocabularyEntry) {
        vocabularyDao.deleteVocabulary(entry)
    }

    // ========== Vocabulary Extraction ==========

    /**
     * Extract vocabulary from a completed conversation
     */
    suspend fun extractVocabularyFromConversation(
        conversationId: Long,
        userId: Long,
        autoTranslate: Boolean = true
    ): List<VocabularyEntry> {
        val messages = messageDao.getMessagesByConversation(conversationId).first()
        val extractedWords = vocabularyExtractor.extractFromMessages(messages, userId, conversationId)

        // Translate words if requested
        val translatedWords = if (autoTranslate) {
            extractedWords.map { entry ->
                try {
                    val translation = geminiApi.translateToKorean(entry.word)
                    entry.copy(meaning = translation)
                } catch (e: Exception) {
                    entry // Keep original if translation fails
                }
            }
        } else {
            extractedWords
        }

        // Save to database (avoid duplicates)
        val savedWords = mutableListOf<VocabularyEntry>()
        for (word in translatedWords) {
            val existing = vocabularyDao.findByWord(userId, word.word)
            if (existing == null) {
                val id = vocabularyDao.insertVocabulary(word)
                savedWords.add(word.copy(id = id))
            } else {
                savedWords.add(existing)
            }
        }

        return savedWords
    }

    /**
     * Add a single vocabulary entry from user input
     */
    suspend fun addCustomVocabulary(
        userId: Long,
        word: String,
        reading: String? = null,
        meaning: String,
        exampleSentence: String? = null,
        difficulty: Int = 1,
        addToReviewQueue: Boolean = true
    ): Long {
        // Check for duplicates
        val existing = vocabularyDao.findByWord(userId, word)
        if (existing != null) {
            throw IllegalArgumentException("この単語は既に追加されています")
        }

        val entry = VocabularyEntry(
            userId = userId,
            word = word,
            reading = reading,
            meaning = meaning,
            exampleSentence = exampleSentence,
            difficulty = difficulty,
            sourceConversationId = null, // Mark as custom entry
            nextReviewAt = if (addToReviewQueue) System.currentTimeMillis() else System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        )
        return vocabularyDao.insertVocabulary(entry)
    }

    /**
     * Check if a word already exists for this user
     */
    suspend fun wordExists(userId: Long, word: String): Boolean {
        return vocabularyDao.findByWord(userId, word) != null
    }

    /**
     * Get custom vocabulary (entries with no source conversation)
     */
    fun getCustomVocabulary(userId: Long): Flow<List<VocabularyEntry>> {
        return vocabularyDao.getAllVocabulary(userId).map { entries ->
            entries.filter { it.sourceConversationId == null }
        }
    }

    // ========== Review Sessions ==========

    /**
     * Get vocabulary entries for a review session
     */
    suspend fun getReviewSession(
        userId: Long,
        config: ReviewSessionConfig = ReviewSessionConfig()
    ): List<VocabularyEntry> {
        val words = mutableListOf<VocabularyEntry>()

        // Add due reviews
        if (config.includeDue) {
            val dueWords = vocabularyDao.getDueForReview(
                userId = userId,
                currentTime = System.currentTimeMillis(),
                limit = config.maxReviewWords
            )
            words.addAll(dueWords)
        }

        // Add new words if there's room
        if (config.includeNew && words.size < config.maxReviewWords) {
            val newWordLimit = minOf(
                config.maxNewWords,
                config.maxReviewWords - words.size
            )
            val newWords = vocabularyDao.getNewWords(userId, newWordLimit)
            words.addAll(newWords)
        }

        // Shuffle for better learning
        return words.shuffled()
    }

    /**
     * Submit a review for a vocabulary entry
     */
    suspend fun submitReview(
        vocabularyId: Long,
        quality: ReviewQuality,
        timeSpentMs: Long = 0
    ): VocabularyEntry {
        val entry = vocabularyDao.getVocabularyById(vocabularyId).first()
            ?: throw IllegalArgumentException("Vocabulary entry not found")

        // Calculate next review using SM-2 algorithm
        val updatedEntry = SpacedRepetitionAlgorithm.calculateNextReview(entry, quality)

        // Check if mastered
        val masteredEntry = if (SpacedRepetitionAlgorithm.isMastered(updatedEntry)) {
            updatedEntry.copy(isMastered = true)
        } else {
            updatedEntry
        }

        // Update in database
        vocabularyDao.updateVocabulary(masteredEntry)

        // Record review history
        val history = ReviewHistory(
            vocabularyId = vocabularyId,
            quality = quality.value,
            timeSpentMs = timeSpentMs
        )
        vocabularyDao.insertReviewHistory(history)

        return masteredEntry
    }

    // ========== Statistics ==========

    fun getVocabularyStats(userId: Long): Flow<VocabularyStats> {
        return combine(
            vocabularyDao.getTotalWordCount(userId),
            vocabularyDao.getMasteredWordCount(userId),
            vocabularyDao.getDueWordCount(userId, System.currentTimeMillis()),
            vocabularyDao.getNewWordCount(userId),
            vocabularyDao.getReviewedTodayCount(userId, getTodayStart())
        ) { total, mastered, due, new, reviewedToday ->
            // Calculate overall accuracy
            val allWords = vocabularyDao.getAllVocabulary(userId).first()
            val totalReviews = allWords.sumOf { it.reviewCount }
            val totalCorrect = allWords.sumOf { it.correctCount }
            val accuracy = if (totalReviews > 0) {
                totalCorrect.toFloat() / totalReviews
            } else {
                0f
            }

            VocabularyStats(
                totalWords = total,
                masteredWords = mastered,
                dueForReview = due,
                newWords = new,
                reviewedToday = reviewedToday,
                accuracyRate = accuracy
            )
        }
    }

    fun getReviewHistory(vocabularyId: Long): Flow<List<ReviewHistory>> {
        return vocabularyDao.getReviewHistory(vocabularyId)
    }

    // ========== Search ==========

    fun searchVocabulary(userId: Long, query: String): Flow<List<VocabularyEntry>> {
        return vocabularyDao.searchVocabulary(userId, query)
    }

    // ========== Helper Functions ==========

    private fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
