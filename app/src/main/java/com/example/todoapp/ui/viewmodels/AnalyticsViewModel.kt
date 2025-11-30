package com.example.todoapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.repository.DailyProgressRepository
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.PhoneUsageRepository
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.util.ActivityHeatmapGenerator
import com.example.todoapp.util.AiAnalyticsEngine
import com.example.todoapp.util.AnalyticsUtils
import com.example.todoapp.util.DailyDataPoint
import com.example.todoapp.util.GoalProgressData
import com.example.todoapp.util.GoalProjectionEngine
import com.example.todoapp.util.UsageStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class AnalyticsFilter(val days: Int, val label: String) {
    WEEK(7, "7D"),
    MONTH(30, "30D"),
    QUARTER(90, "90D")
}

class AnalyticsViewModel(
    private val dailyProgressRepository: DailyProgressRepository,
    private val phoneUsageRepository: PhoneUsageRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(AnalyticsFilter.WEEK)
    val filter: StateFlow<AnalyticsFilter> = _filter.asStateFlow()

    // Heatmap view state
    private val _heatmapView = MutableStateFlow(ActivityHeatmapGenerator.HeatmapView.MONTHLY)
    val heatmapView: StateFlow<ActivityHeatmapGenerator.HeatmapView> = _heatmapView.asStateFlow()

    // Get date range based on filter (extended to cover all needs)
    private fun getDateRange(days: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val start = calendar.timeInMillis
        return Pair(start, end)
    }

    // Active goals
    val activeGoals: StateFlow<List<GoalEntity>> = goalRepository.allActiveGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All tasks
    private val allTasks = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All progress data (for the extended period to calculate streaks etc)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allProgressData = goalRepository.allActiveGoals.flatMapLatest { goals ->
        val (start, _) = getDateRange(365) // Get full year for calculations
        dailyProgressRepository.getProgressBetweenDates(start, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All phone usage data (for full analysis)
    private val allPhoneUsageForAnalysis = phoneUsageRepository.getUsageBetweenDates(
        getDateRange(365).first, 
        System.currentTimeMillis()
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All phone usage data (for chart based on filter)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allPhoneUsageData = _filter.flatMapLatest { filter ->
        val (start, end) = getDateRange(filter.days)
        phoneUsageRepository.getUsageBetweenDates(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Analytics Report
    val aiAnalyticsReport: StateFlow<AiAnalyticsEngine.AnalyticsReport?> = combine(
        activeGoals,
        allTasks,
        allProgressData,
        allPhoneUsageForAnalysis
    ) { goals, tasks, progress, phoneUsage ->
        if (goals.isEmpty() && progress.isEmpty()) null
        else AiAnalyticsEngine.generateAnalyticsReport(goals, tasks, progress, phoneUsage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Goal progress data with all calculated metrics
    val goalProgressDataList: StateFlow<List<GoalProgressData>> = combine(
        activeGoals,
        allProgressData
    ) { goals, progressList ->
        goals.map { goal ->
            val (todayMin, todayCompleted) = AnalyticsUtils.calculateDailyCompletion(goal.id, progressList)
            GoalProgressData(
                goal = goal,
                overallProgress = AnalyticsUtils.calculateOverallGoalProgress(goal, progressList),
                daysRemaining = AnalyticsUtils.calculateDaysRemaining(goal),
                currentStreak = AnalyticsUtils.calculateStreak(goal.id, progressList),
                todayMinutes = todayMin,
                todayCompleted = todayCompleted,
                sparklineData = AnalyticsUtils.getStreakSparklineData(goal.id, progressList, 10)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study time chart data
    @OptIn(ExperimentalCoroutinesApi::class)
    val studyChartData: StateFlow<List<DailyDataPoint>> = combine(
        _filter,
        allProgressData
    ) { filter, progressList ->
        AnalyticsUtils.getStudyMinutesForPeriod(progressList, filter.days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phone usage chart data
    val phoneUsageChartData: StateFlow<List<DailyDataPoint>> = combine(
        _filter,
        allPhoneUsageData
    ) { filter, usageList ->
        AnalyticsUtils.getPhoneUsageForPeriod(usageList, filter.days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's phone usage
    val todayPhoneUsage: StateFlow<Int> = allPhoneUsageData.map { usageList ->
        val today = AnalyticsUtils.getStartOfDay()
        usageList.filter { it.date == today }.sumOf { it.totalMinutesUsed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Phone usage status
    val phoneUsageStatus: StateFlow<UsageStatus> = todayPhoneUsage.map { minutes ->
        AnalyticsUtils.getPhoneUsageStatus(minutes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageStatus.GOOD)

    // Summary stats
    val totalStudyMinutes: StateFlow<Int> = combine(
        _filter,
        allProgressData
    ) { filter, progressList ->
        val (start, end) = getDateRange(filter.days)
        progressList.filter { it.date in start..end }.sumOf { it.minutesDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val avgStudyMinutes: StateFlow<Int> = combine(
        _filter,
        allProgressData
    ) { filter, progressList ->
        val (start, end) = getDateRange(filter.days)
        val filtered = progressList.filter { it.date in start..end }
        if (filtered.isEmpty()) 0
        else {
            val days = filtered.groupBy { it.date }.size.coerceAtLeast(1)
            filtered.sumOf { it.minutesDone } / days
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overallTargetMetPercentage: StateFlow<Int> = combine(
        _filter,
        allProgressData
    ) { filter, progressList ->
        val (start, end) = getDateRange(filter.days)
        val filtered = progressList.filter { it.date in start..end }
        if (filtered.isEmpty()) 0
        else {
            val metCount = filtered.count { it.wasTargetMet }
            (metCount * 100) / filtered.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val maxStreak: StateFlow<Int> = goalProgressDataList.map { list ->
        list.maxOfOrNull { it.currentStreak } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Activity Heatmap Data
    @OptIn(ExperimentalCoroutinesApi::class)
    val heatmapData: StateFlow<ActivityHeatmapGenerator.HeatmapData?> = combine(
        _heatmapView,
        allProgressData,
        allPhoneUsageForAnalysis,
        allTasks
    ) { view, progress, phoneUsage, tasks ->
        ActivityHeatmapGenerator.generateHeatmap(
            view = view,
            progressData = progress,
            phoneUsageData = phoneUsage,
            tasks = tasks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Goal Projections
    val goalProjections: StateFlow<List<GoalProjectionEngine.GoalProjection>> = combine(
        activeGoals,
        allProgressData
    ) { goals, progress ->
        GoalProjectionEngine.calculateAllProjections(goals, progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projection Summary
    val projectionSummary: StateFlow<GoalProjectionEngine.ProjectionSummary?> = goalProjections.map { projections ->
        if (projections.isEmpty()) null
        else GoalProjectionEngine.calculateSummary(projections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selected goal for detailed projection view
    private val _selectedProjectionGoalId = MutableStateFlow<Long?>(null)
    val selectedProjectionGoalId: StateFlow<Long?> = _selectedProjectionGoalId.asStateFlow()

    val selectedProjection: StateFlow<GoalProjectionEngine.GoalProjection?> = combine(
        goalProjections,
        _selectedProjectionGoalId
    ) { projections, selectedId ->
        selectedId?.let { id -> projections.find { it.goal.id == id } }
            ?: projections.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setFilter(newFilter: AnalyticsFilter) {
        _filter.value = newFilter
    }

    fun setHeatmapView(view: ActivityHeatmapGenerator.HeatmapView) {
        _heatmapView.value = view
    }

    fun selectProjectionGoal(goalId: Long?) {
        _selectedProjectionGoalId.value = goalId
    }
}
