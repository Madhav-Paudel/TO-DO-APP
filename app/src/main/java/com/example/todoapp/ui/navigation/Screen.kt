package com.example.todoapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Onboarding : Screen("onboarding", "Welcome", Icons.Default.Star)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.CheckCircle)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object Assistant : Screen("assistant", "Coach", Icons.Default.Face)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object ModelManager : Screen("model_manager", "Model Manager", Icons.Default.Settings)
    object Timer : Screen("timer/{goalId}", "Timer", Icons.Default.Timer) {
        fun createRoute(goalId: Long) = "timer/$goalId"
    }
    object GoalDetail : Screen("goal_detail/{goalId}", "Goal Detail", Icons.Default.Flag) {
        fun createRoute(goalId: Long) = "goal_detail/$goalId"
    }
}
