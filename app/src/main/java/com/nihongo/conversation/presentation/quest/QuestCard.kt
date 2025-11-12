package com.nihongo.conversation.presentation.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.Quest
import com.nihongo.conversation.data.local.entity.QuestType

/**
 * Quest Card UI Component
 * Shows quest title, progress, and reward
 */
@Composable
fun QuestCard(
    quest: Quest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Content
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quest type icon
                Icon(
                    imageVector = when(quest.type) {
                        QuestType.MESSAGE_COUNT -> Icons.Default.Message
                        QuestType.SCENARIO_COMPLETE -> Icons.Default.CheckCircle
                        QuestType.VOICE_ONLY_SESSION -> Icons.Default.Mic
                        QuestType.VOCABULARY_REVIEW -> Icons.Default.Book
                        QuestType.PRONUNCIATION_PRACTICE -> Icons.Default.RecordVoiceOver
                        QuestType.GRAMMAR_ANALYSIS -> Icons.Default.Analytics
                        QuestType.CONVERSATION_LENGTH -> Icons.Default.Timer
                        QuestType.NEW_SCENARIO -> Icons.Default.AddCircle
                    },
                    contentDescription = quest.type.name,
                    modifier = Modifier.size(40.dp),
                    tint = if (quest.isCompleted) Color(0xFFFFD700)
                           else MaterialTheme.colorScheme.primary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { quest.progress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (quest.isCompleted) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = "${quest.currentValue} / ${quest.targetValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Reward
            Box(
                modifier = Modifier
                    .background(
                        if (quest.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (quest.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "완료",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "⭐",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (quest.isCompleted) "완료!"
                               else "${quest.rewardPoints}P",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (quest.isCompleted) Color(0xFF4CAF50)
                                else Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}
