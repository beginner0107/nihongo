package com.nihongo.conversation.core.recommendation

import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User
import com.nihongo.conversation.presentation.home.ScenarioRecommendation
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * AI-powered scenario recommendation engine
 *
 * Scoring algorithm:
 * - Level matching: 50% (same level gets highest score)
 * - Category preference: 30% (favorite categories)
 * - Popularity: 20% (completion count)
 */
@Singleton
class ScenarioRecommendationEngine @Inject constructor() {

    fun getRecommendation(
        allScenarios: List<Scenario>,
        user: User?,
        completedScenarioIds: List<Long>,
        userLevel: Int
    ): ScenarioRecommendation {
        // 1. Filter: Not completed + user level ±1
        val candidates = allScenarios
            .filter { it.id !in completedScenarioIds }
            .filter { it.difficulty in (userLevel - 1)..(userLevel + 1) }

        if (candidates.isEmpty()) {
            // Fallback: random scenario from all
            val fallback = allScenarios.randomOrNull()
                ?: throw IllegalStateException("No scenarios available")
            return buildRecommendation(fallback, userLevel, "랜덤 추천")
        }

        // 2. Score calculation
        val scored = candidates.map { scenario ->
            var score = 0.0

            // Level matching (50%)
            score += when (scenario.difficulty - userLevel) {
                0 -> 0.5    // Same level
                -1, 1 -> 0.3  // ±1
                else -> 0.0
            }

            // Category preference (30%)
            val favoriteCategories = user?.favoriteScenarios
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?.let { favoriteIds ->
                    allScenarios.filter { it.id in favoriteIds }.map { it.category }
                } ?: emptyList()

            if (scenario.category in favoriteCategories) {
                score += 0.3
            }

            // Popularity (20%) - placeholder (실제로는 DB에서 completion count 가져와야 함)
            // For now, use random value between 0.0 and 0.2
            score += Random.nextDouble(0.0, 0.2)

            scenario to score
        }

        // 3. Select best scenario
        val best = scored.maxByOrNull { it.second }?.first
            ?: candidates.random()

        // 4. Generate recommendation reason
        val reason = when {
            best.difficulty == userLevel -> "${getDifficultyLabel(userLevel)} 학습자님께 추천"
            best.difficulty < userLevel -> "복습으로 좋아요!"
            else -> "도전해보세요!"
        }

        return buildRecommendation(best, userLevel, reason)
    }

    private fun buildRecommendation(
        scenario: Scenario,
        userLevel: Int,
        reason: String
    ): ScenarioRecommendation {
        return ScenarioRecommendation(
            scenario = scenario,
            reason = reason,
            estimatedTime = 5, // Placeholder: 5분 (실제로는 시나리오 복잡도 기반 계산)
            difficulty = getDifficultyLabel(scenario.difficulty),
            difficultyLevel = scenario.difficulty,
            category = getCategoryLabel(scenario.category)
        )
    }

    private fun getDifficultyLabel(difficulty: Int): String {
        return when (difficulty) {
            1 -> "초급"
            2 -> "중급"
            3 -> "고급"
            else -> "초급"
        }
    }

    private fun getCategoryLabel(category: String): String {
        return when (category) {
            "DAILY_LIFE" -> "🏠 일상 생활"
            "WORK" -> "💼 직장/업무"
            "TRAVEL" -> "✈️ 여행"
            "ENTERTAINMENT" -> "🎵 엔터테인먼트"
            "ESPORTS" -> "🎮 e스포츠"
            "TECH" -> "💻 기술/개발"
            "FINANCE" -> "💰 금융/재테크"
            "CULTURE" -> "🎭 문화"
            "HOUSING" -> "🏢 부동산/주거"
            "HEALTH" -> "🏥 건강/의료"
            "STUDY" -> "📚 학습/교육"
            "DAILY_CONVERSATION" -> "💬 일상 회화"
            "JLPT_PRACTICE" -> "📖 JLPT 연습"
            "BUSINESS" -> "🤝 비즈니스"
            "ROMANCE" -> "💕 연애/관계"
            "EMERGENCY" -> "🚨 긴급 상황"
            else -> "📚 기타"
        }
    }
}
