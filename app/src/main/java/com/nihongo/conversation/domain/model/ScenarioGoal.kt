package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Types of scenario goals
 */
enum class GoalType {
    COMPLETE_TASK,          // Complete a specific task (e.g., order food, book hotel)
    REACH_KEYWORD,          // Mention specific keywords
    MAINTAIN_POLITENESS,    // Keep appropriate politeness level
    AVOID_MISTAKES,         // Make fewer than N grammar mistakes
    TIME_LIMIT,             // Complete within time limit
    PERSUADE,               // Convince AI character
    GET_INFORMATION,        // Extract specific information
    BUILD_RAPPORT           // Achieve positive relationship score
}

/**
 * Individual goal for a scenario
 */
@Entity(
    tableName = "scenario_goals",
    foreignKeys = [
        ForeignKey(
            entity = Scenario::class,
            parentColumns = ["id"],
            childColumns = ["scenarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scenarioId"]),
        Index(value = ["isRequired"])
    ]
)
data class ScenarioGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val scenarioId: Long,

    // Goal details
    val goalType: GoalType,
    val description: String,              // Japanese description
    val descriptionKorean: String,        // Korean translation

    // Success criteria
    val targetValue: Int = 1,             // e.g., 3 keywords, 5 mistakes max
    val keywords: String? = null,         // Comma-separated for REACH_KEYWORD

    // Goal priority
    val isRequired: Boolean = true,       // Must complete for success
    val points: Int = 10,                 // Points awarded for completion
    val order: Int = 0                    // Display order
)

/**
 * Scenario outcome/ending
 */
@Entity(
    tableName = "scenario_outcomes",
    foreignKeys = [
        ForeignKey(
            entity = Scenario::class,
            parentColumns = ["id"],
            childColumns = ["scenarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scenarioId"]),
        Index(value = ["outcomeType"])
    ]
)
data class ScenarioOutcome(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val scenarioId: Long,

    // Outcome details
    val outcomeType: OutcomeType,
    val title: String,                    // Japanese title
    val titleKorean: String,              // Korean translation
    val description: String,              // What happened
    val descriptionKorean: String,

    // Trigger conditions
    val requiredGoals: String? = null,    // Comma-separated goal IDs
    val minScore: Int = 0,                // Minimum score needed
    val maxScore: Int = 100,              // Maximum score for this outcome
    val triggerKeywords: String? = null   // Keywords that trigger this ending
)

/**
 * Types of outcomes
 */
enum class OutcomeType {
    PERFECT_SUCCESS,    // 完璧 - All goals achieved
    SUCCESS,            // 成功 - Main goals achieved
    PARTIAL_SUCCESS,    // 部分的成功 - Some goals achieved
    FAILURE,            // 失敗 - Failed main objectives
    BAD_ENDING,         // バッドエンド - Negative outcome
    SPECIAL             // 特別 - Hidden/special ending
}

/**
 * Conversation branch point
 */
@Entity(
    tableName = "scenario_branches",
    foreignKeys = [
        ForeignKey(
            entity = Scenario::class,
            parentColumns = ["id"],
            childColumns = ["scenarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scenarioId"]),
        Index(value = ["triggerPoint"])
    ]
)
data class ScenarioBranch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val scenarioId: Long,

    // Trigger condition
    val triggerPoint: Int,                // Message count when this activates
    val triggerKeywords: String? = null,  // Keywords that activate branch

    // Branch paths
    val pathAPrompt: String,              // AI prompt for path A
    val pathADescription: String,         // What this leads to (Japanese)
    val pathADescriptionKorean: String,   // Korean translation

    val pathBPrompt: String,              // AI prompt for path B
    val pathBDescription: String,
    val pathBDescriptionKorean: String,

    // Optional third path
    val pathCPrompt: String? = null,
    val pathCDescription: String? = null,
    val pathCDescriptionKorean: String? = null
)

/**
 * User's progress in a scenario (conversation)
 */
data class ScenarioProgress(
    val scenarioId: Long,
    val conversationId: Long,
    val completedGoals: Set<Long> = emptySet(),
    val currentScore: Int = 0,
    val messageCount: Int = 0,
    val selectedBranches: Map<Long, String> = emptyMap(), // branchId -> selected path (A/B/C)
    val achievedKeywords: Set<String> = emptySet(),
    val grammarMistakes: Int = 0,
    val startTime: Long = System.currentTimeMillis()
) {
    val elapsedTimeMinutes: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000 / 60).toInt()

    val completionPercentage: Int
        get() = if (completedGoals.isEmpty()) 0 else (currentScore * 100) / 100
}

/**
 * Enhanced scenario type with task-based categories
 */
enum class ScenarioCategory {
    DAILY_CONVERSATION,     // 日常会話
    JOB_INTERVIEW,         // 面接
    COMPLAINT_HANDLING,    // クレーム対応
    EMERGENCY,             // 緊急事態
    DATING,                // デート
    BUSINESS,              // ビジネス
    RELATIONSHIP,          // 恋人との会話
    SHOPPING,              // 買い物
    TRAVEL                 // 旅行
}
