package com.nihongo.conversation.presentation.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVocabularyScreen(
    onBackClick: () -> Unit,
    onVocabularySaved: () -> Unit = {},
    viewModel: AddVocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message and navigate back
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("単語を追加しました！")
            viewModel.resetSavedState()
            onVocabularySaved()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("単語を追加") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clipboard suggestion
            AnimatedVisibility(visible = uiState.clipboardText != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "クリップボード: ${uiState.clipboardText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.importFromClipboard() }
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "インポート",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissClipboardSuggestion() }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "閉じる",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Instructions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "学びたい単語を追加しましょう",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "追加した単語は単語帳で復習できます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Word field (required)
            OutlinedTextField(
                value = uiState.word,
                onValueChange = { viewModel.onWordChanged(it) },
                label = { Text("単語 *") },
                supportingText = {
                    if (uiState.wordError != null) {
                        Text(uiState.wordError!!)
                    } else {
                        Text("日本語で入力してください")
                    }
                },
                isError = uiState.wordError != null,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.TextFields, contentDescription = null)
                },
                singleLine = true
            )

            // Reading field (optional)
            OutlinedTextField(
                value = uiState.reading,
                onValueChange = { viewModel.onReadingChanged(it) },
                label = { Text("読み方") },
                supportingText = { Text("ひらがな・カタカナで入力 (任意)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                },
                singleLine = true
            )

            // Meaning field (required)
            OutlinedTextField(
                value = uiState.meaning,
                onValueChange = { viewModel.onMeaningChanged(it) },
                label = { Text("意味 *") },
                supportingText = {
                    if (uiState.meaningError != null) {
                        Text(uiState.meaningError!!)
                    } else {
                        Text("韓国語で入力してください")
                    }
                },
                isError = uiState.meaningError != null,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Translate, contentDescription = null)
                },
                singleLine = true
            )

            // Example sentence field (optional)
            OutlinedTextField(
                value = uiState.exampleSentence,
                onValueChange = { viewModel.onExampleSentenceChanged(it) },
                label = { Text("例文") },
                supportingText = { Text("使い方の例 (任意)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                minLines = 2,
                maxLines = 4
            )

            // Difficulty selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "難易度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (uiState.difficulty) {
                                1 -> "⭐ とても簡単"
                                2 -> "⭐⭐ 簡単"
                                3 -> "⭐⭐⭐ 普通"
                                4 -> "⭐⭐⭐⭐ 難しい"
                                5 -> "⭐⭐⭐⭐⭐ とても難しい"
                                else -> "普通"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Slider(
                        value = uiState.difficulty.toFloat(),
                        onValueChange = { viewModel.onDifficultyChanged(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Add to review queue checkbox
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "すぐに復習に追加",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "今日から復習を開始します",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.addToReviewQueue,
                        onCheckedChange = { viewModel.onAddToReviewQueueChanged(it) }
                    )
                }
            }

            // Save button
            Button(
                onClick = { viewModel.saveVocabulary() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "単語を追加",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Clear form button
            OutlinedButton(
                onClick = { viewModel.clearForm() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("クリア")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
