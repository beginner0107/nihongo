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

/**
 * Today's Recommendation Card - AI-powered scenario recommendation
 *
 * Cute pastel design with friendly messaging
 * Height: ~180dp
 */
@Composable
fun TodayRecommendationCard(
    recommendation: ScenarioRecommendation,
    onRefresh: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pastel background based on difficulty
    val cardColor = when (recommendation.difficultyLevel) {
        1 -> Color(0xFFE5F3FF)  // Pastel blue for beginner
        2 -> Color(0xFFFFF9E5)  // Pastel yellow for intermediate
        3 -> Color(0xFFFFE5EC)  // Pastel pink for advanced
        else -> Color(0xFFE5FFE5)  // Pastel green
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recommendation reason with cute emoji
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B4A3A)
                )
            }

            // Scenario title with emoji
            Text(
                text = "${recommendation.scenario.thumbnailEmoji} ${recommendation.scenario.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )

            // Metadata row (시간 · 난이도 · 카테고리)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recommendation.estimatedTime}분 소요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (recommendation.difficultyLevel) {
                        1 -> MaterialTheme.colorScheme.primaryContainer
                        2 -> MaterialTheme.colorScheme.tertiaryContainer
                        3 -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = recommendation.difficulty,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = recommendation.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Friendly description
            Text(
                text = "\"${recommendation.scenario.description}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B4A3A).copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )

            // Action buttons row with cute messaging
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B4A3A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "다른 추천",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("🔄 다른 추천")
                }

                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (recommendation.difficultyLevel) {
                            1 -> Color(0xFF42A5F5)  // Blue for beginner
                            2 -> Color(0xFFFFCA28)  // Yellow for intermediate
                            3 -> Color(0xFFEC407A)  // Pink for advanced
                            else -> Color(0xFF66BB6A)  // Green
                        },
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "시작하기",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("대화 시작 →", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Data class for scenario recommendation
 */
data class ScenarioRecommendation(
    val scenario: com.nihongo.conversation.domain.model.Scenario,
    val reason: String,               // "초급 학습자님께 추천"
    val estimatedTime: Int,           // 5 (분)
    val difficulty: String,           // "초급"
    val difficultyLevel: Int,         // 1, 2, 3
    val category: String              // "일상 생활"
)
