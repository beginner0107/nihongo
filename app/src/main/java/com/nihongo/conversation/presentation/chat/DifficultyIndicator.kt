package com.nihongo.conversation.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays vocabulary complexity indicator for AI messages
 */
@Composable
fun DifficultyIndicator(
    complexityScore: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    if (complexityScore == 0) return // Not analyzed

    val (label, color, stars) = getComplexityInfo(complexityScore)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Complexity",
                tint = color,
                modifier = Modifier.size(14.dp)
            )

            // Stars
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(stars) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Label
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

/**
 * Compact difficulty indicator with just stars
 */
@Composable
fun CompactDifficultyIndicator(
    complexityScore: Int,
    modifier: Modifier = Modifier
) {
    if (complexityScore == 0) return

    val (_, color, stars) = getComplexityInfo(complexityScore)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(stars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

/**
 * Get complexity information (label, color, star count)
 */
private fun getComplexityInfo(complexityScore: Int): Triple<String, Color, Int> {
    return when (complexityScore) {
        1 -> Triple(
            "基本",
            Color(0xFF4CAF50), // Green
            1
        )
        2 -> Triple(
            "一般",
            Color(0xFF8BC34A), // Light Green
            2
        )
        3 -> Triple(
            "中級",
            Color(0xFFFFC107), // Amber
            3
        )
        4 -> Triple(
            "上級",
            Color(0xFFFF9800), // Orange
            4
        )
        5 -> Triple(
            "専門",
            Color(0xFFF44336), // Red
            5
        )
        else -> Triple(
            "不明",
            Color.Gray,
            0
        )
    }
}

/**
 * Detailed complexity explanation dialog/card
 */
@Composable
fun ComplexityExplanation(
    complexityScore: Int,
    modifier: Modifier = Modifier
) {
    val (description, details) = when (complexityScore) {
        1 -> "基本 (N5-N4レベル)" to "シンプルな単語と文法。初心者向け。"
        2 -> "一般 (N4-N3レベル)" to "日常的な表現。基礎から少し進んだレベル。"
        3 -> "中級 (N3-N2レベル)" to "自然な会話表現。中級学習者向け。"
        4 -> "上級 (N2-N1レベル)" to "高度な文法と語彙。上級学習者向け。"
        5 -> "専門 (N1+レベル)" to "専門用語や敬語を含む。ネイティブレベル。"
        else -> "分析中" to ""
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        if (details.isNotEmpty()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
