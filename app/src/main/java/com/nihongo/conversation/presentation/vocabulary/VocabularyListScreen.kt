package com.nihongo.conversation.presentation.vocabulary

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.VocabularyEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyListScreen(
    onNavigateBack: () -> Unit,
    onStartFlashcard: () -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val vocabularyList by viewModel.vocabularyList.collectAsState()
    val vocabularyStats by viewModel.vocabularyStats.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("単語帳") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    // Anki Export Button
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = vocabularyList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Download, "Ankiにエクスポート")
                    }
                    // Flashcard Button
                    IconButton(onClick = onStartFlashcard) {
                        Icon(Icons.Default.Style, "フラッシュカード")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "単語を追加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Card
            vocabularyStats?.let { stats ->
                StatsCard(stats = stats)
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchVocabulary(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("検索") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.searchVocabulary("")
                        }) {
                            Icon(Icons.Default.Clear, "クリア")
                        }
                    }
                }
            )

            // Vocabulary List
            val displayList = if (searchQuery.isNotEmpty()) {
                uiState.searchResults
            } else {
                vocabularyList
            }

            if (displayList.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it.id }) { entry ->
                        VocabularyCard(
                            entry = entry,
                            onToggleMastered = { viewModel.toggleMastered(entry) },
                            onDelete = { viewModel.deleteVocabulary(entry) }
                        )
                    }
                }
            }
        }
    }

    // Show messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    // Anki Export Dialog
    if (showExportDialog) {
        AnkiExportDialog(
            vocabularyCount = vocabularyList.size,
            preview = viewModel.getExportPreview(vocabularyList),
            onConfirm = {
                val file = viewModel.exportToAnki(vocabularyList)
                if (file != null) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Ankiにエクスポート"))
                        showExportDialog = false
                    } catch (e: Exception) {
                        // Error handling is done in viewModel
                    }
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun AnkiExportDialog(
    vocabularyCount: Int,
    preview: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text("Ankiにエクスポート") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$vocabularyCount 個の単語をエクスポートします",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "プレビュー:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "※ エクスポートしたCSVファイルをAnkiで開いてインポートしてください",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("エクスポート")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun StatsCard(stats: com.nihongo.conversation.domain.model.VocabularyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "学習状況",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("総単語数", stats.totalWords.toString(), Icons.Default.Book)
                StatItem("習得済み", stats.masteredWords.toString(), Icons.Default.CheckCircle)
                StatItem("復習予定", stats.dueForReview.toString(), Icons.Default.Schedule)
            }

            LinearProgressIndicator(
                progress = {
                    if (stats.totalWords > 0) {
                        stats.masteredWords.toFloat() / stats.totalWords
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "正解率: ${(stats.accuracyRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyCard(
    entry: VocabularyEntry,
    onToggleMastered: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* TODO: Show detail */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                entry.reading?.let { reading ->
                    Text(
                        text = reading,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = entry.meaning,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Progress info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (entry.isMastered) {
                        AssistChip(
                            onClick = {},
                            label = { Text("習得済み") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                        )
                    }

                    if (entry.reviewCount > 0) {
                        Text(
                            text = "復習回数: ${entry.reviewCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "メニュー")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (entry.isMastered) "未習得にする" else "習得済みにする") },
                    onClick = {
                        onToggleMastered()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            if (entry.isMastered) Icons.Default.CheckCircleOutline else Icons.Default.CheckCircle,
                            null
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("削除") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "単語がありません",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "会話から単語を追加するか\n+ ボタンで手動追加してください",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
