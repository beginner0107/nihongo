package com.nihongo.conversation.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.MatchType
import com.nihongo.conversation.domain.model.PronunciationResult
import com.nihongo.conversation.domain.model.EnhancedPronunciationResult
import com.nihongo.conversation.domain.model.ProblematicSound
import com.nihongo.conversation.presentation.pronunciation.PitchAccentVisualization
import com.nihongo.conversation.presentation.pronunciation.IntonationVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationPracticeSheet(
    targetText: String,
    result: PronunciationResult?,
    enhancedResult: EnhancedPronunciationResult? = null,
    isRecording: Boolean,
    bestScore: Int? = null,
    previousAttempts: Int = 0,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onRetry: () -> Unit,
    onSpeak: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "発音練習",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "閉じる"
                    )
                }
            }

            HorizontalDivider()

            // Target Text Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "練習する文章",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = { onSpeak(targetText) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "お手本を聞く",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Previous Best Score Display
            if (bestScore != null && bestScore > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "前回のベスト:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$bestScore 点",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (previousAttempts > 0) {
                        Text(
                            text = "$previousAttempts 回目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Recording Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isRecording) {
                            onStopRecording()
                        } else {
                            onStartRecording()
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = if (isRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isRecording) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Mic
                        },
                        contentDescription = if (isRecording) "停止" else "録音",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (isRecording) {
                Text(
                    text = "話してください...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Results Section
            AnimatedVisibility(
                visible = result != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                result?.let {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider()

                        // Score Display
                        AccuracyScoreCard(
                            score = it.accuracyScore,
                            bestScore = bestScore
                        )

                        // Comparison Display
                        ComparisonCard(result = it)

                        // Enhanced Pronunciation Features
                        enhancedResult?.let { enhanced ->
                            HorizontalDivider()

                            // Overall Score Display
                            EnhancedScoreCard(score = enhanced.overallScore)

                            // Pitch Accent Analysis
                            enhanced.pitchAccent?.let { pitchAccent ->
                                PitchAccentVisualization(analysis = pitchAccent)
                            }

                            // Intonation Analysis
                            enhanced.intonation?.let { intonation ->
                                IntonationVisualizer(analysis = intonation)
                            }

                            // Speed & Rhythm Analysis
                            enhanced.rhythm?.let { rhythm ->
                                RhythmAnalysisCard(rhythm = rhythm)
                            }

                            // Problematic Sounds
                            if (enhanced.problematicSounds.isNotEmpty()) {
                                ProblematicSoundsCard(sounds = enhanced.problematicSounds)
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("もう一度")
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("完了")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AccuracyScoreCard(score: Int, bestScore: Int? = null) {
    val improvement = if (bestScore != null && bestScore > 0) {
        score - bestScore
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                score >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                score >= 60 -> Color(0xFFFFC107).copy(alpha = 0.1f)
                else -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "正確度",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score >= 80 -> Color(0xFF4CAF50)
                        score >= 60 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = "点",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Improvement indicator
            if (improvement != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            improvement > 0 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            improvement < 0 -> Color(0xFFF44336).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                improvement > 0 -> Icons.Default.TrendingUp
                                improvement < 0 -> Icons.Default.TrendingDown
                                else -> Icons.Default.Remove
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when {
                                improvement > 0 -> Color(0xFF4CAF50)
                                improvement < 0 -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = when {
                                improvement > 0 -> "+$improvement"
                                improvement < 0 -> "$improvement"
                                else -> "同じ"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                improvement > 0 -> Color(0xFF4CAF50)
                                improvement < 0 -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Score feedback
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when {
                        score >= 80 -> Icons.Default.SentimentVerySatisfied
                        score >= 60 -> Icons.Default.SentimentSatisfied
                        else -> Icons.Default.SentimentDissatisfied
                    },
                    contentDescription = null,
                    tint = when {
                        score >= 80 -> Color(0xFF4CAF50)
                        score >= 60 -> Color(0xFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = when {
                        score >= 90 && improvement != null && improvement > 0 -> "新記録！完璧です！"
                        score >= 90 -> "完璧です！"
                        score >= 80 && improvement != null && improvement > 0 -> "上達しました！"
                        score >= 80 -> "とても良い！"
                        score >= 70 -> "良いです！"
                        score >= 60 -> "もう少しです"
                        else -> "もう一度練習しましょう"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ComparisonCard(result: PronunciationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "比較",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            // Expected text
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "お手本:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = result.expectedText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            HorizontalDivider()

            // Recognized text with highlighting
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "あなたの発音:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                val annotatedText = buildAnnotatedString {
                    result.wordComparison.forEach { match ->
                        val color = when (match.matchType) {
                            MatchType.EXACT -> Color(0xFF4CAF50)
                            MatchType.SIMILAR -> Color(0xFFFFC107)
                            MatchType.DIFFERENT -> Color(0xFFF44336)
                            MatchType.MISSING -> Color(0xFF9E9E9E)
                        }

                        val displayText = match.recognizedWord ?: match.expectedWord

                        withStyle(
                            style = SpanStyle(
                                color = color,
                                fontWeight = FontWeight.Medium,
                                background = color.copy(alpha = 0.15f)
                            )
                        ) {
                            append(displayText)
                        }
                    }
                }

                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyLarge,
                    letterSpacing = MaterialTheme.typography.bodyLarge.letterSpacing
                )
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF4CAF50), label = "正確")
                LegendItem(color = Color(0xFFFFC107), label = "近い")
                LegendItem(color = Color(0xFFF44336), label = "違う")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Enhanced score card showing all pronunciation dimensions
 */
@Composable
fun EnhancedScoreCard(score: com.nihongo.conversation.domain.model.PronunciationScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "総合評価",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Overall score and grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${score.overall.toInt()} 点",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = getGradeLabel(score.grade),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Icon(
                    imageVector = getGradeIcon(score.grade),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            // Individual scores
            ScoreDimension("正確性", score.accuracy)
            ScoreDimension("ピッチ", score.pitch)
            ScoreDimension("イントネーション", score.intonation)
            ScoreDimension("リズム", score.rhythm)
            ScoreDimension("明瞭さ", score.clarity)
            ScoreDimension("自然さ", score.naturalness)
        }
    }
}

@Composable
private fun ScoreDimension(label: String, score: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${score.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier.fillMaxWidth(),
            color = getScoreColor(score)
        )
    }
}

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFFF5722)
    }
}

private fun getGradeLabel(grade: com.nihongo.conversation.domain.model.PronunciationGrade): String {
    return when (grade) {
        com.nihongo.conversation.domain.model.PronunciationGrade.NATIVE_LIKE -> "ネイティブレベル"
        com.nihongo.conversation.domain.model.PronunciationGrade.EXCELLENT -> "優秀"
        com.nihongo.conversation.domain.model.PronunciationGrade.GOOD -> "良い"
        com.nihongo.conversation.domain.model.PronunciationGrade.FAIR -> "まあまあ"
        com.nihongo.conversation.domain.model.PronunciationGrade.NEEDS_WORK -> "要改善"
        com.nihongo.conversation.domain.model.PronunciationGrade.BEGINNER -> "初心者"
    }
}

private fun getGradeIcon(grade: com.nihongo.conversation.domain.model.PronunciationGrade): ImageVector {
    return when (grade) {
        com.nihongo.conversation.domain.model.PronunciationGrade.NATIVE_LIKE -> Icons.Default.Stars
        com.nihongo.conversation.domain.model.PronunciationGrade.EXCELLENT -> Icons.Default.EmojiEvents
        com.nihongo.conversation.domain.model.PronunciationGrade.GOOD -> Icons.Default.ThumbUp
        com.nihongo.conversation.domain.model.PronunciationGrade.FAIR -> Icons.Default.SentimentNeutral
        com.nihongo.conversation.domain.model.PronunciationGrade.NEEDS_WORK -> Icons.Default.TrendingUp
        com.nihongo.conversation.domain.model.PronunciationGrade.BEGINNER -> Icons.Default.School
    }
}

/**
 * Rhythm analysis card
 */
@Composable
fun RhythmAnalysisCard(rhythm: com.nihongo.conversation.domain.model.RhythmAnalysis) {
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
                text = "リズム・速度分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Speed rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "スピード:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = getSpeedColor(rhythm.speedRating)
                ) {
                    Text(
                        text = getSpeedLabel(rhythm.speedRating),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Rhythm score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "リズムスコア:")
                Text(
                    text = "${rhythm.rhythmScore.toInt()} / 100",
                    fontWeight = FontWeight.Bold
                )
            }

            // Naturalness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "自然さ:")
                Text(
                    text = "${rhythm.naturalness.toInt()} / 100",
                    fontWeight = FontWeight.Bold
                )
            }

            // Average mora duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "平均モーラ時間:")
                Text(
                    text = "${rhythm.averageMoraDuration} ms",
                    fontWeight = FontWeight.Bold
                )
            }

            // Native comparison if available
            rhythm.comparison?.let { comparison ->
                HorizontalDivider()

                Text(
                    text = "ネイティブとの比較",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "速度比:")
                    Text(
                        text = "${(comparison.speedRatio * 100).toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "リズム類似度:")
                    Text(
                        text = "${comparison.rhythmSimilarity.toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ピッチ類似度:")
                    Text(
                        text = "${comparison.pitchSimilarity.toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getSpeedColor(rating: com.nihongo.conversation.domain.model.SpeedRating): Color {
    return when (rating) {
        com.nihongo.conversation.domain.model.SpeedRating.TOO_SLOW -> Color(0xFF2196F3)
        com.nihongo.conversation.domain.model.SpeedRating.SLOW -> Color(0xFF4CAF50)
        com.nihongo.conversation.domain.model.SpeedRating.NATURAL -> Color(0xFF4CAF50)
        com.nihongo.conversation.domain.model.SpeedRating.FAST -> Color(0xFFFFC107)
        com.nihongo.conversation.domain.model.SpeedRating.TOO_FAST -> Color(0xFFFF5722)
    }
}

private fun getSpeedLabel(rating: com.nihongo.conversation.domain.model.SpeedRating): String {
    return when (rating) {
        com.nihongo.conversation.domain.model.SpeedRating.TOO_SLOW -> "遅すぎる"
        com.nihongo.conversation.domain.model.SpeedRating.SLOW -> "やや遅い"
        com.nihongo.conversation.domain.model.SpeedRating.NATURAL -> "自然"
        com.nihongo.conversation.domain.model.SpeedRating.FAST -> "やや速い"
        com.nihongo.conversation.domain.model.SpeedRating.TOO_FAST -> "速すぎる"
    }
}

/**
 * Problematic sounds card
 */
@Composable
fun ProblematicSoundsCard(sounds: List<ProblematicSound>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF6C00)
                )
                Text(
                    text = "要注意の音",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF6C00)
                )
            }

            sounds.forEach { sound ->
                ProblematicSoundItem(sound = sound)
            }
        }
    }
}

@Composable
private fun ProblematicSoundItem(sound: ProblematicSound) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = getSeverityColor(sound.severity)
                    ) {
                        Text(
                            text = sound.mora,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = getSoundTypeLabel(sound.soundType),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = getSeverityLabel(sound.severity),
                    style = MaterialTheme.typography.labelSmall,
                    color = getSeverityColor(sound.severity)
                )
            }

            Text(
                text = sound.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            sound.nativeExample?.let { example ->
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "💡 $example",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun getSeverityColor(severity: com.nihongo.conversation.domain.model.IssueSeverity): Color {
    return when (severity) {
        com.nihongo.conversation.domain.model.IssueSeverity.CRITICAL -> Color(0xFFD32F2F)
        com.nihongo.conversation.domain.model.IssueSeverity.HIGH -> Color(0xFFFF5722)
        com.nihongo.conversation.domain.model.IssueSeverity.MEDIUM -> Color(0xFFFFC107)
        com.nihongo.conversation.domain.model.IssueSeverity.LOW -> Color(0xFF2196F3)
    }
}

private fun getSeverityLabel(severity: com.nihongo.conversation.domain.model.IssueSeverity): String {
    return when (severity) {
        com.nihongo.conversation.domain.model.IssueSeverity.CRITICAL -> "重大"
        com.nihongo.conversation.domain.model.IssueSeverity.HIGH -> "高"
        com.nihongo.conversation.domain.model.IssueSeverity.MEDIUM -> "中"
        com.nihongo.conversation.domain.model.IssueSeverity.LOW -> "低"
    }
}

private fun getSoundTypeLabel(soundType: com.nihongo.conversation.domain.model.JapaneseSound): String {
    return when (soundType) {
        com.nihongo.conversation.domain.model.JapaneseSound.RA_GYOU -> "ら行"
        com.nihongo.conversation.domain.model.JapaneseSound.TSU_CHU -> "つ/ちゅ"
        com.nihongo.conversation.domain.model.JapaneseSound.LONG_VOWEL -> "長音"
        com.nihongo.conversation.domain.model.JapaneseSound.DOUBLE_CONSONANT -> "促音（っ）"
        com.nihongo.conversation.domain.model.JapaneseSound.N_SOUND -> "ん"
        com.nihongo.conversation.domain.model.JapaneseSound.WA_WO -> "は/を"
        com.nihongo.conversation.domain.model.JapaneseSound.GA_NGA -> "が/んが"
        com.nihongo.conversation.domain.model.JapaneseSound.SHI_CHI -> "し/ち"
        com.nihongo.conversation.domain.model.JapaneseSound.TSU_SU -> "つ/す"
        com.nihongo.conversation.domain.model.JapaneseSound.FU_HU -> "ふ"
    }
}
