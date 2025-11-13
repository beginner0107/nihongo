package com.nihongo.conversation.presentation.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.VoiceRecording

/**
 * Phase 2: 대화 복습 모드 화면
 * 
 * 기능:
 * - 전체 대화 목록 표시
 * - 개별 메시지 재생
 * - 전체 재생 (사용자 음성 + AI TTS)
 * - 선택적 재생 (내 음성만 / AI만)
 * - 재생 속도 조절
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationReviewScreen(
    conversationId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ConversationReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 대화 복습") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                },
                actions = {
                    // 재생 속도 선택
                    IconButton(onClick = { viewModel.toggleSpeedMenu() }) {
                        Icon(Icons.Default.Speed, "속도")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 복습 통계
            ReviewStatistics(
                totalMessages = uiState.messages.size,
                voiceMessageCount = uiState.voiceMessageCount,
                totalDuration = uiState.totalDuration,
                modifier = Modifier.padding(16.dp)
            )
            
            Divider()
            
            // 재생 컨트롤 바
            PlaybackControlBar(
                playbackState = playbackState,
                playbackMode = uiState.playbackMode,
                onPlayAll = { viewModel.playAll() },
                onPlayUserOnly = { viewModel.playUserOnly() },
                onPlayAiOnly = { viewModel.playAiOnly() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onStop = { viewModel.stop() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            Divider()
            
            // 메시지 목록
            MessageReviewList(
                messages = uiState.messages,
                currentPlayingId = uiState.currentPlayingMessageId,
                onPlayMessage = { messageId -> viewModel.playMessage(messageId) },
                onStopMessage = { viewModel.stop() },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
        
        // 속도 선택 드롭다운
        if (uiState.showSpeedMenu) {
            SpeedSelectionDialog(
                currentSpeed = uiState.playbackSpeed,
                onSpeedSelected = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    viewModel.toggleSpeedMenu()
                },
                onDismiss = { viewModel.toggleSpeedMenu() }
            )
        }
    }
}

@Composable
private fun ReviewStatistics(
    totalMessages: Int,
    voiceMessageCount: Int,
    totalDuration: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Message,
                label = "전체 메시지",
                value = totalMessages.toString()
            )
            StatItem(
                icon = Icons.Default.Mic,
                label = "음성 녹음",
                value = "$voiceMessageCount/${totalMessages / 2}"
            )
            StatItem(
                icon = Icons.Default.Timer,
                label = "재생 시간",
                value = totalDuration
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PlaybackControlBar(
    playbackState: PlaybackState,
    playbackMode: PlaybackMode,
    onPlayAll: () -> Unit,
    onPlayUserOnly: () -> Unit,
    onPlayAiOnly: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 재생 모드 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = playbackMode == PlaybackMode.ALL,
                onClick = onPlayAll,
                label = { Text("▶️ 전체 재생") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = playbackMode == PlaybackMode.USER_ONLY,
                onClick = onPlayUserOnly,
                label = { Text("👤 내 음성만") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = playbackMode == PlaybackMode.AI_ONLY,
                onClick = onPlayAiOnly,
                label = { Text("🤖 AI만") },
                modifier = Modifier.weight(1f)
            )
        }
        
        // 재생 상태 표시 및 컨트롤
        AnimatedVisibility(
            visible = playbackState is PlaybackState.Playing || playbackState is PlaybackState.Paused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (playbackState) {
                        is PlaybackState.Playing -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, "일시정지")
                            }
                        }
                        is PlaybackState.Paused -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayArrow, "재생")
                            }
                        }
                        else -> {}
                    }
                    
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, "정지")
                    }
                }
                
                // 진행률
                when (playbackState) {
                    is PlaybackState.Playing -> {
                        val state = playbackState as PlaybackState.Playing
                        Text(
                            text = "${state.currentIndex + 1}/${state.totalCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is PlaybackState.Paused -> {
                        val state = playbackState as PlaybackState.Paused
                        Text(
                            text = "${state.currentIndex + 1}/${state.totalCount} (일시정지)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun MessageReviewList(
    messages: List<MessageWithRecording>,
    currentPlayingId: Long?,
    onPlayMessage: (Long) -> Unit,
    onStopMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = messages,
            key = { it.message.id }
        ) { item ->
            MessageReviewItem(
                item = item,
                isPlaying = currentPlayingId == item.message.id,
                onPlay = { onPlayMessage(item.message.id) },
                onStop = onStopMessage
            )
        }
    }
}

@Composable
private fun MessageReviewItem(
    item: MessageWithRecording,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else if (item.message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘
            Icon(
                imageVector = if (item.message.isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = if (item.message.isUser) "사용자" else "AI",
                tint = if (item.message.isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
            
            // 메시지 내용
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                
                // 녹음 정보
                if (item.recording != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "녹음됨",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(item.recording.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 재생 버튼
            if (item.message.isUser && item.recording != null || !item.message.isUser) {
                FilledIconButton(
                    onClick = if (isPlaying) onStop else onPlay,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "정지" else "재생"
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("재생 속도") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                    FilterChip(
                        selected = currentSpeed == speed,
                        onClick = { onSpeedSelected(speed) },
                        label = {
                            Text(
                                text = "${speed}x",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, remainingSeconds)
    } else {
        String.format("0:%02d", remainingSeconds)
    }
}
