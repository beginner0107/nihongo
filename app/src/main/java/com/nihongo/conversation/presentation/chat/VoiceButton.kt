package com.nihongo.conversation.presentation.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.core.voice.VoiceState

enum class VoiceButtonStyle {
    FILLED,   // Current style - filled background
    OUTLINED  // New style - outline only, more subtle
}

@Composable
fun VoiceButton(
    voiceState: VoiceState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
    style: VoiceButtonStyle = VoiceButtonStyle.FILLED
) {
    val isListening = voiceState is VoiceState.Listening
    val isSpeaking = voiceState is VoiceState.Speaking

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background when listening (for both styles)
        if (isListening && style == VoiceButtonStyle.FILLED) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        // Main button - different styles
        when (style) {
            VoiceButtonStyle.FILLED -> {
                FilledIconButton(
                    onClick = {
                        if (isListening) {
                            onStopRecording()
                        } else if (!isSpeaking) {
                            onStartRecording()
                        }
                    },
                    enabled = !isSpeaking,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            isListening -> MaterialTheme.colorScheme.error
                            isSpeaking -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "停止" else "録音",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            VoiceButtonStyle.OUTLINED -> {
                // More subtle outlined button for modern chat UI
                OutlinedIconButton(
                    onClick = {
                        if (isListening) {
                            onStopRecording()
                        } else if (!isSpeaking) {
                            onStartRecording()
                        }
                    },
                    enabled = !isSpeaking,
                    modifier = if (isListening) {
                        modifier.scale(scale) // Apply pulsing when listening
                    } else {
                        modifier
                    },
                    border = BorderStroke(
                        width = if (isListening) 2.dp else 1.dp,
                        color = when {
                            isListening -> MaterialTheme.colorScheme.error
                            isSpeaking -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        }
                    ),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = when {
                            isListening -> MaterialTheme.colorScheme.error
                            isSpeaking -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        containerColor = when {
                            isListening -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "停止" else "録音",
                        modifier = Modifier.size(if (modifier == Modifier.size(40.dp)) 18.dp else 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceStateIndicator(
    voiceState: VoiceState,
    modifier: Modifier = Modifier
) {
    if (voiceState !is VoiceState.Idle) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (voiceState) {
                    is VoiceState.Listening -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "聞いています...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is VoiceState.Speaking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "話しています...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is VoiceState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = voiceState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
