package com.nihongo.conversation.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.Quest

/**
 * Learning Status Card - Integrated view of streak, daily goal, level, and top quests
 *
 * Phase 11 - Option C Component
 * Height: ~180dp
 */
@Composable
fun LearningStatusCard(
    streak: Int,
    todayMessages: Int,
    dailyGoal: Int,
    level: Int,
    points: Int,
    topQuests: List<Quest>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Core metrics (스트릭, 메시지, 레벨)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Streak chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${streak}일 연속",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Messages chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$todayMessages/$dailyGoal 메시지",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Level chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lv.$level",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Row 2: Progress bar
            val progress = if (dailyGoal > 0) todayMessages.toFloat() / dailyGoal else 0f
            val isGoalAchieved = todayMessages >= dailyGoal

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Row 3: Top 2 quests (축약형)
            if (topQuests.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topQuests.take(2).forEach { quest ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = getQuestIcon(quest),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${quest.title} (${quest.currentValue}/${quest.targetValue})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (quest.isCompleted)
                                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else
                                    Color(0xFFFFD700).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (quest.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "완료!",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    } else {
                                        Text(text = "⭐", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "${quest.rewardPoints}P",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB300)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getQuestIcon(quest: Quest): androidx.compose.ui.graphics.vector.ImageVector {
    return when (quest.type) {
        com.nihongo.conversation.data.local.entity.QuestType.MESSAGE_COUNT -> Icons.Default.Message
        com.nihongo.conversation.data.local.entity.QuestType.SCENARIO_COMPLETE -> Icons.Default.CheckCircle
        com.nihongo.conversation.data.local.entity.QuestType.VOICE_ONLY_SESSION -> Icons.Default.Mic
        com.nihongo.conversation.data.local.entity.QuestType.VOCABULARY_REVIEW -> Icons.Default.Style
        com.nihongo.conversation.data.local.entity.QuestType.PRONUNCIATION_PRACTICE -> Icons.Default.RecordVoiceOver
        com.nihongo.conversation.data.local.entity.QuestType.GRAMMAR_ANALYSIS -> Icons.Default.School
        com.nihongo.conversation.data.local.entity.QuestType.NEW_SCENARIO -> Icons.Default.Add
        com.nihongo.conversation.data.local.entity.QuestType.CONVERSATION_LENGTH -> Icons.Default.Timer
    }
}
