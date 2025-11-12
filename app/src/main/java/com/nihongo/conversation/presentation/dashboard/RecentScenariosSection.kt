package com.nihongo.conversation.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.conversation.domain.model.Scenario

/**
 * Recent Scenarios Section - Shows recently accessed scenarios
 */
@Composable
fun RecentScenariosSection(
    scenarios: List<Scenario>,
    onScenarioClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (scenarios.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "최근 학습",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Scenario list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scenarios.take(3).forEach { scenario ->
                RecentScenarioItem(
                    scenario = scenario,
                    onClick = { onScenarioClick(scenario.id) }
                )
            }
        }
    }
}

@Composable
private fun RecentScenarioItem(
    scenario: Scenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji Icon
            Text(
                text = scenario.thumbnailEmoji,
                fontSize = 36.sp
            )

            // Title and metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = scenario.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category
                    Text(
                        text = getCategoryLabel(scenario.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Difficulty badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (scenario.difficulty) {
                            1 -> MaterialTheme.colorScheme.primaryContainer
                            2 -> MaterialTheme.colorScheme.tertiaryContainer
                            3 -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when (scenario.difficulty) {
                                1 -> "초급"
                                2 -> "중급"
                                3 -> "고급"
                                else -> "초급"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryLabel(category: String): String {
    return when (category) {
        "DAILY_LIFE" -> "🏠 일상 생활"
        "WORK" -> "💼 업무"
        "TRAVEL" -> "✈️ 여행"
        "ENTERTAINMENT" -> "🎵 엔터테인먼트"
        "ESPORTS" -> "🎮 e스포츠"
        "TECH" -> "💻 기술"
        else -> "📚 기타"
    }
}
