package com.example.todoapp.util

import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Goal Projection Engine
 * Calculates progress, predicts completion, and provides intelligent recommendations
 */
object GoalProjectionEngine {

    // ==================== DATA MODELS ====================

    enum class GoalStatus {
        ON_TRACK,           // Green - progressing well
        SLIGHT_DELAY,       // Yellow - needs attention
        BEHIND_SCHEDULE,    // Red - high risk of delay
        COMPLETED,          // Goal achieved
        NOT_STARTED         // No progress yet
    }

    data class GoalProjection(
        val goal: GoalEntity,
        val percentCompleted: Float,           // 0-100
        val percentTimeElapsed: Float,         // 0-100
        val status: GoalStatus,
        val statusBadge: String,
        val statusDescription: String,
        val estimatedCompletionDate: Long?,
        val daysRemaining: Int,
        val daysTotal: Int,
        val willFinishOnTime: Boolean,
        val daysAheadOrBehind: Int,            // Positive = ahead, Negative = behind
        val currentPace: Float,                // Minutes per day (actual)
        val requiredPace: Float,               // Minutes per day (to finish on time)
        val originalPace: Float,               // Minutes per day (original target)
        val paceAdjustment: Int,               // Additional minutes needed per day
        val recommendation: String,
        val chartData: ProjectionChartData,
        val recentPerformance: RecentPerformance,
        val confidenceLevel: Float             // 0-100 confidence in projection
    )

    data class ProjectionChartData(
        val actualProgress: List<ChartPoint>,      // Real data
        val expectedProgress: List<ChartPoint>,    // Linear expected line
        val requiredPace: List<ChartPoint>,        // What's needed to finish
        val projectedProgress: List<ChartPoint>,   // Based on recent performance
        val goalTarget: Float,                     // Total minutes goal
        val maxValue: Float                        // For chart scaling
    )

    data class ChartPoint(
        val day: Int,              // Day number from start
        val value: Float,          // Cumulative minutes
        val date: Long,
        val isProjected: Boolean = false
    )

    data class RecentPerformance(
        val last10DaysAverage: Float,          // Average minutes last 10 days
        val last7DaysAverage: Float,           // Average minutes last 7 days
        val last3DaysAverage: Float,           // Average minutes last 3 days
        val trend: PerformanceTrend,
        val consistencyScore: Float,           // 0-100, how consistent
        val activeDaysRatio: Float             // Ratio of active days
    )

    enum class PerformanceTrend {
        IMPROVING,
        STABLE,
        DECLINING
    }

    // ==================== PROJECTION CALCULATION ====================

