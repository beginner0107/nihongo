package com.nihongo.conversation.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.core.voice.VoiceState
import com.nihongo.conversation.domain.model.VoiceOnlySession

/**
 * Main visual indicator for voice-only mode
 * Shows speaking/listening states with animations
 */
@Composable
fun VoiceOnlyIndicator(
    voiceState: VoiceState,
    session: VoiceOnlySession,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Timer at top
        ConversationTimer(session = session)

        // Large animated circle indicator
        VoiceStateCircle(voiceState = voiceState)

        // State label
        Text(
            text = getVoiceStateLabel(voiceState),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = getVoiceStateColor(voiceState)
        )

        // Instruction text
        Text(
            text = getVoiceStateInstruction(voiceState),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Progress indicator
        if (session.isActive) {
            LinearProgressIndicator(
                progress = session.completionPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Animated circle that pulses based on voice state
 */
@Composable
fun VoiceStateCircle(
    voiceState: VoiceState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState is VoiceState.Listening || voiceState is VoiceState.Speaking) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState is VoiceState.Processing || voiceState is VoiceState.Thinking) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .scale(scale)
            .background(
                color = getVoiceStateColor(voiceState).copy(alpha = alpha * 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    color = getVoiceStateColor(voiceState).copy(alpha = alpha * 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getVoiceStateIcon(voiceState),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = getVoiceStateColor(voiceState)
            )
        }
    }
}

/**
 * Timer showing elapsed and remaining time
 */
@Composable
fun ConversationTimer(
    session: VoiceOnlySession,
    modifier: Modifier = Modifier
) {
    // Update every second
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = formatTime(session.elapsedSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "残り ${session.remainingMinutes} 分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Transcript review dialog shown after conversation
 */
@Composable
fun TranscriptReviewDialog(
    session: VoiceOnlySession,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("会話終了！")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary stats
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("会話時間:")
                            Text(
                                text = "${session.elapsedMinutes} 分",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("メッセージ数:")
                            Text(
                                text = "${session.transcript.size} 件",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "会話の記録を確認しますか？",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Transcript preview (first few messages)
                session.transcript.take(3).forEach { entry ->
                    TranscriptEntryCard(entry = entry)
                }

                if (session.transcript.size > 3) {
                    Text(
                        text = "... 他 ${session.transcript.size - 3} 件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * Single transcript entry card
 */
@Composable
fun TranscriptEntryCard(
    entry: com.nihongo.conversation.domain.model.TranscriptEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isUser) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (entry.isUser) Icons.Default.Person else Icons.Default.Computer,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (entry.isUser) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )

            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.isUser) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

/**
 * Helper functions
 */
@Composable
private fun getVoiceStateColor(state: VoiceState): Color {
    return when (state) {
        is VoiceState.Idle -> MaterialTheme.colorScheme.outline
        is VoiceState.Listening -> MaterialTheme.colorScheme.primary
        is VoiceState.Processing -> MaterialTheme.colorScheme.secondary
        is VoiceState.Speaking -> MaterialTheme.colorScheme.tertiary
        is VoiceState.Thinking -> MaterialTheme.colorScheme.secondary
        is VoiceState.Error -> MaterialTheme.colorScheme.error
    }
}

private fun getVoiceStateIcon(state: VoiceState): androidx.compose.ui.graphics.vector.ImageVector {
    return when (state) {
        is VoiceState.Idle -> Icons.Default.MicNone
        is VoiceState.Listening -> Icons.Default.Mic
        is VoiceState.Processing -> Icons.Default.Sync
        is VoiceState.Speaking -> Icons.Default.VolumeUp
        is VoiceState.Thinking -> Icons.Default.Psychology
        is VoiceState.Error -> Icons.Default.Error
    }
}

private fun getVoiceStateLabel(state: VoiceState): String {
    return when (state) {
        is VoiceState.Idle -> "準備完了"
        is VoiceState.Listening -> "聞いています..."
        is VoiceState.Processing -> "処理中..."
        is VoiceState.Speaking -> "話しています..."
        is VoiceState.Thinking -> "考え中..."
        is VoiceState.Error -> "エラー"
    }
}

private fun getVoiceStateInstruction(state: VoiceState): String {
    return when (state) {
        is VoiceState.Idle -> "ボタンを押して話し始めてください"
        is VoiceState.Listening -> "何でも話してください"
        is VoiceState.Processing -> "少々お待ちください"
        is VoiceState.Speaking -> "よく聞いてください"
        is VoiceState.Thinking -> "応答を準備しています"
        is VoiceState.Error -> state.message
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
