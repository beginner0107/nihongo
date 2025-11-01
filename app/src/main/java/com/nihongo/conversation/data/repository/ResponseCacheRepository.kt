package com.nihongo.conversation.data.repository

import com.nihongo.conversation.core.cache.FuzzyMatcher
import com.nihongo.conversation.data.local.CacheAnalyticsDao
import com.nihongo.conversation.data.local.CachedResponseDao
import com.nihongo.conversation.data.local.ConversationPatternDao
import com.nihongo.conversation.data.remote.GeminiApiService
import com.nihongo.conversation.domain.model.CacheAnalytics
import com.nihongo.conversation.domain.model.CachedResponse
import com.nihongo.conversation.domain.model.ConversationPattern
import com.nihongo.conversation.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Response caching result
 */
sealed class CachedResult {
    data class CacheHit(
        val response: String,
        val pattern: ConversationPattern,
        val cachedResponse: CachedResponse,
        val similarity: Float
    ) : CachedResult()

    data class CacheMiss(
        val response: String,
        val fromApi: Boolean = true
    ) : CachedResult()

    data class Error(val exception: Exception) : CachedResult()
}

/**
 * Repository for managing response cache with smart fallback
 * Reduces API calls by matching user input to cached patterns
 */
@Singleton
class ResponseCacheRepository @Inject constructor(
    private val patternDao: ConversationPatternDao,
    private val responseDao: CachedResponseDao,
    private val analyticsDao: CacheAnalyticsDao,
    private val geminiService: GeminiApiService,
    private val fuzzyMatcher: FuzzyMatcher
) {

    /**
     * Get response for user input with caching
     * 1. Try to find matching pattern (>=80% similarity)
     * 2. Return cached response if found
     * 3. Fall back to Gemini API if no match
     * 4. Optionally learn from new patterns
     */
    suspend fun getResponse(
        userInput: String,
        scenarioId: Long,
        difficultyLevel: Int,
        conversationHistory: List<Message>,
        systemPrompt: String,
        userId: Long,
        conversationTurn: Int = 0,
        similarityThreshold: Float = FuzzyMatcher.DEFAULT_THRESHOLD,
        enableLearning: Boolean = true
    ): CachedResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // Step 1: Try to find matching pattern
            val matchingPattern = findMatchingPattern(
                userInput = userInput,
                scenarioId = scenarioId,
                difficultyLevel = difficultyLevel,
                conversationTurn = conversationTurn,
                threshold = similarityThreshold
            )

            if (matchingPattern != null) {
                // Cache hit!
                val (pattern, similarity) = matchingPattern

                // Get a response for this pattern (prefer least used for variety)
                val cachedResponse = responseDao.getLeastUsedResponse(pattern.id)
                    ?: responseDao.getRandomResponse(pattern.id)

                if (cachedResponse != null) {
                    // Update usage statistics
                    patternDao.incrementUsage(pattern.id, similarity)
                    responseDao.incrementUsage(cachedResponse.id)

                    // Record analytics
                    recordCacheHit(
                        userId = userId,
                        scenarioId = scenarioId,
                        similarity = similarity,
                        responseTime = System.currentTimeMillis() - startTime
                    )

                    return@withContext CachedResult.CacheHit(
                        response = cachedResponse.response,
                        pattern = pattern,
                        cachedResponse = cachedResponse,
                        similarity = similarity
                    )
                }
            }

            // Step 2: Cache miss - call Gemini API
            val apiResponse = geminiService.sendMessage(
                message = userInput,
                conversationHistory = conversationHistory.map { it.content to it.isUser },
                systemPrompt = systemPrompt
            )

            // Record analytics
            recordCacheMiss(
                userId = userId,
                scenarioId = scenarioId,
                responseTime = System.currentTimeMillis() - startTime
            )

            // Step 3: Learn from this interaction (optional)
            if (enableLearning && apiResponse.isNotEmpty()) {
                learnNewPattern(
                    userInput = userInput,
                    aiResponse = apiResponse,
                    scenarioId = scenarioId,
                    difficultyLevel = difficultyLevel,
                    conversationTurn = conversationTurn
                )
            }

            CachedResult.CacheMiss(
                response = apiResponse,
                fromApi = true
            )

        } catch (e: Exception) {
            // Record error in analytics
            recordCacheMiss(
                userId = userId,
                scenarioId = scenarioId,
                responseTime = System.currentTimeMillis() - startTime
            )

            CachedResult.Error(e)
        }
    }

    /**
     * Find matching pattern using fuzzy matching
     * Phase 2: Two-stage matching with precomputed tokens
     */
    private suspend fun findMatchingPattern(
        userInput: String,
        scenarioId: Long,
        difficultyLevel: Int,
        conversationTurn: Int,
        threshold: Float
    ): Pair<ConversationPattern, Float>? {
        // Get relevant patterns for this scenario and difficulty
        val patterns = patternDao.getTopPatterns(
            scenarioId = scenarioId,
            difficultyLevel = difficultyLevel,
            turn = conversationTurn,
            limit = 50 // Check top 50 most used patterns
        )

        if (patterns.isEmpty()) return null

        // Phase 2: Precompute normalized input and tokens once
        val normalizedInput = fuzzyMatcher.normalizeForJapanese(userInput)
        val inputTokens = fuzzyMatcher.tokenizeJapanese(userInput)

        // Phase 2: Stage 1 - Cheap filtering (substring + token overlap)
        val candidates = patterns.filter { pattern ->
            val normalizedPattern = fuzzyMatcher.normalizeForJapanese(pattern.pattern)

            // Quick substring check
            val hasSubstring = normalizedInput.contains(normalizedPattern) ||
                              normalizedPattern.contains(normalizedInput)

            if (hasSubstring) return@filter true

            // Token overlap check (Jaccard similarity approximation)
            val patternTokens = fuzzyMatcher.tokenizeJapanese(pattern.pattern)
            val intersection = inputTokens.intersect(patternTokens).size
            val union = inputTokens.union(patternTokens).size

            // Keep if >30% token overlap
            union > 0 && (intersection.toFloat() / union) > 0.3f
        }

        // Phase 2: Stage 2 - Expensive Levenshtein only on candidates
        var bestMatch: Pair<ConversationPattern, Float>? = null
        var bestScore = 0.0f

        for (pattern in candidates) {
            val keywords = pattern.keywords.split(",").filter { it.isNotBlank() }
            val similarity = fuzzyMatcher.calculateSimilarityWithKeywords(
                input = userInput,
                pattern = pattern.pattern,
                keywords = keywords
            )

            if (similarity > bestScore) {
                bestScore = similarity
                bestMatch = Pair(pattern, similarity)
            }
        }

        // Return match if above threshold
        return if (bestScore >= threshold) bestMatch else null
    }

    /**
     * Learn from new interaction by creating pattern and caching response
     */
    private suspend fun learnNewPattern(
        userInput: String,
        aiResponse: String,
        scenarioId: Long,
        difficultyLevel: Int,
        conversationTurn: Int
    ) {
        // Extract keywords from user input (simple implementation)
        val keywords = extractKeywords(userInput)

        // Create new pattern
        val pattern = ConversationPattern(
            pattern = userInput,
            scenarioId = scenarioId,
            difficultyLevel = difficultyLevel,
            category = "learned", // Mark as auto-learned
            conversationTurn = conversationTurn,
            keywords = keywords.joinToString(","),
            usageCount = 1 // Start with 1 use
        )

        val patternId = patternDao.insertPattern(pattern)

        // Cache the response
        val cachedResponse = CachedResponse(
            patternId = patternId,
            response = aiResponse,
            variation = 0,
            complexityScore = calculateComplexity(aiResponse),
            generatedByApi = true,
            isVerified = false
        )

        responseDao.insertResponse(cachedResponse)
    }

    /**
     * Extract important keywords from Japanese text
     * Simple implementation - can be enhanced with NLP
     */
    private fun extractKeywords(text: String): List<String> {
        // Remove particles and common words
        val particles = setOf("は", "が", "を", "に", "へ", "と", "で", "から", "まで", "の", "も")
        val commonWords = setOf("です", "ます", "ください", "お願い", "します")

        // Split into characters and filter
        return text.toCharArray()
            .map { it.toString() }
            .filter { it !in particles }
            .distinct()
            .take(10) // Limit to 10 keywords
    }

    /**
     * Calculate complexity score for response
     */
    private fun calculateComplexity(text: String): Int {
        // Simple heuristic: longer = more complex
        val length = text.length
        return when {
            length < 20 -> 1
            length < 50 -> 2
            else -> 3
        }
    }

    /**
     * Record cache hit in analytics
     */
    private suspend fun recordCacheHit(
        userId: Long,
        scenarioId: Long,
        similarity: Float,
        responseTime: Long
    ) {
        val today = getCurrentDate()

        val existing = analyticsDao.getAnalyticsForDay(userId, scenarioId, today)

        if (existing != null) {
            // Update existing analytics
            val updated = existing.copy(
                cacheHits = existing.cacheHits + 1,
                averageSimilarityScore = (existing.averageSimilarityScore * existing.cacheHits + similarity) / (existing.cacheHits + 1),
                averageResponseTime = (existing.averageResponseTime * existing.cacheHits + responseTime) / (existing.cacheHits + 1),
                tokensSaved = existing.tokensSaved + estimateTokensSaved(),
                estimatedCostSaved = existing.estimatedCostSaved + estimateCostSaved()
            )
            analyticsDao.updateAnalytics(updated)
        } else {
            // Create new analytics entry
            val newAnalytics = CacheAnalytics(
                date = today,
                userId = userId,
                scenarioId = scenarioId,
                cacheHits = 1,
                cacheMisses = 0,
                apiCalls = 0,
                averageSimilarityScore = similarity,
                averageResponseTime = responseTime,
                tokensSaved = estimateTokensSaved(),
                estimatedCostSaved = estimateCostSaved()
            )
            analyticsDao.insertAnalytics(newAnalytics)
        }
    }

    /**
     * Record cache miss in analytics
     */
    private suspend fun recordCacheMiss(
        userId: Long,
        scenarioId: Long,
        responseTime: Long
    ) {
        val today = getCurrentDate()

        val existing = analyticsDao.getAnalyticsForDay(userId, scenarioId, today)

        if (existing != null) {
            val updated = existing.copy(
                cacheMisses = existing.cacheMisses + 1,
                apiCalls = existing.apiCalls + 1,
                averageResponseTime = (existing.averageResponseTime * (existing.cacheHits + existing.cacheMisses) + responseTime) /
                        (existing.cacheHits + existing.cacheMisses + 1)
            )
            analyticsDao.updateAnalytics(updated)
        } else {
            val newAnalytics = CacheAnalytics(
                date = today,
                userId = userId,
                scenarioId = scenarioId,
                cacheHits = 0,
                cacheMisses = 1,
                apiCalls = 1,
                averageResponseTime = responseTime
            )
            analyticsDao.insertAnalytics(newAnalytics)
        }
    }

    /**
     * Get cache performance statistics
     */
    suspend fun getCacheStats(userId: Long, scenarioId: Long? = null, days: Int = 7): CachePerformanceStats {
        val analytics = if (scenarioId != null) {
            analyticsDao.getRecentAnalytics(userId, scenarioId, days)
        } else {
            analyticsDao.getAllUserAnalytics(userId, days)
        }

        val totalHits = analytics.sumOf { it.cacheHits }
        val totalMisses = analytics.sumOf { it.cacheMisses }
        val totalRequests = totalHits + totalMisses

        return CachePerformanceStats(
            totalRequests = totalRequests,
            cacheHits = totalHits,
            cacheMisses = totalMisses,
            hitRate = if (totalRequests > 0) totalHits.toFloat() / totalRequests else 0f,
            apiCallsSaved = totalHits,
            tokensSaved = analytics.sumOf { it.tokensSaved },
            estimatedCostSaved = analytics.sumOf { it.estimatedCostSaved.toDouble() }.toFloat(),
            averageResponseTime = if (analytics.isNotEmpty()) {
                analytics.map { it.averageResponseTime }.average().toLong()
            } else 0L,
            averageSimilarity = if (totalHits > 0) {
                analytics.sumOf { (it.averageSimilarityScore * it.cacheHits).toDouble() }.toFloat() / totalHits
            } else 0f
        )
    }

    // Helper functions

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun estimateTokensSaved(): Long {
        // Rough estimate: average conversation uses ~200 tokens
        return 200L
    }

    private fun estimateCostSaved(): Float {
        // Gemini Flash pricing: ~$0.075 per 1M input tokens, $0.30 per 1M output tokens
        // Average request: 100 input + 100 output = 200 tokens = ~$0.0000375
        return 0.0000375f
    }
}

/**
 * Cache performance statistics
 */
data class CachePerformanceStats(
    val totalRequests: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val hitRate: Float,
    val apiCallsSaved: Int,
    val tokensSaved: Long,
    val estimatedCostSaved: Float,
    val averageResponseTime: Long,
    val averageSimilarity: Float
) {
    val hitRatePercentage: Int get() = (hitRate * 100).toInt()

    fun getFormattedStats(): String {
        return """
            Cache Performance:
            - Hit Rate: $hitRatePercentage%
            - Requests: $totalRequests (${cacheHits} hits, ${cacheMisses} misses)
            - API Calls Saved: $apiCallsSaved
            - Tokens Saved: ${tokensSaved.toLocaleString()}
            - Cost Saved: $${String.format("%.4f", estimatedCostSaved)}
            - Avg Response Time: ${averageResponseTime}ms
            - Avg Similarity: ${String.format("%.1f%%", averageSimilarity * 100)}
        """.trimIndent()
    }

    private fun Long.toLocaleString(): String {
        return String.format("%,d", this)
    }
}