    fun calculateProjection(
        goal: GoalEntity,
        progressData: List<DailyProgressEntity>
    ): GoalProjection {
        val now = System.currentTimeMillis()
        val goalProgress = progressData.filter { it.goalId == goal.id }
        
        // Time calculations
        val totalDays = calculateTotalDays(goal.startDate, goal.endDate)
        val elapsedDays = calculateElapsedDays(goal.startDate, now, goal.endDate)
        val remainingDays = max(0, totalDays - elapsedDays)
        val percentTimeElapsed = if (totalDays > 0) (elapsedDays.toFloat() / totalDays * 100).coerceIn(0f, 100f) else 0f
        
        // Progress calculations
        val totalMinutesDone = goalProgress.sumOf { it.minutesDone }
        val totalMinutesTarget = goal.dailyTargetMinutes * totalDays
        val percentCompleted = if (totalMinutesTarget > 0) (totalMinutesDone.toFloat() / totalMinutesTarget * 100).coerceIn(0f, 100f) else 0f
        
        // Performance analysis
        val recentPerformance = analyzeRecentPerformance(goalProgress, goal.dailyTargetMinutes)
        
        // Pace calculations
        val activeDays = goalProgress.filter { it.minutesDone > 0 }.size.coerceAtLeast(1)
        val currentPace = totalMinutesDone.toFloat() / max(1, elapsedDays)
        val originalPace = goal.dailyTargetMinutes.toFloat()
        val remainingMinutes = max(0, totalMinutesTarget - totalMinutesDone)
        val requiredPace = if (remainingDays > 0) remainingMinutes.toFloat() / remainingDays else 0f
        
        // Status determination
        val status = determineStatus(percentCompleted, percentTimeElapsed, recentPerformance.trend)
        
        // Completion prediction
        val (willFinish, estimatedDate, daysAheadOrBehind) = predictCompletion(
            totalMinutesDone, 
            totalMinutesTarget, 
            recentPerformance.last7DaysAverage,
            goal.endDate,
            remainingDays
        )
        
        // Recommendations
        val paceAdjustment = if (requiredPace > originalPace) (requiredPace - originalPace).roundToInt() else 0
        val recommendation = generateRecommendation(status, paceAdjustment, originalPace, requiredPace, recentPerformance)
        
        // Chart data
        val chartData = generateChartData(
            goal = goal,
            progressData = goalProgress,
            totalDays = totalDays,
            elapsedDays = elapsedDays,
            recentPerformance = recentPerformance
        )
        
        // Confidence level based on data quality and consistency
        val confidenceLevel = calculateConfidence(goalProgress.size, recentPerformance.consistencyScore)
        
        return GoalProjection(
            goal = goal,
            percentCompleted = percentCompleted,
            percentTimeElapsed = percentTimeElapsed,
            status = status,
            statusBadge = getStatusBadge(status),
            statusDescription = getStatusDescription(status, daysAheadOrBehind),
            estimatedCompletionDate = estimatedDate,
            daysRemaining = remainingDays,
            daysTotal = totalDays,
            willFinishOnTime = willFinish,
            daysAheadOrBehind = daysAheadOrBehind,
            currentPace = currentPace,
            requiredPace = requiredPace,
            originalPace = originalPace,
            paceAdjustment = paceAdjustment,
            recommendation = recommendation,
            chartData = chartData,
            recentPerformance = recentPerformance,
            confidenceLevel = confidenceLevel
        )
    }

    private fun calculateTotalDays(startDate: Long, endDate: Long): Int {
        return TimeUnit.MILLISECONDS.toDays(endDate - startDate).toInt().coerceAtLeast(1)
    }

    private fun calculateElapsedDays(startDate: Long, now: Long, endDate: Long): Int {
        val effectiveNow = min(now, endDate)
        return TimeUnit.MILLISECONDS.toDays(effectiveNow - startDate).toInt().coerceAtLeast(0)
    }

    // ==================== PERFORMANCE ANALYSIS ====================

