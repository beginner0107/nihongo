package com.nihongo.conversation.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongo.conversation.domain.model.GrammarComponent
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarBottomSheet(
    grammarExplanation: GrammarExplanation?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "문법 분석 중...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (grammarExplanation != null) {
            GrammarContent(
                grammarExplanation = grammarExplanation,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun GrammarContent(
    grammarExplanation: GrammarExplanation,
    onDismiss: () -> Unit
) {
    var showDetailedExplanation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with close button
        item {
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
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "문법 분석",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기"
                    )
                }
            }
        }

        // Original sentence with highlighted components
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "원문",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildHighlightedText(
                            grammarExplanation.originalText,
                            grammarExplanation.components
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    )
                }
            }
        }

        // Quick overview
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = grammarExplanation.overallExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Grammar components breakdown
        if (grammarExplanation.components.isNotEmpty()) {
            item {
                Text(
                    text = "문법 요소",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(grammarExplanation.components) { component ->
                GrammarComponentCard(component)
            }
        }

        // Detailed explanation toggle
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetailedExplanation = !showDetailedExplanation },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "상세 설명",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (showDetailedExplanation) "접기" else "펼치기",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Detailed explanation content
        if (showDetailedExplanation) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = grammarExplanation.detailedExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Examples from conversation
        if (grammarExplanation.examples.isNotEmpty()) {
            item {
                Text(
                    text = "대화 예시",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(grammarExplanation.examples) { example ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Related patterns
        if (grammarExplanation.relatedPatterns.isNotEmpty()) {
            item {
                Text(
                    text = "관련 문법 패턴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grammarExplanation.relatedPatterns.forEach { pattern ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pattern,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarComponentCard(component: GrammarComponent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        color = getGrammarTypeColor(component.type),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Japanese text
                    Text(
                        text = component.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getGrammarTypeColor(component.type)
                    )

                    // Grammar type badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getGrammarTypeColor(component.type).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = component.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = getGrammarTypeColor(component.type),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Explanation
                Text(
                    text = component.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Build highlighted annotated string with grammar components
 */
private fun buildHighlightedText(
    originalText: String,
    components: List<GrammarComponent>
): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0

        // Sort components by start index to process in order
        val sortedComponents = components.sortedBy { it.startIndex }

        sortedComponents.forEach { component ->
            // Add text before this component (if any)
            if (currentIndex < component.startIndex) {
                append(originalText.substring(currentIndex, component.startIndex))
            }

            // Add highlighted component
            withStyle(
                style = SpanStyle(
                    color = getGrammarTypeColor(component.type),
                    fontWeight = FontWeight.Bold,
                    background = getGrammarTypeColor(component.type).copy(alpha = 0.15f)
                )
            ) {
                append(component.text)
            }

            currentIndex = component.endIndex
        }

        // Add remaining text after last component
        if (currentIndex < originalText.length) {
            append(originalText.substring(currentIndex))
        }
    }
}

/**
 * Get color for grammar type
 */
private fun getGrammarTypeColor(type: GrammarType): Color {
    return Color(android.graphics.Color.parseColor("#${type.colorCode.substring(4)}"))
}
