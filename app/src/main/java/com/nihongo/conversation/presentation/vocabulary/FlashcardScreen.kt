package com.nihongo.conversation.presentation.vocabulary

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.data.repository.VocabularyRepository
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.domain.model.ReviewSessionConfig
import com.nihongo.conversation.domain.model.ReviewQuality
import com.nihongo.conversation.domain.model.VocabularyEntry
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadReviewSession()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("フラッシュカード復習") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    Text(
                        text = "${uiState.currentIndex + 1} / ${uiState.cards.size}",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.cards.isEmpty() -> {
                EmptyReviewState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onNavigateBack = onNavigateBack
                )
            }
            uiState.isComplete -> {
                ReviewCompleteState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onNavigateBack = onNavigateBack,
                    stats = uiState.sessionStats
                )
            }
            else -> {
                FlashcardContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    card = uiState.currentCard,
                    onAnswer = { quality -> viewModel.submitAnswer(quality) }
                )
            }
        }
    }
}

@Composable
fun FlashcardContent(
    modifier: Modifier = Modifier,
    card: VocabularyEntry?,
    onAnswer: (ReviewQuality) -> Unit
) {
    if (card == null) return

    var showAnswer by remember(card.id) { mutableStateOf(false) }
    var rotationY by remember(card.id) { mutableFloatStateOf(0f) }

    val rotation by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = tween(300),
        label = "card_flip"
    )

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            onClick = {
                showAnswer = !showAnswer
                rotationY = if (showAnswer) 180f else 0f
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!showAnswer || rotation < 90f) {
                    // Front side - Question
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = card.word,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        card.reading?.let { reading ->
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "タップして答えを表示",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Back side - Answer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = card.meaning,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        card.exampleSentence?.let { example ->
                            HorizontalDivider()
                            Text(
                                text = "例文",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Answer buttons (only show when answer is visible)
        if (showAnswer) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "どのくらい覚えていましたか？",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerButton(
                        text = "もう一度",
                        quality = ReviewQuality.INCORRECT,
                        color = MaterialTheme.colorScheme.error,
                        onClick = { onAnswer(ReviewQuality.INCORRECT) },
                        modifier = Modifier.weight(1f)
                    )
                    AnswerButton(
                        text = "難しい",
                        quality = ReviewQuality.DIFFICULT,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { onAnswer(ReviewQuality.DIFFICULT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerButton(
                        text = "普通",
                        quality = ReviewQuality.HESITANT,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onAnswer(ReviewQuality.HESITANT) },
                        modifier = Modifier.weight(1f)
                    )
                    AnswerButton(
                        text = "簡単",
                        quality = ReviewQuality.EASY,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onAnswer(ReviewQuality.EASY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerButton(
    text: String,
    quality: ReviewQuality,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text)
    }
}

@Composable
fun EmptyReviewState(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "復習する単語がありません",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "新しい単語を追加してください",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onNavigateBack) {
                Text("戻る")
            }
        }
    }
}

@Composable
fun ReviewCompleteState(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    stats: SessionStats
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "お疲れ様でした！",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("復習完了: ${stats.totalReviewed}単語")
                    Text("正解率: ${stats.accuracyPercent}%")
                }
            }

            Button(onClick = onNavigateBack) {
                Text("単語帳に戻る")
            }
        }
    }
}

// ============ ViewModel ============

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val profileRepository: com.nihongo.conversation.data.repository.ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    fun loadReviewSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try{
                val user = profileRepository.getCurrentUser().first() ?: return@launch
                val config = ReviewSessionConfig(
                    includeDue = true,
                    includeNew = true,
                    maxReviewWords = 20,
                    maxNewWords = 5
                )
                val cards = vocabularyRepository.getReviewSession(user.id, config)

                _uiState.update {
                    it.copy(
                        cards = cards,
                        currentIndex = 0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun submitAnswer(quality: ReviewQuality) {
        viewModelScope.launch {
            val currentCard = _uiState.value.currentCard ?: return@launch

            try {
                vocabularyRepository.submitReview(currentCard.id, quality)

                val newStats = _uiState.value.sessionStats.copy(
                    totalReviewed = _uiState.value.sessionStats.totalReviewed + 1,
                    correctCount = _uiState.value.sessionStats.correctCount +
                            if (quality.value >= 3) 1 else 0
                )

                val nextIndex = _uiState.value.currentIndex + 1
                if (nextIndex >= _uiState.value.cards.size) {
                    _uiState.update {
                        it.copy(
                            isComplete = true,
                            sessionStats = newStats
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            currentIndex = nextIndex,
                            sessionStats = newStats
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

data class FlashcardUiState(
    val cards: List<VocabularyEntry> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val sessionStats: SessionStats = SessionStats()
) {
    val currentCard: VocabularyEntry?
        get() = cards.getOrNull(currentIndex)
}

data class SessionStats(
    val totalReviewed: Int = 0,
    val correctCount: Int = 0
) {
    val accuracyPercent: Int
        get() = if (totalReviewed > 0) {
            ((correctCount.toFloat() / totalReviewed) * 100).toInt()
        } else {
            0
        }
}

private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}