    private fun analyzeRecentPerformance(
        progressData: List<DailyProgressEntity>,
        targetMinutes: Int
    ): RecentPerformance {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Get data for different periods
        fun getAverageForDays(days: Int): Pair<Float, Int> {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startTime = calendar.timeInMillis
            
            val recentData = progressData.filter { it.date >= startTime }
            val total = recentData.sumOf { it.minutesDone }
            val activeDays = recentData.filter { it.minutesDone > 0 }.size
            return Pair(total.toFloat() / days, activeDays)
        }
        
        val (last10Avg, active10) = getAverageForDays(10)
        val (last7Avg, active7) = getAverageForDays(7)
        val (last3Avg, active3) = getAverageForDays(3)
        
        // Determine trend
        val trend = when {
            last3Avg > last7Avg * 1.1f -> PerformanceTrend.IMPROVING
            last3Avg < last7Avg * 0.9f -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
        
        // Calculate consistency (how often target is met)
        val last10Days = progressData.takeLast(10)
        val targetMetCount = last10Days.count { it.minutesDone >= targetMinutes }
        val consistencyScore = if (last10Days.isNotEmpty()) {
            (targetMetCount.toFloat() / last10Days.size * 100)
        } else 0f
        
        // Active days ratio
        val activeDaysRatio = if (progressData.isNotEmpty()) {
            progressData.count { it.minutesDone > 0 }.toFloat() / progressData.size
        } else 0f
        
        return RecentPerformance(
            last10DaysAverage = last10Avg,
            last7DaysAverage = last7Avg,
            last3DaysAverage = last3Avg,
            trend = trend,
            consistencyScore = consistencyScore,
            activeDaysRatio = activeDaysRatio
        )
    }

    // ==================== STATUS DETERMINATION ====================

    private fun determineStatus(
        percentCompleted: Float,
        percentTimeElapsed: Float,
        trend: PerformanceTrend
    ): GoalStatus {
        if (percentCompleted >= 100) return GoalStatus.COMPLETED
        if (percentCompleted == 0f && percentTimeElapsed > 0) return GoalStatus.NOT_STARTED
        
        val progressRatio = if (percentTimeElapsed > 0) percentCompleted / percentTimeElapsed else 1f
        
        return when {
            progressRatio >= 0.95f -> GoalStatus.ON_TRACK
            progressRatio >= 0.75f -> {
                // Consider trend for borderline cases
                if (trend == PerformanceTrend.IMPROVING) GoalStatus.ON_TRACK
                else GoalStatus.SLIGHT_DELAY
            }
            progressRatio >= 0.5f -> GoalStatus.SLIGHT_DELAY
            else -> GoalStatus.BEHIND_SCHEDULE
        }
    }

    private fun getStatusBadge(status: GoalStatus): String {
        return when (status) {
            GoalStatus.ON_TRACK -> "On Track"
            GoalStatus.SLIGHT_DELAY -> "Needs Attention"
            GoalStatus.BEHIND_SCHEDULE -> "High Risk of Delay"
            GoalStatus.COMPLETED -> "Completed!"
            GoalStatus.NOT_STARTED -> "Not Started"
        }
    }

    private fun getStatusDescription(status: GoalStatus, daysAheadOrBehind: Int): String {
        return when (status) {
            GoalStatus.ON_TRACK -> {
                if (daysAheadOrBehind > 0) "You're $daysAheadOrBehind days ahead of schedule!"
                else "Great work! Keep up the momentum."
            }
            GoalStatus.SLIGHT_DELAY -> {
                val behind = kotlin.math.abs(daysAheadOrBehind)
                "You're $behind days behind. A small push can get you back on track!"
            }
            GoalStatus.BEHIND_SCHEDULE -> {
                val behind = kotlin.math.abs(daysAheadOrBehind)
                "You're $behind days behind schedule. Increased effort needed."
            }
            GoalStatus.COMPLETED -> "Congratulations! You've achieved your goal!"
            GoalStatus.NOT_STARTED -> "Start today to build momentum!"
        }
    }

    // ==================== COMPLETION PREDICTION ====================

    private fun predictCompletion(
        totalDone: Int,
        totalTarget: Int,
        recentAverage: Float,
        endDate: Long,
        remainingDays: Int
    ): Triple<Boolean, Long?, Int> {
        if (totalDone >= totalTarget) {
            return Triple(true, System.currentTimeMillis(), 0)
        }
        
        val remainingMinutes = totalTarget - totalDone
        
        // Estimate days to complete based on recent performance
        val daysToComplete = if (recentAverage > 0) {
            ceil(remainingMinutes / recentAverage).toInt()
        } else {
            Int.MAX_VALUE
        }
        
        val willFinish = daysToComplete <= remainingDays
        val daysAheadOrBehind = remainingDays - daysToComplete
        
        // Calculate estimated completion date
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysToComplete)
        val estimatedDate = if (daysToComplete < Int.MAX_VALUE) calendar.timeInMillis else null
        
        return Triple(willFinish, estimatedDate, daysAheadOrBehind)
    }

    // ==================== RECOMMENDATIONS ====================

