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
import com.nihongo.conversation.presentation.quest.QuestCompletedDialog
import com.nihongo.conversation.presentation.quest.QuestViewModel

/**
 * Home Screen - Simplified for Phase 11
 *
 * Option C: Smart Summary (스마트 요약)
 * - LearningStatusCard (학습 현황 + 퀘스트 통합) - TODO
 * - TodayRecommendationCard (AI 추천 1개) - TODO
 * - QuickActionsRow (이어하기/랜덤/전체) - TODO
 *
 * Total height: ~440dp (1 화면 이내)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScenarioSelected: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},  // Phase 12: 설정 버튼 추가
    onReviewClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
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
                },
                actions = {
                    // Phase 12: 설정 버튼 추가
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정"
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 1. Learning Status Card (~180dp)
                item {
                    LearningStatusCard(
                        streak = uiState.currentStreak,
                        todayMessages = uiState.todayMessageCount,
                        dailyGoal = uiState.dailyGoal,
                        level = uiState.userLevel,
                        points = uiState.totalPoints,
                        topQuests = questUiState.quests.take(2)
                    )
                }

                // 2. Compact Streak Card (~100dp) - Phase 1 completion
                item {
                    CompactStreakCard(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak
                    )
                }

                // 3. Today's Recommendation Card (~160dp)
                item {
                    val recommendation = uiState.todayRecommendation
                    if (recommendation != null) {
                        TodayRecommendationCard(
                            recommendation = recommendation,
                            onRefresh = { viewModel.refreshRecommendation() },
                            onStart = { onScenarioSelected(recommendation.scenario.id) }
                        )
                    }
                }

                // 4. Quick Actions Row (~100dp)
                item {
                    QuickActionsRow(
                        onResume = { viewModel.resumeLastConversation() },
                        onRandom = { viewModel.startRandomScenario() },
                        onViewAll = { /* TODO: Navigate to ScenarioListScreen */ }
                    )
                }

                // 5. Conversation History Card
                item {
                    ConversationHistoryCard(
                        onViewHistory = onReviewClick
                    )
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

/**
 * Conversation History Card - Navigate to past conversations
 */
@Composable
fun ConversationHistoryCard(
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "대화 히스토리",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            Text(
                text = "과거 대화를 복습하고 음성 녹음을 다시 들어보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("히스토리 보기")
            }
        }
    }
}
