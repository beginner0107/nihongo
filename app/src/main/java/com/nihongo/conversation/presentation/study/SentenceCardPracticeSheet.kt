package com.nihongo.conversation.presentation.study

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceCardPracticeSheet(
    card: SentenceCard,
    practiceMode: PracticeMode,
    fillInBlankExercise: FillInTheBlankExercise? = null,
    onPlayAudio: () -> Unit,
    onRecord: () -> Unit,
    onSubmitAnswer: (String, ReviewDifficulty) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userAnswer by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf<ReviewDifficulty?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = modifier
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
                Column {
                    Text(
                        text = "文カード練習",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getPracticeModeLabel(practiceMode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "閉じる")
                }
            }

            HorizontalDivider()

            // Context Card (if available)
            if (card.conversationContext != null || card.scenarioTitle != null) {
                ContextCard(
                    context = card.conversationContext,
                    scenarioTitle = card.scenarioTitle
                )
            }

            // Practice Content based on mode
            when (practiceMode) {
                PracticeMode.READING -> ReadingModeContent(
                    card = card,
                    showAnswer = showAnswer,
                    onShowAnswer = { showAnswer = true }
                )

                PracticeMode.LISTENING -> ListeningModeContent(
                    card = card,
                    userAnswer = userAnswer,
                    showAnswer = showAnswer,
                    onAnswerChange = { userAnswer = it },
                    onPlayAudio = onPlayAudio,
                    onShowAnswer = { showAnswer = true }
                )

                PracticeMode.FILL_IN_BLANK -> fillInBlankExercise?.let { exercise ->
                    FillInBlankModeContent(
                        exercise = exercise,
                        userAnswer = userAnswer,
                        showAnswer = showAnswer,
                        onAnswerChange = { userAnswer = it },
                        onShowAnswer = { showAnswer = true }
                    )
                }

                PracticeMode.SPEAKING -> SpeakingModeContent(
                    card = card,
                    showAnswer = showAnswer,
                    onRecord = onRecord,
                    onPlayAudio = onPlayAudio,
                    onShowAnswer = { showAnswer = true }
                )

                PracticeMode.MIXED -> {
                    // Same as reading for now
                    ReadingModeContent(
                        card = card,
                        showAnswer = showAnswer,
                        onShowAnswer = { showAnswer = true }
                    )
                }
            }

            // Grammar Pattern Card (if applicable)
            if (card.pattern != null) {
                GrammarPatternCard(
                    pattern = card.pattern,
                    explanation = card.patternExplanation
                )
            }

            // Answer revealed - show difficulty buttons
            AnimatedVisibility(
                visible = showAnswer,
                enter = fadeIn() + expandVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider()

                    Text(
                        text = "どれくらい難しかったですか？",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    DifficultyButtons(
                        selectedDifficulty = selectedDifficulty,
                        onSelectDifficulty = { difficulty ->
                            selectedDifficulty = difficulty
                            onSubmitAnswer(userAnswer, difficulty)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContextCard(
    context: String?,
    scenarioTitle: String?
) {
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "文脈",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            scenarioTitle?.let {
                Text(
                    text = "シナリオ: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            context?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun ReadingModeContent(
    card: SentenceCard,
    showAnswer: Boolean,
    onShowAnswer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Japanese sentence
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = card.sentence,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Romanization (if available)
        card.romanization?.let { romaji ->
            Text(
                text = romaji,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!showAnswer) {
            Button(
                onClick = onShowAnswer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("答えを見る")
            }
        } else {
            // Show translation
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = card.translation,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ListeningModeContent(
    card: SentenceCard,
    userAnswer: String,
    showAnswer: Boolean,
    onAnswerChange: (String) -> Unit,
    onPlayAudio: () -> Unit,
    onShowAnswer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Audio playback button
        FilledTonalButton(
            onClick = onPlayAudio,
            modifier = Modifier.fillMaxWidth(),
            enabled = card.hasAudio
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (card.hasAudio) "音声を再生" else "音声なし")
        }

        // User input
        if (!showAnswer) {
            OutlinedTextField(
                value = userAnswer,
                onValueChange = onAnswerChange,
                label = { Text("聞こえた文を入力") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("聞き取った文を入力してください") }
            )

            Button(
                onClick = onShowAnswer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("答え合わせ")
            }
        } else {
            // Show correct answer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "正解:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = card.sentence,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (userAnswer.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "あなたの答え:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = userAnswer,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Translation
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = card.translation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FillInBlankModeContent(
    exercise: FillInTheBlankExercise,
    userAnswer: String,
    showAnswer: Boolean,
    onAnswerChange: (String) -> Unit,
    onShowAnswer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Blanked sentence
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = exercise.blankedSentence,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp),
                letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing
            )
        }

        // Hints
        if (exercise.hints.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF9C4)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ヒント:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17)
                        )
                    }
                    exercise.hints.forEachIndexed { index, hint ->
                        Text(
                            text = "${index + 1}. $hint",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF57F17)
                        )
                    }
                }
            }
        }

        // Distractors as choice chips
        if (exercise.distractors.isNotEmpty() && !showAnswer) {
            Text(
                text = "選択肢:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add correct answers + distractors, shuffled
                val allChoices = (exercise.blanks.map { it.correctAnswer } + exercise.distractors).shuffled()

                items(allChoices) { choice ->
                    FilterChip(
                        selected = userAnswer == choice,
                        onClick = { onAnswerChange(choice) },
                        label = { Text(choice) }
                    )
                }
            }
        }

        // User input
        if (!showAnswer) {
            OutlinedTextField(
                value = userAnswer,
                onValueChange = onAnswerChange,
                label = { Text("空欄に入る言葉") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${exercise.blanks.size} つの空欄") }
            )

            Button(
                onClick = onShowAnswer,
                modifier = Modifier.fillMaxWidth(),
                enabled = userAnswer.isNotEmpty()
            ) {
                Text("答え合わせ")
            }
        } else {
            // Show complete sentence with answers highlighted
            Card(
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
                        text = "正解:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = exercise.sentence,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    HorizontalDivider()

                    Text(
                        text = "空欄の答え:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    exercise.blanks.forEachIndexed { index, blank ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            Text(
                                text = blank.correctAnswer,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            blank.hint?.let {
                                Text(
                                    text = "($it)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
private fun SpeakingModeContent(
    card: SentenceCard,
    showAnswer: Boolean,
    onRecord: () -> Unit,
    onPlayAudio: () -> Unit,
    onShowAnswer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Translation (target to speak)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "日本語で言ってください:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = card.translation,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Record button
        FilledTonalButton(
            onClick = onRecord,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("録音する")
        }

        if (!showAnswer) {
            Button(
                onClick = onShowAnswer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("答えを見る")
            }
        } else {
            // Show Japanese sentence
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "正解:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = card.sentence,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Play native audio
            if (card.hasAudio) {
                OutlinedButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("お手本を聞く")
                }
            }
        }
    }
}

@Composable
private fun GrammarPatternCard(
    pattern: String,
    explanation: String?
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE1F5FE)
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
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color(0xFF0277BD)
                )
                Text(
                    text = "文法パターン",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0277BD)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFB3E5FC)
            ) {
                Text(
                    text = pattern,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF01579B),
                    modifier = Modifier.padding(12.dp)
                )
            }

            explanation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF01579B)
                )
            }
        }
    }
}

