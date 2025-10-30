package com.nihongo.conversation.presentation.pronunciation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.PhraseStats
import com.nihongo.conversation.presentation.stats.StatCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationHistoryScreen(
    onBackClick: () -> Unit,
    onPhraseClick: (String) -> Unit = {},
    viewModel: PronunciationHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("発音練習履歴") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
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
                // Overall Statistics
                item {
                    Text(
                        "概要",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OverallStatsSection(
                        stats = uiState.stats,
                        formatDuration = { viewModel.formatDuration(it) }
                    )
                }

                // Improvement Trend
                if (uiState.stats.improvementTrend.isNotEmpty()) {
                    item {
                        Text(
                            "7日間の推移",
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
                                    "平均正確度の推移",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                com.nihongo.conversation.presentation.stats.LineChart(
                                    data = uiState.stats.improvementTrend,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    lineColor = MaterialTheme.colorScheme.primary,
                                    pointColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Weak Phrases (need practice)
                if (uiState.stats.weakPhrases.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "要練習フレーズ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    items(uiState.stats.weakPhrases.take(5)) { phraseStats ->
                        PhraseStatsCard(
                            phraseStats = phraseStats,
                            onClick = { onPhraseClick(phraseStats.expectedText) },
                            getAccuracyColor = { viewModel.getAccuracyColor(it) }
                        )
                    }
                }

                // Mastered Phrases
                if (uiState.stats.masteredPhrases.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "マスターしたフレーズ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    items(uiState.stats.masteredPhrases.take(5)) { phraseStats ->
                        PhraseStatsCard(
                            phraseStats = phraseStats,
                            onClick = { onPhraseClick(phraseStats.expectedText) },
                            getAccuracyColor = { viewModel.getAccuracyColor(it) }
                        )
                    }
                }

                // Filter and Sort Controls
                item {
                    Text(
                        "全ての練習フレーズ",
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
                        // Filter dropdown
                        var filterExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { filterExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(getFilterLabel(uiState.filterBy))
                            }
                            DropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false }
                            ) {
                                FilterOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(getFilterLabel(option)) },
                                        onClick = {
                                            viewModel.setFilterOption(option)
                                            filterExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Sort dropdown
                        var sortExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { sortExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sort, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(getSortLabel(uiState.sortBy))
                            }
                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(getSortLabel(option)) },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            sortExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // All Phrases List
                if (uiState.allPhrases.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "まだ練習していません",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "発音練習を始めましょう！",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.allPhrases) { phraseStats ->
                        PhraseStatsCard(
                            phraseStats = phraseStats,
                            onClick = { onPhraseClick(phraseStats.expectedText) },
                            getAccuracyColor = { viewModel.getAccuracyColor(it) }
                        )
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
fun OverallStatsSection(
    stats: com.nihongo.conversation.domain.model.PronunciationStats,
    formatDuration: (Long) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "総練習回数",
                value = stats.totalAttempts.toString(),
                icon = {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "練習フレーズ",
                value = stats.uniquePhrases.toString(),
                icon = {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "平均正確度",
                value = "${stats.averageAccuracy.toInt()}%",
                icon = {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFFFC107)
                    )
                },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "最高正確度",
                value = "${stats.bestAccuracy}%",
                icon = {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (stats.totalPracticeTimeMs > 0) {
            StatCard(
                title = "総練習時間",
                value = formatDuration(stats.totalPracticeTimeMs),
                icon = {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PhraseStatsCard(
    phraseStats: PhraseStats,
    onClick: () -> Unit,
    getAccuracyColor: (Double) -> Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = phraseStats.expectedText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Average score
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = getAccuracyColor(phraseStats.averageScore)
                        )
                        Text(
                            text = "平均: ${phraseStats.averageScore.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = getAccuracyColor(phraseStats.averageScore)
                        )
                    }

                    // Best score
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Text(
                            text = "最高: ${phraseStats.bestScore}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${phraseStats.attemptCount}回練習",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = formatRelativeDate(phraseStats.latestAttemptDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getFilterLabel(filter: FilterOption): String {
    return when (filter) {
        FilterOption.ALL -> "全て"
        FilterOption.WEAK -> "要練習"
        FilterOption.LEARNING -> "学習中"
        FilterOption.MASTERED -> "マスター"
    }
}

private fun getSortLabel(sort: SortOption): String {
    return when (sort) {
        SortOption.LATEST -> "最新順"
        SortOption.ACCURACY_HIGH -> "正確度高"
        SortOption.ACCURACY_LOW -> "正確度低"
        SortOption.ATTEMPTS -> "練習回数"
    }
}

private fun formatRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 7 -> {
            val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
        days > 0 -> "${days}日前"
        hours > 0 -> "${hours}時間前"
        minutes > 0 -> "${minutes}分前"
        else -> "たった今"
    }
}
