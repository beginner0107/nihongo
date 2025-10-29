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
    onSettingsClick: () -> Unit = {},
    viewModel: ScenarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "シナリオを選択",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "学習したいシーンを選んでください",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定"
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
                contentDescription = "開始",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: Int) {
    val (text, color) = when (difficulty) {
        1 -> "初級" to MaterialTheme.colorScheme.tertiary
        2 -> "中級" to MaterialTheme.colorScheme.secondary
        3 -> "上級" to MaterialTheme.colorScheme.error
        else -> "初級" to MaterialTheme.colorScheme.tertiary
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
        1L -> Icons.Default.Restaurant      // レストラン
        2L -> Icons.Default.ShoppingCart    // 買い物
        3L -> Icons.Default.Hotel           // ホテル
        4L -> Icons.Default.People          // 友達
        5L -> Icons.Default.Phone           // 電話
        6L -> Icons.Default.MedicalServices // 病院
        else -> Icons.Default.Chat
    }
}
