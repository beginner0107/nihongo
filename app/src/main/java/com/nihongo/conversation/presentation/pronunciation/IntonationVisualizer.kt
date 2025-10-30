package com.nihongo.conversation.presentation.pronunciation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.*

/**
 * Visualizes sentence-level intonation patterns
 * Shows rising/falling patterns for questions, statements, etc.
 */
@Composable
fun IntonationVisualizer(
    analysis: IntonationAnalysis,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "イントネーション分析",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Sentence type indicator
        SentenceTypeCard(sentenceType = analysis.sentenceType)

        // Pitch contour visualization
        IntonationContourGraph(
            pitchContour = analysis.pitchContour,
            finalRise = analysis.finalRise,
            finalFall = analysis.finalFall
        )

        // Match status
        IntonationMatchCard(
            matchesExpected = analysis.matchesExpected,
            sentenceType = analysis.sentenceType
        )

        // Suggestions
        if (analysis.suggestions.isNotEmpty()) {
            SuggestionsCard(suggestions = analysis.suggestions)
        }
    }
}

/**
 * Card showing sentence type with appropriate icon
 */
@Composable
private fun SentenceTypeCard(sentenceType: SentenceType) {
    data class SentenceTypeInfo(val icon: ImageVector, val label: String, val description: String, val color: Color)

    val info = when (sentenceType) {
        SentenceType.STATEMENT -> SentenceTypeInfo(
            Icons.Default.ChatBubble,
            "平叙文",
            "文末が下がる",
            MaterialTheme.colorScheme.primary
        )
        SentenceType.QUESTION -> SentenceTypeInfo(
            Icons.Default.HelpOutline,
            "疑問文",
            "文末が上がる",
            MaterialTheme.colorScheme.secondary
        )
        SentenceType.EXCLAMATION -> SentenceTypeInfo(
            Icons.Default.Warning,
            "感嘆文",
            "強い感情",
            MaterialTheme.colorScheme.tertiary
        )
        SentenceType.COMMAND -> SentenceTypeInfo(
            Icons.Default.Campaign,
            "命令文",
            "急激に下がる",
            MaterialTheme.colorScheme.error
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = info.color.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                tint = info.color,
                modifier = Modifier.size(32.dp)
            )

            Column {
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = info.color
                )
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Graph showing intonation contour with rise/fall indicators
 */
@Composable
private fun IntonationContourGraph(
    pitchContour: List<PitchPoint>,
    finalRise: Boolean,
    finalFall: Boolean
) {
    if (pitchContour.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "イントネーション曲線",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Intonation contour canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 40f

                // Find min/max for scaling
                val minFreq = pitchContour.minOf { it.frequency }
                val maxFreq = pitchContour.maxOf { it.frequency }
                val freqRange = maxFreq - minFreq

                if (freqRange == 0f) return@Canvas

                val timeRange = (pitchContour.last().time - pitchContour.first().time).toFloat()

                // Draw grid
                for (i in 0..4) {
                    val y = height * i / 4
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw baseline (middle)
                val baseline = height / 2
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(padding, baseline),
                    end = Offset(width - padding, baseline),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw pitch contour
                val path = Path()
                pitchContour.forEachIndexed { index, point ->
                    val x = padding + ((point.time - pitchContour.first().time) / timeRange) * (width - 2 * padding)
                    val normalizedPitch = (point.frequency - minFreq) / freqRange
                    val y = height - (normalizedPitch * height * 0.7f) - (height * 0.15f)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Draw filled area under curve
                val fillPath = Path().apply {
                    addPath(path)
                    val lastX = padding + (width - 2 * padding)
                    lineTo(lastX, height)
                    lineTo(padding, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    color = Color(0xFF2196F3).copy(alpha = 0.2f)
                )

                // Draw contour line
                drawPath(
                    path = path,
                    color = Color(0xFF2196F3),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw rise/fall indicator at end
                if (pitchContour.size >= 2) {
                    val lastPoint = pitchContour.last()
                    val secondLastPoint = pitchContour[pitchContour.size - 2]

                    val x = padding + ((lastPoint.time - pitchContour.first().time) / timeRange) * (width - 2 * padding)
                    val lastNormalized = (lastPoint.frequency - minFreq) / freqRange
                    val y = height - (lastNormalized * height * 0.7f) - (height * 0.15f)

                    // Draw arrow indicating rise or fall
                    val arrowColor = if (finalRise) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    val arrowSize = 15f
                    val arrowPath = Path().apply {
                        if (finalRise) {
                            // Up arrow
                            moveTo(x, y - arrowSize)
                            lineTo(x - arrowSize / 2, y)
                            lineTo(x + arrowSize / 2, y)
                            close()
                        } else if (finalFall) {
                            // Down arrow
                            moveTo(x, y + arrowSize)
                            lineTo(x - arrowSize / 2, y)
                            lineTo(x + arrowSize / 2, y)
                            close()
                        }
                    }

                    drawPath(
                        path = arrowPath,
                        color = arrowColor
                    )
                }
            }

            // Rise/Fall indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IntonationIndicator(
                    label = "文末上昇",
                    isActive = finalRise,
                    color = Color(0xFF4CAF50)
                )
                IntonationIndicator(
                    label = "文末下降",
                    isActive = finalFall,
                    color = Color(0xFFFF5722)
                )
            }
        }
    }
}

/**
 * Rise/fall indicator chip
 */
@Composable
private fun IntonationIndicator(
    label: String,
    isActive: Boolean,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) color else Color.Gray,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) color else Color.Gray
            )
        }
    }
}

/**
 * Card showing if intonation matches expected
 */
@Composable
private fun IntonationMatchCard(
    matchesExpected: Boolean,
    sentenceType: SentenceType
) {
    data class MatchInfo(val bgColor: Color, val textColor: Color, val icon: ImageVector, val message: String)

    val info = if (matchesExpected) {
        MatchInfo(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle,
            getSuccessMessage(sentenceType)
        )
    } else {
        MatchInfo(
            Color(0xFFFFF3E0),
            Color(0xFFEF6C00),
            Icons.Default.Info,
            getImprovementMessage(sentenceType)
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = info.bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                tint = info.textColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = info.message,
                style = MaterialTheme.typography.bodyMedium,
                color = info.textColor
            )
        }
    }
}

/**
 * Suggestions card with improvement tips
 */
@Composable
private fun SuggestionsCard(suggestions: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "改善のヒント",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            suggestions.forEach { suggestion ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Get success message based on sentence type
 */
private fun getSuccessMessage(sentenceType: SentenceType): String {
    return when (sentenceType) {
        SentenceType.STATEMENT -> "完璧です！平叙文のイントネーションが自然です。"
        SentenceType.QUESTION -> "素晴らしい！疑問文の上昇イントネーションが正確です。"
        SentenceType.EXCLAMATION -> "よくできました！感嘆文の感情が伝わります。"
        SentenceType.COMMAND -> "正確です！命令文の強いトーンが出ています。"
    }
}

/**
 * Get improvement message based on sentence type
 */
private fun getImprovementMessage(sentenceType: SentenceType): String {
    return when (sentenceType) {
        SentenceType.STATEMENT -> "文末をもう少し下げてみましょう。平叙文は下降調です。"
        SentenceType.QUESTION -> "文末を上げて疑問の気持ちを表現しましょう。"
        SentenceType.EXCLAMATION -> "感情をもっと込めて、音程の変化を大きくしましょう。"
        SentenceType.COMMAND -> "もっと強く、急激に下げてみましょう。"
    }
}
