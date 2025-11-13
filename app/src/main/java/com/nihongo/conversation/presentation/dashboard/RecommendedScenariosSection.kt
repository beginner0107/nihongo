package com.nihongo.conversation.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Recommended Scenarios Section - Shows 2-3 quick-start scenarios
 */
@Composable
fun RecommendedScenariosSection(
    scenarios: List<Scenario>,
    onScenarioClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (scenarios.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "추천 시나리오",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Horizontal scrolling cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(scenarios.take(3)) { scenario ->
                CompactScenarioCard(
                    scenario = scenario,
                    onClick = { onScenarioClick(scenario.id) }
                )
            }
        }
    }
}

@Composable
private fun CompactScenarioCard(
    scenario: Scenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(160.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Emoji Icon
            Text(
                text = scenario.thumbnailEmoji,
                fontSize = 44.sp
            )

            // Title
            Text(
                text = scenario.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // Metadata
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category
                Text(
                    text = getCategoryEmoji(scenario.category),
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "DAILY_LIFE" -> "🏠"
        "WORK" -> "💼"
        "TRAVEL" -> "✈️"
        "ENTERTAINMENT" -> "🎵"
        "ESPORTS" -> "🎮"
        "TECH" -> "💻"
        else -> "📚"
    }
}
