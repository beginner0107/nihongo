package com.nihongo.conversation.presentation.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.FeedbackSeverity
import com.nihongo.conversation.domain.model.FeedbackType
import com.nihongo.conversation.domain.model.GrammarFeedback

/**
 * Card displaying a single piece of grammar feedback
 */
@Composable
fun FeedbackCard(
    feedback: GrammarFeedback,
    onAcknowledge: (() -> Unit)? = null,
    onApply: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getFeedbackColor(feedback.feedbackType, feedback.severity)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Type and Severity
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
                        imageVector = getFeedbackIcon(feedback.feedbackType),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = getFeedbackIconColor(feedback.severity)
                    )
                    Text(
                        text = getFeedbackTypeLabel(feedback.feedbackType),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                SeverityBadge(severity = feedback.severity)
            }

            // Original and Corrected Text
            if (feedback.correctedText != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Original (with strikethrough)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = feedback.originalText,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Corrected
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = feedback.correctedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Explanation (always visible)
            Text(
                text = feedback.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Better Expression (if available)
            if (feedback.betterExpression != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                text = "より良い表現:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = feedback.betterExpression,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Expand/Collapse button for additional notes
            if (feedback.additionalNotes != null) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "詳細を隠す" else "詳細を見る",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = feedback.additionalNotes!!,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons
            if (onAcknowledge != null || onApply != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onAcknowledge != null && !feedback.userAcknowledged) {
                        TextButton(onClick = onAcknowledge) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("確認済み", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (onApply != null && feedback.correctedText != null && !feedback.userAppliedCorrection) {
                        TextButton(onClick = onApply) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("適用", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Severity badge showing error level
 */
@Composable
fun SeverityBadge(severity: FeedbackSeverity) {
    val (text, color) = when (severity) {
        FeedbackSeverity.ERROR -> "間違い" to MaterialTheme.colorScheme.error
        FeedbackSeverity.WARNING -> "注意" to Color(0xFFED6C02) // Orange
        FeedbackSeverity.INFO -> "情報" to MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Get background color based on feedback type and severity
 */
@Composable
fun getFeedbackColor(type: FeedbackType, severity: FeedbackSeverity): Color {
    return when (severity) {
        FeedbackSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer
        FeedbackSeverity.WARNING -> Color(0xFFFFF3E0) // Light orange
        FeedbackSeverity.INFO -> MaterialTheme.colorScheme.secondaryContainer
    }
}

/**
 * Get icon for feedback type
 */
fun getFeedbackIcon(type: FeedbackType): ImageVector {
    return when (type) {
        FeedbackType.GRAMMAR_ERROR -> Icons.Default.ReportProblem
        FeedbackType.UNNATURAL -> Icons.Default.Psychology
        FeedbackType.BETTER_EXPRESSION -> Icons.Default.Lightbulb
        FeedbackType.CONVERSATION_FLOW -> Icons.Default.TrendingUp
        FeedbackType.POLITENESS_LEVEL -> Icons.Default.Person
    }
}

/**
 * Get icon color based on severity
 */
@Composable
fun getFeedbackIconColor(severity: FeedbackSeverity): Color {
    return when (severity) {
        FeedbackSeverity.ERROR -> MaterialTheme.colorScheme.error
        FeedbackSeverity.WARNING -> Color(0xFFED6C02) // Orange
        FeedbackSeverity.INFO -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Get Japanese label for feedback type
 */
fun getFeedbackTypeLabel(type: FeedbackType): String {
    return when (type) {
        FeedbackType.GRAMMAR_ERROR -> "文法"
        FeedbackType.UNNATURAL -> "不自然な表現"
        FeedbackType.BETTER_EXPRESSION -> "より良い表現"
        FeedbackType.CONVERSATION_FLOW -> "会話の流れ"
        FeedbackType.POLITENESS_LEVEL -> "敬語レベル"
    }
}

/**
 * Compact version of feedback card for inline display
 */
@Composable
fun CompactFeedbackCard(
    feedback: GrammarFeedback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getFeedbackColor(feedback.feedbackType, feedback.severity).copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFeedbackIcon(feedback.feedbackType),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = getFeedbackIconColor(feedback.severity)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = getFeedbackTypeLabel(feedback.feedbackType),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = feedback.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
