package com.nihongo.conversation.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    onBackClick: () -> Unit = {},
    onConversationClick: (Long, Long) -> Unit = { _, _ -> }, // (userId, scenarioId)
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showScenarioFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "会話履歴",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${uiState.filteredConversations.size}件の会話",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("シナリオや会話内容を検索...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "クリア"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status filters
                item {
                    FilterChip(
                        selected = uiState.selectedFilter == ConversationFilter.ALL,
                        onClick = { viewModel.onFilterChange(ConversationFilter.ALL) },
                        label = { Text("すべて") },
                        leadingIcon = if (uiState.selectedFilter == ConversationFilter.ALL) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }

                item {
                    FilterChip(
                        selected = uiState.selectedFilter == ConversationFilter.ACTIVE,
                        onClick = { viewModel.onFilterChange(ConversationFilter.ACTIVE) },
                        label = { Text("進行中") },
                        leadingIcon = if (uiState.selectedFilter == ConversationFilter.ACTIVE) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }

                item {
                    FilterChip(
                        selected = uiState.selectedFilter == ConversationFilter.COMPLETED,
                        onClick = { viewModel.onFilterChange(ConversationFilter.COMPLETED) },
                        label = { Text("完了") },
                        leadingIcon = if (uiState.selectedFilter == ConversationFilter.COMPLETED) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }

                // Scenario filter
                item {
                    FilterChip(
                        selected = uiState.selectedScenarioId != null,
                        onClick = { showScenarioFilter = true },
                        label = {
                            Text(
                                if (uiState.selectedScenarioId != null) {
                                    uiState.availableScenarios.find { it.id == uiState.selectedScenarioId }?.title ?: "シナリオ"
                                } else {
                                    "シナリオ"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (uiState.selectedScenarioId != null) {
                            {
                                IconButton(
                                    onClick = { viewModel.onScenarioFilterChange(null) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "クリア",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                uiState.filteredConversations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedFilter != ConversationFilter.ALL || uiState.selectedScenarioId != null) {
                                    "条件に一致する会話が見つかりません"
                                } else {
                                    "会話履歴がありません"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.conversations.isEmpty()) {
                                Text(
                                    text = "シナリオから会話を始めましょう！",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.filteredConversations,
                            key = { it.conversation.id }
                        ) { item ->
                            ConversationHistoryCard(
                                item = item,
                                onResumeClick = {
                                    onConversationClick(item.conversation.userId, item.conversation.scenarioId)
                                },
                                onDeleteClick = {
                                    viewModel.showDeleteDialog(item.conversation)
                                },
                                formatDate = viewModel::formatDate,
                                formatDuration = viewModel::formatDuration
                            )
                        }
                    }
                }
            }
        }

        // Scenario Filter Modal
        if (showScenarioFilter) {
            AlertDialog(
                onDismissRequest = { showScenarioFilter = false },
                icon = {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = null)
                },
                title = { Text("シナリオで絞り込み") },
                text = {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text("すべてのシナリオ") },
                                modifier = Modifier.clickable {
                                    viewModel.onScenarioFilterChange(null)
                                    showScenarioFilter = false
                                },
                                leadingContent = {
                                    if (uiState.selectedScenarioId == null) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }

                        items(uiState.availableScenarios) { scenario ->
                            ListItem(
                                headlineContent = { Text(scenario.title) },
                                modifier = Modifier.clickable {
                                    viewModel.onScenarioFilterChange(scenario.id)
                                    showScenarioFilter = false
                                },
                                leadingContent = {
                                    if (uiState.selectedScenarioId == scenario.id) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showScenarioFilter = false }) {
                        Text("閉じる")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("会話を削除") },
                text = { Text("この会話を削除してもよろしいですか？この操作は元に戻せません。") },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::deleteConversation,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("削除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}

@Composable
fun ConversationHistoryCard(
    item: ConversationItem,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    formatDate: (Long) -> String,
    formatDuration: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.scenario?.title ?: "会話",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (item.conversation.isCompleted) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = if (item.conversation.isCompleted) "完了" else "進行中",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.conversation.isCompleted) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Last message preview
            if (item.lastMessage != null) {
                Text(
                    text = item.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(item.conversation.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Messages
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.messageCount}件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Duration
                if (item.duration > 60000) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(item.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resume/Continue button
                Button(
                    onClick = onResumeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (item.conversation.isCompleted) {
                            Icons.Default.Replay
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (item.conversation.isCompleted) "再開" else "続ける")
                }

                // Delete button
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
