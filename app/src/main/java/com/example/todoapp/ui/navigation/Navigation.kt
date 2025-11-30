package com.example.todoapp.ui.navigation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.AppViewModelProvider
import com.example.todoapp.ui.screens.AnalyticsScreen
import com.example.todoapp.ui.screens.GoalDetailScreen
import com.example.todoapp.ui.screens.GoalsScreen
import com.example.todoapp.ui.screens.HomeScreen
import com.example.todoapp.ui.screens.OnboardingScreen
import com.example.todoapp.ui.screens.SettingsScreen
import com.example.todoapp.ui.screens.TasksScreen
import com.example.todoapp.ui.screens.AssistantScreen

private const val PREFS_NAME = "todoapp_prefs"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

@Composable
fun Navigation(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val hasCompletedOnboarding = remember { prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false) }
    
    val startDestination = if (hasCompletedOnboarding) Screen.Home.route else Screen.Onboarding.route
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel(factory = AppViewModelProvider.Factory),
                onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                onNavigateToTimer = {
                    // Navigate to timer with a default goal ID of -1 (create new session)
                    navController.navigate(Screen.Goals.route)
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Goals.route) {
            GoalsScreen(
                viewModel = viewModel(factory = AppViewModelProvider.Factory),
                onStartSession = { goalId ->
                    navController.navigate(Screen.Timer.createRoute(goalId))
                },
                onGoalClick = { goalId ->
                    navController.navigate(Screen.GoalDetail.createRoute(goalId))
                }
            )
        }
        composable(
            route = Screen.GoalDetail.route,
            arguments = listOf(androidx.navigation.navArgument("goalId") { type = androidx.navigation.NavType.LongType }),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("goalId") ?: return@composable
            GoalDetailScreen(
                goalId = goalId,
                viewModel = viewModel(factory = AppViewModelProvider.Factory),
                onBack = { navController.popBackStack() },
                onStartSession = { id ->
                    navController.navigate(Screen.Timer.createRoute(id))
                },
                onDelete = { navController.popBackStack() }
            )
        }
        composable(Screen.Tasks.route) {
            TasksScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory))
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory))
        }
        composable(Screen.Assistant.route) {
            AssistantScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory))
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(factory = AppViewModelProvider.Factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Timer.route,
            arguments = listOf(androidx.navigation.navArgument("goalId") { type = androidx.navigation.NavType.LongType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("goalId") ?: return@composable
            com.example.todoapp.ui.screens.TimerScreen(
                goalId = goalId,
                onStop = { navController.popBackStack() },
                viewModel = viewModel(factory = AppViewModelProvider.Factory)
            )
        }
    }
}
