package com.nihongo.conversation.presentation.chat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Voice language selection
    var showLanguageSheet by remember { mutableStateOf(false) }

    // Edit message dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMessageId by remember { mutableLongStateOf(0L) }
    var editingMessageText by remember { mutableStateOf("") }

    // Delete message confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingMessageId by remember { mutableLongStateOf(0L) }

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
                title = {
                    Column {
                        // First line: Emoji + Title + Favorite star
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${uiState.scenario?.thumbnailEmoji ?: "💬"} ${uiState.scenario?.title ?: stringResource(R.string.chat)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.isFavoriteScenario) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "즐겨찾기",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFFD700) // Gold color
                                )
                            }
                        }

                        // Second line: Category · Difficulty
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.scenarioCategory?.let { category ->
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.scenarioCategory != null && uiState.scenarioDifficulty != null) {
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            uiState.scenarioDifficulty?.let { difficulty ->
                                Surface(
                                    color = when (difficulty) {
                                        "초급" -> MaterialTheme.colorScheme.primaryContainer
                                        "중급" -> MaterialTheme.colorScheme.tertiaryContainer
                                        "고급" -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = difficulty,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = when (difficulty) {
                                            "초급" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            "중급" -> MaterialTheme.colorScheme.onTertiaryContainer
                                            "고급" -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
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
                        } else null,
                        onEditMessage = if (message.isUser) {
                            {
                                editingMessageId = message.id
                                editingMessageText = message.content
                                showEditDialog = true
                            }
                        } else null,
                        onDeleteMessage = if (message.isUser) {
                            {
                                deletingMessageId = message.id
                                showDeleteDialog = true
                            }
                        } else null,
                        onAddToVocabulary = if (!message.isUser) {
                            { viewModel.addToVocabulary(message.id) }
                        } else null,
                        onToggleFurigana = if (!message.isUser) {
                            { viewModel.toggleMessageFurigana(message.id) }
                        } else null,
                        showFurigana = message.id in uiState.messagesWithFurigana,
                        furiganaType = uiState.furiganaType,

                        // Phase 5: Message bookmarking & sharing
                        isMessageSaved = uiState.savedMessages.contains(message.id),
                        onToggleBookmark = {
                            if (uiState.savedMessages.contains(message.id)) {
                                viewModel.unsaveMessage(message.id)
                            } else {
                                viewModel.saveMessage(message.id)
                            }
                        },
                        onShareMessage = {
                            val shareText = viewModel.getShareText(message)
                            val intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(intent, "共有")
                            )
                        },

                        // User message features
                        isUserTranslationExpanded = message.id in uiState.expandedUserTranslations,
                        userTranslation = uiState.userTranslations[message.id],
                        userTranslationError = uiState.userTranslationErrors[message.id],
                        onToggleUserTranslation = if (message.isUser) {
                            { viewModel.toggleUserTranslation(message.id) }
                        } else null,
                        onRequestUserTranslation = if (message.isUser) {
                            { viewModel.requestUserTranslation(message.id, message.content) }
                        } else null,
                        onRetryUserTranslation = if (message.isUser) {
                            { viewModel.retryUserTranslation(message.id, message.content) }
                        } else null,

                        showUserFurigana = message.id in uiState.userMessagesWithFurigana,
                        onToggleUserFurigana = if (message.isUser) {
                            { viewModel.toggleUserMessageFurigana(message.id) }
                        } else null,

                        isUserGrammarExpanded = message.id in uiState.expandedUserGrammarFeedback,
                        userGrammarFeedback = uiState.userGrammarFeedback[message.id]?.items,
                        isUserGrammarAnalyzing = message.id in uiState.userGrammarAnalyzing,
                        userGrammarError = uiState.userGrammarErrors[message.id],
                        onToggleUserGrammarFeedback = if (message.isUser) {
                            { viewModel.toggleUserGrammarFeedback(message.id) }
                        } else null,
                        onRequestUserGrammarFeedback = if (message.isUser) {
                            { viewModel.requestUserGrammarFeedback(message.id, message.content) }
                        } else null,
                        onRetryUserGrammarFeedback = if (message.isUser) {
                            { viewModel.retryUserGrammarAnalysis(message.id, message.content) }
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
                onRequestHint = viewModel::requestHints,
                selectedVoiceLanguage = uiState.selectedVoiceLanguage,
                onShowLanguageSheet = { showLanguageSheet = true }
            )
        }

        // Voice language selection bottom sheet
        if (showLanguageSheet) {
            VoiceLanguageBottomSheet(
                selectedLanguage = uiState.selectedVoiceLanguage,
                onSelectLanguage = viewModel::setVoiceLanguage,
                onDismiss = { showLanguageSheet = false }
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

        // Edit Message Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                },
                title = { Text("메시지 편집") },
                text = {
                    OutlinedTextField(
                        value = editingMessageText,
                        onValueChange = { editingMessageText = it },
                        label = { Text("메시지") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editingMessageText.isNotBlank()) {
                                viewModel.updateMessage(editingMessageId, editingMessageText)
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }

        // Delete Message Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("메시지 삭제") },
                text = { Text("이 메시지를 삭제하시겠습니까? 이 작업은 취소할 수 없습니다.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMessage(deletingMessageId)
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
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
    onPracticePronunciation: (() -> Unit)? = null,
    onEditMessage: (() -> Unit)? = null,
    onDeleteMessage: (() -> Unit)? = null,
    onAddToVocabulary: (() -> Unit)? = null,
    onToggleFurigana: (() -> Unit)? = null,
    showFurigana: Boolean = false,
    furiganaType: com.nihongo.conversation.domain.model.FuriganaType = com.nihongo.conversation.domain.model.FuriganaType.HIRAGANA,

    // Phase 5: Message bookmarking & sharing
    isMessageSaved: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
    onShareMessage: (() -> Unit)? = null,

    // User message features
    isUserTranslationExpanded: Boolean = false,
    userTranslation: String? = null,
    userTranslationError: String? = null,
    onToggleUserTranslation: (() -> Unit)? = null,
    onRequestUserTranslation: (() -> Unit)? = null,
    onRetryUserTranslation: (() -> Unit)? = null,

    showUserFurigana: Boolean = false,
    onToggleUserFurigana: (() -> Unit)? = null,

    isUserGrammarExpanded: Boolean = false,
    userGrammarFeedback: List<com.nihongo.conversation.domain.model.GrammarFeedback>? = null,
    isUserGrammarAnalyzing: Boolean = false,
    userGrammarError: String? = null,
    onToggleUserGrammarFeedback: (() -> Unit)? = null,
    onRequestUserGrammarFeedback: (() -> Unit)? = null,
    onRetryUserGrammarFeedback: (() -> Unit)? = null
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Display content with optional furigana for AI and user messages
                val displayText = remember(message.content, message.isUser, showFurigana, showUserFurigana, furiganaType) {
                    when {
                        message.isUser && showUserFurigana -> {
                            // User message with furigana enabled
                            com.nihongo.conversation.core.grammar.KuromojiGrammarAnalyzer.addFuriganaToKanji(
                                message.content,
                                furiganaType
                            )
                        }
                        !message.isUser && showFurigana -> {
                            // AI message with furigana enabled
                            com.nihongo.conversation.core.grammar.KuromojiGrammarAnalyzer.addFuriganaToKanji(
                                message.content,
                                furiganaType
                            )
                        }
                        else -> {
                            // Furigana disabled: show as-is
                            message.content
                        }
                    }
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp
                    ),
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )

                // Show translation button and translation for AI messages
                if (!message.isUser && onToggleTranslation != null && onRequestTranslation != null) {
                    // Button row: Translation + Furigana toggle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Translation toggle button
                        TextButton(
                            onClick = onToggleTranslation,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "번역",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTranslationExpanded) "번역 숨기기" else "한국어 번역",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Furigana toggle button
                        TextButton(
                            onClick = { onToggleFurigana?.invoke() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (showFurigana) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        ) {
                            Text(
                                text = if (showFurigana) "漢\nあ" else "漢",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (showFurigana) 9.sp else 14.sp,
                                lineHeight = if (showFurigana) 10.sp else 14.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (showFurigana) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showFurigana) "후리가나 끄기" else "후리가나 켜기",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
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
                                    style = MaterialTheme.typography.bodyMedium,
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

                // User message features (translation, furigana, grammar feedback)
                if (message.isUser && onToggleUserTranslation != null && onRequestUserTranslation != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Translation button
                        TextButton(
                            onClick = onToggleUserTranslation,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "번역",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUserTranslationExpanded) "번역 숨기기" else "한국어 번역",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Furigana button
                        if (onToggleUserFurigana != null) {
                            TextButton(
                                onClick = onToggleUserFurigana,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (showUserFurigana) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            ) {
                                Text(
                                    text = if (showUserFurigana) "漢\nあ" else "漢",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = if (showUserFurigana) 9.sp else 14.sp,
                                    lineHeight = if (showUserFurigana) 10.sp else 14.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (showUserFurigana) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showUserFurigana) "후리가나 끄기" else "후리가나 켜기",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        // Grammar feedback button
                        if (onToggleUserGrammarFeedback != null) {
                            TextButton(
                                onClick = onToggleUserGrammarFeedback,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isUserGrammarExpanded) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Spellcheck,
                                    contentDescription = "문법 확인",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isUserGrammarExpanded) "피드백 숨기기" else "문법 확인",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Show translation when expanded
                    if (isUserTranslationExpanded) {
                        when {
                            userTranslation != null -> {
                                // Success - show translation
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = userTranslation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            userTranslationError != null && onRetryUserTranslation != null -> {
                                // Error - show error message and retry button
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
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
                                            text = userTranslationError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    TextButton(
                                        onClick = onRetryUserTranslation,
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
                                    onRequestUserTranslation?.invoke()
                                }
                                Text(
                                    text = "번역 중...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Show grammar feedback when expanded
                    if (isUserGrammarExpanded) {
                        when {
                            userGrammarFeedback != null && userGrammarFeedback.isNotEmpty() -> {
                                // Has feedback - show feedback cards
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    userGrammarFeedback.forEach { feedback ->
                                        GrammarFeedbackCard(feedback = feedback)
                                    }
                                }
                            }
                            userGrammarFeedback != null && userGrammarFeedback.isEmpty() -> {
                                // No feedback - perfect!
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50), // Green
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "문법상 문제 없습니다!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            userGrammarError != null && onRetryUserGrammarFeedback != null -> {
                                // Error - show error and retry
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
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
                                            text = userGrammarError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    TextButton(
                                        onClick = onRetryUserGrammarFeedback,
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
                            isUserGrammarAnalyzing -> {
                                // Analyzing - show loading
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "문법 분석 중...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            else -> {
                                // Not loaded yet - request analysis
                                LaunchedEffect(message.id) {
                                    onRequestUserGrammarFeedback?.invoke()
                                }
                            }
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
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

            // 사전에서 검색 (Search in Jisho.org)
            androidx.compose.material3.DropdownMenuItem(
                text = { androidx.compose.material3.Text("사전에서 검색") },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = null
                    )
                },
                onClick = {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://jisho.org/search/${android.net.Uri.encode(message.content)}")
                        )
                        context.startActivity(intent)
                        showContextMenu = false
                    } catch (e: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(
                            context,
                            "브라우저 앱을 찾을 수 없습니다",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showContextMenu = false
                    }
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

                // 単語帳に追加 (Add to vocabulary)
                if (onAddToVocabulary != null) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.add_to_vocabulary)) },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.BookmarkAdd,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onAddToVocabulary()
                            showContextMenu = false
                        }
                    )
                }
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

            // Phase 5: Bookmark and Share section
            if (onToggleBookmark != null || onShareMessage != null) {
                androidx.compose.material3.HorizontalDivider()

                // 즐겨찾기 토글 (Bookmark toggle)
                if (onToggleBookmark != null) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            androidx.compose.material3.Text(
                                if (isMessageSaved) "保存を解除" else "保存する"
                            )
                        },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = if (isMessageSaved)
                                    androidx.compose.material.icons.Icons.Default.BookmarkRemove
                                else
                                    androidx.compose.material.icons.Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = if (isMessageSaved)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onToggleBookmark()
                            showContextMenu = false
                        }
                    )
                }

                // 공유 (Share)
                if (onShareMessage != null) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text("共有") },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.Share,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onShareMessage()
                            showContextMenu = false
                        }
                    )
                }
            }

            // 편집 (Only for user messages)
            if (message.isUser && onEditMessage != null) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { androidx.compose.material3.Text("편집") },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onEditMessage()
                        showContextMenu = false
                    }
                )
            }

            // 삭제 (Only for user messages)
            if (message.isUser && onDeleteMessage != null) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { androidx.compose.material3.Text("삭제") },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        onDeleteMessage()
                        showContextMenu = false
                    },
                    colors = androidx.compose.material3.MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error
                    )
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
    onRequestHint: () -> Unit = {},
    selectedVoiceLanguage: com.nihongo.conversation.core.voice.VoiceLanguage = com.nihongo.conversation.core.voice.VoiceLanguage.JAPANESE,
    onShowLanguageSheet: () -> Unit = {}
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
            // Language selection button (independent)
            IconButton(
                onClick = onShowLanguageSheet,
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = when (selectedVoiceLanguage) {
                        com.nihongo.conversation.core.voice.VoiceLanguage.JAPANESE -> "🇯🇵"
                        com.nihongo.conversation.core.voice.VoiceLanguage.KOREAN -> "🇰🇷"
                    },
                    fontSize = 24.sp
                )
            }

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

            // Hint button
            IconButton(
                onClick = onRequestHint,
                enabled = enabled,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "힌트",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Voice button (simplified - no badge)
            VoiceInputButton(
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                voiceState = voiceState
            )

            // Send button
            Button(
                onClick = onSend,
                enabled = enabled && text.isNotBlank()
            ) {
                Text("전송")
            }
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

/**
 * Grammar Feedback Card - displays individual grammar feedback item
 */
@Composable
fun GrammarFeedbackCard(feedback: com.nihongo.conversation.domain.model.GrammarFeedback) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when (feedback.severity) {
            com.nihongo.conversation.domain.model.FeedbackSeverity.ERROR ->
                MaterialTheme.colorScheme.errorContainer
            com.nihongo.conversation.domain.model.FeedbackSeverity.WARNING ->
                MaterialTheme.colorScheme.tertiaryContainer
            com.nihongo.conversation.domain.model.FeedbackSeverity.INFO ->
                MaterialTheme.colorScheme.primaryContainer
        },
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Title: Feedback type + Severity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (feedback.severity) {
                        com.nihongo.conversation.domain.model.FeedbackSeverity.ERROR ->
                            Icons.Default.Error
                        com.nihongo.conversation.domain.model.FeedbackSeverity.WARNING ->
                            Icons.Default.Warning
                        com.nihongo.conversation.domain.model.FeedbackSeverity.INFO ->
                            Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (feedback.severity) {
                        com.nihongo.conversation.domain.model.FeedbackSeverity.ERROR ->
                            MaterialTheme.colorScheme.error
                        com.nihongo.conversation.domain.model.FeedbackSeverity.WARNING ->
                            MaterialTheme.colorScheme.tertiary
                        com.nihongo.conversation.domain.model.FeedbackSeverity.INFO ->
                            MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = when (feedback.feedbackType) {
                        com.nihongo.conversation.domain.model.FeedbackType.GRAMMAR_ERROR -> "문법 오류"
                        com.nihongo.conversation.domain.model.FeedbackType.UNNATURAL -> "부자연스러움"
                        com.nihongo.conversation.domain.model.FeedbackType.BETTER_EXPRESSION -> "더 나은 표현"
                        com.nihongo.conversation.domain.model.FeedbackType.CONVERSATION_FLOW -> "대화 흐름"
                        com.nihongo.conversation.domain.model.FeedbackType.POLITENESS_LEVEL -> "존댓말 레벨"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Explanation
            Text(
                text = feedback.explanation,
                style = MaterialTheme.typography.bodySmall
            )

            // Corrected text suggestion
            if (feedback.correctedText != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = when (feedback.severity) {
                        com.nihongo.conversation.domain.model.FeedbackSeverity.ERROR ->
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                        com.nihongo.conversation.domain.model.FeedbackSeverity.WARNING ->
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                        com.nihongo.conversation.domain.model.FeedbackSeverity.INFO ->
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = feedback.correctedText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Better expression
            if (feedback.betterExpression != null) {
                Text(
                    text = "💡 ${feedback.betterExpression}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Voice Input Button (Simplified)
 * Simple mic button without language badge
 */
@Composable
fun VoiceInputButton(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    voiceState: com.nihongo.conversation.core.voice.VoiceState
) {
    Surface(
        modifier = Modifier.size(48.dp),
        onClick = {
            when (voiceState) {
                is com.nihongo.conversation.core.voice.VoiceState.Listening -> onStopRecording()
                else -> onStartRecording()
            }
        },
        shape = CircleShape,
        color = when (voiceState) {
            is com.nihongo.conversation.core.voice.VoiceState.Listening -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = when (voiceState) {
                    is com.nihongo.conversation.core.voice.VoiceState.Idle -> Icons.Default.Mic
                    is com.nihongo.conversation.core.voice.VoiceState.Listening -> Icons.Default.MicOff
                    is com.nihongo.conversation.core.voice.VoiceState.Processing -> Icons.Default.Sync
                    else -> Icons.Default.Mic
                },
                contentDescription = "음성 입력",
                tint = Color.White,
                modifier = when (voiceState) {
                    is com.nihongo.conversation.core.voice.VoiceState.Processing ->
                        Modifier.size(24.dp).rotate(infiniteAnimation())
                    else -> Modifier.size(24.dp)
                }
            )
        }
    }
}

@Composable
private fun infiniteAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    return rotation
}

/**
 * Voice Language Selection Bottom Sheet
 * Allows users to switch between Japanese and Korean voice input
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLanguageBottomSheet(
    selectedLanguage: com.nihongo.conversation.core.voice.VoiceLanguage,
    onSelectLanguage: (com.nihongo.conversation.core.voice.VoiceLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "음성 입력 언어 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Japanese option
            ListItem(
                headlineContent = {
                    Text("日本語 (일본어)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                supportingContent = {
                    Text("일본어로 직접 음성 입력", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = {
                    Text("🇯🇵", fontSize = 40.sp)
                },
                trailingContent = {
                    if (selectedLanguage == com.nihongo.conversation.core.voice.VoiceLanguage.JAPANESE) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "선택됨",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                modifier = Modifier
                    .clickable {
                        onSelectLanguage(com.nihongo.conversation.core.voice.VoiceLanguage.JAPANESE)
                        onDismiss()
                    }
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Korean option
            ListItem(
                headlineContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("한국어 (Korean)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "자동번역",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                supportingContent = {
                    Text(
                        "한국어 → 일본어로 자동 번역",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Text("🇰🇷", fontSize = 40.sp)
                },
                trailingContent = {
                    if (selectedLanguage == com.nihongo.conversation.core.voice.VoiceLanguage.KOREAN) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "선택됨",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                modifier = Modifier
                    .clickable {
                        onSelectLanguage(com.nihongo.conversation.core.voice.VoiceLanguage.KOREAN)
                        onDismiss()
                    }
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Guidance text
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            "언어를 변경하면 음성 입력 언어가 바뀝니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "한국어 선택 시 자동으로 일본어로 번역됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
