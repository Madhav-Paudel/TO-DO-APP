package com.example.todoapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.local.TaskEntity
import com.example.todoapp.data.repository.DailyProgressRepository
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.PhoneUsageRepository
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.util.AnalyticsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// Data classes for Home Dashboard
data class HomeTaskItem(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val priority: Int,
    val goalId: Long?
)

data class HomeGoalItem(
    val id: Long,
    val title: String,
    val category: String,
    val dailyTargetMinutes: Int,
    val todayMinutes: Int,
    val todayProgress: Float,
    val currentStreak: Int,
    val sparklineData: List<Boolean>,
    val isCompletedToday: Boolean
)

data class TodayOverviewData(
    val totalStudyMinutes: Int,
    val studyGoalMinutes: Int,
    val studyProgress: Float,
    val phoneUsageMinutes: Int,
    val phoneUsageLimit: Int,
    val phoneProgress: Float,
    val completedTasks: Int,
    val totalTasks: Int,
    val taskProgress: Float,
    val motivationalLine: String
)

data class HomeUiState(
    val todayOverview: TodayOverviewData = TodayOverviewData(
        totalStudyMinutes = 0,
        studyGoalMinutes = 120,
        studyProgress = 0f,
        phoneUsageMinutes = 0,
        phoneUsageLimit = 60,
        phoneProgress = 0f,
        completedTasks = 0,
        totalTasks = 0,
        taskProgress = 0f,
        motivationalLine = "Start your day strong! 💪"
    ),
    val goals: List<HomeGoalItem> = emptyList(),
    val tasks: List<HomeTaskItem> = emptyList(),
    val isLoading: Boolean = true,
    val allTasksCompleted: Boolean = false,
    val showConfetti: Boolean = false
)

class HomeViewModel(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val phoneUsageRepository: PhoneUsageRepository
) : ViewModel() {

    private val todayStart = getStartOfDay()
    private val todayEnd = todayStart + 24 * 60 * 60 * 1000

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Combined state for the entire home dashboard
    val homeState: StateFlow<HomeUiState> = combine(
        taskRepository.getTasksByDate(todayStart, todayEnd),
        goalRepository.allActiveGoals,
        dailyProgressRepository.getProgressByDate(todayStart),
        phoneUsageRepository.getUsageByDate(todayStart)
    ) { tasks, goals, todayProgress, phoneUsage ->
        
        // Calculate task stats
        val completedTasks = tasks.count { it.isCompleted }
        val totalTasks = tasks.size
        val taskProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
        val allCompleted = totalTasks > 0 && completedTasks == totalTasks
        
        // Calculate study stats
        val totalStudyMinutes = todayProgress.sumOf { it.minutesDone }
        val totalDailyGoal = goals.sumOf { it.dailyTargetMinutes }.takeIf { it > 0 } ?: 120
        val studyProgress = (totalStudyMinutes.toFloat() / totalDailyGoal).coerceIn(0f, 1f)
        
        // Phone usage
        val phoneMinutes = phoneUsage?.totalMinutesUsed ?: 0
        val phoneLimit = 60 // Default limit
        val phoneProgress = (phoneMinutes.toFloat() / phoneLimit).coerceIn(0f, 1.5f)
        
        // Generate motivational line
        val motivationalLine = getMotivationalLine(studyProgress, taskProgress, phoneProgress)
        
        // Map goals with progress data
        val homeGoals = goals.map { goal ->
            val goalProgress = todayProgress.filter { it.goalId == goal.id }
            val todayMinutes = goalProgress.sumOf { it.minutesDone }
            val todayProgressPercent = (todayMinutes.toFloat() / goal.dailyTargetMinutes).coerceIn(0f, 1f)
            val isCompleted = todayMinutes >= goal.dailyTargetMinutes
            
            HomeGoalItem(
                id = goal.id,
                title = goal.title,
                category = goal.category,
                dailyTargetMinutes = goal.dailyTargetMinutes,
                todayMinutes = todayMinutes,
                todayProgress = todayProgressPercent,
                currentStreak = calculateSimpleStreak(goal.id, todayProgress, isCompleted),
                sparklineData = List(7) { false }, // Simplified for now
                isCompletedToday = isCompleted
            )
        }
        
        // Map tasks with priority
        val homeTasks = tasks.map { task ->
            HomeTaskItem(
                id = task.id,
                title = task.title,
                isCompleted = task.isCompleted,
                priority = task.priority,
                goalId = task.goalId
            )
        }.sortedWith(compareBy({ it.isCompleted }, { -it.priority }))
        
        HomeUiState(
            todayOverview = TodayOverviewData(
                totalStudyMinutes = totalStudyMinutes,
                studyGoalMinutes = totalDailyGoal,
                studyProgress = studyProgress,
                phoneUsageMinutes = phoneMinutes,
                phoneUsageLimit = phoneLimit,
                phoneProgress = phoneProgress,
                completedTasks = completedTasks,
                totalTasks = totalTasks,
                taskProgress = taskProgress,
                motivationalLine = motivationalLine
            ),
            goals = homeGoals,
            tasks = homeTasks,
            isLoading = false,
            allTasksCompleted = allCompleted,
            showConfetti = allCompleted && totalTasks > 0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
            }
        }
    }

    fun dismissConfetti() {
        // Can be used to dismiss confetti manually
    }

    private fun calculateSimpleStreak(goalId: Long, todayProgress: List<DailyProgressEntity>, isCompletedToday: Boolean): Int {
        // Simplified streak calculation - just check if completed today
        return if (isCompletedToday) {
            val previousStreak = todayProgress.filter { it.goalId == goalId && it.wasTargetMet }.size
            previousStreak.coerceAtLeast(1)
        } else 0
    }

    private fun getMotivationalLine(studyProgress: Float, taskProgress: Float, phoneProgress: Float): String {
        return when {
            studyProgress >= 1f && taskProgress >= 1f -> "🎉 Amazing! You crushed it today!"
            studyProgress >= 0.8f -> "🔥 You're on fire! Keep going!"
            taskProgress >= 0.5f -> "💪 Great progress on your tasks!"
            phoneProgress > 1f -> "📱 Consider taking a break from your phone"
            studyProgress >= 0.3f -> "📚 You're making progress, keep it up!"
            else -> "✨ Start your day strong! Every minute counts."
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
