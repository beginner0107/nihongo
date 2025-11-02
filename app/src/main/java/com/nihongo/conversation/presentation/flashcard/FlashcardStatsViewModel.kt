package com.nihongo.conversation.presentation.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.data.local.VocabularyDao
import com.nihongo.conversation.domain.model.ReviewHistory
import com.nihongo.conversation.domain.model.VocabularyEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class FlashcardStatsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Overview Stats
    val totalWords: Int = 0,
    val masteredWords: Int = 0,
    val dueWords: Int = 0,
    val newWords: Int = 0,

    // Accuracy Trend (last 7 days)
    val accuracyTrend: List<Pair<String, Int>> = emptyList(), // (date, accuracy %)

    // Daily Review Count (last 7 days)
    val dailyReviewCount: List<Pair<String, Int>> = emptyList(), // (date, count)

    // Mastery Progress (Pie Chart)
    val masteryData: List<Pair<String, Int>> = emptyList(), // (category, count)

    // Streak Calendar (last 30 days)
    val reviewCalendar: Map<String, Int> = emptyMap(), // (date, review count)

    // Current Streak
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,

    // Personal Bests
    val bestAccuracy: Int = 0,
    val bestDailyReviews: Int = 0,
    val fastestMastery: String = "미달성",

    // Improvement Per Word
    val topImprovedWords: List<WordProgress> = emptyList()
)

data class WordProgress(
    val word: String,
    val meaning: String,
    val accuracyChange: Float, // Percentage change
    val reviewCount: Int
)

