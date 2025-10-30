package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Analytics for cache performance tracking
 * Helps optimize cache hit rate and API usage
 */
@Entity(tableName = "cache_analytics")
data class CacheAnalytics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Time period
    val date: String, // YYYY-MM-DD format
    val userId: Long,
    val scenarioId: Long,

    // Cache performance
    val cacheHits: Int = 0, // Requests served from cache
    val cacheMisses: Int = 0, // Requests that needed API call
    val apiCalls: Int = 0, // Total API calls made

    // Quality metrics
    val averageSimilarityScore: Float = 0.0f, // Average similarity for cache hits
    val averageResponseTime: Long = 0, // In milliseconds
    val userContinuationRate: Float = 0.0f, // % of conversations that continued

    // Resource usage
    val tokensSaved: Long = 0, // Estimated tokens saved by caching
    val estimatedCostSaved: Float = 0.0f, // Estimated USD saved

    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Summary statistics for cache performance
 */
data class CachePerformanceSummary(
    val totalRequests: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val hitRate: Float, // cacheHits / totalRequests
    val apiCallsSaved: Int,
    val estimatedCostSaved: Float,
    val averageResponseTime: Long
) {
    companion object {
        fun calculate(analytics: List<CacheAnalytics>): CachePerformanceSummary {
            val totalHits = analytics.sumOf { it.cacheHits }
            val totalMisses = analytics.sumOf { it.cacheMisses }
            val totalRequests = totalHits + totalMisses

            return CachePerformanceSummary(
                totalRequests = totalRequests,
                cacheHits = totalHits,
                cacheMisses = totalMisses,
                hitRate = if (totalRequests > 0) totalHits.toFloat() / totalRequests else 0f,
                apiCallsSaved = totalHits,
                estimatedCostSaved = analytics.sumOf { it.estimatedCostSaved.toDouble() }.toFloat(),
                averageResponseTime = if (analytics.isNotEmpty()) {
                    analytics.map { it.averageResponseTime }.average().toLong()
                } else 0L
            )
        }
    }
}
