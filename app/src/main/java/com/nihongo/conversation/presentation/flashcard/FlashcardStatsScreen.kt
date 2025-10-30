package com.nihongo.conversation.presentation.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.presentation.stats.BarChart
import com.nihongo.conversation.presentation.stats.LineChart
import com.nihongo.conversation.presentation.stats.PieChart
import com.nihongo.conversation.presentation.stats.StatCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStatsScreen(
    onBackClick: () -> Unit,
    viewModel: FlashcardStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("단어장 통계") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Stats Cards
                item {
                    Text(
                        "개요",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "총 단어 수",
                            value = uiState.totalWords.toString(),
                            icon = {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "마스터함",
                            value = uiState.masteredWords.toString(),
                            icon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "복습 대기",
                            value = uiState.dueWords.toString(),
                            icon = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "신규",
                            value = uiState.newWords.toString(),
                            icon = {
                                Icon(
                                    Icons.Default.FiberNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Streak Section
                item {
                    Text(
                        "학습 스트릭",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "현재 스트릭",
                            value = "${uiState.currentStreak}일",
                            subtitle = "연속 학습 일수",
                            icon = {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B35)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "최장 스트릭",
                            value = "${uiState.longestStreak}일",
                            subtitle = "역대 최고 기록",
                            icon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Calendar Heatmap
                item {
                    CalendarHeatmap(
                        reviewCalendar = uiState.reviewCalendar,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mastery Progress Pie Chart
                if (uiState.masteryData.isNotEmpty()) {
                    item {
                        Text(
                            "학습 현황",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PieChart(
                                    data = uiState.masteryData,
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary,      // マスター済み
                                        MaterialTheme.colorScheme.primary,       // 学習中
                                        MaterialTheme.colorScheme.secondary      // 新規
                                    ),
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(16.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Legend
                                uiState.masteryData.forEachIndexed { index, (label, count) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (index) {
                                                            0 -> MaterialTheme.colorScheme.tertiary
                                                            1 -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.secondary
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(label)
                                        }
                                        Text(
                                            "$count 単語",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Accuracy Trend Line Chart
                if (uiState.accuracyTrend.isNotEmpty()) {
                    item {
                        Text(
                            "정답률 추이",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "최근 7일간 평균 정답률",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LineChart(
                                    data = uiState.accuracyTrend,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    lineColor = MaterialTheme.colorScheme.tertiary,
                                    pointColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                // Daily Review Count Bar Chart
                if (uiState.dailyReviewCount.isNotEmpty()) {
                    item {
                        Text(
                            "복습 횟수",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "최근 7일간 복습한 단어 수",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                BarChart(
                                    data = uiState.dailyReviewCount,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    barColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Personal Bests
                item {
                    Text(
                        "개인 최고 기록",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AchievementRow(
                                icon = Icons.Default.WorkspacePremium,
                                title = "最高正解率",
                                value = "${uiState.bestAccuracy}%",
                                iconTint = Color(0xFFFFD700)
                            )
                            Divider()
                            AchievementRow(
                                icon = Icons.Default.TrendingUp,
                                title = "1日最多復習",
                                value = "${uiState.bestDailyReviews}単語",
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                            Divider()
                            AchievementRow(
                                icon = Icons.Default.Speed,
                                title = "最速習得",
                                value = uiState.fastestMastery,
                                iconTint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Top Improved Words
                if (uiState.topImprovedWords.isNotEmpty()) {
                    item {
                        Text(
                            "最も上達した単語",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.topImprovedWords.forEachIndexed { index, wordProgress ->
                                    if (index > 0) {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                    WordProgressItem(
                                        rank = index + 1,
                                        wordProgress = wordProgress
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error Snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("閉じる")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun CalendarHeatmap(
    reviewCalendar: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "学習カレンダー（過去30日）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "濃い色ほど多くの単語を復習した日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Calendar grid (7 columns for weeks)
            val sortedDates = reviewCalendar.keys.sorted()
            val maxCount = reviewCalendar.values.maxOrNull() ?: 1

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Create rows of 7 days each
                sortedDates.chunked(7).forEach { weekDates ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        weekDates.forEach { date ->
                            val count = reviewCalendar[date] ?: 0
                            CalendarDay(
                                date = date,
                                count = count,
                                maxCount = maxCount,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining days in the week with empty boxes
                        repeat(7 - weekDates.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "少",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                repeat(5) { level ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(level / 4f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                Text(
                    "多",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CalendarDay(
    date: String,
    count: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    val intensity = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFormat = SimpleDateFormat("d", Locale.getDefault())
    val parsedDate = dateFormat.parse(date)
    val dayOfMonth = parsedDate?.let { displayFormat.format(it) } ?: ""

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(getHeatmapColor(intensity))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (count > 0) {
            Text(
                dayOfMonth,
                fontSize = 10.sp,
                color = if (intensity > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun getHeatmapColor(intensity: Float): Color {
    return when {
        intensity == 0f -> MaterialTheme.colorScheme.surface
        intensity < 0.25f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        intensity < 0.5f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        intensity < 0.75f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun AchievementRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun WordProgressItem(
    rank: Int,
    wordProgress: WordProgress,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    wordProgress.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    wordProgress.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${wordProgress.reviewCount}回復習",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "+${wordProgress.accuracyChange.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                "向上",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
