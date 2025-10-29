package com.nihongo.conversation.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nihongo.conversation.presentation.chat.ChatScreen
import com.nihongo.conversation.presentation.profile.ProfileScreen
import com.nihongo.conversation.presentation.review.ReviewScreen
import com.nihongo.conversation.presentation.scenario.ScenarioListScreen
import com.nihongo.conversation.presentation.settings.SettingsScreen
import com.nihongo.conversation.presentation.stats.StatsScreen

sealed class Screen(val route: String) {
    data object ScenarioList : Screen("scenarios")
    data object Settings : Screen("settings")
    data object Review : Screen("review")
    data object Stats : Screen("stats")
    data object Profile : Screen("profile")

    data object Chat : Screen("chat/{userId}/{scenarioId}") {
        fun createRoute(userId: Long, scenarioId: Long) = "chat/$userId/$scenarioId"
    }
}

@Composable
fun NihongoNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ScenarioList.route
    ) {
        composable(route = Screen.ScenarioList.route) {
            ScenarioListScreen(
                onScenarioSelected = { scenarioId ->
                    navController.navigate(Screen.Chat.createRoute(1L, scenarioId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onStatsClick = {
                    navController.navigate(Screen.Stats.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Review.route) {
            ReviewScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Stats.route) {
            StatsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("scenarioId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 1L
            val scenarioId = backStackEntry.arguments?.getLong("scenarioId") ?: 1L
            ChatScreen(
                userId = userId,
                scenarioId = scenarioId,
                onBackClick = { navController.popBackStack() },
                onReviewClick = { navController.navigate(Screen.Review.route) }
            )
        }
    }
}
