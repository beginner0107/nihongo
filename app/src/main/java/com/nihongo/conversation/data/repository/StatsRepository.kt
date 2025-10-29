package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.ConversationDao
import com.nihongo.conversation.data.local.MessageDao
import com.nihongo.conversation.data.local.ScenarioDao
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.Message
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class DailyStats(
    val date: Date,
    val messageCount: Int,
    val studyTimeMinutes: Int,
    val conversationsCount: Int
)

data class ScenarioProgress(
    val scenarioId: Long,
    val scenarioTitle: String,
    val conversationsCount: Int,
    val messagesCount: Int
)

data class StudyStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStudyDate: Date?
)

data class WeeklyStats(
    val startDate: Date,
    val endDate: Date,
    val totalMessages: Int,
    val totalStudyMinutes: Int,
    val totalConversations: Int,
    val dailyStats: List<DailyStats>
)

@Singleton
class StatsRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val scenarioDao: ScenarioDao
) {
    private val userId = 1L // TODO: Get from user session

    /**
     * Calculate daily statistics for a date range
     */
    suspend fun getDailyStats(startDate: Date, endDate: Date): List<DailyStats> {
        val conversations = conversationDao.getConversationsByUser(userId).first()
        val calendar = Calendar.getInstance()

        val dailyStatsMap = mutableMapOf<String, MutableList<Conversation>>()
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Group conversations by date
        conversations.forEach { conversation ->
            val convDate = Date(conversation.createdAt)
            if (convDate.time in startDate.time..endDate.time) {
                val dateKey = dateFormatter.format(convDate)
                dailyStatsMap.getOrPut(dateKey) { mutableListOf() }.add(conversation)
            }
        }

        // Calculate stats for each day
        return dailyStatsMap.map { (dateKey, convos) ->
            val date = dateFormatter.parse(dateKey) ?: Date()
            var totalMessages = 0
            var estimatedMinutes = 0

            convos.forEach { conversation ->
                val messages = messageDao.getMessagesByConversation(conversation.id).first()
                totalMessages += messages.size
                estimatedMinutes += estimateStudyTime(messages)
            }

            DailyStats(
                date = date,
                messageCount = totalMessages,
                studyTimeMinutes = estimatedMinutes,
                conversationsCount = convos.size
            )
        }.sortedBy { it.date }
    }

    /**
     * Get weekly statistics
     */
    suspend fun getWeeklyStats(): WeeklyStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val dailyStats = getDailyStats(startDate, endDate)

        return WeeklyStats(
            startDate = startDate,
            endDate = endDate,
            totalMessages = dailyStats.sumOf { it.messageCount },
            totalStudyMinutes = dailyStats.sumOf { it.studyTimeMinutes },
            totalConversations = dailyStats.sumOf { it.conversationsCount },
            dailyStats = dailyStats
        )
    }

    /**
     * Get monthly statistics
     */
    suspend fun getMonthlyStats(): WeeklyStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val dailyStats = getDailyStats(startDate, endDate)

        return WeeklyStats(
            startDate = startDate,
            endDate = endDate,
            totalMessages = dailyStats.sumOf { it.messageCount },
            totalStudyMinutes = dailyStats.sumOf { it.studyTimeMinutes },
            totalConversations = dailyStats.sumOf { it.conversationsCount },
            dailyStats = dailyStats
        )
    }

    /**
     * Calculate scenario completion rates
     */
    suspend fun getScenarioProgress(): List<ScenarioProgress> {
        val scenarios = scenarioDao.getAllScenarios().first()
        val conversations = conversationDao.getConversationsByUser(userId).first()

        return scenarios.map { scenario ->
            val scenarioConversations = conversations.filter { it.scenarioId == scenario.id }
            var totalMessages = 0

            scenarioConversations.forEach { conversation ->
                val messages = messageDao.getMessagesByConversation(conversation.id).first()
                totalMessages += messages.size
            }

            ScenarioProgress(
                scenarioId = scenario.id,
                scenarioTitle = scenario.title,
                conversationsCount = scenarioConversations.size,
                messagesCount = totalMessages
            )
        }.filter { it.conversationsCount > 0 || it.messagesCount > 0 }
    }

    /**
     * Calculate study streak (consecutive days)
     */
    suspend fun getStudyStreak(): StudyStreak {
        val conversations = conversationDao.getConversationsByUser(userId).first()
        if (conversations.isEmpty()) {
            return StudyStreak(0, 0, null)
        }

        // Get unique study dates
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val studyDates = conversations
            .map { dateFormatter.format(Date(it.createdAt)) }
            .distinct()
            .mapNotNull { dateFormatter.parse(it) }
            .sortedDescending()

        if (studyDates.isEmpty()) {
            return StudyStreak(0, 0, null)
        }

        // Calculate current streak
        var currentStreak = 0
        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Check if studied today or yesterday
        val lastStudyDate = studyDates.first()
        val daysSinceLastStudy = daysBetween(lastStudyDate, today)

        if (daysSinceLastStudy <= 1) {
            currentStreak = 1
            var previousDate = lastStudyDate

            for (i in 1 until studyDates.size) {
                val currentDate = studyDates[i]
                val daysDiff = daysBetween(currentDate, previousDate)

                if (daysDiff == 1) {
                    currentStreak++
                    previousDate = currentDate
                } else {
                    break
                }
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 1

        for (i in 1 until studyDates.size) {
            val daysDiff = daysBetween(studyDates[i], studyDates[i - 1])

            if (daysDiff == 1) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        return StudyStreak(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastStudyDate = lastStudyDate
        )
    }

    /**
     * Get total statistics (all time)
     */
    suspend fun getTotalStats(): Triple<Int, Int, Int> {
        val conversations = conversationDao.getConversationsByUser(userId).first()
        var totalMessages = 0
        var totalMinutes = 0

        conversations.forEach { conversation ->
            val messages = messageDao.getMessagesByConversation(conversation.id).first()
            totalMessages += messages.size
            totalMinutes += estimateStudyTime(messages)
        }

        return Triple(conversations.size, totalMessages, totalMinutes)
    }

    /**
     * Estimate study time based on message count and length
     * Assumes: ~30 seconds per message exchange (reading, thinking, typing)
     */
    private fun estimateStudyTime(messages: List<Message>): Int {
        if (messages.isEmpty()) return 0

        val avgTimePerMessage = 0.5 // minutes
        val baseTime = messages.size * avgTimePerMessage

        // Add bonus time for longer messages (more reading/typing)
        val bonusTime = messages.sumOf { message ->
            when {
                message.content.length > 100 -> 0.5
                message.content.length > 50 -> 0.25
                else -> 0.0
            }
        }

        return (baseTime + bonusTime).toInt().coerceAtLeast(1)
    }

    /**
     * Calculate days between two dates
     */
    private fun daysBetween(date1: Date, date2: Date): Int {
        val calendar1 = Calendar.getInstance().apply {
            time = date1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calendar2 = Calendar.getInstance().apply {
            time = date2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = calendar2.timeInMillis - calendar1.timeInMillis
        return kotlin.math.abs((diffInMillis / (1000 * 60 * 60 * 24)).toInt())
    }
}
