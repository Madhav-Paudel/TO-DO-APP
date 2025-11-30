package com.example.todoapp.util

import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.local.PhoneUsageEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Analytics utility functions for calculating various metrics
 */
object AnalyticsUtils {

    /**
     * Calculate overall goal progress percentage (from start to end date)
     */
    fun calculateOverallGoalProgress(
        goal: GoalEntity,
        progressList: List<DailyProgressEntity>
    ): Float {
        val now = System.currentTimeMillis()
        val totalDays = TimeUnit.MILLISECONDS.toDays(goal.endDate - goal.startDate).toInt().coerceAtLeast(1)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(now - goal.startDate).toInt().coerceIn(0, totalDays)
        
        if (elapsedDays == 0) return 0f
        
        val expectedMinutes = elapsedDays * goal.dailyTargetMinutes
        val actualMinutes = progressList.filter { 
            it.goalId == goal.id && it.date >= goal.startDate && it.date <= now 
        }.sumOf { it.minutesDone }
        
        return (actualMinutes.toFloat() / expectedMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    /**
     * Calculate days remaining until goal end date
     */
    fun calculateDaysRemaining(goal: GoalEntity): Int {
        val now = System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(goal.endDate - now).toInt().coerceAtLeast(0)
    }

    /**
     * Calculate current streak for a goal
     */
    fun calculateStreak(goalId: Long, progressList: List<DailyProgressEntity>): Int {
        val goalProgress = progressList.filter { it.goalId == goalId }
            .sortedByDescending { it.date }
        
        var streak = 0
        var expectedDate = getStartOfDay()
        
        for (progress in goalProgress) {
            if (progress.date == expectedDate && progress.wasTargetMet) {
                streak++
                expectedDate -= TimeUnit.DAYS.toMillis(1)
            } else if (progress.date < expectedDate) {
                // Check if we missed a day
                val daysDiff = TimeUnit.MILLISECONDS.toDays(expectedDate - progress.date).toInt()
                if (daysDiff > 1) break // Gap in streak
                if (progress.wasTargetMet) {
                    streak++
                    expectedDate = progress.date - TimeUnit.DAYS.toMillis(1)
                } else {
                    break
                }
            }
        }
        
        return streak
    }

    /**
     * Get last N days streak data as boolean array (true = target met)
     */
    fun getStreakSparklineData(goalId: Long, progressList: List<DailyProgressEntity>, days: Int = 10): List<Boolean> {
        val result = mutableListOf<Boolean>()
        val today = getStartOfDay()
        
        for (i in (days - 1) downTo 0) {
            val date = today - TimeUnit.DAYS.toMillis(i.toLong())
            val progress = progressList.find { it.goalId == goalId && it.date == date }
            result.add(progress?.wasTargetMet ?: false)
        }
        
        return result
    }

    /**
     * Calculate daily completion for a goal today
     */
    fun calculateDailyCompletion(goalId: Long, progressList: List<DailyProgressEntity>): Pair<Int, Boolean> {
        val today = getStartOfDay()
        val todayProgress = progressList.find { it.goalId == goalId && it.date == today }
        return Pair(todayProgress?.minutesDone ?: 0, todayProgress?.wasTargetMet ?: false)
    }

    /**
     * Get study minutes for a specific period
     */
    fun getStudyMinutesForPeriod(
        progressList: List<DailyProgressEntity>,
        days: Int
    ): List<DailyDataPoint> {
        val today = getStartOfDay()
        val startDate = today - TimeUnit.DAYS.toMillis((days - 1).toLong())
        
        return (0 until days).map { dayOffset ->
            val date = startDate + TimeUnit.DAYS.toMillis(dayOffset.toLong())
            val minutes = progressList
                .filter { it.date == date }
                .sumOf { it.minutesDone }
            DailyDataPoint(date, minutes)
        }
    }

    /**
     * Get phone usage for a specific period
     */
    fun getPhoneUsageForPeriod(
        usageList: List<PhoneUsageEntity>,
        days: Int
    ): List<DailyDataPoint> {
        val today = getStartOfDay()
        val startDate = today - TimeUnit.DAYS.toMillis((days - 1).toLong())
        
        return (0 until days).map { dayOffset ->
            val date = startDate + TimeUnit.DAYS.toMillis(dayOffset.toLong())
            val minutes = usageList
                .filter { it.date == date }
                .sumOf { it.totalMinutesUsed }
            DailyDataPoint(date, minutes)
        }
    }

    /**
     * Get phone usage status color category
     */
    fun getPhoneUsageStatus(minutes: Int): UsageStatus {
        return when {
            minutes < 30 -> UsageStatus.GOOD
            minutes < 60 -> UsageStatus.WARNING
            else -> UsageStatus.DANGER
        }
    }

    /**
     * Get start of today (midnight)
     */
    fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Format minutes to readable string
     */
    fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Get day label from timestamp
     */
    fun getDayLabel(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
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
}

data class DailyDataPoint(
    val date: Long,
    val value: Int
)

enum class UsageStatus {
    GOOD, WARNING, DANGER
}

data class GoalProgressData(
    val goal: GoalEntity,
    val overallProgress: Float,
    val daysRemaining: Int,
    val currentStreak: Int,
    val todayMinutes: Int,
    val todayCompleted: Boolean,
    val sparklineData: List<Boolean>
)
