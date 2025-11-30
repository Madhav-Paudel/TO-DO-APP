package com.example.todoapp.util

import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ProgressCalculator {

    fun calculateStreak(progressList: List<DailyProgressEntity>): Int {
        if (progressList.isEmpty()) return 0

        val sortedProgress = progressList.sortedByDescending { it.date }
        var streak = 0
        val today = getStartOfDay(System.currentTimeMillis())
        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        // Check if there is an entry for today or yesterday to start the streak
        val latestEntry = sortedProgress.firstOrNull() ?: return 0
        
        // If the latest entry is older than yesterday, streak is broken (unless we want to be lenient)
        // Strictly speaking, if no entry for today or yesterday, streak is 0.
        // However, if today is not over, maybe we shouldn't break it yet if yesterday was done?
        // Let's count backwards from today (or yesterday if today is missing).
        
        var currentCheckDate = today
        
        // If no entry for today, check if we have one for yesterday. If not, streak is 0.
        if (sortedProgress.none { it.date == currentCheckDate && it.wasTargetMet }) {
            currentCheckDate = yesterday
            if (sortedProgress.none { it.date == currentCheckDate && it.wasTargetMet }) {
                return 0
            }
        }

        // Now count backwards
        while (true) {
            val hasEntry = sortedProgress.any { it.date == currentCheckDate && it.wasTargetMet }
            if (hasEntry) {
                streak++
                currentCheckDate -= TimeUnit.DAYS.toMillis(1)
            } else {
                break
            }
        }
        
        return streak
    }

    fun calculateOverallProgress(goal: GoalEntity, progressList: List<DailyProgressEntity>): Float {
        val totalMinutesDone = progressList.sumOf { it.minutesDone }
        
        // Calculate total expected minutes
        // Total days = (endDate - startDate) / 1 day
        // Ensure at least 1 day
        val diff = goal.endDate - goal.startDate
        val totalDays = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(1)
        val totalExpectedMinutes = totalDays * goal.dailyTargetMinutes
        
        if (totalExpectedMinutes == 0L) return 0f
        
        return (totalMinutesDone.toFloat() / totalExpectedMinutes).coerceIn(0f, 1f)
    }

    fun checkDailyTarget(goal: GoalEntity, minutesDone: Int): Boolean {
        return minutesDone >= goal.dailyTargetMinutes
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