    private fun generateRecommendation(
        status: GoalStatus,
        paceAdjustment: Int,
        originalPace: Float,
        requiredPace: Float,
        performance: RecentPerformance
    ): String {
        return when (status) {
            GoalStatus.COMPLETED -> "Amazing! You've completed your goal. Consider setting a new challenge!"
            
            GoalStatus.ON_TRACK -> {
                when (performance.trend) {
                    PerformanceTrend.IMPROVING -> "Excellent progress! Your recent improvement is paying off."
                    PerformanceTrend.DECLINING -> "You're on track, but recent activity has dipped. Stay consistent!"
                    PerformanceTrend.STABLE -> "Steady progress! Maintain your ${originalPace.roundToInt()} min/day routine."
                }
            }
            
            GoalStatus.SLIGHT_DELAY -> {
                val newTarget = requiredPace.roundToInt()
                val increase = paceAdjustment
                "Increase daily time from ${originalPace.roundToInt()} min to $newTarget min (+$increase min) to catch up."
            }
            
            GoalStatus.BEHIND_SCHEDULE -> {
                val newTarget = requiredPace.roundToInt()
                val formatted = formatMinutes(newTarget)
                "You need $formatted daily to finish on time. Consider extending your goal or adjusting the target."
            }
            
            GoalStatus.NOT_STARTED -> "Start with just ${originalPace.roundToInt()} minutes today. Every journey begins with a single step!"
        }
    }

    private fun formatMinutes(minutes: Int): String {
        return if (minutes >= 60) {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        } else {
            "${minutes} min"
        }
    }

    // ==================== CHART DATA GENERATION ====================

    private fun generateChartData(
        goal: GoalEntity,
        progressData: List<DailyProgressEntity>,
        totalDays: Int,
        elapsedDays: Int,
        recentPerformance: RecentPerformance
    ): ProjectionChartData {
        val targetPerDay = goal.dailyTargetMinutes.toFloat()
        val totalTarget = targetPerDay * totalDays
        
        // Group progress by day
        val progressByDay = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = goal.startDate
        setToStartOfDay(calendar)
        val startTime = calendar.timeInMillis
        
        progressData.forEach { progress ->
            val dayNumber = TimeUnit.MILLISECONDS.toDays(progress.date - startTime).toInt()
            if (dayNumber >= 0 && dayNumber < totalDays) {
                progressByDay[dayNumber] = (progressByDay[dayNumber] ?: 0) + progress.minutesDone
            }
        }
        
        // Generate actual progress (cumulative)
        val actualProgress = mutableListOf<ChartPoint>()
        var cumulative = 0f
        for (day in 0..elapsedDays) {
            cumulative += (progressByDay[day] ?: 0)
            calendar.timeInMillis = startTime
            calendar.add(Calendar.DAY_OF_YEAR, day)
            actualProgress.add(ChartPoint(day, cumulative, calendar.timeInMillis, false))
        }
        
        // Generate expected progress (linear)
        val expectedProgress = mutableListOf<ChartPoint>()
        for (day in 0..totalDays) {
            val expected = targetPerDay * day
            calendar.timeInMillis = startTime
            calendar.add(Calendar.DAY_OF_YEAR, day)
            expectedProgress.add(ChartPoint(day, expected, calendar.timeInMillis, false))
        }
        
        // Generate required pace line (from current point to goal)
        val requiredPacePoints = mutableListOf<ChartPoint>()
        val currentProgress = actualProgress.lastOrNull()?.value ?: 0f
        val remainingDays = totalDays - elapsedDays
        val dailyRequired = if (remainingDays > 0) (totalTarget - currentProgress) / remainingDays else 0f
        
        for (day in elapsedDays..totalDays) {
            val projected = currentProgress + dailyRequired * (day - elapsedDays)
            calendar.timeInMillis = startTime
            calendar.add(Calendar.DAY_OF_YEAR, day)
            requiredPacePoints.add(ChartPoint(day, projected.coerceAtMost(totalTarget), calendar.timeInMillis, true))
        }
        
        // Generate projected progress (based on recent performance)
        val projectedProgress = mutableListOf<ChartPoint>()
        val projectionBase = recentPerformance.last7DaysAverage
        var projectedCumulative = currentProgress
        
        for (day in elapsedDays..totalDays) {
            if (day == elapsedDays) {
                projectedProgress.add(ChartPoint(day, projectedCumulative, 0L, true))
            } else {
                projectedCumulative += projectionBase
                calendar.timeInMillis = startTime
                calendar.add(Calendar.DAY_OF_YEAR, day)
                projectedProgress.add(ChartPoint(day, projectedCumulative, calendar.timeInMillis, true))
            }
        }
        
        // Calculate max value for scaling
        val maxValue = maxOf(
            totalTarget,
            projectedProgress.maxOfOrNull { it.value } ?: 0f,
            actualProgress.maxOfOrNull { it.value } ?: 0f
        ) * 1.1f
        
        return ProjectionChartData(
            actualProgress = actualProgress,
            expectedProgress = expectedProgress,
            requiredPace = requiredPacePoints,
            projectedProgress = projectedProgress,
            goalTarget = totalTarget,
            maxValue = maxValue
        )
    }

