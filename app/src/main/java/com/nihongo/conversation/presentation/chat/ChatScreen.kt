package com.nihongo.conversation.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: Long,
    scenarioId: Long,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val listState = rememberLazyListState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.scenario?.title ?: "Chat") },
                actions = {
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        onSpeakMessage = if (!message.isUser) {
                            { viewModel.speakMessage(message.content) }
                        } else null
                    )
                }

                if (uiState.isLoading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                }
            }

            // Voice state indicator
            VoiceStateIndicator(
                voiceState = voiceState,
                modifier = Modifier.padding(8.dp)
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
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
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onSpeakMessage: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onSpeakMessage != null) {
                        Modifier.clickable { onSpeakMessage() }
                    } else Modifier
                )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
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
                placeholder = { Text("メッセージを入力...") },
                enabled = enabled,
                maxLines = 4
            )

            // Send button
            Button(
                onClick = onSend,
                enabled = enabled && text.isNotBlank()
            ) {
                Text("送信")
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
