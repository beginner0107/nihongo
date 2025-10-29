package com.nihongo.conversation.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Bar Chart for daily study time
 */
@Composable
fun BarChart(
    data: List<Pair<String, Int>>, // (label, value)
    modifier: Modifier = Modifier,
    maxValue: Int = data.maxOfOrNull { it.second } ?: 1,
    barColor: Color = MaterialTheme.colorScheme.primary,
    label: String = "分"
) {
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val barWidth = (size.width - (data.size + 1) * 16f) / data.size
            val maxBarHeight = size.height - 60f

            data.forEachIndexed { index, (_, value) ->
                val barHeight = (value.toFloat() / maxValue) * maxBarHeight
                val x = 16f + index * (barWidth + 16f)
                val y = size.height - 40f - barHeight

                // Draw bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Draw value on top of bar
                if (value > 0) {
                    val textLayoutResult = textMeasurer.measure(
                        text = value.toString(),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = x + barWidth / 2 - textLayoutResult.size.width / 2,
                            y = y - textLayoutResult.size.height - 4f
                        )
                    )
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (labelText, _) ->
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Line Chart for messages per day
 */
@Composable
fun LineChart(
    data: List<Pair<String, Int>>, // (label, value)
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    pointColor: Color = MaterialTheme.colorScheme.secondary
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxValue = data.maxOfOrNull { it.second } ?: 1

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (data.size < 2) return@Canvas

            val pointSpacing = size.width / (data.size - 1)
            val maxHeight = size.height - 60f

            val points = data.mapIndexed { index, (_, value) ->
                val x = index * pointSpacing
                val y = size.height - 40f - ((value.toFloat() / maxValue) * maxHeight)
                Offset(x, y)
            }

            // Draw line
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f)
            )

            // Draw points and values
            points.forEachIndexed { index, point ->
                // Draw point
                drawCircle(
                    color = pointColor,
                    radius = 6f,
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = point
                )

                // Draw value
                val value = data[index].second
                if (value > 0) {
                    val textLayoutResult = textMeasurer.measure(
                        text = value.toString(),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = point.x - textLayoutResult.size.width / 2,
                            y = point.y - textLayoutResult.size.height - 12f
                        )
                    )
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (labelText, _) ->
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Pie Chart for scenario completion rate
 */
@Composable
fun PieChart(
    data: List<Pair<String, Int>>, // (label, value)
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.second }.toFloat()
    if (total == 0f) return

    // Read MaterialTheme outside of draw scope
    val fallbackColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val radius = min(size.width, size.height) / 2
            val center = Offset(size.width / 2, size.height / 2)
            var startAngle = -90f

            data.forEachIndexed { index, (_, value) ->
                val sweepAngle = (value / total) * 360f
                val color = colors.getOrElse(index) { fallbackColor }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle
            }

            // Draw white circle in center for donut effect
            drawCircle(
                color = Color.White,
                radius = radius * 0.5f,
                center = center
            )
        }
    }
}

/**
 * Legend for Pie Chart
 */
@Composable
fun ChartLegend(
    items: List<Pair<String, Color>>, // (label, color)
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(color = color)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Simple stat card
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
