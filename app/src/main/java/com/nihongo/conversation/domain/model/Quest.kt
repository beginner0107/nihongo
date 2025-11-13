package com.nihongo.conversation.domain.model

import com.nihongo.conversation.data.local.entity.QuestType

/**
 * Domain model for daily quest
 */
data class Quest(
    val id: Long,
    val title: String,
    val description: String,
    val type: QuestType,
    val targetValue: Int,
    val currentValue: Int,
    val rewardPoints: Int,
    val expiresAt: Long,
    val isCompleted: Boolean,
    val progress: Float = if (targetValue > 0) {
        (currentValue.toFloat() / targetValue).coerceAtMost(1f)
    } else {
        0f
    }
)

/**
 * Domain model for user points and level
 */
data class UserPoints(
    val userId: Long,
    val totalPoints: Int,
    val todayPoints: Int,
    val weeklyPoints: Int,
    val level: Int,
    val pointsToNextLevel: Int = (level * 100) - (totalPoints % 100),
    val weeklyRank: Int?
)
