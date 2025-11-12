package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.GrammarFeedbackDao
import com.nihongo.conversation.data.local.MistakePatternRaw
import com.nihongo.conversation.data.local.dao.GrammarFeedbackCacheDao
import com.nihongo.conversation.data.local.entity.GrammarFeedbackCacheEntity
import com.nihongo.conversation.data.remote.GeminiApiService
import com.nihongo.conversation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrammarFeedbackRepository @Inject constructor(
    private val grammarFeedbackDao: GrammarFeedbackDao,
    private val grammarFeedbackCacheDao: GrammarFeedbackCacheDao,
    private val geminiApiService: GeminiApiService
) {
    companion object {
        private const val CACHE_EXPIRY_DAYS = 30
    }

    /**
     * Analyze user message for grammar errors and suggestions
     * Returns list of feedback items
     * Uses cache for repeated analysis (30-day expiry)
     */
    suspend fun analyzeMessage(
        userId: Long,
        messageId: Long,
        userMessage: String,
        conversationContext: List<String>,
        userLevel: Int
    ): List<GrammarFeedback> {
        return try {
            // Check cache first
            val cachedResult = grammarFeedbackCacheDao.getCachedFeedback(userMessage, userLevel)
            if (cachedResult != null) {
                // Cache hit! Parse and return immediately
                return parseFeedbackFromJson(
                    jsonText = cachedResult.feedbackJson,
                    userId = userId,
                    messageId = messageId,
                    originalText = userMessage
                ).also { feedbackList ->
                    // Save to GrammarFeedback table for this message
                    if (feedbackList.isNotEmpty()) {
                        grammarFeedbackDao.insertFeedbackList(feedbackList)
                    }
                }
            }

            // Cache miss - call Gemini API
            val analysisResult = geminiApiService.analyzeGrammarAndStyle(
                userMessage = userMessage,
                conversationContext = conversationContext,
                userLevel = userLevel
            )

            // Cache the raw JSON response
            grammarFeedbackCacheDao.cacheFeedback(
                GrammarFeedbackCacheEntity(
                    messageText = userMessage,
                    feedbackJson = analysisResult,
                    userLevel = userLevel,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Parse and save feedback
            val feedbackList = parseFeedbackFromJson(
                jsonText = analysisResult,
                userId = userId,
                messageId = messageId,
                originalText = userMessage
            )

            // Save to database
            if (feedbackList.isNotEmpty()) {
                grammarFeedbackDao.insertFeedbackList(feedbackList)
            }

            feedbackList
        } catch (e: Exception) {
            // Return empty list on error
            emptyList()
        }
    }

    /**
     * Parse JSON response from Gemini into GrammarFeedback objects
     */
    private fun parseFeedbackFromJson(
        jsonText: String,
        userId: Long,
        messageId: Long,
        originalText: String
    ): List<GrammarFeedback> {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            val feedbackList = mutableListOf<GrammarFeedback>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val feedback = GrammarFeedback(
                    userId = userId,
                    messageId = messageId,
                    originalText = originalText,
                    correctedText = obj.optString("correctedText", null),
                    feedbackType = FeedbackType.valueOf(obj.getString("type")),
                    severity = FeedbackSeverity.valueOf(obj.getString("severity")),
                    explanation = obj.getString("explanation"),
                    betterExpression = obj.optString("betterExpression", null),
                    additionalNotes = obj.optString("additionalNotes", null),
                    grammarPattern = obj.optString("grammarPattern", null)
                )

                feedbackList.add(feedback)
            }

            feedbackList
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get feedback for a specific message
     */
    fun getFeedbackForMessage(messageId: Long): Flow<List<GrammarFeedback>> {
        return grammarFeedbackDao.getFeedbackForMessage(messageId)
    }

    /**
     * Get recent feedback for user
     */
    fun getRecentFeedback(userId: Long, limit: Int = 50): Flow<List<GrammarFeedback>> {
        return grammarFeedbackDao.getRecentFeedback(userId, limit)
    }

    /**
     * Get unacknowledged feedback (for notifications/badges)
     */
    fun getUnacknowledgedFeedback(userId: Long): Flow<List<GrammarFeedback>> {
        return grammarFeedbackDao.getUnacknowledgedFeedback(userId)
    }

    /**
     * Mark feedback as acknowledged
     */
    suspend fun acknowledgeFeedback(feedbackId: Long) {
        grammarFeedbackDao.markAsAcknowledged(feedbackId)
    }

    /**
     * Mark feedback correction as applied by user
     */
    suspend fun markCorrectionApplied(feedbackId: Long) {
        grammarFeedbackDao.markAsApplied(feedbackId)
    }

    /**
     * Get common mistake patterns for user
     */
    suspend fun getCommonMistakes(
        userId: Long,
        daysBack: Int = 30,
        limit: Int = 10
    ): List<MistakePattern> {
        val startTime = getStartTime(daysBack)
        val rawData = grammarFeedbackDao.getCommonMistakes(userId, startTime, limit)

        return rawData.map { raw ->
            // Calculate improvement rate
            val improvementRate = calculateImprovementRate(userId, raw.grammarPattern, startTime)

            MistakePattern(
                grammarPattern = raw.grammarPattern,
                count = raw.count,
                lastOccurrence = raw.lastOccurrence,
                improvementRate = improvementRate
            )
        }
    }

    /**
     * Calculate improvement rate for a specific grammar pattern
     * Returns percentage (0.0 to 1.0) showing how much user has improved
     */
    private suspend fun calculateImprovementRate(
        userId: Long,
        grammarPattern: String,
        startTime: Long
    ): Double {
        // Get all feedback for this pattern
        val allFeedback = grammarFeedbackDao.getRecentFeedback(userId, 1000)
            .map { list ->
                list.filter {
                    it.grammarPattern == grammarPattern &&
                    it.createdAt >= startTime
                }
            }

        // This is a simplified calculation
        // In real implementation, you'd track if user stopped making the same mistake
        return 0.0 // TODO: Implement actual improvement tracking
    }

    /**
     * Get weekly progress summary
     */
    suspend fun getWeeklyProgress(userId: Long, weeksBack: Int = 4): List<FeedbackProgress> {
        val calendar = Calendar.getInstance()
        val progressList = mutableListOf<FeedbackProgress>()

        for (weekOffset in 0 until weeksBack) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.add(Calendar.WEEK_OF_YEAR, -weekOffset)
            val weekStart = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_WEEK, 7)
            val weekEnd = calendar.timeInMillis

            val totalFeedback = grammarFeedbackDao.getFeedbackCount(userId, weekStart)
            val grammarErrors = grammarFeedbackDao.getFeedbackCountByType(
                userId, FeedbackType.GRAMMAR_ERROR, weekStart
            )
            val unnaturalExpressions = grammarFeedbackDao.getFeedbackCountByType(
                userId, FeedbackType.UNNATURAL, weekStart
            )
            val improvementCount = grammarFeedbackDao.getAppliedCorrectionsCount(userId, weekStart)

            // Estimate total messages (feedback + messages without issues)
            val totalMessages = totalFeedback + (totalFeedback * 0.3).toInt() // Rough estimate

            val progress = FeedbackProgress(
                weekStart = weekStart,
                totalMessages = totalMessages,
                totalFeedback = totalFeedback,
                grammarErrors = grammarErrors,
                unnaturalExpressions = unnaturalExpressions,
                improvementCount = improvementCount,
                feedbackRate = if (totalMessages > 0) {
                    totalFeedback.toDouble() / totalMessages
                } else 0.0
            )

            progressList.add(progress)
        }

        return progressList.reversed()
    }

    /**
     * Get feedback statistics grouped by type
     */
    suspend fun getFeedbackStatsByType(userId: Long, daysBack: Int = 30): Map<FeedbackType, Int> {
        val startTime = getStartTime(daysBack)
        val stats = grammarFeedbackDao.getFeedbackStatsByType(userId, startTime)

        return stats.associate { it.feedbackType to it.count }
    }

    /**
     * Get daily feedback counts for chart visualization
     */
    suspend fun getDailyFeedbackCounts(userId: Long, daysBack: Int = 30): Map<String, Int> {
        val startTime = getStartTime(daysBack)
        val dailyCounts = grammarFeedbackDao.getDailyFeedbackCounts(userId, startTime)

        return dailyCounts.associate { it.date to it.count }
    }

    /**
     * Delete all feedback for a user
     */
    suspend fun deleteAllFeedback(userId: Long) {
        grammarFeedbackDao.deleteAllFeedbackForUser(userId)
    }

    /**
     * Get feedback grouped by severity
     */
    fun getFeedbackBySeverity(userId: Long, severity: FeedbackSeverity): Flow<List<GrammarFeedback>> {
        return grammarFeedbackDao.getRecentFeedback(userId, 100)
            .map { list -> list.filter { it.severity == severity } }
    }

    /**
     * Check if user has recurring issues with specific pattern
     */
    suspend fun hasRecurringIssue(userId: Long, grammarPattern: String, threshold: Int = 3): Boolean {
        val last30Days = getStartTime(30)
        val mistakes = grammarFeedbackDao.getCommonMistakes(userId, last30Days, 100)

        return mistakes.any { it.grammarPattern == grammarPattern && it.count >= threshold }
    }

    /**
     * Get personalized study suggestions based on common mistakes
     */
    suspend fun getStudySuggestions(userId: Long): List<String> {
        val commonMistakes = getCommonMistakes(userId, 30, 5)

        return commonMistakes.map { mistake ->
            when {
                mistake.grammarPattern.contains("助詞") -> "助詞の使い方を復習しましょう"
                mistake.grammarPattern.contains("敬語") -> "敬語の使い方を学びましょう"
                mistake.grammarPattern.contains("時制") -> "動詞の時制に注意しましょう"
                mistake.grammarPattern.contains("語順") -> "文の構造を確認しましょう"
                else -> "${mistake.grammarPattern}を復習しましょう"
            }
        }
    }

    /**
     * Clean up expired grammar feedback cache (older than 30 days)
     * Returns number of deleted entries
     */
    suspend fun cleanupExpiredCache(): Int {
        val expiryTime = System.currentTimeMillis() - (CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
        return grammarFeedbackCacheDao.deleteExpiredCache(expiryTime)
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): Pair<Int, Int> {
        val totalCount = grammarFeedbackCacheDao.getCacheCount()
        val expiredCount = cleanupExpiredCache()
        return Pair(totalCount - expiredCount, expiredCount)
    }

    private fun getStartTime(daysBack: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
