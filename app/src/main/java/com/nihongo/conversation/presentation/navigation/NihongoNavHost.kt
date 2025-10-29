package com.nihongo.conversation.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nihongo.conversation.presentation.chat.ChatScreen

sealed class Screen(val route: String) {
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
        startDestination = Screen.Chat.createRoute(1L, 1L)
    ) {
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("scenarioId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 1L
            val scenarioId = backStackEntry.arguments?.getLong("scenarioId") ?: 1L
            ChatScreen(userId = userId, scenarioId = scenarioId)
        }
    }
}
