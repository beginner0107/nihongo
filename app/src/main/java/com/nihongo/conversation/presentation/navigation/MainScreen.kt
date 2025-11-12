package com.nihongo.conversation.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
 * Main screen with bottom navigation bar
 * This wraps HomeScreen, ScenarioListScreen, StatsScreen, ProfileScreen
 */
@Composable
fun MainScreen(
    onScenarioSelected: (Long) -> Unit,
    onFlashcardClick: () -> Unit = {},
    onAddVocabularyClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCreateScenarioClick: () -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Scenarios,
        BottomNavItem.Stats,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onScenarioSelected = onScenarioSelected,
                    onSettingsClick = onSettingsClick  // Phase 12: 설정 버튼 추가
                )
            }

            composable(BottomNavItem.Scenarios.route) {
                ScenarioListScreen(
                    onScenarioSelected = onScenarioSelected,
                    onFlashcardClick = onFlashcardClick,
                    onAddVocabularyClick = onAddVocabularyClick,
                    onSettingsClick = onSettingsClick,
                    onStatsClick = { navController.navigate(BottomNavItem.Stats.route) },
                    onProfileClick = { navController.navigate(BottomNavItem.Profile.route) },
                    onCreateScenarioClick = onCreateScenarioClick
                )
            }

            composable(BottomNavItem.Stats.route) {
                StatsScreen(
                    onBackClick = { /* No back button in bottom nav context */ }
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onBackClick = { /* No back button in bottom nav context */ },
                    onSaveSuccess = { /* Stay on profile screen */ }
                )
            }
        }
    }
}
