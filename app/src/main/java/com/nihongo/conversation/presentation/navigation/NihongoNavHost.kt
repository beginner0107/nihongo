package com.nihongo.conversation.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.nihongo.conversation.presentation.chat.ChatScreen
import com.nihongo.conversation.presentation.flashcard.FlashcardReviewScreen
import com.nihongo.conversation.presentation.flashcard.FlashcardStatsScreen
import com.nihongo.conversation.presentation.profile.ProfileScreen
import com.nihongo.conversation.presentation.pronunciation.PronunciationHistoryScreen
import com.nihongo.conversation.presentation.review.ReviewScreen
import com.nihongo.conversation.presentation.review.ConversationReviewScreen
import com.nihongo.conversation.presentation.scenario.ScenarioListScreen
import com.nihongo.conversation.presentation.settings.SettingsScreen
import com.nihongo.conversation.presentation.stats.StatsScreen
import com.nihongo.conversation.presentation.user.UserSelectionScreen
import com.nihongo.conversation.presentation.vocabulary.AddVocabularyScreen
import com.nihongo.conversation.presentation.scenario.CreateScenarioScreen
import com.nihongo.conversation.presentation.onboarding.OnboardingScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object UserSelection : Screen("user_selection")
    data object Main : Screen("main")  // NEW: Main screen with bottom nav
    data object ScenarioList : Screen("scenarios")
    data object Flashcard : Screen("flashcard")
    data object FlashcardStats : Screen("flashcard_stats")
    data object PronunciationHistory : Screen("pronunciation_history")
    data object AddVocabulary : Screen("add_vocabulary")
    data object Settings : Screen("settings")
    data object Review : Screen("review")
    data object Stats : Screen("stats")
    data object Profile : Screen("profile")
    data object CreateScenario : Screen("create_scenario")

    data object Chat : Screen("chat/{userId}/{scenarioId}") {
        fun createRoute(userId: Long, scenarioId: Long) = "chat/$userId/$scenarioId"
    }

    data object ConversationReview : Screen("conversation_review/{conversationId}") {
        fun createRoute(conversationId: Long) = "conversation_review/$conversationId"
    }
}

@Composable
fun NihongoNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.UserSelection.route,
    onOnboardingComplete: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.UserSelection.route) {
                        // Remove onboarding from back stack
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.UserSelection.route) {
            UserSelectionScreen(
                onUserSelected = {
                    navController.navigate(Screen.Main.route) {
                        // Remove user selection from back stack
                        popUpTo(Screen.UserSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Main.route) {
            MainScreen(
                onScenarioSelected = { scenarioId ->
                    navController.navigate(Screen.Chat.createRoute(1L, scenarioId))
                },
                onFlashcardClick = {
                    navController.navigate(Screen.Flashcard.route)
                },
                onAddVocabularyClick = {
                    navController.navigate(Screen.AddVocabulary.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onCreateScenarioClick = {
                    navController.navigate(Screen.CreateScenario.route)
                },
                onReviewClick = {
                    navController.navigate(Screen.Review.route)
                }
            )
        }

        composable(route = Screen.ScenarioList.route) {
            ScenarioListScreen(
                onScenarioSelected = { scenarioId ->
                    navController.navigate(Screen.Chat.createRoute(1L, scenarioId))
                },
                onFlashcardClick = {
                    navController.navigate(Screen.Flashcard.route)
                },
                onAddVocabularyClick = {
                    navController.navigate(Screen.AddVocabulary.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onStatsClick = {
                    navController.navigate(Screen.Stats.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onCreateScenarioClick = {
                    navController.navigate(Screen.CreateScenario.route)
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Flashcard.route) {
            FlashcardReviewScreen(
                onBackClick = { navController.popBackStack() },
                onStatsClick = { navController.navigate(Screen.FlashcardStats.route) }
            )
        }

        composable(route = Screen.FlashcardStats.route) {
            FlashcardStatsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.PronunciationHistory.route) {
            PronunciationHistoryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AddVocabulary.route) {
            AddVocabularyScreen(
                onBackClick = { navController.popBackStack() },
                onVocabularySaved = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Review.route) {
            ReviewScreen(
                onBackClick = { navController.popBackStack() },
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.ConversationReview.createRoute(conversationId))
                }
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

        composable(route = Screen.CreateScenario.route) {
            CreateScenarioScreen(
                onBackClick = { navController.popBackStack() },
                onScenarioCreated = { navController.popBackStack() }
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
                onReviewClick = { conversationId ->
                    navController.navigate(Screen.ConversationReview.createRoute(conversationId))
                }
            )
        }

        composable(
            route = Screen.ConversationReview.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            ConversationReviewScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
