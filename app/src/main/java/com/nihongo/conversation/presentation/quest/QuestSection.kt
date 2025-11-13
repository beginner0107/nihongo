package com.nihongo.conversation.presentation.quest

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.Quest
import com.nihongo.conversation.domain.model.UserPoints

/**
 * Quest Section for Home Screen
 * Shows user level, points, and active quests
 */
@Composable
fun QuestSection(
    quests: List<Quest>,
    userPoints: UserPoints?,
    onQuestClick: (Quest) -> Unit,
    modifier: Modifier = Modifier
) {
    if (quests.isEmpty() && userPoints == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with level and points
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "퀘스트",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "오늘의 퀘스트",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // User level and points
            if (userPoints != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lv.${userPoints.level}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "·",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "⭐",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${userPoints.todayPoints}P",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Quest cards
        quests.take(3).forEach { quest ->
            QuestCard(
                quest = quest,
                onClick = { onQuestClick(quest) }
            )
        }

        // Show all quests hint if more than 3
        if (quests.size > 3) {
            Text(
                text = "더 많은 퀘스트를 보려면 탭하세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Quest Completed Dialog
 * Shows when user completes a quest
 */
@Composable
fun QuestCompletedDialog(
    rewardPoints: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "퀘스트 완료",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "퀘스트 완료!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "축하합니다! 퀘스트를 완료했습니다.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFFFFD700).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "+${rewardPoints}P",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}
