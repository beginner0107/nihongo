package com.nihongo.conversation.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.presentation.dashboard.DailyChallengeCard
import com.nihongo.conversation.presentation.dashboard.RecentScenariosSection
import com.nihongo.conversation.presentation.dashboard.RecommendedScenariosSection
import com.nihongo.conversation.presentation.dashboard.TodayLearningCard
import com.nihongo.conversation.presentation.quest.QuestCompletedDialog
import com.nihongo.conversation.presentation.quest.QuestSection
import com.nihongo.conversation.presentation.quest.QuestViewModel
import com.nihongo.conversation.presentation.scenario.ScenarioViewModel

/**
 * Home Screen - Quick start and daily overview
 *
 * Shows:
 * - Daily Challenge (featured quest)
 * - Today's Learning (progress + streak + time)
 * - Recommended Scenarios (2-3 cards)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScenarioSelected: (Long) -> Unit,
    viewModel: ScenarioViewModel = hiltViewModel(),
    questViewModel: QuestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val questUiState by questViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "홈",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "빠른 학습 시작",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Daily Challenge (Featured Quest)
                // TODO: Get from QuestViewModel when Phase 2 is complete
                item {
                    // Placeholder for now
                    // DailyChallengeCard(featuredQuest = null)
                }

                // Today's Learning
                item {
                    TodayLearningCard(
                        todayMessageCount = uiState.todayMessageCount,
                        dailyGoal = uiState.dailyGoal,
                        currentStreak = uiState.currentStreak,
                        remainingHours = uiState.remainingHours,
                        remainingMinutes = uiState.remainingMinutes
                    )
                }

                // Quest Section
                item {
                    if (questUiState.quests.isNotEmpty()) {
                        QuestSection(
                            quests = questUiState.quests,
                            userPoints = questUiState.userPoints,
                            onQuestClick = { /* Quest details - future implementation */ }
                        )
                    }
                }

                // Recommended Scenarios
                item {
                    val recommendedScenarios = uiState.recommendedScenarios
                        .map { it.scenario }
                        .take(3)

                    if (recommendedScenarios.isNotEmpty()) {
                        RecommendedScenariosSection(
                            scenarios = recommendedScenarios,
                            onScenarioClick = onScenarioSelected
                        )
                    }
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        onFlashcardClick = { /* TODO: Navigate to flashcard */ },
                        onVocabularyClick = { /* TODO: Navigate to vocabulary */ },
                        onPronunciationClick = { /* TODO: Navigate to pronunciation */ }
                    )
                }

                // Recent Scenarios
                item {
                    val recentScenarios = uiState.scenarios
                        .filter { !uiState.recommendedScenarios.map { it.scenario.id }.contains(it.id) }
                        .take(3)

                    if (recentScenarios.isNotEmpty()) {
                        RecentScenariosSection(
                            scenarios = recentScenarios,
                            onScenarioClick = onScenarioSelected
                        )
                    }
                }
            }
        }
    }

    // Quest completed dialog
    if (questUiState.showQuestCompletedDialog) {
        QuestCompletedDialog(
            rewardPoints = questUiState.lastCompletedQuestReward,
            onDismiss = { questViewModel.dismissQuestCompletedDialog() }
        )
    }
}

@Composable
private fun QuickActionsSection(
    onFlashcardClick: () -> Unit,
    onVocabularyClick: () -> Unit,
    onPronunciationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "빠른 실행",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Style,
                label = "단어장",
                onClick = onFlashcardClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Add,
                label = "단어 추가",
                onClick = onVocabularyClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.RecordVoiceOver,
                label = "발음 연습",
                onClick = onPronunciationClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
