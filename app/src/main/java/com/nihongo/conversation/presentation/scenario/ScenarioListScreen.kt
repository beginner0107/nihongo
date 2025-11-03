package com.nihongo.conversation.presentation.scenario

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.domain.model.Scenario

// 카테고리 정의 (주요 탭만 표시)
sealed class ScenarioCategory(val id: String?, val label: String, val icon: String) {
    object All : ScenarioCategory(null, "전체", "📚")
    object Favorite : ScenarioCategory("FAVORITE", "즐겨찾기", "⭐")
    object Entertainment : ScenarioCategory("ENTERTAINMENT", "엔터", "🎵")
    object Work : ScenarioCategory("WORK", "직장", "💼")
    object DailyLife : ScenarioCategory("DAILY_LIFE", "일상", "🏠")
    object Travel : ScenarioCategory("TRAVEL", "여행", "✈️")
    object Tech : ScenarioCategory("TECH", "기술", "💻")
    object Esports : ScenarioCategory("ESPORTS", "게임", "🎮")
    object JLPT : ScenarioCategory("JLPT_PRACTICE", "JLPT", "📖")
    object Other : ScenarioCategory("OTHER", "기타", "🎭")
}

// 섹션 헤더용 카테고리 매핑 (16개 전체 카테고리)
fun getCategoryLabel(category: String): String {
    return when (category) {
        "DAILY_LIFE" -> "🏠 일상 생활"
        "WORK" -> "💼 직장/업무"
        "TRAVEL" -> "✈️ 여행"
        "ENTERTAINMENT" -> "🎵 엔터테인먼트"
        "ESPORTS" -> "🎮 e스포츠"
        "TECH" -> "💻 기술/개발"
        "FINANCE" -> "💰 금융/재테크"
        "CULTURE" -> "🎭 문화"
        "HOUSING" -> "🏢 부동산/주거"
        "HEALTH" -> "🏥 건강/의료"
        "STUDY" -> "📚 학습/교육"
        "DAILY_CONVERSATION" -> "💬 일상 회화"
        "JLPT_PRACTICE" -> "📖 JLPT 연습"
        "BUSINESS" -> "🤝 비즈니스"
        "ROMANCE" -> "💕 연애/관계"
        "EMERGENCY" -> "🚨 긴급 상황"
        else -> "📚 기타"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScenarioListScreen(
    onScenarioSelected: (Long) -> Unit,
    onFlashcardClick: () -> Unit = {},
    onAddVocabularyClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCreateScenarioClick: () -> Unit = {},
    viewModel: ScenarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "시나리오 선택",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "학습할 상황을 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "프로필"
                        )
                    }
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "통계"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Add custom vocabulary FAB
                SmallFloatingActionButton(
                    onClick = onAddVocabularyClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "단어 추가"
                    )
                }

                // Create custom scenario FAB
                SmallFloatingActionButton(
                    onClick = onCreateScenarioClick,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "시나리오 만들기"
                    )
                }

                // Flashcard review FAB
                ExtendedFloatingActionButton(
                    onClick = onFlashcardClick,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "단어장"
                        )
                    },
                    text = { Text("단어장") }
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 검색창
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("🔍 시나리오 검색...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색"
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "지우기")
                            }
                        }
                    },
                    singleLine = true
                )

                // 필터 칩 (검색어나 난이도 필터가 선택된 경우에만 표시)
                if (uiState.searchQuery.isNotEmpty() || uiState.selectedDifficulties.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "필터:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilterChip(
                            selected = 1 in uiState.selectedDifficulties,
                            onClick = { viewModel.toggleDifficulty(1) },
                            label = { Text("초급") },
                            leadingIcon = {
                                if (1 in uiState.selectedDifficulties) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        FilterChip(
                            selected = 2 in uiState.selectedDifficulties,
                            onClick = { viewModel.toggleDifficulty(2) },
                            label = { Text("중급") },
                            leadingIcon = {
                                if (2 in uiState.selectedDifficulties) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        FilterChip(
                            selected = 3 in uiState.selectedDifficulties,
                            onClick = { viewModel.toggleDifficulty(3) },
                            label = { Text("고급") },
                            leadingIcon = {
                                if (3 in uiState.selectedDifficulties) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Clear all filters button
                        if (uiState.searchQuery.isNotEmpty() || uiState.selectedDifficulties.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("초기화", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 탭 Row (9개 주요 카테고리)
                val categories = listOf(
                    ScenarioCategory.All,
                    ScenarioCategory.Favorite,
                    ScenarioCategory.Entertainment,
                    ScenarioCategory.Work,
                    ScenarioCategory.DailyLife,
                    ScenarioCategory.Travel,
                    ScenarioCategory.Tech,
                    ScenarioCategory.Esports,
                    ScenarioCategory.JLPT,
                    ScenarioCategory.Other
                )

                ScrollableTabRow(
                    selectedTabIndex = categories.indexOfFirst { it.id == uiState.selectedCategory },
                    edgePadding = 16.dp
                ) {
                    categories.forEach { category ->
                        Tab(
                            selected = uiState.selectedCategory == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(category.icon)
                                    Text(category.label)
                                }
                            }
                        )
                    }
                }

                // 시나리오 리스트 (섹션 헤더 포함)
                val groupedScenarios = if (uiState.selectedCategory == null) {
                    // "전체" 탭: 카테고리별로 그룹화
                    uiState.scenarios.groupBy { it.category }
                } else {
                    // 특정 카테고리: 그룹화 없이 표시
                    mapOf("" to uiState.scenarios)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recommendation banner (only show on "전체" tab)
                    if (uiState.selectedCategory == null && uiState.recommendedScenarios.isNotEmpty()) {
                        item {
                            RecommendationBanner(
                                recommendations = uiState.recommendedScenarios,
                                onScenarioClick = onScenarioSelected
                            )
                        }
                    }

                    groupedScenarios.forEach { (category, scenarios) ->
                        // 섹션 헤더 (전체 탭에서만 표시)
                        if (uiState.selectedCategory == null && category.isNotEmpty()) {
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Text(
                                        text = "${getCategoryLabel(category)} (${scenarios.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // 시나리오 카드들
                        items(scenarios) { scenario ->
                            ScenarioCard(
                                scenario = scenario,
                                isFavorite = uiState.favoriteScenarioIds.contains(scenario.id),
                                onClick = { onScenarioSelected(scenario.id) },
                                onFavoriteClick = { viewModel.toggleFavorite(scenario.id) },
                                onDelete = if (scenario.isCustom) {
                                    { viewModel.deleteCustomScenario(scenario.id) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScenarioCard(
    scenario: Scenario,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(20.dp),  // Increased padding for better touch
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First row: Title + Favorite star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${scenario.thumbnailEmoji} ${scenario.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Favorite button (larger touch area)
                IconButton(
                    onClick = { onFavoriteClick() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기",
                        modifier = Modifier.size(28.dp),  // Larger star icon
                        tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Second row: Category + Difficulty badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getCategoryLabel(scenario.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Difficulty badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (scenario.difficulty) {
                        1 -> MaterialTheme.colorScheme.primaryContainer
                        2 -> MaterialTheme.colorScheme.tertiaryContainer
                        3 -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (scenario.difficulty) {
                            1 -> "초급"
                            2 -> "중급"
                            3 -> "고급"
                            else -> "초급"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (scenario.difficulty) {
                            1 -> MaterialTheme.colorScheme.onPrimaryContainer
                            2 -> MaterialTheme.colorScheme.onTertiaryContainer
                                3 -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (scenario.isCustom) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "커스텀",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Third row: Description
            Text(
                text = scenario.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            // Bottom row: Delete button for custom scenarios (if applicable)
            if (onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onDelete() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("삭제")
                    }
                }
            }
        }
    }
}



@Composable
fun RecommendationBanner(
    recommendations: List<com.nihongo.conversation.core.recommendation.ScoredScenario>,
    onScenarioClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "맞춤 추천",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = "학습 기록을 기반으로 당신에게 딱 맞는 시나리오를 추천합니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            // Recommendation cards (horizontal scrollable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendations.take(3).forEach { scored ->
                    RecommendationCard(
                        scoredScenario = scored,
                        onClick = { onScenarioClick(scored.scenario.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    scoredScenario: com.nihongo.conversation.core.recommendation.ScoredScenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emoji
            Text(
                text = scoredScenario.scenario.thumbnailEmoji,
                style = MaterialTheme.typography.headlineMedium
            )

            // Title
            Text(
                text = scoredScenario.scenario.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2
            )

            // Reason
            if (scoredScenario.reason.isNotBlank()) {
                Text(
                    text = scoredScenario.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Difficulty badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (scoredScenario.scenario.difficulty) {
                    1 -> MaterialTheme.colorScheme.primaryContainer
                    2 -> MaterialTheme.colorScheme.tertiaryContainer
                    3 -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when (scoredScenario.scenario.difficulty) {
                        1 -> "초급"
                        2 -> "중급"
                        3 -> "상급"
                        else -> "초급"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (scoredScenario.scenario.difficulty) {
                        1 -> MaterialTheme.colorScheme.onPrimaryContainer
                        2 -> MaterialTheme.colorScheme.onTertiaryContainer
                        3 -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

