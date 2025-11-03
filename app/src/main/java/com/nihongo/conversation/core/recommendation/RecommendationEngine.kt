package com.nihongo.conversation.core.recommendation

import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.Scenario
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * Recommendation engine that suggests scenarios based on:
 * 1. User's completion history
 * 2. Difficulty progression
 * 3. Topic diversity
 * 4. Time since last completion
 */
@Singleton
class RecommendationEngine @Inject constructor() {

    /**
     * Calculate recommendation scores for scenarios
     *
     * @param scenarios All available scenarios
     * @param completedConversations User's completed conversations
     * @param currentLevel User's current difficulty level (1-3)
     * @return List of scenarios sorted by recommendation score (highest first)
     */
    fun getRecommendations(
        scenarios: List<Scenario>,
        completedConversations: List<Conversation>,
        currentLevel: Int
    ): List<ScoredScenario> {
        if (scenarios.isEmpty()) return emptyList()

        // Calculate completion stats
        val completionMap = completedConversations
            .groupBy { it.scenarioId }
            .mapValues { it.value.size }

        val lastCompletionMap = completedConversations
            .groupBy { it.scenarioId }
            .mapValues { it.value.maxOf { conv -> conv.updatedAt } }

        val completedCategories = completedConversations
            .mapNotNull { conv -> scenarios.find { it.id == conv.scenarioId }?.category }
            .groupingBy { it }
            .eachCount()

        // Score each scenario
        val scoredScenarios = scenarios.map { scenario ->
            val score = calculateScore(
                scenario = scenario,
                completionCount = completionMap[scenario.id] ?: 0,
                lastCompletionTime = lastCompletionMap[scenario.id],
                completedCategories = completedCategories,
                currentLevel = currentLevel
            )

            ScoredScenario(
                scenario = scenario,
                score = score,
                reason = generateReason(
                    scenario = scenario,
                    completionCount = completionMap[scenario.id] ?: 0,
                    currentLevel = currentLevel
                )
            )
        }

        return scoredScenarios.sortedByDescending { it.score }
    }

    /**
     * Calculate recommendation score for a scenario
     * Score range: 0.0 to 1.0
     */
    private fun calculateScore(
        scenario: Scenario,
        completionCount: Int,
        lastCompletionTime: Long?,
        completedCategories: Map<String, Int>,
        currentLevel: Int
    ): Double {
        var score = 0.0

        // 1. Difficulty match (0.0 - 0.35)
        score += calculateDifficultyScore(scenario.difficulty, currentLevel)

        // 2. Freshness (0.0 - 0.25)
        score += calculateFreshnessScore(completionCount, lastCompletionTime)

        // 3. Topic diversity (0.0 - 0.20)
        score += calculateDiversityScore(scenario.category, completedCategories)

        // 4. Popularity boost (0.0 - 0.10)
        score += calculatePopularityScore(scenario)

        // 5. Recency penalty (0.0 - 0.10)
        score += calculateRecencyScore(lastCompletionTime)

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Difficulty matching score
     * Prioritize scenarios matching user's current level
     */
    private fun calculateDifficultyScore(scenarioDifficulty: Int, currentLevel: Int): Double {
        return when (scenarioDifficulty - currentLevel) {
            0 -> 0.35    // Perfect match
            1 -> 0.25    // Slightly harder (good for progression)
            -1 -> 0.20   // Slightly easier (good for confidence)
            2 -> 0.10    // Much harder
            -2 -> 0.05   // Much easier
            else -> 0.0
        }
    }

    /**
     * Freshness score - prefer unplayed or rarely played scenarios
     */
    private fun calculateFreshnessScore(completionCount: Int, lastCompletionTime: Long?): Double {
        return when {
            completionCount == 0 -> 0.25                    // Never played
            completionCount == 1 -> 0.20                    // Played once
            completionCount == 2 -> 0.15                    // Played twice
            completionCount <= 5 -> 0.10                    // Occasionally played
            else -> 0.05 * exp(-(completionCount - 5) / 5.0) // Heavily played (decay)
        }
    }

    /**
     * Diversity score - prefer under-represented categories
     */
    private fun calculateDiversityScore(
        category: String,
        completedCategories: Map<String, Int>
    ): Double {
        val categoryCount = completedCategories[category] ?: 0
        val maxCount = completedCategories.values.maxOrNull() ?: 0

        return when {
            categoryCount == 0 -> 0.20                      // Never tried this category
            maxCount == 0 -> 0.10                           // No history yet
            else -> {
                val ratio = categoryCount.toDouble() / maxCount
                0.20 * (1 - ratio)                          // Less explored = higher score
            }
        }
    }

    /**
     * Popularity score based on scenario characteristics
     */
    private fun calculatePopularityScore(scenario: Scenario): Double {
        var score = 0.0

        // Common daily scenarios get a boost
        val dailyKeywords = listOf("挨拶", "買い物", "レストラン", "道案内", "電話")
        if (dailyKeywords.any { scenario.title.contains(it) }) {
            score += 0.05
        }

        // Practical scenarios get a boost
        if (scenario.category in listOf("shopping", "restaurant", "transport", "medical")) {
            score += 0.05
        }

        return score
    }

    /**
     * Recency score - small penalty for recently completed
     */
    private fun calculateRecencyScore(lastCompletionTime: Long?): Double {
        if (lastCompletionTime == null) return 0.10

        val hoursSince = (System.currentTimeMillis() - lastCompletionTime) / (1000 * 60 * 60)

        return when {
            hoursSince < 1 -> -0.10     // Just completed
            hoursSince < 6 -> -0.05     // Very recent
            hoursSince < 24 -> 0.0      // Recent
            hoursSince < 72 -> 0.05     // A few days ago
            else -> 0.10                // Long time ago
        }
    }

    /**
     * Generate human-readable recommendation reason
     */
    private fun generateReason(
        scenario: Scenario,
        completionCount: Int,
        currentLevel: Int
    ): String {
        val reasons = mutableListOf<String>()

        // Difficulty match
        when (scenario.difficulty - currentLevel) {
            0 -> reasons.add("레벨에 딱 맞아요")
            1 -> reasons.add("다음 레벨 도전")
            -1 -> reasons.add("복습하기 좋아요")
        }

        // Freshness
        when (completionCount) {
            0 -> reasons.add("새로운 시나리오")
            1 -> reasons.add("한 번 더 연습")
        }

        // Pick top 2 reasons
        return reasons.take(2).joinToString(" • ")
    }

    /**
     * Get top N recommendations
     */
    fun getTopRecommendations(
        scenarios: List<Scenario>,
        completedConversations: List<Conversation>,
        currentLevel: Int,
        limit: Int = 3
    ): List<ScoredScenario> {
        return getRecommendations(scenarios, completedConversations, currentLevel)
            .take(limit)
    }
}

/**
 * Scenario with recommendation score
 */
data class ScoredScenario(
    val scenario: Scenario,
    val score: Double,
    val reason: String
)