    private fun setToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    // ==================== CONFIDENCE CALCULATION ====================

    private fun calculateConfidence(dataPoints: Int, consistencyScore: Float): Float {
        // More data = higher confidence
        val dataConfidence = min(100f, dataPoints * 10f)
        // Consistency also matters
        val combinedConfidence = (dataConfidence * 0.6f + consistencyScore * 0.4f)
        return combinedConfidence.coerceIn(0f, 100f)
    }

    // ==================== BATCH CALCULATIONS ====================

    fun calculateAllProjections(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>
    ): List<GoalProjection> {
        return goals.map { goal ->
            calculateProjection(goal, progressData)
        }
    }

    // ==================== SUMMARY STATISTICS ====================

    data class ProjectionSummary(
        val totalGoals: Int,
        val onTrackCount: Int,
        val needsAttentionCount: Int,
        val behindScheduleCount: Int,
        val completedCount: Int,
        val overallHealthScore: Int,  // 0-100
        val averageProgress: Float,
        val mostAtRisk: GoalProjection?,
        val bestPerforming: GoalProjection?
    )

    fun calculateSummary(projections: List<GoalProjection>): ProjectionSummary {
        val onTrack = projections.count { it.status == GoalStatus.ON_TRACK }
        val needsAttention = projections.count { it.status == GoalStatus.SLIGHT_DELAY }
        val behind = projections.count { it.status == GoalStatus.BEHIND_SCHEDULE }
        val completed = projections.count { it.status == GoalStatus.COMPLETED }
        
        // Health score weighted by status
        val healthScore = if (projections.isNotEmpty()) {
            val score = (completed * 100 + onTrack * 80 + needsAttention * 50 + behind * 20) / projections.size
            score.coerceIn(0, 100)
        } else 100
        
        val avgProgress = if (projections.isNotEmpty()) {
            projections.map { it.percentCompleted }.average().toFloat()
        } else 0f
        
        val activeProjections = projections.filter { it.status != GoalStatus.COMPLETED }
        val mostAtRisk = activeProjections.minByOrNull { it.percentCompleted - it.percentTimeElapsed }
        val bestPerforming = activeProjections.maxByOrNull { it.percentCompleted - it.percentTimeElapsed }
        
        return ProjectionSummary(
            totalGoals = projections.size,
            onTrackCount = onTrack,
            needsAttentionCount = needsAttention,
            behindScheduleCount = behind,
            completedCount = completed,
            overallHealthScore = healthScore,
            averageProgress = avgProgress,
            mostAtRisk = mostAtRisk,
            bestPerforming = bestPerforming
        )
    }
}
