package com.nihongo.conversation.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Message

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    userId: Long,
    scenarioId: Long,
    onBackClick: () -> Unit = {},
    onReviewClick: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission handling
    var hasRecordPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }

    LaunchedEffect(Unit) {
        viewModel.initConversation(userId, scenarioId)
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show toast when new chat starts
    LaunchedEffect(uiState.showNewChatToast) {
        if (uiState.showNewChatToast) {
            snackbarHostState.showSnackbar(
                message = "新しいチャットを開始しました",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissNewChatToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.scenario?.title ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    // Show "New Chat" button only if there are messages
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "新しいチャット"
                            )
                        }
                    }
                    // Show "End Chat" button only if there are messages
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showEndChatDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "チャット終了"
                            )
                        }
                    }
                    IconButton(onClick = onReviewClick) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = "復習"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleAutoSpeak() }) {
                        Icon(
                            imageVector = if (uiState.autoSpeak) {
                                Icons.Default.VolumeUp
                            } else {
                                Icons.Default.VolumeOff
                            },
                            contentDescription = "自動音声"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Remember optimized animations
            val messageEnterTransition = ChatAnimations.rememberMessageEnterTransition()
            val messageExitTransition = ChatAnimations.rememberMessageExitTransition()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = messageEnterTransition,
                        exit = messageExitTransition
                    ) {
                        MessageBubble(
                            message = message,
                            onSpeakMessage = if (!message.isUser) {
                                { viewModel.speakMessage(message.content) }
                            } else null,
                            onLongPress = { viewModel.requestGrammarExplanation(message.content) },
                            isTranslationExpanded = message.id in uiState.expandedTranslations,
                            translation = uiState.translations[message.id],
                            translationError = uiState.translationErrors[message.id],
                            onToggleTranslation = if (!message.isUser) {
                                { viewModel.toggleMessageTranslation(message.id) }
                            } else null,
                            onRequestTranslation = if (!message.isUser) {
                                { viewModel.requestTranslation(message.id, message.content) }
                            } else null,
                            onRetryTranslation = if (!message.isUser) {
                                { viewModel.retryTranslation(message.id, message.content) }
                            } else null,
                            onPracticePronunciation = if (!message.isUser) {
                                { viewModel.startPronunciationPractice(message.content) }
                            } else null
                        )
                    }
                }

                if (uiState.isLoading) {
                    item(key = "typing_indicator") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.widthIn(max = 100.dp)
                            ) {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }

            // Voice state indicator
            AnimatedVisibility(
                visible = voiceState !is com.nihongo.conversation.core.voice.VoiceState.Idle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                VoiceStateIndicator(
                    voiceState = voiceState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Error display
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                uiState.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            MessageInput(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                enabled = !uiState.isLoading,
                voiceState = voiceState,
                onStartRecording = {
                    if (hasRecordPermission) {
                        viewModel.startVoiceRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = viewModel::stopVoiceRecording,
                onRequestHint = viewModel::requestHints
            )
        }

        // Hint Dialog
        if (uiState.showHintDialog) {
            HintDialog(
                hints = uiState.hints,
                isLoading = uiState.isLoadingHints,
                onDismiss = viewModel::dismissHintDialog,
                onSpeakHint = viewModel::speakMessage,
                onUseHint = viewModel::useHint
            )
        }

        // Grammar Bottom Sheet
        if (uiState.showGrammarSheet) {
            GrammarBottomSheet(
                grammarExplanation = uiState.grammarExplanation,
                isLoading = uiState.isLoadingGrammar,
                errorMessage = uiState.grammarError,
                originalSentence = uiState.currentGrammarSentence,
                onDismiss = viewModel::dismissGrammarSheet,
                onRetry = viewModel::retryGrammarAnalysis
            )
        }

        // End Chat Confirmation Dialog
        if (uiState.showEndChatDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = viewModel::dismissEndChatDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null
                    )
                },
                title = {
                    Text("채팅을 종료하시겠습니까?")
                },
                text = {
                    Text("현재 대화를 기록에 저장하고 새로운 채팅을 시작합니다.")
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmEndChat) {
                        Text("종료")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissEndChatDialog) {
                        Text("취소")
                    }
                }
            )
        }

        // Pronunciation Practice Sheet
        uiState.pronunciationTargetText?.let { targetText ->
            if (uiState.showPronunciationSheet) {
                PronunciationPracticeSheet(
                    targetText = targetText,
                    result = uiState.pronunciationResult,
                    isRecording = uiState.isPronunciationRecording,
                    onStartRecording = viewModel::startPronunciationRecording,
                    onStopRecording = viewModel::stopPronunciationRecording,
                    onRetry = viewModel::retryPronunciation,
                    onSpeak = viewModel::speakMessage,
                    onDismiss = viewModel::dismissPronunciationSheet
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onSpeakMessage: (() -> Unit)? = null,
    onLongPress: () -> Unit = {},
    isTranslationExpanded: Boolean = false,
    translation: String? = null,
    translationError: String? = null,
    onToggleTranslation: (() -> Unit)? = null,
    onRequestTranslation: (() -> Unit)? = null,
    onRetryTranslation: (() -> Unit)? = null,
    onPracticePronunciation: (() -> Unit)? = null
) {
    val timeFormatter = remember {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = { onSpeakMessage?.invoke() },
                    onLongClick = onLongPress
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )

                // Show translation button and translation for AI messages
                if (!message.isUser && onToggleTranslation != null && onRequestTranslation != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Translation toggle button
                    TextButton(
                        onClick = onToggleTranslation,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "번역",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTranslationExpanded) "번역 숨기기" else "한국어 번역",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Show translation when expanded
                    if (isTranslationExpanded) {
                        when {
                            translation != null -> {
                                // Success - show translation
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = translation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            translationError != null && onRetryTranslation != null -> {
                                // Error - show error message and retry button
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = translationError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    TextButton(
                                        onClick = onRetryTranslation,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "다시 시도",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "다시 시도",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            else -> {
                                // Loading - request translation
                                LaunchedEffect(message.id) {
                                    onRequestTranslation?.invoke()
                                }
                                Text(
                                    text = "번역 중...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Pronunciation practice button
                    if (onPracticePronunciation != null) {
                        TextButton(
                            onClick = onPracticePronunciation,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "発音練習",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "発音練習",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Show difficulty indicator for AI messages
                if (!message.isUser && message.complexityScore > 0) {
                    CompactDifficultyIndicator(
                        complexityScore = message.complexityScore
                    )
                }

                Text(
                    text = timeFormatter.format(java.util.Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    voiceState: com.nihongo.conversation.core.voice.VoiceState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onRequestHint: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Voice button
            VoiceButton(
                voiceState = voiceState,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording
            )

            // Text input
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지를 입력하세요...") },
                enabled = enabled,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSend()
                        }
                    }
                )
            )

            // Send button
            Button(
                onClick = onSend,
                enabled = enabled && text.isNotBlank()
            ) {
                Text("전송")
            }
        }

        // Hint button
        TextButton(
            onClick = onRequestHint,
            modifier = Modifier.padding(horizontal = 8.dp),
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("힌트 요청 (Korean-Japanese)")
        }
    }
}
