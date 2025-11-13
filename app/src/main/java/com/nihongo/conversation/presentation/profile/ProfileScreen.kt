package com.nihongo.conversation.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nihongo.conversation.core.theme.AppDesignSystem
import com.nihongo.conversation.presentation.components.ColoredChip
import com.nihongo.conversation.presentation.components.StandardCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                contentPadding = PaddingValues(
                    horizontal = 0.dp,  // StandardCard가 자체 horizontal padding 가짐
                    vertical = AppDesignSystem.Spacing.sectionSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(AppDesignSystem.Spacing.sectionSpacing)
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
    // Phase 12: StandardCard 사용
    StandardCard(modifier = modifier) {
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
                style = MaterialTheme.typography.headlineSmall,  // Phase 12: titleMedium → headlineSmall
                fontWeight = FontWeight.Bold
            )
        }

        content()
    }
}

