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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on scenario
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getScenarioIcon(scenario.id),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title with badges (difficulty + custom)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Difficulty badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (scenario.difficulty) {
                            1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            2 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            3 -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = when (scenario.difficulty) {
                                1 -> "초급"
                                2 -> "중급"
                                3 -> "고급"
                                else -> "초급"
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (scenario.difficulty) {
                                1 -> MaterialTheme.colorScheme.tertiary
                                2 -> MaterialTheme.colorScheme.secondary
                                3 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scenario.isCustom) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "커스텀",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Favorite button (always visible)
            IconButton(
                onClick = { onFavoriteClick() }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기",
                    tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button for custom scenarios or arrow for default scenarios
            if (onDelete != null) {
                IconButton(
                    onClick = {
                        onDelete()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "시작",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: Int) {
    val (text, color) = when (difficulty) {
        1 -> "초급" to MaterialTheme.colorScheme.tertiary
        2 -> "중급" to MaterialTheme.colorScheme.secondary
        3 -> "고급" to MaterialTheme.colorScheme.error
        else -> "초급" to MaterialTheme.colorScheme.tertiary
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun getScenarioIcon(scenarioId: Long): ImageVector {
    return when (scenarioId) {
        1L -> Icons.Default.Restaurant      // 레스토랑
        2L -> Icons.Default.ShoppingCart    // 쇼핑
        3L -> Icons.Default.Hotel           // 호텔
        4L -> Icons.Default.People          // 친구
        5L -> Icons.Default.Phone           // 전화
        6L -> Icons.Default.MedicalServices // 병원
        10L -> Icons.Default.Work           // 취업 면접
        11L -> Icons.Default.Report         // 클레임 대응
        12L -> Icons.Default.LocalHospital  // 긴급 상황
        13L -> Icons.Default.Favorite       // 데이트
        14L -> Icons.Default.BusinessCenter // 비즈니스 프레젠테이션
        15L -> Icons.Default.Chat           // 여자친구와의 대화
        16L -> Icons.Default.BusinessCenter // IT기업 기술 면접 (커스텀)
        else -> Icons.Default.Chat
    }
}
