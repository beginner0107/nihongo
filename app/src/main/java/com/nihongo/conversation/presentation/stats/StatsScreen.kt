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
import com.nihongo.conversation.core.theme.AppDesignSystem
import com.nihongo.conversation.data.repository.ScenarioProgress
import com.nihongo.conversation.presentation.components.ColoredChip
import com.nihongo.conversation.presentation.components.StandardCard
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
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,  // Phase 12: tertiaryContainer → primaryContainer (통일성)
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
                contentPadding = PaddingValues(
                    horizontal = 0.dp,  // StandardCard가 자체 horizontal padding 가짐
                    vertical = AppDesignSystem.Spacing.sectionSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(AppDesignSystem.Spacing.sectionSpacing)
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
    // Phase 12: StandardCard로 통일 (색상은 유지)
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = AppDesignSystem.Spacing.cardHorizontalPadding),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDesignSystem.Spacing.cardInnerPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDesignSystem.Spacing.elementSpacing)
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
                        style = MaterialTheme.typography.headlineSmall,  // Phase 12: titleMedium → headlineSmall
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

/**
 * Phase 12: Color-coded chips를 사용하는 통계 항목 Row
 */
@Composable
private fun StatItemRow(
    label: String,
    value: String,
    chipColor: Color,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        ColoredChip(
            text = value,
            color = chipColor,
            textColor = valueColor
        )
    }
}

@Composable
fun SummaryStatsRow(
    conversations: Int,
    messages: Int,
    minutes: Int,
    modifier: Modifier = Modifier
) {
    // Phase 12: StandardCard 사용 및 color-coded chips로 표시
    StandardCard(modifier = modifier) {
        Text(
            text = "학습 통계",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        StatItemRow(
            label = "💬 대화 수",
            value = "${conversations}개",
            chipColor = AppDesignSystem.Colors.primaryChip()
        )

        StatItemRow(
            label = "📨 메시지 수",
            value = "${messages}개",
            chipColor = AppDesignSystem.Colors.secondaryChip()
        )

        StatItemRow(
            label = "⏱️ 학습 시간",
            value = "${minutes}분",
            chipColor = AppDesignSystem.Colors.tertiaryChip()
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
    // Phase 12: StandardCard 사용
    StandardCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,  // Phase 12: titleMedium → headlineSmall
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

    // Phase 12: StandardCard 사용
    StandardCard(modifier = modifier) {
        Text(
            text = "시나리오별 진행도",
            style = MaterialTheme.typography.headlineSmall,  // Phase 12: titleMedium → headlineSmall
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
