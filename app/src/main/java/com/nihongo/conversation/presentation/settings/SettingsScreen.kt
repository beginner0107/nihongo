package com.nihongo.conversation.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.userSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Difficulty Level Section
            SettingsSection(
                title = "난이도 레벨",
                icon = Icons.Default.TrendingUp
            ) {
                DifficultySlider(
                    value = settings.difficultyLevel,
                    onValueChange = { viewModel.updateDifficultyLevel(it) }
                )
            }

            HorizontalDivider()

            // Speech Speed Section
            SettingsSection(
                title = "음성 속도",
                icon = Icons.Default.Speed
            ) {
                SpeechSpeedSlider(
                    value = settings.speechSpeed,
                    onValueChange = { viewModel.updateSpeechSpeed(it) }
                )
            }

            HorizontalDivider()

            // Auto Speak Toggle
            SettingsSection(
                title = "음성 설정",
                icon = Icons.Default.VolumeUp
            ) {
                SettingsToggle(
                    label = "AI 응답 자동 읽기",
                    description = "AI의 답변을 자동으로 음성으로 읽어줍니다",
                    checked = settings.autoSpeak,
                    onCheckedChange = { viewModel.updateAutoSpeak(it) }
                )
            }

            HorizontalDivider()

            // Romaji Toggle
            SettingsSection(
                title = "표시 설정",
                icon = Icons.Default.Translate
            ) {
                SettingsToggle(
                    label = "로마자 표시",
                    description = "힌트에 로마자를 표시합니다",
                    checked = settings.showRomaji,
                    onCheckedChange = { viewModel.updateShowRomaji(it) }
                )
            }

            HorizontalDivider()

            // Translation Model Management
            val modelState by viewModel.translationModelState.collectAsState()
            TranslationModelSection(
                modelState = modelState,
                onDownloadClick = { viewModel.downloadTranslationModel() },
                onDeleteClick = { viewModel.deleteTranslationModel() }
            )

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "설정은 모든 대화에 적용됩니다. 난이도가 높을수록 AI는 더 복잡한 일본어를 사용합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
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

@Composable
fun DifficultySlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (value) {
                    1 -> "초급 (Beginner)"
                    2 -> "중급 (Intermediate)"
                    3 -> "상급 (Advanced)"
                    else -> "초급"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (value) {
                    1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    2 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    3 -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = "Level $value",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (value) {
                        1 -> MaterialTheme.colorScheme.tertiary
                        2 -> MaterialTheme.colorScheme.secondary
                        3 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 1f..3f,
            steps = 1
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("초급", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("중급", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("상급", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SpeechSpeedSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "속도",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${String.format("%.1f", value)}x",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.5f..2.0f,
            steps = 14  // 0.5, 0.6, 0.7, ..., 2.0
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "느림 (0.5x)",
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "보통 (1.0x)",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.333f),  // 1.0 is at 1/3 position (0.5 to 2.0)
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
            Text(
                text = "빠름 (2.0x)",
                modifier = Modifier.align(Alignment.CenterEnd),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun TranslationModelSection(
    modelState: TranslationModelState,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    SettingsSection(
        title = "번역 모델",
        icon = Icons.Default.CloudDownload
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (modelState) {
                    is TranslationModelState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("모델 상태 확인 중...")
                        }
                    }

                    is TranslationModelState.NotDownloaded -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "번역 모델이 다운로드되지 않았습니다",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "• 빠른 번역 속도 (1-2초)\n• 오프라인 사용 가능\n• 용량: 약 50MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onDownloadClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("모델 다운로드 (WiFi 권장)")
                            }
                        }
                    }

                    is TranslationModelState.Downloading -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "다운로드 중...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "WiFi 연결을 유지해주세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is TranslationModelState.Downloaded -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "모델 다운로드 완료",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "용량: ${modelState.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("모델 삭제 (저장 공간 확보)")
                            }
                        }
                    }

                    is TranslationModelState.Error -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "오류",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = modelState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = onDownloadClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("다시 시도")
                            }
                        }
                    }
                }

                // Info text
                HorizontalDivider()
                Text(
                    text = "💡 번역 우선순위: 로컬 사전(즉시) → ML Kit(1-2초) → Gemini API(10초)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
