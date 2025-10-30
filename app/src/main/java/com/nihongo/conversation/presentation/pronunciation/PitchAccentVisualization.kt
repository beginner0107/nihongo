package com.nihongo.conversation.presentation.pronunciation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.*

/**
 * Visualizes pitch accent pattern with high/low indicators
 */
@Composable
fun PitchAccentVisualization(
    analysis: PitchAccentAnalysis,
    modifier: Modifier = Modifier,
    showExpectedPattern: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "ピッチアクセント分析",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Accent type badge
        AccentTypeBadge(accentType = analysis.accentType)

        // Mora-by-mora pitch visualization
        MoraPitchPattern(morae = analysis.morae)

        // Pitch contour graph
        PitchContourGraph(pitchPoints = analysis.pitchPattern)

        // Pattern string (LHHLL format)
        PatternStringDisplay(
            userPattern = analysis.patternString,
            expectedPattern = showExpectedPattern
        )

        // Confidence indicator
        ConfidenceIndicator(confidence = analysis.confidence)

        // Match status
        if (showExpectedPattern != null) {
            MatchStatusCard(matchesNative = analysis.matchesNative)
        }
    }
}

/**
 * Badge showing accent type with Japanese description
 */
@Composable
private fun AccentTypeBadge(accentType: AccentType) {
    val (label, description, color) = when (accentType) {
        AccentType.HEIBAN -> Triple("平板", "平板（下がらない）", MaterialTheme.colorScheme.primary)
        AccentType.ATAMADAKA -> Triple("頭高", "頭高（最初で下がる）", MaterialTheme.colorScheme.secondary)
        AccentType.NAKADAKA -> Triple("中高", "中高（中間で下がる）", MaterialTheme.colorScheme.tertiary)
        AccentType.ODAKA -> Triple("尾高", "尾高（最後で下がる）", MaterialTheme.colorScheme.error)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(8.dp)
            ) {}

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Show each mora with high/low pitch indicator
 */
@Composable
private fun MoraPitchPattern(morae: List<Mora>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "モーラ別ピッチ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mora boxes with pitch indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                morae.forEachIndexed { index, mora ->
                    MoraBox(
                        mora = mora,
                        index = index,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Single mora box with pitch visualization
 */
@Composable
private fun MoraBox(
    mora: Mora,
    index: Int,
    modifier: Modifier = Modifier
) {
    val pitchColor = if (mora.isHigh) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // High/Low indicator
        Surface(
            shape = CircleShape,
            color = pitchColor,
            modifier = Modifier.size(12.dp)
        ) {}

        // Mora text
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = pitchColor.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = mora.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = pitchColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Pitch frequency (Hz)
        Text(
            text = "${mora.pitch.toInt()} Hz",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Graph showing pitch contour over time
 */
@Composable
private fun PitchContourGraph(
    pitchPoints: List<PitchPoint>,
    modifier: Modifier = Modifier
) {
    if (pitchPoints.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ピッチ曲線",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Animated pitch contour
            val animatedProgress = remember { Animatable(0f) }

            LaunchedEffect(pitchPoints) {
                animatedProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }

            // Find min/max frequencies for scaling
            val minFreq = pitchPoints.minOf { it.frequency }
            val maxFreq = pitchPoints.maxOf { it.frequency }
            val freqRange = maxFreq - minFreq

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height

                if (freqRange == 0f) return@Canvas

                // Draw grid lines
                for (i in 0..4) {
                    val y = height * i / 4
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw pitch contour
                val path = Path()
                val timeRange = (pitchPoints.last().time - pitchPoints.first().time).toFloat()

                pitchPoints.forEachIndexed { index, point ->
                    val x = ((point.time - pitchPoints.first().time) / timeRange) * width
                    val normalizedPitch = (point.frequency - minFreq) / freqRange
                    val y = height - (normalizedPitch * height * 0.8f) - (height * 0.1f)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Draw path with animation
                drawPath(
                    path = path,
                    color = Color(0xFF1976D2),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw points
                val visiblePoints = (pitchPoints.size * animatedProgress.value).toInt()
                pitchPoints.take(visiblePoints).forEach { point ->
                    val x = ((point.time - pitchPoints.first().time) / timeRange) * width
                    val normalizedPitch = (point.frequency - minFreq) / freqRange
                    val y = height - (normalizedPitch * height * 0.8f) - (height * 0.1f)

                    drawCircle(
                        color = Color(0xFF1976D2),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // Frequency range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${minFreq.toInt()} Hz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${maxFreq.toInt()} Hz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Display pattern string in LHHLL format
 */
@Composable
private fun PatternStringDisplay(
    userPattern: String,
    expectedPattern: String?
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ピッチパターン",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // User pattern
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "あなた:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = userPattern,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Expected pattern (if provided)
            if (expectedPattern != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "正解:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = expectedPattern,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Show confidence level
 */
@Composable
private fun ConfidenceIndicator(confidence: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "信頼度:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LinearProgressIndicator(
            progress = confidence,
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = when {
                confidence >= 0.8f -> Color(0xFF4CAF50)
                confidence >= 0.6f -> Color(0xFFFFC107)
                else -> Color(0xFFFF5722)
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Match status card
 */
@Composable
private fun MatchStatusCard(matchesNative: Boolean) {
    data class MatchInfo(val bgColor: Color, val textColor: Color, val icon: String, val message: String)

    val info = if (matchesNative) {
        MatchInfo(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "✓",
            "完璧です！ネイティブと同じピッチです。"
        )
    } else {
        MatchInfo(
            Color(0xFFFFF3E0),
            Color(0xFFEF6C00),
            "!",
            "もう少しです。正解のパターンを練習しましょう。"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = info.bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.icon,
                style = MaterialTheme.typography.headlineSmall,
                color = info.textColor
            )
            Text(
                text = info.message,
                style = MaterialTheme.typography.bodyMedium,
                color = info.textColor
            )
        }
    }
}
