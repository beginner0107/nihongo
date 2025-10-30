package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.PronunciationHistoryDao
import com.nihongo.conversation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PronunciationHistoryRepository @Inject constructor(
    private val pronunciationHistoryDao: PronunciationHistoryDao
) {

    /**
     * Save a pronunciation practice attempt
     */
    suspend fun savePractice(
        userId: Long,
        expectedText: String,
        result: PronunciationResult,
        messageId: Long? = null,
        vocabularyId: Long? = null,
        source: PracticeSource = PracticeSource.CHAT,
        durationMs: Long = 0
    ): Long {
        // Get attempt number for this text
        val existingPractices = pronunciationHistoryDao.getPracticesByText(userId, expectedText).first()
        val attemptNumber = existingPractices.size + 1

        // Serialize word comparison to JSON
        val wordComparisonJson = serializeWordComparison(result.wordComparison)

        val practice = PronunciationHistory(
            userId = userId,
            messageId = messageId,
            vocabularyId = vocabularyId,
            expectedText = expectedText,
            recognizedText = result.recognizedText,
            accuracyScore = result.accuracyScore,
            wordComparisonJson = wordComparisonJson,
            practicedAt = System.currentTimeMillis(),
            durationMs = durationMs,
            attemptNumber = attemptNumber,
            source = source
        )

        return pronunciationHistoryDao.insertPractice(practice)
    }

    /**
     * Get all practices for a user
     */
    fun getAllPractices(userId: Long): Flow<List<PronunciationHistory>> {
        return pronunciationHistoryDao.getAllPractices(userId)
    }

    /**
     * Get practices for a specific message
     */
    fun getPracticesByMessage(messageId: Long): Flow<List<PronunciationHistory>> {
        return pronunciationHistoryDao.getPracticesByMessage(messageId)
    }

    /**
     * Get practices for a specific text
     */
    fun getPracticesByText(userId: Long, expectedText: String): Flow<List<PronunciationHistory>> {
        return pronunciationHistoryDao.getPracticesByText(userId, expectedText)
    }

    /**
     * Get best score for a specific text
     */
    suspend fun getBestScore(userId: Long, expectedText: String): Int {
        return pronunciationHistoryDao.getBestScore(userId, expectedText) ?: 0
    }

    /**
     * Get latest practice for a specific text
     */
    suspend fun getLatestPractice(userId: Long, expectedText: String): PronunciationHistory? {
        return pronunciationHistoryDao.getLatestPractice(userId, expectedText)
    }

    /**
     * Get overall pronunciation statistics for a user
     */
    suspend fun getStatistics(userId: Long): PronunciationStats {
        val totalAttempts = pronunciationHistoryDao.getTotalPracticeCount(userId)
        val uniquePhrases = pronunciationHistoryDao.getUniquePhraseCount(userId)
        val averageAccuracy = pronunciationHistoryDao.getAverageAccuracy(userId) ?: 0.0
        val bestAccuracy = pronunciationHistoryDao.getBestAccuracy(userId) ?: 0
        val totalPracticeTime = pronunciationHistoryDao.getTotalPracticeTime(userId) ?: 0L

        // Get improvement trend (last 7 days)
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val trendData = pronunciationHistoryDao.getAccuracyTrendRaw(userId, sevenDaysAgo)
        val improvementTrend = trendData.map {
            formatDate(it.date) to it.avgScore.toInt()
        }

        // Get weak phrases
        val weakPhrasesRaw = pronunciationHistoryDao.getWeakPhrases(userId, 10)
        val weakPhrases = weakPhrasesRaw.map { it.toPhraseStats() }

        // Get mastered phrases
        val masteredPhrasesRaw = pronunciationHistoryDao.getMasteredPhrases(userId, 10)
        val masteredPhrases = masteredPhrasesRaw.map { it.toPhraseStats() }

        // Get recent practices
        val recentPractices = pronunciationHistoryDao.getRecentPractices(userId, 20)

        return PronunciationStats(
            totalAttempts = totalAttempts,
            uniquePhrases = uniquePhrases,
            averageAccuracy = averageAccuracy,
            bestAccuracy = bestAccuracy,
            totalPracticeTimeMs = totalPracticeTime,
            improvementTrend = improvementTrend,
            weakPhrases = weakPhrases,
            masteredPhrases = masteredPhrases,
            recentPractices = recentPractices
        )
    }

    /**
     * Get phrases grouped by statistics
     */
    suspend fun getPhrasesWithStats(userId: Long): List<PhraseStats> {
        val rawStats = pronunciationHistoryDao.getPhrasesWithStats(userId)
        return rawStats.map { it.toPhraseStats() }
    }

    /**
     * Get weak phrases that need more practice
     */
    suspend fun getWeakPhrases(userId: Long, limit: Int = 10): List<PhraseStats> {
        val rawStats = pronunciationHistoryDao.getWeakPhrases(userId, limit)
        return rawStats.map { it.toPhraseStats() }
    }

    /**
     * Get mastered phrases
     */
    suspend fun getMasteredPhrases(userId: Long, limit: Int = 10): List<PhraseStats> {
        val rawStats = pronunciationHistoryDao.getMasteredPhrases(userId, limit)
        return rawStats.map { it.toPhraseStats() }
    }

    /**
     * Delete all practices for a user
     */
    suspend fun deleteAllForUser(userId: Long) {
        pronunciationHistoryDao.deleteAllForUser(userId)
    }

    /**
     * Deserialize word comparison JSON to list
     */
    fun deserializeWordComparison(json: String): List<WordMatch> {
        val result = mutableListOf<WordMatch>()
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                WordMatch(
                    expectedWord = obj.getString("expected"),
                    recognizedWord = obj.optString("recognized").takeIf { it.isNotEmpty() },
                    matchType = MatchType.valueOf(obj.getString("matchType"))
                )
            )
        }

        return result
    }

    /**
     * Serialize word comparison to JSON string
     */
    private fun serializeWordComparison(wordMatches: List<WordMatch>): String {
        val jsonArray = JSONArray()

        for (match in wordMatches) {
            val obj = JSONObject().apply {
                put("expected", match.expectedWord)
                put("recognized", match.recognizedWord ?: "")
                put("matchType", match.matchType.name)
            }
            jsonArray.put(obj)
        }

        return jsonArray.toString()
    }

    /**
     * Format date for display
     */
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
}

/**
 * Extension function to convert raw query result to PhraseStats
 */
private fun com.nihongo.conversation.data.local.PhraseStatsRaw.toPhraseStats(): PhraseStats {
    // Calculate improvement rate (assuming first attempt was ~60% of current)
    val estimatedFirstScore = avgScore * 0.6
    val improvementRate = if (estimatedFirstScore > 0) {
        ((avgScore - estimatedFirstScore) / estimatedFirstScore) * 100
    } else 0.0

    return PhraseStats(
        expectedText = expectedText,
        attemptCount = attemptCount,
        bestScore = bestScore,
        averageScore = avgScore,
        latestScore = bestScore, // Approximation
        latestAttemptDate = latestAttempt,
        improvementRate = improvementRate
    )
}
