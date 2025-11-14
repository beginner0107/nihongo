package com.nihongo.conversation.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Hero Card - Main greeting card with core learning metrics
 *
 * Cute, simple design combining:
 * - Greeting message with random emoji
 * - Core metrics (streak, level, points) in one row
 * - Progress bar with encouraging message
 *
 * Height: ~200dp
 */
@Composable
fun HeroCard(
    streak: Int,
    todayMessages: Int,
    dailyGoal: Int,
    level: Int,
    points: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    // Random greeting emoji (changes each composition)
    val greetingEmoji = remember {
        listOf("👋", "🌸", "🌟", "💖", "🎀", "✨").random()
    }

    // Encouraging messages based on progress
    val progress = if (dailyGoal > 0) todayMessages.toFloat() / dailyGoal else 0f
    val remainingMessages = (dailyGoal - todayMessages).coerceAtLeast(0)
    val isGoalAchieved = todayMessages >= dailyGoal

    val encouragingMessage = when {
        isGoalAchieved -> "🎉 오늘 목표 달성! 대단해요!"
        progress >= 0.7f -> "💪 거의 다 왔어요! ${remainingMessages}개만 더!"
        progress >= 0.3f -> "✨ 잘하고 있어요! ${remainingMessages}개 남았어요"
        else -> "🌱 오늘 대화 목표까지 ${remainingMessages}개 남았어요"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PastelPink
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: Cute greeting
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "안녕하세요! $greetingEmoji",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF6B4A3A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "오늘도 화이팅!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Middle: Core metrics chips (Streak, Level, Points)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak chip
                MetricChip(
                    icon = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF6B35)) },
                    value = "${streak}일",
                    label = "연속",
                    backgroundColor = Color(0xFFFFE5E5),
                    textColor = Color(0xFFD32F2F)
                )

                // Level chip
                MetricChip(
                    icon = { Icon(Icons.Default.School, null, tint = Color(0xFF5E35B1)) },
                    value = "Lv.$level",
                    label = "",
                    backgroundColor = Color(0xFFEDE7F6),
                    textColor = Color(0xFF5E35B1)
                )

                // Points chip
                MetricChip(
                    icon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300)) },
                    value = "${points}P",
                    label = "",
                    backgroundColor = Color(0xFFFFF9E5),
                    textColor = Color(0xFFF57C00)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom: Progress bar + encouraging message
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.coerceAtMost(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (isGoalAchieved) Color(0xFF4CAF50) else Color(0xFFE91E63),
                    trackColor = Color.White.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = encouragingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B4A3A),
                        modifier = Modifier.weight(1f)
                    )

                    // Longest streak badge (small)
                    if (longestStreak > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color.White.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "최장 ${longestStreak}일",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B4A3A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric Chip - Reusable chip for displaying metrics (streak, level, points)
 */
@Composable
private fun MetricChip(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Pastel color palette
private val PastelPink = Color(0xFFFFE5EC)
private val PastelYellow = Color(0xFFFFF9E5)
private val PastelBlue = Color(0xFFE5F3FF)
private val PastelGreen = Color(0xFFE5FFE5)
