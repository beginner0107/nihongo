package com.nihongo.conversation.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Scenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableScenarios by viewModel.availableScenarios.collectAsState()

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSaveSuccess()
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "프로필",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "당신의 정보를 설정하세요",
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
                    IconButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "저장"
                            )
                        }
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar Section
                item {
                    ProfileSection(
                        title = "아바타",
                        icon = Icons.Default.Person
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Large preview
                            Avatar(
                                avatarId = uiState.selectedAvatarId,
                                size = 100
                            )

                            // Selector grid
                            AvatarSelector(
                                selectedAvatarId = uiState.selectedAvatarId,
                                onAvatarSelected = viewModel::selectAvatar
                            )
                        }
                    }
                }

                // Basic Info Section
                item {
                    ProfileSection(
                        title = "기본 정보",
                        icon = Icons.Default.Info
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = viewModel::updateName,
                                label = { Text("이름") },
                                placeholder = { Text("홍길동") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = uiState.bio,
                                onValueChange = viewModel::updateBio,
                                label = { Text("자기소개") },
                                placeholder = { Text("간단한 자기소개를 작성해주세요") },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                minLines = 2
                            )
                        }
                    }
                }

                // Learning Goal Section
                item {
                    ProfileSection(
                        title = "학습 목표",
                        icon = Icons.Default.EmojiEvents
                    ) {
                        OutlinedTextField(
                            value = uiState.learningGoal,
                            onValueChange = viewModel::updateLearningGoal,
                            label = { Text("목표") },
                            placeholder = { Text("일본 여행을 위해, 애니메이션을 자막 없이 보기 위해...") },
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            minLines = 2
                        )
                    }
                }

                // Level Section
                item {
                    ProfileSection(
                        title = "일본어 레벨",
                        icon = Icons.Default.TrendingUp
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = getLevelText(uiState.level),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Slider(
                                value = uiState.level.toFloat(),
                                onValueChange = { viewModel.updateLevel(it.toInt()) },
                                valueRange = 1f..3f,
                                steps = 1,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "초급",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "중급",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "상급",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Favorite Scenarios Section
                item {
                    ProfileSection(
                        title = "즐겨찾기 시나리오",
                        icon = Icons.Default.Favorite
                    ) {
                        if (availableScenarios.isEmpty()) {
                            Text(
                                text = "시나리오가 로드되지 않았습니다",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableScenarios.forEach { scenario ->
                                    ScenarioCheckbox(
                                        scenario = scenario,
                                        isSelected = uiState.selectedScenarios.contains(scenario.id),
                                        onToggle = { viewModel.toggleScenario(scenario.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Native Language Section
                item {
                    ProfileSection(
                        title = "모국어",
                        icon = Icons.Default.Language
                    ) {
                        OutlinedTextField(
                            value = uiState.nativeLanguage,
                            onValueChange = viewModel::updateNativeLanguage,
                            label = { Text("모국어") },
                            placeholder = { Text("한국어, 영어 등") },
                            leadingIcon = {
                                Icon(Icons.Default.Translate, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Error display
                if (uiState.error != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Save button (bottom)
                item {
                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isSaving) "저장 중..." else "저장하기")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

@Composable
fun ScenarioCheckbox(
    scenario: Scenario,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = scenario.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = scenario.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DifficultyBadge(difficulty = scenario.difficulty)
    }
}

@Composable
fun DifficultyBadge(difficulty: Int) {
    val (text, color) = when (difficulty) {
        1 -> "초급" to MaterialTheme.colorScheme.tertiary
        2 -> "중급" to MaterialTheme.colorScheme.secondary
        3 -> "상급" to MaterialTheme.colorScheme.error
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

private fun getLevelText(level: Int): String {
    return when (level) {
        1 -> "초급 (JLPT N5-N4)"
        2 -> "중급 (JLPT N3-N2)"
        3 -> "상급 (JLPT N1)"
        else -> "초급"
    }
}
