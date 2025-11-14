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

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Recording Toggle
                SettingsToggle(
                    label = "음성 녹음 저장",
                    description = "음성 인식 후 자동으로 음성 파일을 저장합니다 (일본어만, 복습용)",
                    checked = settings.enableVoiceRecording,
                    onCheckedChange = { viewModel.updateEnableVoiceRecording(it) }
                )
            }

            HorizontalDivider()

            // Display Settings
            SettingsSection(
                title = "표시 설정",
                icon = Icons.Default.Translate
            ) {
                // Romaji Toggle
                SettingsToggle(
                    label = "로마자 표시",
                    description = "힌트에 로마자를 표시합니다",
                    checked = settings.showRomaji,
                    onCheckedChange = { viewModel.updateShowRomaji(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Furigana Toggle
                SettingsToggle(
                    label = "한자 읽기 표시 (후리가나)",
                    description = "AI 메시지의 한자에 읽기를 표시합니다",
                    checked = settings.showFurigana,
                    onCheckedChange = { viewModel.updateShowFurigana(it) }
                )

                // Furigana Type Selector (only shown when furigana is enabled)
                if (settings.showFurigana) {
                    Spacer(modifier = Modifier.height(12.dp))

                    FuriganaTypeSelector(
                        currentType = settings.furiganaType,
                        onTypeChange = { viewModel.updateFuriganaType(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Recording Toggle
                SettingsToggle(
                    label = "음성 녹음 저장",
                    description = "STT 후 발음 연습용 음성 파일을 저장합니다 (10초)",
                    checked = settings.enableVoiceRecording,
                    onCheckedChange = { viewModel.updateEnableVoiceRecording(it) }
                )
            }

            HorizontalDivider()

            // Theme Mode
            SettingsSection(
                title = "테마",
                icon = Icons.Default.DarkMode
            ) {
                ThemeModeSelector(
                    currentMode = settings.themeMode,
                    onModeChange = { viewModel.updateThemeMode(it) }
                )
            }

            HorizontalDivider()

            // Accessibility Settings
            SettingsSection(
                title = "접근성",
                icon = Icons.Default.Accessibility
            ) {
                // Text Size
                TextSizeSelector(
                    currentSize = settings.textSize,
                    onSizeChange = { viewModel.updateTextSize(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // High Contrast Mode
                SettingsToggle(
                    label = "고대비 모드",
                    description = "텍스트와 배경의 대비를 높여 가독성을 향상시킵니다",
                    checked = settings.contrastMode.isHighContrast,
                    onCheckedChange = {
                        viewModel.updateContrastMode(
                            if (it) com.nihongo.conversation.domain.model.ContrastMode.HIGH
                            else com.nihongo.conversation.domain.model.ContrastMode.NORMAL
                        )
                    }
                )
            }

            HorizontalDivider()

            // Cache Management
            SettingsSection(
                title = "캐시 관리",
                icon = Icons.Default.Storage
            ) {
                val cacheSize by viewModel.cacheSize.collectAsState()
                val cacheCleanupState by viewModel.cacheCleanupState.collectAsState()

                CacheManagementSection(
                    cacheSize = cacheSize,
                    cleanupState = cacheCleanupState,
                    onRefresh = { viewModel.loadCacheSize() },
                    onClearAll = { viewModel.clearAllCaches() },
                    onDismissSuccess = { viewModel.resetCacheCleanupState() }
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

        // Clarification about difficulty level
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "이 설정은 모든 시나리오의 AI 대화 스타일에 적용됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SpeechSpeedSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "속도",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        // Speed selection buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedButton(
                label = "0.5x",
                description = "느림",
                isSelected = value == 0.5f,
                onClick = { onValueChange(0.5f) },
                modifier = Modifier.weight(1f)
            )
            SpeedButton(
                label = "1.0x",
                description = "보통",
                isSelected = value == 1.0f,
                onClick = { onValueChange(1.0f) },
                modifier = Modifier.weight(1f)
            )
            SpeedButton(
                label = "1.5x",
                description = "빠름",
                isSelected = value == 1.5f,
                onClick = { onValueChange(1.5f) },
                modifier = Modifier.weight(1f)
            )
            SpeedButton(
                label = "2.0x",
                description = "매우 빠름",
                isSelected = value == 2.0f,
                onClick = { onValueChange(2.0f) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SpeedButton(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier.height(64.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
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

@Composable
fun TextSizeSelector(
    currentSize: com.nihongo.conversation.domain.model.TextSizePreference,
    onSizeChange: (com.nihongo.conversation.domain.model.TextSizePreference) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "텍스트 크기",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.nihongo.conversation.domain.model.TextSizePreference.values().forEach { size ->
                FilterChip(
                    selected = currentSize == size,
                    onClick = { onSizeChange(size) },
                    label = {
                        Text(
                            text = when (size) {
                                com.nihongo.conversation.domain.model.TextSizePreference.SMALL -> "작게"
                                com.nihongo.conversation.domain.model.TextSizePreference.NORMAL -> "보통"
                                com.nihongo.conversation.domain.model.TextSizePreference.LARGE -> "크게"
                                com.nihongo.conversation.domain.model.TextSizePreference.XLARGE -> "아주 크게"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Preview text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "こんにちは",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * currentSize.scale
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CacheManagementSection(
    cacheSize: com.nihongo.conversation.core.cache.CacheSize,
    cleanupState: com.nihongo.conversation.presentation.settings.CacheCleanupState,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onDismissSuccess: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cache size display
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "전체 캐시 크기",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = cacheSize.formatTotal(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()

                // Breakdown
                CacheInfoRow(
                    label = "이미지 캐시",
                    value = cacheSize.formatCoil()
                )
                CacheInfoRow(
                    label = "번역 캐시",
                    value = "${cacheSize.translationEntries}개 항목"
                )
                CacheInfoRow(
                    label = "앱 캐시",
                    value = cacheSize.formatAppCache()
                )
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                enabled = cleanupState !is com.nihongo.conversation.presentation.settings.CacheCleanupState.Cleaning
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("새로고침")
            }

            Button(
                onClick = onClearAll,
                modifier = Modifier.weight(1f),
                enabled = cleanupState !is com.nihongo.conversation.presentation.settings.CacheCleanupState.Cleaning
            ) {
                if (cleanupState is com.nihongo.conversation.presentation.settings.CacheCleanupState.Cleaning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("정리 중...")
                } else {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("전체 삭제")
                }
            }
        }

        // Success/Error message
        when (cleanupState) {
            is com.nihongo.conversation.presentation.settings.CacheCleanupState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "캐시가 성공적으로 정리되었습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Auto-dismiss after 2 seconds
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    onDismissSuccess()
                }
            }
            is com.nihongo.conversation.presentation.settings.CacheCleanupState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = cleanupState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            else -> { /* Idle or Cleaning - no message */ }
        }

        // Info text
        HorizontalDivider()
        Text(
            text = "💡 자동 정리: 매일 자동으로 오래된 캐시를 정리합니다 (번역 캐시 30일 보관)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CacheInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ThemeModeSelector(
    currentMode: com.nihongo.conversation.domain.model.ThemeMode,
    onModeChange: (com.nihongo.conversation.domain.model.ThemeMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "앱 테마를 선택하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Light Mode
            ThemeModeChip(
                label = "라이트",
                icon = Icons.Default.LightMode,
                isSelected = currentMode == com.nihongo.conversation.domain.model.ThemeMode.LIGHT,
                onClick = { onModeChange(com.nihongo.conversation.domain.model.ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )

            // Dark Mode
            ThemeModeChip(
                label = "다크",
                icon = Icons.Default.DarkMode,
                isSelected = currentMode == com.nihongo.conversation.domain.model.ThemeMode.DARK,
                onClick = { onModeChange(com.nihongo.conversation.domain.model.ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )

            // System Mode
            ThemeModeChip(
                label = "시스템",
                icon = Icons.Default.Brightness4,
                isSelected = currentMode == com.nihongo.conversation.domain.model.ThemeMode.SYSTEM,
                onClick = { onModeChange(com.nihongo.conversation.domain.model.ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
        }

        // Description
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when (currentMode) {
                        com.nihongo.conversation.domain.model.ThemeMode.LIGHT -> "밝은 테마가 항상 적용됩니다"
                        com.nihongo.conversation.domain.model.ThemeMode.DARK -> "어두운 테마가 항상 적용됩니다"
                        com.nihongo.conversation.domain.model.ThemeMode.SYSTEM -> "기기 설정에 따라 자동으로 변경됩니다"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        modifier = modifier.height(72.dp)
    )
}

/**
 * Furigana Type Selector
 * Allows user to choose between Hiragana and Katakana furigana
 */
@Composable
fun FuriganaTypeSelector(
    currentType: com.nihongo.conversation.domain.model.FuriganaType,
    onTypeChange: (com.nihongo.conversation.domain.model.FuriganaType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "표시 방식",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hiragana option
            FuriganaTypeChip(
                label = "ひらがな",
                example = "注文(ちゅうもん)",
                isSelected = currentType == com.nihongo.conversation.domain.model.FuriganaType.HIRAGANA,
                onClick = { onTypeChange(com.nihongo.conversation.domain.model.FuriganaType.HIRAGANA) },
                modifier = Modifier.weight(1f)
            )

            // Katakana option
            FuriganaTypeChip(
                label = "カタカナ",
                example = "注文(チュウモン)",
                isSelected = currentType == com.nihongo.conversation.domain.model.FuriganaType.KATAKANA,
                onClick = { onTypeChange(com.nihongo.conversation.domain.model.FuriganaType.KATAKANA) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Furigana Type Chip
 * Individual chip for selecting furigana display type
 */
@Composable
fun FuriganaTypeChip(
    label: String,
    example: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier.height(64.dp)
    )
}
