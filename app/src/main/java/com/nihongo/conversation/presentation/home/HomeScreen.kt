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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                // Error UI
                val errorMessage = uiState.error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage ?: "알 수 없는 오류",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.retry() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("다시 시도")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    // 1. Hero Card - Main greeting with core metrics (~200dp)
                    item {
                        HeroCard(
                            streak = uiState.currentStreak,
                            todayMessages = uiState.todayMessageCount,
                            dailyGoal = uiState.dailyGoal,
                            level = uiState.userLevel,
                            points = uiState.totalPoints,
                            longestStreak = uiState.longestStreak
                        )
                    }

                    // 2. Today's Recommendation Card (~180dp)
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

                    // 3. Quick Actions Row (~80dp)
                    item {
                        QuickActionsRow(
                            onResume = { viewModel.resumeLastConversation() },
                            onRandom = { viewModel.startRandomScenario() },
                            onViewAll = { /* Navigate via bottom nav to Scenarios tab */ }
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
