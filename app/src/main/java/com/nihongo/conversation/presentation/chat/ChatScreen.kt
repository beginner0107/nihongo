package com.nihongo.conversation.presentation.chat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.R

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
    val context = LocalContext.current
    var hasRecordPermission by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted

        if (!isGranted) {
            // Check if permission was permanently denied
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.RECORD_AUDIO
            ) ?: false

            isPermanentlyDenied = !shouldShowRationale && activity != null
            showPermissionDeniedDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initConversation(userId, scenarioId)

        // Check if permission is already granted
        hasRecordPermission = context.checkSelfPermission(
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Request permission if not granted
        if (!hasRecordPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Smart auto-scroll: only scroll if user is near bottom
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val lastItemIndex = uiState.messages.size - 1

            // Auto-scroll only if user is within 2 items of the bottom
            val isNearBottom = lastItemIndex - lastVisibleIndex <= 2

            if (isNearBottom) {
                listState.animateScrollToItem(lastItemIndex)
            }
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
                title = { Text(uiState.scenario?.title ?: stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Show "New Chat" button only if there are messages
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.new_chat)
                            )
                        }
                    }
                    // Show "End Chat" button only if there are messages
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showEndChatDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = stringResource(R.string.end_chat)
                            )
                        }
                    }
                    IconButton(onClick = onReviewClick) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = stringResource(R.string.review)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleAutoSpeak() }) {
                        Icon(
                            imageVector = if (uiState.autoSpeak) {
                                Icons.Default.VolumeUp
                            } else {
                                Icons.Default.VolumeOff
                            },
                            contentDescription = stringResource(R.string.auto_speak)
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
                    // No AnimatedVisibility wrapper - messages are stable content
                    // LazyColumn handles item animations internally via animateItemPlacement
                    MessageBubble(
                        message = message,
                        // Enable TTS for both user and AI messages
                        onSpeakMessage = { viewModel.speakMessage(message.content) },
                        onSpeakSlowly = if (!message.isUser) {
                            { viewModel.speakMessageSlowly(message.content) }
                        } else null,  // Slow speech only for AI messages
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
                    Text(stringResource(R.string.end_chat_title))
                },
                text = {
                    Text(stringResource(R.string.end_chat_message))
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmEndChat) {
                        Text(stringResource(R.string.end_chat_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissEndChatDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Permission Denied Dialog
        if (showPermissionDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDeniedDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null
                    )
                },
                title = {
                    Text(
                        if (isPermanentlyDenied)
                            stringResource(R.string.mic_permission_needed)
                        else
                            stringResource(R.string.mic_permission_about)
                    )
                },
                text = {
                    Text(
                        if (isPermanentlyDenied) {
                            stringResource(R.string.mic_permission_permanently_denied)
                        } else {
                            stringResource(R.string.mic_permission_rationale)
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isPermanentlyDenied) {
                                // Open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } else {
                                // Try requesting permission again
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            showPermissionDeniedDialog = false
                        }
                    ) {
                        Text(
                            if (isPermanentlyDenied)
                                stringResource(R.string.mic_permission_open_settings)
                            else
                                stringResource(R.string.mic_permission_grant)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDeniedDialog = false }) {
                        Text(stringResource(R.string.later))
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

        // Korean→Japanese Translation Dialog
        uiState.koreanToJapaneseResult?.let { result ->
            if (uiState.showKoreanToJapaneseDialog) {
                KoreanToJapaneseDialog(
                    result = result,
                    onDismiss = viewModel::dismissKorToJpnDialog,
                    onUseJapanese = {
                        viewModel.sendJapaneseMessage(result.japanese)
                        viewModel.dismissKorToJpnDialog()
                    },
                    onSpeak = {
                        viewModel.speakMessage(result.japanese)
                    }
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
    onSpeakSlowly: (() -> Unit)? = null,
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box {
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
                        onLongClick = { showContextMenu = true }
                    )
            ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Display content with furigana for AI messages only
                val displayText = remember(message.content, message.isUser) {
                    if (!message.isUser) {
                        // Add furigana to AI messages: "注文(ちゅうもん)"
                        com.nihongo.conversation.core.grammar.KuromojiGrammarAnalyzer.addFuriganaToKanji(message.content)
                    } else {
                        // User messages shown as-is
                        message.content
                    }
                }

                Text(
                    text = displayText,
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
                                    text = stringResource(R.string.translation_loading),
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
                                contentDescription = stringResource(R.string.pronunciation_title),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.pronunciation_title),
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

        // Context Menu
        androidx.compose.material3.DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            // 복사 (Always available)
            androidx.compose.material3.DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.copy)) },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.ContentCopy,
                        contentDescription = null
                    )
                },
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.content))
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.copy_success),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    showContextMenu = false
                }
            )

            // 읽기 (If TTS available)
            if (onSpeakMessage != null) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { androidx.compose.material3.Text(stringResource(R.string.read_aloud)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.VolumeUp,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onSpeakMessage()
                        showContextMenu = false
                    }
                )

                // 천천히 읽기 (Read slowly - NEW)
                if (onSpeakSlowly != null) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.read_slowly)) },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.Speed,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onSpeakSlowly()
                            showContextMenu = false
                        }
                    )
                }
            }

            // 문법 분석 (If AI message)
            if (!message.isUser) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { androidx.compose.material3.Text(stringResource(R.string.grammar_analysis)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.MenuBook,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onLongPress()
                        showContextMenu = false
                    }
                )

                // 단어장에 추가 (Add to vocabulary - NEW)
                androidx.compose.material3.DropdownMenuItem(
                    text = { androidx.compose.material3.Text(stringResource(R.string.add_to_vocabulary)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.BookmarkAdd,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        // TODO: Implement add to vocabulary
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.added_to_vocabulary),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showContextMenu = false
                    }
                )
            }

            // 번역 토글 (If AI message and translation available)
            if (!message.isUser && onToggleTranslation != null) {
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        androidx.compose.material3.Text(
                            if (isTranslationExpanded)
                                stringResource(R.string.hide_translation)
                            else
                                stringResource(R.string.show_translation)
                        )
                    },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.Translate,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onToggleTranslation()
                        showContextMenu = false
                    }
                )
            }
        }
    }  // Close Box
    }  // Close Column
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
                placeholder = { Text("일본어 또는 한국어로 입력하세요 💬") },
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

/**
 * Korean→Japanese Translation Dialog
 * Shows translation result with pronunciation guide and TTS playback
 */
@Composable
fun KoreanToJapaneseDialog(
    result: KoreanToJapaneseResult,
    onDismiss: () -> Unit,
    onUseJapanese: () -> Unit,
    onSpeak: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("한국어 → 일본어 변환") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Original Korean text
                Text(
                    text = "한국어: ${result.korean}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Japanese + Pronunciation (clickable for TTS)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpeak() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Japanese text
                        Text(
                            text = result.japanese,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Pronunciation guide
                        Text(
                            text = result.romanization,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // TTS hint
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "음성 듣기",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "클릭하면 소리가 재생됩니다",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUseJapanese) {
                Text("이 문장 보내기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
