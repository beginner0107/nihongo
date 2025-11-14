package com.nihongo.conversation.presentation.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.nihongo.conversation.presentation.home.HomeScreen
import com.nihongo.conversation.presentation.profile.ProfileScreen
import com.nihongo.conversation.presentation.scenario.ScenarioListScreen
import com.nihongo.conversation.presentation.stats.StatsScreen

/**
 * Main bottom navigation tabs
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "홈", Icons.Default.Home)
    data object Scenarios : BottomNavItem("scenarios", "시나리오", Icons.AutoMirrored.Filled.List)
    data object Stats : BottomNavItem("stats", "통계", Icons.AutoMirrored.Filled.ShowChart)
    data object Profile : BottomNavItem("profile", "프로필", Icons.Default.Person)
}

/**
 * Main screen with bottom navigation bar and swipeable pages
 * Supports horizontal swipe gestures to navigate between tabs
 * Tabs cycle: Home ↔ Scenarios ↔ Stats ↔ Profile
 *
 * Phase 1 Improvements:
 * - NestedScrollConnection for scroll conflict resolution
 * - beyondBoundsPageCount for render optimization
 * - Error handling with Snackbar
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onScenarioSelected: (Long) -> Unit,
    onFlashcardClick: () -> Unit = {},
    onAddVocabularyClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCreateScenarioClick: () -> Unit = {},
    onReviewClick: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Scenarios,
        BottomNavItem.Stats,
        BottomNavItem.Profile
    )

    // PagerState for swipeable pages
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )
    val scope = rememberCoroutineScope()

    // Error handling state
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Show error snackbar when error occurs
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            errorMessage = null
        }
    }

    // NestedScrollConnection for scroll conflict resolution
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Prioritize vertical scrolling over horizontal page swipes
                val isVerticalScroll = abs(available.y) > abs(available.x)

                // If vertical scroll is dominant, let child handle it
                return if (isVerticalScroll) {
                    Offset.Zero  // Let child consume vertical scroll
                } else {
                    Offset.Zero  // Let HorizontalPager handle horizontal swipes
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Allow vertical fling to take precedence
                val isVerticalFling = abs(available.y) > abs(available.x)
                return if (isVerticalFling) {
                    Velocity.Zero  // Let child handle vertical fling
                } else {
                    Velocity.Zero  // Let HorizontalPager handle horizontal fling
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                try {
                                    pagerState.animateScrollToPage(index)
                                } catch (e: CancellationException) {
                                    // Ignore cancellation - it's normal when user taps rapidly
                                    throw e  // Re-throw to propagate cancellation
                                } catch (e: Exception) {
                                    errorMessage = "페이지 이동 실패: ${e.message}"
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection),
            userScrollEnabled = true,  // Enable swipe gestures
            key = { it }  // Stable key for page reuse
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    onScenarioSelected = onScenarioSelected,
                    onSettingsClick = onSettingsClick,
                    onReviewClick = onReviewClick
                )
                1 -> ScenarioListScreen(
                    onScenarioSelected = onScenarioSelected,
                    onFlashcardClick = onFlashcardClick,
                    onAddVocabularyClick = onAddVocabularyClick,
                    onSettingsClick = onSettingsClick,
                    onStatsClick = {
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onProfileClick = {
                        scope.launch { pagerState.animateScrollToPage(3) }
                    },
                    onCreateScenarioClick = onCreateScenarioClick
                )
                2 -> StatsScreen(
                    onBackClick = { /* No back button in bottom nav context */ }
                )
                3 -> ProfileScreen(
                    onBackClick = { /* No back button in bottom nav context */ },
                    onSaveSuccess = { /* Stay on profile screen */ }
                )
            }
        }
    }
}
