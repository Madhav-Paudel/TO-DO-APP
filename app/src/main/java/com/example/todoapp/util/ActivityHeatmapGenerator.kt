package com.example.todoapp.util

import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.PhoneUsageEntity
import com.example.todoapp.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * GitHub-style Activity Heatmap Generator
 * Generates heatmap data from user activity
 */
object ActivityHeatmapGenerator {

    // ==================== DATA MODELS ====================

    enum class HeatmapView {
        WEEKLY, MONTHLY, YEARLY
    }

    enum class ActivityLevel {
        NONE, LOW, MEDIUM, HIGH, VERY_HIGH
    }

    data class HeatmapCell(
        val date: Long,
        val dayOfWeek: Int, // 1 = Sunday, 7 = Saturday
        val weekIndex: Int,
        val studyMinutes: Int,
        val phoneMinutes: Int,
        val tasksCompleted: Int,
        val activityLevel: ActivityLevel,
        val isToday: Boolean = false,
        val isInCurrentMonth: Boolean = true,
        val formattedDate: String,
        val dayOfMonth: Int
    )

    data class HeatmapData(
        val cells: List<HeatmapCell>,
        val weekCount: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val gaps: List<GapInfo>,
        val totalStudyMinutes: Int,
        val totalActiveDays: Int,
        val monthLabel: String,
        val yearLabel: String
    )

