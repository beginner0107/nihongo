package com.nihongo.conversation.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.data.repository.ScenarioProgress
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBackClick: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "학습 통계",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "당신의 진도를 확인하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer
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
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = uiState.error ?: "오류가 발생했습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Period Toggle
                item {
                    TimePeriodToggle(
                        selectedPeriod = uiState.timePeriod,
                        onPeriodChange = viewModel::setTimePeriod
                    )
                }

                // Streak Counter
                item {
                    uiState.studyStreak?.let { streak ->
                        StreakCard(streak = streak)
                    }
                }

                // Summary Stats
                item {
                    uiState.totalStats?.let { (conversations, messages, minutes) ->
                        SummaryStatsRow(
                            conversations = conversations,
                            messages = messages,
                            minutes = minutes
                        )
                    }
                }

                // Daily Study Time (Bar Chart)
                item {
                    val periodStats = viewModel.getCurrentPeriodStats()
                    periodStats?.let { stats ->
                        ChartCard(
                            title = "학습 시간",
                            subtitle = "${uiState.timePeriod.toKorean()}의 학습 시간"
                        ) {
                            val chartData = prepareChartData(stats.dailyStats, uiState.timePeriod) { it.studyTimeMinutes }
                            BarChart(
                                data = chartData,
                                barColor = MaterialTheme.colorScheme.primary,
                                label = "분"
                            )
                        }
                    }
                }

                // Messages Per Day (Line Chart)
                item {
                    val periodStats = viewModel.getCurrentPeriodStats()
                    periodStats?.let { stats ->
                        ChartCard(
                            title = "메시지 수",
                            subtitle = "${uiState.timePeriod.toKorean()}의 메시지 수"
                        ) {
                            val chartData = prepareChartData(stats.dailyStats, uiState.timePeriod) { it.messageCount }
                            LineChart(
                                data = chartData,
                                lineColor = MaterialTheme.colorScheme.secondary,
                                pointColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Scenario Progress (Pie Chart)
                item {
                    if (uiState.scenarioProgress.isNotEmpty()) {
                        ScenarioProgressCard(
                            scenarioProgress = uiState.scenarioProgress
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePeriodToggle(
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            selected = selectedPeriod == TimePeriod.WEEK,
            onClick = { onPeriodChange(TimePeriod.WEEK) },
            label = { Text("주간") },
            leadingIcon = if (selectedPeriod == TimePeriod.WEEK) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = selectedPeriod == TimePeriod.MONTH,
            onClick = { onPeriodChange(TimePeriod.MONTH) },
            label = { Text("월간") },
            leadingIcon = if (selectedPeriod == TimePeriod.MONTH) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
    }
}

@Composable
fun StreakCard(
    streak: com.nihongo.conversation.data.repository.StudyStreak,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "연속 학습",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${streak.currentStreak} 일간",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "최장: ${streak.longestStreak}일",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
fun SummaryStatsRow(
    conversations: Int,
    messages: Int,
    minutes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "대화 수",
            value = conversations.toString(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "메시지",
            value = messages.toString(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "학습 시간",
            value = "${minutes}분",
            icon = {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
fun ScenarioProgressCard(
    scenarioProgress: List<ScenarioProgress>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.surfaceTint,
        Color(0xFFFFA726) // Orange
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "시나리오별 진행도",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chartData = scenarioProgress.map { it.scenarioTitle to it.conversationsCount }
                PieChart(
                    data = chartData,
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )

                ChartLegend(
                    items = scenarioProgress.mapIndexed { index, progress ->
                        "${progress.scenarioTitle} (${progress.conversationsCount})" to colors.getOrElse(index) { colors[0] }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun prepareChartData(
    dailyStats: List<com.nihongo.conversation.data.repository.DailyStats>,
    timePeriod: TimePeriod,
    valueSelector: (com.nihongo.conversation.data.repository.DailyStats) -> Int
): List<Pair<String, Int>> {
    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormatter = SimpleDateFormat("E", Locale.KOREAN) // Day of week

    return when (timePeriod) {
        TimePeriod.WEEK -> {
            // 7 days of the week (Mon-Sun)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            (0..6).map { dayOffset ->
                val date = calendar.time
                val dateKey = dateFormatter.format(date)
                val stat = dailyStats.find { dateFormatter.format(it.date) == dateKey }
                val label = labelFormatter.format(date)
                calendar.add(Calendar.DAY_OF_WEEK, 1)
                label to (stat?.let(valueSelector) ?: 0)
            }
        }
        TimePeriod.MONTH -> {
            // Group by weeks in the month
            dailyStats.groupBy { stat ->
                val cal = Calendar.getInstance()
                cal.time = stat.date
                cal.get(Calendar.WEEK_OF_MONTH)
            }.map { (weekNum, stats) ->
                val totalValue = stats.sumOf(valueSelector)
                "Week $weekNum" to totalValue
            }
        }
    }
}

private fun TimePeriod.toKorean(): String {
    return when (this) {
        TimePeriod.WEEK -> "이번 주"
        TimePeriod.MONTH -> "이번 달"
    }
}
