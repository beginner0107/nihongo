package com.nihongo.conversation.presentation.scenario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Scenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioListScreen(
    onScenarioSelected: (Long) -> Unit,
    onFlashcardClick: () -> Unit = {},
    onAddVocabularyClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: ScenarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "시나리오 선택",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "학습할 상황을 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "프로필"
                        )
                    }
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "통계"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Add custom vocabulary FAB
                SmallFloatingActionButton(
                    onClick = onAddVocabularyClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "단어 추가"
                    )
                }

                // Flashcard review FAB
                ExtendedFloatingActionButton(
                    onClick = onFlashcardClick,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "단어장"
                        )
                    },
                    text = { Text("단어장") }
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.scenarios) { scenario ->
                    ScenarioCard(
                        scenario = scenario,
                        onClick = { onScenarioSelected(scenario.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScenarioCard(
    scenario: Scenario,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon based on scenario
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getScenarioIcon(scenario.id),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title and difficulty
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DifficultyBadge(difficulty = scenario.difficulty)
                }

                // Description
                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "시작",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: Int) {
    val (text, color) = when (difficulty) {
        1 -> "초급" to MaterialTheme.colorScheme.tertiary
        2 -> "중급" to MaterialTheme.colorScheme.secondary
        3 -> "고급" to MaterialTheme.colorScheme.error
        else -> "초급" to MaterialTheme.colorScheme.tertiary
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun getScenarioIcon(scenarioId: Long): ImageVector {
    return when (scenarioId) {
        1L -> Icons.Default.Restaurant      // 레스토랑
        2L -> Icons.Default.ShoppingCart    // 쇼핑
        3L -> Icons.Default.Hotel           // 호텔
        4L -> Icons.Default.People          // 친구
        5L -> Icons.Default.Phone           // 전화
        6L -> Icons.Default.MedicalServices // 병원
        10L -> Icons.Default.Work           // 취업 면접
        11L -> Icons.Default.Report         // 클레임 대응
        12L -> Icons.Default.LocalHospital  // 긴급 상황
        13L -> Icons.Default.Favorite       // 데이트
        14L -> Icons.Default.BusinessCenter // 비즈니스 프레젠테이션
        15L -> Icons.Default.Chat           // 여자친구와의 대화
        else -> Icons.Default.Chat
    }
}
