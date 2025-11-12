package com.nihongo.conversation.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Today's Learning Card - Combines progress + streak in one compact card
 */
@Composable
fun TodayLearningCard(
    todayMessageCount: Int,
    dailyGoal: Int,
    currentStreak: Int,
    remainingHours: Int,
    remainingMinutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "오늘의 학습",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Messages
                MetricItem(
                    icon = Icons.Default.Message,
                    value = "$todayMessageCount / $dailyGoal",
                    label = "메시지",
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp)
                )

                // Streak
                MetricItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${currentStreak}일",
                    label = "연속 학습",
                    color = Color(0xFFFF6B35)
                )

                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp)
                )

                // Time Remaining
                MetricItem(
                    icon = Icons.Default.Timer,
                    value = "${remainingHours}h ${remainingMinutes}m",
                    label = "남은 시간",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
