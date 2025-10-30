package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.ScenarioBranchDao
import com.nihongo.conversation.data.local.ScenarioGoalDao
import com.nihongo.conversation.data.local.ScenarioOutcomeDao
import com.nihongo.conversation.domain.model.GoalType
import com.nihongo.conversation.domain.model.OutcomeType
import com.nihongo.conversation.domain.model.ScenarioBranch
import com.nihongo.conversation.domain.model.ScenarioGoal
import com.nihongo.conversation.domain.model.ScenarioOutcome
import com.nihongo.conversation.domain.model.ScenarioProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScenarioGoalRepository @Inject constructor(
    private val scenarioGoalDao: ScenarioGoalDao,
    private val scenarioOutcomeDao: ScenarioOutcomeDao,
    private val scenarioBranchDao: ScenarioBranchDao
) {

    /**
     * Get all goals for a scenario
     */
    fun getGoalsForScenario(scenarioId: Long): Flow<List<ScenarioGoal>> {
        return scenarioGoalDao.getGoalsForScenario(scenarioId)
    }

    /**
     * Get all outcomes for a scenario
     */
    suspend fun getOutcomesForScenario(scenarioId: Long): List<ScenarioOutcome> {
        return scenarioOutcomeDao.getOutcomesForScenario(scenarioId)
    }

    /**
     * Get branches for a scenario
     */
    suspend fun getBranchesForScenario(scenarioId: Long): List<ScenarioBranch> {
        return scenarioBranchDao.getBranchesForScenario(scenarioId)
    }

    /**
     * Check if a branch should be triggered at current message count
     */
    suspend fun getBranchAtPoint(scenarioId: Long, messageCount: Int): ScenarioBranch? {
        return scenarioBranchDao.getBranchAtPoint(scenarioId, messageCount)
    }

    /**
     * Evaluate if a goal is completed based on progress
     */
    fun evaluateGoal(goal: ScenarioGoal, progress: ScenarioProgress): Boolean {
        return when (goal.goalType) {
            GoalType.COMPLETE_TASK -> {
                // Check if task-related keywords were mentioned
                val keywords = goal.keywords?.split(",") ?: emptyList()
                keywords.any { keyword ->
                    progress.achievedKeywords.contains(keyword.trim())
                }
            }
            GoalType.REACH_KEYWORD -> {
                // Check if specific keywords were mentioned
                val keywords = goal.keywords?.split(",") ?: emptyList()
                val mentionedCount = keywords.count { keyword ->
                    progress.achievedKeywords.contains(keyword.trim())
                }
                mentionedCount >= goal.targetValue
            }
            GoalType.MAINTAIN_POLITENESS -> {
                // Check grammar mistakes (politeness violations would be flagged)
                progress.grammarMistakes <= goal.targetValue
            }
            GoalType.AVOID_MISTAKES -> {
                // Check total grammar mistakes
                progress.grammarMistakes <= goal.targetValue
            }
            GoalType.TIME_LIMIT -> {
                // Check elapsed time
                progress.elapsedTimeMinutes <= goal.targetValue
            }
            GoalType.PERSUADE -> {
                // Check if persuasion keywords were used
                val keywords = goal.keywords?.split(",") ?: emptyList()
                keywords.any { keyword ->
                    progress.achievedKeywords.contains(keyword.trim())
                }
            }
            GoalType.GET_INFORMATION -> {
                // Check if information keywords were obtained
                val keywords = goal.keywords?.split(",") ?: emptyList()
                val obtained = keywords.count { keyword ->
                    progress.achievedKeywords.contains(keyword.trim())
                }
                obtained >= goal.targetValue
            }
            GoalType.BUILD_RAPPORT -> {
                // Check if rapport-building phrases were used
                val keywords = goal.keywords?.split(",") ?: emptyList()
                val used = keywords.count { keyword ->
                    progress.achievedKeywords.contains(keyword.trim())
                }
                used >= goal.targetValue
            }
        }
    }

    /**
     * Calculate completion score based on goals
     */
    suspend fun calculateScore(scenarioId: Long, progress: ScenarioProgress): Int {
        val goals = scenarioGoalDao.getGoalsForScenario(scenarioId).first()
        var totalScore = 0

        goals.forEach { goal ->
            if (evaluateGoal(goal, progress)) {
                totalScore += goal.points
            }
        }

        return totalScore
    }

    /**
     * Determine which outcome to show based on progress
     */
    suspend fun determineOutcome(scenarioId: Long, progress: ScenarioProgress): ScenarioOutcome? {
        val score = calculateScore(scenarioId, progress)

        // Get outcome based on score
        val scoreOutcome = scenarioOutcomeDao.getOutcomeByScore(scenarioId, score)
        if (scoreOutcome != null) return scoreOutcome

        // Fallback to default outcome
        val outcomes = scenarioOutcomeDao.getOutcomesForScenario(scenarioId)
        return outcomes.firstOrNull { it.outcomeType == OutcomeType.PARTIAL_SUCCESS }
            ?: outcomes.firstOrNull()
    }

    /**
     * Extract keywords from user message for goal tracking
     */
    fun extractKeywords(message: String): Set<String> {
        // Simple keyword extraction - can be enhanced with NLP
        val keywords = mutableSetOf<String>()

        // Common task completion keywords
        val taskKeywords = listOf(
            "注文", "予約", "お願い", "ください", "します", "しました",
            "できます", "わかりました", "承知", "確認",
            "ありがとう", "すみません", "失礼", "申し訳"
        )

        taskKeywords.forEach { keyword ->
            if (message.contains(keyword)) {
                keywords.add(keyword)
            }
        }

        return keywords
    }

    /**
     * Update progress with new message
     */
    fun updateProgress(
        progress: ScenarioProgress,
        userMessage: String,
        grammarMistakes: Int = 0
    ): ScenarioProgress {
        val newKeywords = extractKeywords(userMessage)

        return progress.copy(
            messageCount = progress.messageCount + 1,
            achievedKeywords = progress.achievedKeywords + newKeywords,
            grammarMistakes = progress.grammarMistakes + grammarMistakes
        )
    }

    /**
     * Check if all required goals are completed
     */
    suspend fun areRequiredGoalsCompleted(scenarioId: Long, progress: ScenarioProgress): Boolean {
        val requiredGoals = scenarioGoalDao.getRequiredGoals(scenarioId)
        return requiredGoals.all { goal -> evaluateGoal(goal, progress) }
    }
}