@Composable
private fun DifficultyButtons(
    selectedDifficulty: ReviewDifficulty?,
    onSelectDifficulty: (ReviewDifficulty) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DifficultyButton(
            difficulty = ReviewDifficulty.AGAIN,
            label = "もう一度",
            color = Color(0xFFD32F2F),
            selected = selectedDifficulty == ReviewDifficulty.AGAIN,
            onClick = { onSelectDifficulty(ReviewDifficulty.AGAIN) },
            modifier = Modifier.weight(1f)
        )

        DifficultyButton(
            difficulty = ReviewDifficulty.HARD,
            label = "難しい",
            color = Color(0xFFF57C00),
            selected = selectedDifficulty == ReviewDifficulty.HARD,
            onClick = { onSelectDifficulty(ReviewDifficulty.HARD) },
            modifier = Modifier.weight(1f)
        )

        DifficultyButton(
            difficulty = ReviewDifficulty.GOOD,
            label = "良い",
            color = Color(0xFF388E3C),
            selected = selectedDifficulty == ReviewDifficulty.GOOD,
            onClick = { onSelectDifficulty(ReviewDifficulty.GOOD) },
            modifier = Modifier.weight(1f)
        )

        DifficultyButton(
            difficulty = ReviewDifficulty.EASY,
            label = "簡単",
            color = Color(0xFF1976D2),
            selected = selectedDifficulty == ReviewDifficulty.EASY,
            onClick = { onSelectDifficulty(ReviewDifficulty.EASY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DifficultyButton(
    difficulty: ReviewDifficulty,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) color else color.copy(alpha = 0.2f)
    val contentColor = if (selected) Color.White else color

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, color, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = when (difficulty) {
                    ReviewDifficulty.AGAIN -> "< 1日"
                    ReviewDifficulty.HARD -> "< 6日"
                    ReviewDifficulty.GOOD -> "次回"
                    ReviewDifficulty.EASY -> "× 2倍"
                },
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getPracticeModeLabel(mode: PracticeMode): String {
    return when (mode) {
        PracticeMode.READING -> "読解モード"
        PracticeMode.LISTENING -> "リスニングモード"
        PracticeMode.FILL_IN_BLANK -> "空欄補充モード"
        PracticeMode.SPEAKING -> "スピーキングモード"
        PracticeMode.MIXED -> "ミックスモード"
    }
}
