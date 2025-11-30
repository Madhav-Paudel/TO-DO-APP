package com.example.todoapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.repository.DailyProgressRepository
import com.example.todoapp.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GoalDetailItem(
    val id: Long,
    val title: String,
    val category: String,
    val startDate: Long,
    val endDate: String,
    val endDateTimestamp: Long,
    val dailyTargetMinutes: Int,
    val progress: Float,
    val streak: Int,
    val overallProgress: Float,
    val daysRemaining: Int,
    val totalMinutesToday: Int,
    val isCompletedToday: Boolean
)

data class GoalDetailScreenData(
    val goal: GoalDetailItem? = null,
    val progressHistory: List<DailyProgressEntry> = emptyList(),
    val weeklyData: List<DailyChartPoint> = emptyList(),
    val totalStudyTime: Int = 0,
    val avgDailyTime: Int = 0,
    val bestStreak: Int = 0,
    val daysActive: Int = 0,
    val isLoading: Boolean = true
)

data class DailyProgressEntry(
    val date: Long,
    val dateLabel: String,
    val minutesDone: Int,
    val targetMet: Boolean,
    val targetMinutes: Int
)

data class DailyChartPoint(
    val dayLabel: String,
    val minutes: Int,
    val targetMinutes: Int,
    val percentage: Float
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val dailyProgressRepository: DailyProgressRepository
) : ViewModel() {

    private val _goals = goalRepository.allActiveGoals
    
    private val _selectedGoalDetail = MutableStateFlow(GoalDetailScreenData())
    val selectedGoalDetail: StateFlow<GoalDetailScreenData> = _selectedGoalDetail.asStateFlow()
    
    // Combine goals with their progress
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val goals: StateFlow<List<GoalDetailItem>> = _goals.flatMapLatest { goalsList ->
        kotlinx.coroutines.flow.flow {
            val items = goalsList.map { goal ->
                val progressList = dailyProgressRepository.getProgressByGoal(goal.id).first()
                val streak = com.example.todoapp.util.ProgressCalculator.calculateStreak(progressList)
                val overallProgress = com.example.todoapp.util.ProgressCalculator.calculateOverallProgress(goal, progressList)
                
                // Calculate today's progress %
                val today = getStartOfDay()
                val todayProgress = progressList.find { it.date == today }?.minutesDone ?: 0
                val dailyProgress = if (goal.dailyTargetMinutes > 0) {
                    (todayProgress.toFloat() / goal.dailyTargetMinutes).coerceIn(0f, 1f)
                } else 0f
                
                // Calculate days remaining
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(goal.endDate - System.currentTimeMillis())
                    .toInt().coerceAtLeast(0)

                GoalDetailItem(
                    id = goal.id,
                    title = goal.title,
                    category = goal.category,
                    startDate = goal.startDate,
                    endDate = convertDate(goal.endDate),
                    endDateTimestamp = goal.endDate,
                    dailyTargetMinutes = goal.dailyTargetMinutes,
                    progress = dailyProgress,
                    streak = streak,
                    overallProgress = overallProgress,
                    daysRemaining = daysRemaining,
                    totalMinutesToday = todayProgress,
                    isCompletedToday = todayProgress >= goal.dailyTargetMinutes
                )
            }
            emit(items)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadGoalDetail(goalId: Long) {
        viewModelScope.launch {
            _selectedGoalDetail.value = GoalDetailScreenData(isLoading = true)
            
            val goal = goalRepository.getGoalById(goalId) ?: return@launch
            val progressList = dailyProgressRepository.getProgressByGoal(goalId).first()
            
            val streak = com.example.todoapp.util.ProgressCalculator.calculateStreak(progressList)
            val overallProgress = com.example.todoapp.util.ProgressCalculator.calculateOverallProgress(goal, progressList)
            
            val today = getStartOfDay()
            val todayProgress = progressList.find { it.date == today }?.minutesDone ?: 0
            val dailyProgress = if (goal.dailyTargetMinutes > 0) {
                (todayProgress.toFloat() / goal.dailyTargetMinutes).coerceIn(0f, 1f)
            } else 0f
            
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(goal.endDate - System.currentTimeMillis())
                .toInt().coerceAtLeast(0)
            
            val goalItem = GoalDetailItem(
                id = goal.id,
                title = goal.title,
                category = goal.category,
                startDate = goal.startDate,
                endDate = convertDate(goal.endDate),
                endDateTimestamp = goal.endDate,
                dailyTargetMinutes = goal.dailyTargetMinutes,
                progress = dailyProgress,
                streak = streak,
                overallProgress = overallProgress,
                daysRemaining = daysRemaining,
                totalMinutesToday = todayProgress,
                isCompletedToday = todayProgress >= goal.dailyTargetMinutes
            )
            
            // Create progress history entries (last 30 days)
            val progressHistory = progressList
                .sortedByDescending { it.date }
                .take(30)
                .map { progress ->
                    DailyProgressEntry(
                        date = progress.date,
                        dateLabel = formatDateShort(progress.date),
                        minutesDone = progress.minutesDone,
                        targetMet = progress.wasTargetMet,
                        targetMinutes = goal.dailyTargetMinutes
                    )
                }
            
            // Create weekly chart data (last 7 days)
            val weeklyData = (0..6).reversed().map { daysAgo ->
                val date = today - TimeUnit.DAYS.toMillis(daysAgo.toLong())
                val progress = progressList.find { it.date == date }
                val minutes = progress?.minutesDone ?: 0
                DailyChartPoint(
                    dayLabel = getDayOfWeekShort(date),
                    minutes = minutes,
                    targetMinutes = goal.dailyTargetMinutes,
                    percentage = if (goal.dailyTargetMinutes > 0) 
                        (minutes.toFloat() / goal.dailyTargetMinutes).coerceIn(0f, 1.5f) 
                    else 0f
                )
            }
            
            // Calculate statistics
            val totalStudyTime = progressList.sumOf { it.minutesDone }
            val daysActive = progressList.size
            val avgDailyTime = if (daysActive > 0) totalStudyTime / daysActive else 0
            val bestStreak = calculateBestStreak(progressList)
            
            _selectedGoalDetail.value = GoalDetailScreenData(
                goal = goalItem,
                progressHistory = progressHistory,
                weeklyData = weeklyData,
                totalStudyTime = totalStudyTime,
                avgDailyTime = avgDailyTime,
                bestStreak = bestStreak,
                daysActive = daysActive,
                isLoading = false
            )
        }
    }

    fun addGoal(title: String, dailyTarget: Int, category: String = "General", durationMonths: Int = 1) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, durationMonths)
            
            val goal = GoalEntity(
                title = title,
                category = category.ifBlank { "General" },
                startDate = System.currentTimeMillis(),
                endDate = calendar.timeInMillis,
                dailyTargetMinutes = dailyTarget
            )
            goalRepository.insertGoal(goal)
        }
    }

    fun updateGoal(goalId: Long, title: String, dailyTarget: Int, category: String) {
        viewModelScope.launch {
            val existingGoal = goalRepository.getGoalById(goalId) ?: return@launch
            val updatedGoal = existingGoal.copy(
                title = title,
                category = category,
                dailyTargetMinutes = dailyTarget
            )
            goalRepository.updateGoal(updatedGoal)
            loadGoalDetail(goalId) // Refresh detail
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.deleteGoalById(goalId)
        }
    }

    private fun calculateBestStreak(progressList: List<DailyProgressEntity>): Int {
        if (progressList.isEmpty()) return 0
        
        val sortedDates = progressList
            .filter { it.wasTargetMet }
            .map { it.date }
            .sorted()
        
        if (sortedDates.isEmpty()) return 0
        
        var bestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until sortedDates.size) {
            val diff = sortedDates[i] - sortedDates[i - 1]
            if (diff == TimeUnit.DAYS.toMillis(1)) {
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else if (diff > TimeUnit.DAYS.toMillis(1)) {
                currentStreak = 1
            }
        }
        
        return bestStreak
    }

    private fun convertDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun getDayOfWeekShort(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> ""
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