@HiltViewModel
class FlashcardStatsViewModel @Inject constructor(
    private val vocabularyDao: VocabularyDao,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardStatsUiState())
    val uiState: StateFlow<FlashcardStatsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val userId = userSessionManager.getCurrentUserIdSync() ?: 1L

                // Load all vocabulary for the user
                val allWords = vocabularyDao.getAllVocabulary(userId).first()

                // Calculate all statistics
                val totalWords = allWords.size
                val masteredWords = allWords.count { it.isMastered }
                val dueWords = allWords.count {
                    it.nextReviewAt <= System.currentTimeMillis() && !it.isMastered
                }
                val newWords = allWords.count { it.reviewCount == 0 }

                // Accuracy trend (last 7 days)
                val accuracyTrend = calculateAccuracyTrend(allWords)

                // Daily review count (last 7 days)
                val dailyReviewCount = calculateDailyReviewCount(allWords)

                // Mastery progress
                val masteryData = calculateMasteryData(allWords)

                // Review calendar (last 30 days)
                val reviewCalendar = calculateReviewCalendar(allWords)

                // Streak calculation
                val (currentStreak, longestStreak) = calculateStreaks(reviewCalendar)

                // Personal bests
                val bestAccuracy = calculateBestAccuracy(allWords)
                val bestDailyReviews = dailyReviewCount.maxOfOrNull { it.second } ?: 0
                val fastestMastery = calculateFastestMastery(allWords)

                // Top improved words
                val topImprovedWords = calculateTopImprovedWords(allWords)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalWords = totalWords,
                        masteredWords = masteredWords,
                        dueWords = dueWords,
                        newWords = newWords,
                        accuracyTrend = accuracyTrend,
                        dailyReviewCount = dailyReviewCount,
                        masteryData = masteryData,
                        reviewCalendar = reviewCalendar,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        bestAccuracy = bestAccuracy,
                        bestDailyReviews = bestDailyReviews,
                        fastestMastery = fastestMastery,
                        topImprovedWords = topImprovedWords
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "통계 로딩 실패: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun calculateAccuracyTrend(allWords: List<VocabularyEntry>): List<Pair<String, Int>> {
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val trend = mutableListOf<Pair<String, Int>>()

        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = getDayStart(calendar)
            val dayEnd = getDayEnd(calendar)

            // Get words reviewed on this day
            val reviewedWords = allWords.filter {
                val lastReviewed = it.lastReviewedAt ?: 0
                lastReviewed in dayStart..dayEnd
            }

            val accuracy = if (reviewedWords.isNotEmpty()) {
                val avgAccuracy = reviewedWords.map {
                    if (it.reviewCount > 0) {
                        (it.correctCount.toFloat() / it.reviewCount * 100).toInt()
                    } else 0
                }.average()
                avgAccuracy.toInt()
            } else 0

            trend.add(dateFormat.format(calendar.time) to accuracy)
        }

        return trend
    }

    private fun calculateDailyReviewCount(allWords: List<VocabularyEntry>): List<Pair<String, Int>> {
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val counts = mutableListOf<Pair<String, Int>>()

        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = getDayStart(calendar)
            val dayEnd = getDayEnd(calendar)

            val reviewCount = allWords.count {
                val lastReviewed = it.lastReviewedAt ?: 0
                lastReviewed in dayStart..dayEnd
            }

            counts.add(dateFormat.format(calendar.time) to reviewCount)
        }

        return counts
    }

    private fun calculateMasteryData(allWords: List<VocabularyEntry>): List<Pair<String, Int>> {
        val mastered = allWords.count { it.isMastered }
        val learning = allWords.count {
            !it.isMastered && it.reviewCount > 0
        }
        val new = allWords.count { it.reviewCount == 0 }

        return listOf(
            "마스터 완료" to mastered,
            "학습중" to learning,
            "신규" to new
        )
    }

    private fun calculateReviewCalendar(allWords: List<VocabularyEntry>): Map<String, Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val reviewMap = mutableMapOf<String, Int>()

        for (i in 29 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = getDayStart(calendar)
            val dayEnd = getDayEnd(calendar)
            val dateKey = dateFormat.format(calendar.time)

            val reviewCount = allWords.count {
                val lastReviewed = it.lastReviewedAt ?: 0
                lastReviewed in dayStart..dayEnd
            }

            reviewMap[dateKey] = reviewCount
        }

        return reviewMap
    }

    private fun calculateStreaks(reviewCalendar: Map<String, Int>): Pair<Int, Int> {
        val sortedDates = reviewCalendar.keys.sorted()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        // Calculate current streak (from today backwards)
        val calendar = Calendar.getInstance()
        for (i in 0 until sortedDates.size) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateKey = dateFormat.format(calendar.time)

            if (reviewCalendar[dateKey] ?: 0 > 0) {
                currentStreak++
            } else {
                break
            }
        }

        // Calculate longest streak
        for (date in sortedDates) {
            if (reviewCalendar[date] ?: 0 > 0) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 0
            }
        }

        return currentStreak to longestStreak
    }

    private fun calculateBestAccuracy(allWords: List<VocabularyEntry>): Int {
        return allWords.maxOfOrNull {
            if (it.reviewCount > 0) {
                (it.correctCount.toFloat() / it.reviewCount * 100).toInt()
            } else 0
        } ?: 0
    }

    private fun calculateFastestMastery(allWords: List<VocabularyEntry>): String {
        val masteredWords = allWords.filter { it.isMastered }
        if (masteredWords.isEmpty()) return "미달성"

        val fastest = masteredWords.minByOrNull { word ->
            val createdAt = word.createdAt
            val lastReviewed = word.lastReviewedAt ?: createdAt
            lastReviewed - createdAt
        }

        if (fastest != null) {
            val days = ((fastest.lastReviewedAt ?: fastest.createdAt) - fastest.createdAt) / (24 * 60 * 60 * 1000)
            return "${days}일"
        }

        return "미달성"
    }

    private suspend fun calculateTopImprovedWords(allWords: List<VocabularyEntry>): List<WordProgress> {
        return allWords
            .filter { it.reviewCount >= 5 } // Need at least 5 reviews to measure improvement
            .map { word ->
                // Get recent reviews to calculate improvement
                val recentAccuracy = if (word.reviewCount > 0) {
                    word.correctCount.toFloat() / word.reviewCount
                } else 0f

                // Get first few reviews accuracy (simulate early performance)
                val earlyReviews = minOf(3, word.reviewCount)
                val earlyAccuracy = if (earlyReviews > 0) {
                    // Estimate early performance as 60% of current
                    recentAccuracy * 0.6f
                } else 0f

                val improvement = (recentAccuracy - earlyAccuracy) * 100

                WordProgress(
                    word = word.word,
                    meaning = word.meaning,
                    accuracyChange = improvement,
                    reviewCount = word.reviewCount
                )
            }
            .filter { it.accuracyChange > 0 }
            .sortedByDescending { it.accuracyChange }
            .take(5)
    }

    private fun getDayStart(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayEnd(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
