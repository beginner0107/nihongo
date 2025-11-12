package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.DailyQuestDao
import com.nihongo.conversation.data.local.UserPointsDao
import com.nihongo.conversation.data.local.entity.DailyQuestEntity
import com.nihongo.conversation.data.local.entity.QuestType
import com.nihongo.conversation.data.local.entity.UserPointsEntity
import com.nihongo.conversation.domain.model.Quest
import com.nihongo.conversation.domain.model.UserPoints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestRepository @Inject constructor(
    private val questDao: DailyQuestDao,
    private val pointsDao: UserPointsDao
) {
    /**
     * Get active quests for a user
     */
    fun getActiveQuests(userId: Long): Flow<List<Quest>> {
        return questDao.getActiveQuests(userId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Get user points with reactive updates
     */
    fun getUserPoints(userId: Long): Flow<UserPoints?> {
        return pointsDao.getUserPoints(userId)
            .map { entity ->
                entity?.toDomainModel()
            }
    }

    /**
     * Initialize user points if not exists
     */
    suspend fun initializeUserPoints(userId: Long) {
        val existing = pointsDao.getUserPointsOnce(userId)
        if (existing == null) {
            pointsDao.insertUserPoints(
                UserPointsEntity(
                    userId = userId,
                    totalPoints = 0,
                    todayPoints = 0,
                    weeklyPoints = 0,
                    level = 1,
                    weeklyRank = null,
                    lastResetDate = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Generate daily quests for a user (3 random quests)
     */
    suspend fun generateDailyQuests(userId: Long) {
        // Delete old quests first
        questDao.deleteExpiredQuests()

        // Check if user already has active quests for today
        val activeQuests = questDao.getActiveQuests(userId).first()
        if (activeQuests.isNotEmpty()) {
            return  // Already has quests for today
        }

        // Calculate midnight (next day 00:00:00)
        val midnight = java.time.LocalDate.now()
            .plusDays(1)
            .atStartOfDay()
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Quest templates
        val questTemplates = listOf(
            DailyQuestEntity(
                userId = userId,
                title = "대화 연습하기",
                description = "AI와 10개 메시지 주고받기",
                questType = QuestType.MESSAGE_COUNT.name,
                targetValue = 10,
                rewardPoints = 30,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "시나리오 완주",
                description = "시나리오 1개 완료하기",
                questType = QuestType.SCENARIO_COMPLETE.name,
                targetValue = 1,
                rewardPoints = 50,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "음성 전용 대화",
                description = "음성만으로 대화 완료",
                questType = QuestType.VOICE_ONLY_SESSION.name,
                targetValue = 1,
                rewardPoints = 40,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "단어 복습",
                description = "플래시카드 20개 복습하기",
                questType = QuestType.VOCABULARY_REVIEW.name,
                targetValue = 20,
                rewardPoints = 25,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "발음 연습",
                description = "발음 연습 5회 완료",
                questType = QuestType.PRONUNCIATION_PRACTICE.name,
                targetValue = 5,
                rewardPoints = 35,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "문법 마스터",
                description = "문법 분석 3회 사용",
                questType = QuestType.GRAMMAR_ANALYSIS.name,
                targetValue = 3,
                rewardPoints = 20,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "새로운 도전",
                description = "새로운 시나리오 시작하기",
                questType = QuestType.NEW_SCENARIO.name,
                targetValue = 1,
                rewardPoints = 15,
                expiresAt = midnight
            )
        )

        // Select 3 random quests
        val selectedQuests = questTemplates.shuffled().take(3)
        questDao.insertQuests(selectedQuests)
    }

    /**
     * Update quest progress
     */
    suspend fun updateQuestProgress(questId: Long, value: Int) {
        val quest = questDao.getQuestById(questId)
        if (quest != null && !quest.isCompleted) {
            questDao.updateQuestProgress(questId, value)

            // Auto-complete if target reached
            if (value >= quest.targetValue) {
                completeQuest(questId)
            }
        }
    }

    /**
     * Complete a quest and award points
     */
    suspend fun completeQuest(questId: Long): Int {
        val quest = questDao.getQuestById(questId)
        if (quest != null && !quest.isCompleted) {
            // Mark as completed
            questDao.completeQuest(questId)

            // Award points
            addPoints(quest.userId, quest.rewardPoints)

            return quest.rewardPoints
        }
        return 0
    }

    /**
     * Add points to user account
     */
    suspend fun addPoints(userId: Long, points: Int) {
        // Ensure user points entry exists
        initializeUserPoints(userId)

        // Add points
        pointsDao.addPoints(userId, points)

        // Check for level up
        val userPoints = pointsDao.getUserPointsOnce(userId)
        if (userPoints != null) {
            val newLevel = (userPoints.totalPoints / 100) + 1
            if (newLevel > userPoints.level) {
                pointsDao.updateLevel(userId, newLevel)
            }
        }
    }

    /**
     * Increment quest progress by type (helper for ChatViewModel)
     */
    suspend fun incrementQuestProgressByType(userId: Long, questType: QuestType, amount: Int = 1) {
        val activeQuests = questDao.getActiveQuests(userId).first()
        activeQuests
            .filter { it.questType == questType.name && !it.isCompleted }
            .forEach { quest ->
                val newValue = quest.currentValue + amount
                updateQuestProgress(quest.id, newValue)
            }
    }

    /**
     * Reset daily points at midnight
     */
    suspend fun resetDailyPoints(userId: Long) {
        val now = System.currentTimeMillis()
        pointsDao.resetDailyPoints(userId, now)
    }

    /**
     * Reset weekly points on Monday
     */
    suspend fun resetWeeklyPoints(userId: Long) {
        pointsDao.resetWeeklyPoints(userId)
    }

    /**
     * Get weekly leaderboard
     */
    suspend fun getWeeklyLeaderboard(limit: Int = 10): List<UserPoints> {
        return pointsDao.getTopUsersByWeeklyPoints(limit)
            .map { it.toDomainModel() }
    }
}

// Extension functions for entity to domain model conversion
private fun DailyQuestEntity.toDomainModel() = Quest(
    id = id,
    title = title,
    description = description,
    type = QuestType.valueOf(questType),
    targetValue = targetValue,
    currentValue = currentValue,
    rewardPoints = rewardPoints,
    expiresAt = expiresAt,
    isCompleted = isCompleted
)

private fun UserPointsEntity.toDomainModel() = UserPoints(
    userId = userId,
    totalPoints = totalPoints,
    todayPoints = todayPoints,
    weeklyPoints = weeklyPoints,
    level = level,
    pointsToNextLevel = (level * 100) - (totalPoints % 100),
    weeklyRank = weeklyRank
)