    data class GapInfo(
        val startDate: Long,
        val endDate: Long,
        val dayCount: Int,
        val formattedDate: String
    )

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val streakStartDate: Long?,
        val isActive: Boolean
    )

    // ==================== HEATMAP GENERATION ====================

    fun generateHeatmap(
        view: HeatmapView,
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>,
        tasks: List<TaskEntity>,
        referenceDate: Long = System.currentTimeMillis()
    ): HeatmapData {
        return when (view) {
            HeatmapView.WEEKLY -> generateWeeklyHeatmap(progressData, phoneUsageData, tasks, referenceDate)
            HeatmapView.MONTHLY -> generateMonthlyHeatmap(progressData, phoneUsageData, tasks, referenceDate)
            HeatmapView.YEARLY -> generateYearlyHeatmap(progressData, phoneUsageData, tasks, referenceDate)
        }
    }

    private fun generateWeeklyHeatmap(
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>,
        tasks: List<TaskEntity>,
        referenceDate: Long
    ): HeatmapData {
        val cells = mutableListOf<HeatmapCell>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = referenceDate
        
        // Go to start of current week (Monday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToMonday)
        
        // Go back 4 more weeks (5 weeks total)
        calendar.add(Calendar.WEEK_OF_YEAR, -4)
        setToStartOfDay(calendar)

        val today = getStartOfDay(referenceDate)
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        for (week in 0 until 5) {
            for (day in 0 until 7) {
                val date = calendar.timeInMillis
                val cell = createHeatmapCell(
                    date = date,
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    weekIndex = week,
                    progressData = progressData,
                    phoneUsageData = phoneUsageData,
                    tasks = tasks,
                    today = today,
                    dateFormat = dateFormat,
                    isInCurrentMonth = true
                )
                cells.add(cell)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val streakInfo = calculateStreak(progressData)
        val gaps = findGaps(progressData, cells.first().date, cells.last().date)

        return HeatmapData(
            cells = cells,
            weekCount = 5,
            currentStreak = streakInfo.currentStreak,
            longestStreak = streakInfo.longestStreak,
            gaps = gaps,
            totalStudyMinutes = cells.sumOf { it.studyMinutes },
            totalActiveDays = cells.count { it.studyMinutes > 0 },
            monthLabel = SimpleDateFormat("MMMM", Locale.getDefault()).format(referenceDate),
            yearLabel = SimpleDateFormat("yyyy", Locale.getDefault()).format(referenceDate)
        )
    }

    private fun generateMonthlyHeatmap(
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>,
        tasks: List<TaskEntity>,
        referenceDate: Long
    ): HeatmapData {
        val cells = mutableListOf<HeatmapCell>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = referenceDate
        
        // Go to first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        setToStartOfDay(calendar)

        val currentMonth = calendar.get(Calendar.MONTH)
        val today = getStartOfDay(referenceDate)
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        // Find the Monday before or on the 1st
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        // Generate 6 weeks to cover the full month
        for (week in 0 until 6) {
            for (day in 0 until 7) {
                val date = calendar.timeInMillis
                val isInCurrentMonth = calendar.get(Calendar.MONTH) == currentMonth
                
                val cell = createHeatmapCell(
                    date = date,
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    weekIndex = week,
                    progressData = progressData,
                    phoneUsageData = phoneUsageData,
                    tasks = tasks,
                    today = today,
                    dateFormat = dateFormat,
                    isInCurrentMonth = isInCurrentMonth
                )
                cells.add(cell)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val streakInfo = calculateStreak(progressData)
        val gaps = findGaps(progressData, cells.first().date, cells.last().date)

        return HeatmapData(
            cells = cells,
            weekCount = 6,
            currentStreak = streakInfo.currentStreak,
            longestStreak = streakInfo.longestStreak,
            gaps = gaps,
            totalStudyMinutes = cells.filter { it.isInCurrentMonth }.sumOf { it.studyMinutes },
            totalActiveDays = cells.count { it.isInCurrentMonth && it.studyMinutes > 0 },
            monthLabel = SimpleDateFormat("MMMM", Locale.getDefault()).format(referenceDate),
            yearLabel = SimpleDateFormat("yyyy", Locale.getDefault()).format(referenceDate)
        )
    }

    private fun generateYearlyHeatmap(
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>,
        tasks: List<TaskEntity>,
        referenceDate: Long
    ): HeatmapData {
        val cells = mutableListOf<HeatmapCell>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = referenceDate
        
        // Go back 365 days
        calendar.add(Calendar.DAY_OF_YEAR, -364)
        
        // Find the Sunday/Monday to start
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSunday = if (dayOfWeek == Calendar.SUNDAY) 0 else -(dayOfWeek - Calendar.SUNDAY)
        calendar.add(Calendar.DAY_OF_YEAR, daysToSunday)
        setToStartOfDay(calendar)

        val today = getStartOfDay(referenceDate)
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val startDate = calendar.timeInMillis

        // Calculate weeks needed (approximately 53 weeks for a year)
        val endCalendar = Calendar.getInstance()
        endCalendar.timeInMillis = referenceDate
        val weeksNeeded = 53

        for (week in 0 until weeksNeeded) {
            for (day in 0 until 7) {
                val date = calendar.timeInMillis
                if (date > referenceDate) break
                
                val cell = createHeatmapCell(
                    date = date,
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    weekIndex = week,
                    progressData = progressData,
                    phoneUsageData = phoneUsageData,
                    tasks = tasks,
                    today = today,
                    dateFormat = dateFormat,
                    isInCurrentMonth = true
                )
                cells.add(cell)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val streakInfo = calculateStreak(progressData)
        val gaps = findGaps(progressData, startDate, referenceDate).take(5) // Limit gaps for yearly view

        return HeatmapData(
            cells = cells,
            weekCount = weeksNeeded,
            currentStreak = streakInfo.currentStreak,
            longestStreak = streakInfo.longestStreak,
            gaps = gaps,
            totalStudyMinutes = cells.sumOf { it.studyMinutes },
            totalActiveDays = cells.count { it.studyMinutes > 0 },
            monthLabel = "Last 12 months",
            yearLabel = SimpleDateFormat("yyyy", Locale.getDefault()).format(referenceDate)
        )
    }

    private fun createHeatmapCell(
        date: Long,
        dayOfWeek: Int,
        weekIndex: Int,
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>,
        tasks: List<TaskEntity>,
        today: Long,
        dateFormat: SimpleDateFormat,
        isInCurrentMonth: Boolean
    ): HeatmapCell {
        val dayStart = getStartOfDay(date)
        val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)

        val studyMinutes = progressData
            .filter { it.date == dayStart }
            .sumOf { it.minutesDone }

        val phoneMinutes = phoneUsageData
            .filter { it.date == dayStart }
            .sumOf { it.totalMinutesUsed }

        val tasksCompleted = tasks
            .count { it.isCompleted && it.dueDate in dayStart until dayEnd }

        val activityLevel = calculateActivityLevel(studyMinutes)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date

        return HeatmapCell(
            date = dayStart,
            dayOfWeek = dayOfWeek,
            weekIndex = weekIndex,
            studyMinutes = studyMinutes,
            phoneMinutes = phoneMinutes,
            tasksCompleted = tasksCompleted,
            activityLevel = activityLevel,
            isToday = dayStart == today,
            isInCurrentMonth = isInCurrentMonth,
            formattedDate = dateFormat.format(date),
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun calculateActivityLevel(studyMinutes: Int): ActivityLevel {
        return when {
            studyMinutes == 0 -> ActivityLevel.NONE
            studyMinutes < 15 -> ActivityLevel.LOW
            studyMinutes < 30 -> ActivityLevel.MEDIUM
            studyMinutes < 60 -> ActivityLevel.HIGH
            else -> ActivityLevel.VERY_HIGH
        }
    }

    // ==================== STREAK CALCULATION ====================

    fun calculateStreak(progressData: List<DailyProgressEntity>): StreakInfo {
        if (progressData.isEmpty()) {
            return StreakInfo(0, 0, null, false)
        }

        val today = getStartOfDay(System.currentTimeMillis())
        val sortedDates = progressData
            .filter { it.minutesDone > 0 }
            .map { it.date }
            .distinct()
            .sorted()

        if (sortedDates.isEmpty()) {
            return StreakInfo(0, 0, null, false)
        }

        // Calculate current streak (from today backwards)
        var currentStreak = 0
        var checkDate = today
        
        while (true) {
            val hasActivity = sortedDates.contains(checkDate)
            if (hasActivity) {
                currentStreak++
                checkDate -= TimeUnit.DAYS.toMillis(1)
            } else if (checkDate == today) {
                // Today might not have activity yet, check yesterday
                checkDate -= TimeUnit.DAYS.toMillis(1)
            } else {
                break
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: Long? = null

        for (date in sortedDates) {
            if (previousDate == null || date - previousDate == TimeUnit.DAYS.toMillis(1)) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
            previousDate = date
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        val streakStartDate = if (currentStreak > 0) {
            today - TimeUnit.DAYS.toMillis((currentStreak - 1).toLong())
        } else null

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            streakStartDate = streakStartDate,
            isActive = currentStreak > 0
        )
    }

    // ==================== GAP DETECTION ====================

    private fun findGaps(
        progressData: List<DailyProgressEntity>,
        startDate: Long,
        endDate: Long
    ): List<GapInfo> {
        val gaps = mutableListOf<GapInfo>()
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val activeDates = progressData
            .filter { it.minutesDone > 0 && it.date in startDate..endDate }
            .map { it.date }
            .toSet()

        if (activeDates.isEmpty()) return gaps

        var currentDate = startDate
        var gapStart: Long? = null

        while (currentDate <= endDate) {
            val hasActivity = activeDates.contains(currentDate)
            
            if (!hasActivity && gapStart == null) {
                gapStart = currentDate
            } else if (hasActivity && gapStart != null) {
                val gapDays = TimeUnit.MILLISECONDS.toDays(currentDate - gapStart).toInt()
                if (gapDays >= 2) { // Only count gaps of 2+ days
                    gaps.add(
                        GapInfo(
                            startDate = gapStart,
                            endDate = currentDate - TimeUnit.DAYS.toMillis(1),
                            dayCount = gapDays,
                            formattedDate = dateFormat.format(gapStart)
                        )
                    )
                }
                gapStart = null
            }
            
            currentDate += TimeUnit.DAYS.toMillis(1)
        }

        return gaps.sortedByDescending { it.dayCount }.take(3)
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun setToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        setToStartOfDay(calendar)
        return calendar.timeInMillis
    }

    fun getDayLabel(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "S"
            Calendar.MONDAY -> "M"
            Calendar.TUESDAY -> "T"
            Calendar.WEDNESDAY -> "W"
            Calendar.THURSDAY -> "T"
            Calendar.FRIDAY -> "F"
            Calendar.SATURDAY -> "S"
            else -> ""
        }
    }

    fun getMonthLabels(cells: List<HeatmapCell>): List<Pair<Int, String>> {
        val months = mutableListOf<Pair<Int, String>>()
        val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
        var lastMonth = -1

        cells.groupBy { it.weekIndex }.forEach { (weekIndex, weekCells) ->
            val firstCell = weekCells.firstOrNull()
            if (firstCell != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = firstCell.date
                val month = calendar.get(Calendar.MONTH)
                if (month != lastMonth) {
                    months.add(Pair(weekIndex, dateFormat.format(firstCell.date)))
                    lastMonth = month
                }
            }
        }

        return months
    }
}
