package com.nihongo.conversation.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.core.voice.StorageStatus

/**
 * Phase 3: 음성 설정 및 관리 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎤 음성 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 저장 공간 상태
            item {
                StorageStatusCard(
                    status = uiState.storageStatus,
                    onCleanup = { viewModel.cleanupOldFiles() },
                    onDeleteAll = { viewModel.showDeleteAllDialog() }
                )
            }
            
            // 녹음 품질 설정
            item {
                RecordingQualitySection(
                    quality = uiState.recordingQuality,
                    onQualityChange = { viewModel.setRecordingQuality(it) }
                )
            }
            
            // 자동 정리 설정
            item {
                AutoCleanupSection(
                    enabled = uiState.autoCleanupEnabled,
                    days = uiState.autoCleanupDays,
                    onEnabledChange = { viewModel.setAutoCleanup(it) },
                    onDaysChange = { viewModel.setAutoCleanupDays(it) }
                )
            }
            
            // 통계
            item {
                VoiceStatisticsCard(
                    totalRecordings = uiState.totalRecordings,
                    totalDuration = uiState.totalDuration,
                    averageDuration = uiState.averageDuration,
                    bookmarkedCount = uiState.bookmarkedCount
                )
            }
        }
    }
    
    // 전체 삭제 확인 다이얼로그
    if (uiState.showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAllDialog() },
            title = { Text("전체 음성 삭제") },
            text = { Text("북마크되지 않은 모든 음성 녹음을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllFiles()
                        viewModel.dismissDeleteAllDialog()
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAllDialog() }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 스낵바 메시지
    uiState.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSnackbar()
        }
    }
}

@Composable
private fun StorageStatusCard(
    status: StorageStatus,
    onCleanup: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                is StorageStatus.Critical -> MaterialTheme.colorScheme.errorContainer
                is StorageStatus.Warning -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "저장 공간",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = when (status) {
                        is StorageStatus.Critical -> Icons.Default.Error
                        is StorageStatus.Warning -> Icons.Default.Warning
                        else -> Icons.Default.Storage
                    },
                    contentDescription = null,
                    tint = when (status) {
                        is StorageStatus.Critical -> MaterialTheme.colorScheme.error
                        is StorageStatus.Warning -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("사용 중:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatSize(status.totalSize),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("파일 수:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${status.fileCount}개",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            LinearProgressIndicator(
                progress = (status.totalSize.toFloat() / (100L * 1024 * 1024)).coerceAtMost(1f),
                modifier = Modifier.fillMaxWidth(),
                color = when (status) {
                    is StorageStatus.Critical -> MaterialTheme.colorScheme.error
                    is StorageStatus.Warning -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCleanup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("오래된 파일 정리")
                }
                
                OutlinedButton(
                    onClick = onDeleteAll,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("전체 삭제")
                }
            }
        }
    }
}

@Composable
private fun RecordingQualitySection(
    quality: RecordingQuality,
    onQualityChange: (RecordingQuality) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "녹음 품질",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            RecordingQuality.values().forEach { q ->
                FilterChip(
                    selected = quality == q,
                    onClick = { onQualityChange(q) },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(q.label, fontWeight = FontWeight.Bold)
                                Text(
                                    text = q.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AutoCleanupSection(
    enabled: Boolean,
    days: Int,
    onEnabledChange: (Boolean) -> Unit,
    onDaysChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "자동 정리",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            if (enabled) {
                Text(
                    text = "오래된 녹음 파일을 자동으로 삭제합니다 (북마크 제외)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("삭제 기준:")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { if (days > 7) onDaysChange(days - 7) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "감소", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${days}일",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FilledIconButton(
                            onClick = { if (days < 90) onDaysChange(days + 7) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "증가", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatisticsCard(
    totalRecordings: Int,
    totalDuration: String,
    averageDuration: String,
    bookmarkedCount: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "통계",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            StatRow("총 녹음 수", totalRecordings.toString())
            StatRow("총 녹음 시간", totalDuration)
            StatRow("평균 길이", averageDuration)
            StatRow("북마크", "${bookmarkedCount}개")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class RecordingQuality(
    val label: String,
    val description: String,
    val bitrate: Int,
    val sampleRate: Int
) {
    LOW("저음질", "용량 절약 (64kbps)", 64000, 22050),
    MEDIUM("보통", "균형잡힌 품질 (96kbps)", 96000, 44100),
    HIGH("고음질", "최고 품질 (128kbps)", 128000, 44100)
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
