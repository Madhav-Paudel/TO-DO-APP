package com.example.todoapp.util

import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.local.PhoneUsageEntity
import com.example.todoapp.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * AI-powered Analytics Engine
 * Generates insights, predictions, and suggestions based on user activity data
 */
object AiAnalyticsEngine {

    // ==================== DATA MODELS ====================

    enum class InsightType {
        POSITIVE, NEUTRAL, WARNING, CRITICAL, ACHIEVEMENT
    }

    enum class InsightCategory {
        PRODUCTIVITY, HABITS, GOALS, PHONE_USAGE, STREAK, IMPROVEMENT
    }

    enum class PredictionConfidence {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }

    enum class SuggestionPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    data class AiInsight(
        val id: String = System.currentTimeMillis().toString(),
        val title: String,
        val description: String,
        val icon: String,
        val type: InsightType,
        val category: InsightCategory,
        val metric: String? = null,
        val metricValue: String? = null,
        val trend: Float? = null // Positive = improving, negative = declining
    )

    data class AiPrediction(
        val id: String = System.currentTimeMillis().toString(),
        val title: String,
        val description: String,
        val icon: String,
        val confidence: PredictionConfidence,
        val probability: Int, // 0-100
        val timeframe: String,
        val supportingData: String? = null
    )

    data class AiSuggestion(
        val id: String = System.currentTimeMillis().toString(),
        val title: String,
        val description: String,
        val icon: String,
        val priority: SuggestionPriority,
        val actionLabel: String,
        val category: InsightCategory
    )

    data class AnalyticsReport(
        val insights: List<AiInsight>,
        val predictions: List<AiPrediction>,
        val suggestions: List<AiSuggestion>,
        val summary: String,
        val overallScore: Int, // 0-100
        val generatedAt: Long = System.currentTimeMillis()
    )

    // ==================== MAIN ANALYSIS FUNCTION ====================

    fun generateAnalyticsReport(
        goals: List<GoalEntity>,
        tasks: List<TaskEntity>,
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>
    ): AnalyticsReport {
        val insights = mutableListOf<AiInsight>()
        val predictions = mutableListOf<AiPrediction>()
        val suggestions = mutableListOf<AiSuggestion>()

        // Generate all types of analytics
        insights.addAll(generateProductivityInsights(progressData, phoneUsageData))
        insights.addAll(generateHabitInsights(progressData))
        insights.addAll(generateGoalInsights(goals, progressData))
        insights.addAll(generateStreakInsights(goals, progressData))

        predictions.addAll(generateGoalPredictions(goals, progressData))
        predictions.addAll(generateBehaviorPredictions(progressData, phoneUsageData))
        predictions.addAll(generateBurnoutPredictions(progressData))

        suggestions.addAll(generateTargetSuggestions(goals, progressData))
        suggestions.addAll(generateScheduleSuggestions(progressData))
        suggestions.addAll(generateRestSuggestions(progressData))
        suggestions.addAll(generateGoalSuggestions(goals, progressData))

        // Calculate overall score
        val overallScore = calculateOverallScore(goals, progressData, phoneUsageData)
        
        // Generate summary
        val summary = generateSummary(insights, predictions, overallScore)

        return AnalyticsReport(
            insights = insights.distinctBy { it.title }.take(10),
            predictions = predictions.distinctBy { it.title }.take(6),
            suggestions = suggestions.distinctBy { it.title }.take(6),
            summary = summary,
            overallScore = overallScore
        )
    }

    // ==================== INSIGHT GENERATORS ====================

    private fun generateProductivityInsights(
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()
        val today = AnalyticsUtils.getStartOfDay()
        val weekAgo = today - TimeUnit.DAYS.toMillis(7)
        val monthAgo = today - TimeUnit.DAYS.toMillis(30)

        // Today's productivity
        val todayMinutes = progressData.filter { it.date == today }.sumOf { it.minutesDone }
        val weekMinutes = progressData.filter { it.date in weekAgo..today }.sumOf { it.minutesDone }
        val monthMinutes = progressData.filter { it.date in monthAgo..today }.sumOf { it.minutesDone }

        // Daily average
        val weekDays = progressData.filter { it.date in weekAgo..today }.groupBy { it.date }.size.coerceAtLeast(1)
        val weeklyAvg = weekMinutes / weekDays

        // Previous week comparison
        val prevWeekStart = weekAgo - TimeUnit.DAYS.toMillis(7)
        val prevWeekMinutes = progressData.filter { it.date in prevWeekStart..weekAgo }.sumOf { it.minutesDone }
        val weekTrend = if (prevWeekMinutes > 0) {
            ((weekMinutes - prevWeekMinutes).toFloat() / prevWeekMinutes * 100)
        } else 0f

        // Today insight
        if (todayMinutes > 0) {
            val type = when {
                todayMinutes >= weeklyAvg * 1.2 -> InsightType.ACHIEVEMENT
                todayMinutes >= weeklyAvg -> InsightType.POSITIVE
                todayMinutes >= weeklyAvg * 0.5 -> InsightType.NEUTRAL
                else -> InsightType.WARNING
            }
            insights.add(
                AiInsight(
                    title = when (type) {
                        InsightType.ACHIEVEMENT -> "Outstanding Day! 🌟"
                        InsightType.POSITIVE -> "Great Progress Today"
                        InsightType.NEUTRAL -> "Keep Going!"
                        else -> "Time to Focus"
                    },
                    description = "You've studied ${AnalyticsUtils.formatMinutes(todayMinutes)} today" +
                            if (todayMinutes >= weeklyAvg) ", beating your daily average!" else ".",
                    icon = if (todayMinutes >= weeklyAvg) "🚀" else "📚",
                    type = type,
                    category = InsightCategory.PRODUCTIVITY,
                    metric = "Today",
                    metricValue = AnalyticsUtils.formatMinutes(todayMinutes)
                )
            )
        }

        // Weekly trend insight
        if (weekMinutes > 0) {
            val trendType = when {
                weekTrend >= 20 -> InsightType.ACHIEVEMENT
                weekTrend >= 0 -> InsightType.POSITIVE
                weekTrend >= -20 -> InsightType.NEUTRAL
                else -> InsightType.WARNING
            }
            insights.add(
                AiInsight(
                    title = when {
                        weekTrend >= 20 -> "Productivity Surge! 📈"
                        weekTrend >= 0 -> "Steady Progress"
                        weekTrend >= -20 -> "Slight Dip This Week"
                        else -> "Let's Bounce Back"
                    },
                    description = if (weekTrend >= 0) {
                        "Your study time is up ${weekTrend.roundToInt()}% compared to last week!"
                    } else {
                        "Your study time is down ${abs(weekTrend).roundToInt()}% from last week."
                    },
                    icon = if (weekTrend >= 0) "📈" else "📉",
                    type = trendType,
                    category = InsightCategory.PRODUCTIVITY,
                    metric = "Weekly Trend",
                    metricValue = "${if (weekTrend >= 0) "+" else ""}${weekTrend.roundToInt()}%",
                    trend = weekTrend
                )
            )
        }

        // Monthly milestone
        if (monthMinutes >= 600) { // 10+ hours in a month
            val hours = monthMinutes / 60
            insights.add(
                AiInsight(
                    title = "Monthly Milestone: ${hours}h+ Studied! 🏆",
                    description = "You've dedicated $hours hours to learning this month. That's impressive commitment!",
                    icon = "🏆",
                    type = InsightType.ACHIEVEMENT,
                    category = InsightCategory.PRODUCTIVITY,
                    metric = "This Month",
                    metricValue = "${hours}h"
                )
            )
        }

        // Phone usage vs study correlation
        val todayPhone = phoneUsageData.filter { it.date == today }.sumOf { it.totalMinutesUsed }
        if (todayPhone > 0 && todayMinutes > 0) {
            val ratio = todayMinutes.toFloat() / todayPhone
            if (ratio >= 2) {
                insights.add(
                    AiInsight(
                        title = "Focus Champion! 🎯",
                        description = "You studied ${ratio.roundToInt()}x more than your phone usage today!",
                        icon = "🎯",
                        type = InsightType.ACHIEVEMENT,
                        category = InsightCategory.PHONE_USAGE
                    )
                )
            } else if (ratio < 0.5 && todayPhone > 60) {
                insights.add(
                    AiInsight(
                        title = "Phone vs Study Balance ⚖️",
                        description = "Your phone usage (${todayPhone}m) exceeds study time. Consider a digital detox session.",
                        icon = "⚖️",
                        type = InsightType.WARNING,
                        category = InsightCategory.PHONE_USAGE
                    )
                )
            }
        }

        return insights
    }

    private fun generateHabitInsights(progressData: List<DailyProgressEntity>): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()
        val today = AnalyticsUtils.getStartOfDay()
        val monthAgo = today - TimeUnit.DAYS.toMillis(30)
        
        val recentData = progressData.filter { it.date in monthAgo..today }
        if (recentData.isEmpty()) return insights

        // Analyze study patterns by day of week
        val calendar = Calendar.getInstance()
        val byDayOfWeek = recentData.groupBy { 
            calendar.timeInMillis = it.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        val dayAverages = byDayOfWeek.mapValues { (_, entries) ->
            entries.sumOf { it.minutesDone } / entries.groupBy { it.date }.size.coerceAtLeast(1)
        }

        // Find best and worst days
        val bestDay = dayAverages.maxByOrNull { it.value }
        val worstDay = dayAverages.minByOrNull { it.value }

        if (bestDay != null && bestDay.value > 0) {
            val dayName = getDayName(bestDay.key)
            insights.add(
                AiInsight(
                    title = "Peak Performance Day: $dayName 🌟",
                    description = "You're most productive on ${dayName}s, averaging ${bestDay.value} minutes of study.",
                    icon = "🌟",
                    type = InsightType.POSITIVE,
                    category = InsightCategory.HABITS,
                    metric = dayName,
                    metricValue = "${bestDay.value}m avg"
                )
            )
        }

        // Weekend vs weekday analysis
        val weekdayAvg = dayAverages.filter { it.key in Calendar.MONDAY..Calendar.FRIDAY }
            .values.average().takeIf { !it.isNaN() } ?: 0.0
        val weekendAvg = dayAverages.filter { it.key in listOf(Calendar.SATURDAY, Calendar.SUNDAY) }
            .values.average().takeIf { !it.isNaN() } ?: 0.0

        if (weekdayAvg > 0 && weekendAvg > 0) {
            val diff = ((weekendAvg - weekdayAvg) / weekdayAvg * 100).roundToInt()
            if (abs(diff) > 30) {
                insights.add(
                    AiInsight(
                        title = if (diff < 0) "Weekend Motivation Dip" else "Weekend Warrior! 💪",
                        description = if (diff < 0) {
                            "Your weekend study drops by ${abs(diff)}% compared to weekdays."
                        } else {
                            "You study ${diff}% more on weekends than weekdays!"
                        },
                        icon = if (diff < 0) "📉" else "💪",
                        type = if (diff < 0) InsightType.NEUTRAL else InsightType.POSITIVE,
                        category = InsightCategory.HABITS,
                        trend = diff.toFloat()
                    )
                )
            }
        }

        // Consistency check
        val daysWithStudy = recentData.groupBy { it.date }.count { it.value.sumOf { p -> p.minutesDone } > 0 }
        val totalDays = ((today - monthAgo) / TimeUnit.DAYS.toMillis(1)).toInt().coerceAtLeast(1)
        val consistencyRate = (daysWithStudy.toFloat() / totalDays * 100).roundToInt()

        insights.add(
            AiInsight(
                title = when {
                    consistencyRate >= 80 -> "Incredible Consistency! 🔥"
                    consistencyRate >= 60 -> "Building Good Habits"
                    consistencyRate >= 40 -> "Room for Improvement"
                    else -> "Let's Build Consistency"
                },
                description = "You studied on $daysWithStudy out of the last $totalDays days ($consistencyRate% consistency).",
                icon = when {
                    consistencyRate >= 80 -> "🔥"
                    consistencyRate >= 60 -> "📊"
                    else -> "💡"
                },
                type = when {
                    consistencyRate >= 80 -> InsightType.ACHIEVEMENT
                    consistencyRate >= 60 -> InsightType.POSITIVE
                    consistencyRate >= 40 -> InsightType.NEUTRAL
                    else -> InsightType.WARNING
                },
                category = InsightCategory.HABITS,
                metric = "Consistency",
                metricValue = "$consistencyRate%"
            )
        )

        return insights
    }

    private fun generateGoalInsights(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()
        
        goals.forEach { goal ->
            val goalProgress = progressData.filter { it.goalId == goal.id }
            if (goalProgress.isEmpty()) return@forEach

            val overallProgress = AnalyticsUtils.calculateOverallGoalProgress(goal, progressData)
            val daysRemaining = AnalyticsUtils.calculateDaysRemaining(goal)
            
            // Calculate trend (last 7 days vs previous 7 days)
            val today = AnalyticsUtils.getStartOfDay()
            val weekAgo = today - TimeUnit.DAYS.toMillis(7)
            val twoWeeksAgo = weekAgo - TimeUnit.DAYS.toMillis(7)

            val thisWeekMinutes = goalProgress.filter { it.date in weekAgo..today }.sumOf { it.minutesDone }
            val lastWeekMinutes = goalProgress.filter { it.date in twoWeeksAgo..weekAgo }.sumOf { it.minutesDone }
            
            val trend = if (lastWeekMinutes > 0) {
                ((thisWeekMinutes - lastWeekMinutes).toFloat() / lastWeekMinutes * 100).roundToInt()
            } else if (thisWeekMinutes > 0) 100 else 0

            if (trend != 0 && (thisWeekMinutes > 0 || lastWeekMinutes > 0)) {
                insights.add(
                    AiInsight(
                        title = if (trend >= 0) {
                            "${goal.title} is Rising! 📈"
                        } else {
                            "${goal.title} Needs Attention"
                        },
                        description = if (trend >= 0) {
                            "Your progress on this goal is up ${trend}% this week!"
                        } else {
                            "Progress has slowed by ${abs(trend)}%. Time to refocus!"
                        },
                        icon = if (trend >= 0) "📈" else "📉",
                        type = if (trend >= 0) InsightType.POSITIVE else InsightType.WARNING,
                        category = InsightCategory.GOALS,
                        metric = goal.title,
                        metricValue = "${if (trend >= 0) "+" else ""}$trend%",
                        trend = trend.toFloat()
                    )
                )
            }

            // Milestone achievements
            val progressPercent = (overallProgress * 100).roundToInt()
            val milestones = listOf(25, 50, 75, 90)
            milestones.forEach { milestone ->
                if (progressPercent >= milestone && progressPercent < milestone + 5) {
                    insights.add(
                        AiInsight(
                            title = "🎉 $milestone% Milestone Reached!",
                            description = "You've completed $milestone% of '${goal.title}'! Keep pushing!",
                            icon = "🎉",
                            type = InsightType.ACHIEVEMENT,
                            category = InsightCategory.GOALS,
                            metric = goal.title,
                            metricValue = "$milestone%"
                        )
                    )
                }
            }
        }

        return insights
    }

    private fun generateStreakInsights(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        goals.forEach { goal ->
            val streak = AnalyticsUtils.calculateStreak(goal.id, progressData)
            
            // Streak milestones
            val streakMilestones = listOf(3, 7, 14, 21, 30, 60, 90)
            streakMilestones.forEach { milestone ->
                if (streak == milestone) {
                    insights.add(
                        AiInsight(
                            title = "$milestone-Day Streak! 🔥",
                            description = "Amazing! You've maintained '${goal.title}' for $milestone days straight!",
                            icon = when {
                                milestone >= 30 -> "🏆"
                                milestone >= 14 -> "🔥"
                                else -> "⭐"
                            },
                            type = InsightType.ACHIEVEMENT,
                            category = InsightCategory.STREAK,
                            metric = goal.title,
                            metricValue = "$streak days"
                        )
                    )
                }
            }

            // Streak at risk (missed yesterday)
            val today = AnalyticsUtils.getStartOfDay()
            val yesterday = today - TimeUnit.DAYS.toMillis(1)
            val yesterdayProgress = progressData.find { it.goalId == goal.id && it.date == yesterday }
            val todayProgress = progressData.find { it.goalId == goal.id && it.date == today }

            if (streak > 3 && yesterdayProgress?.wasTargetMet == false && todayProgress == null) {
                insights.add(
                    AiInsight(
                        title = "Streak at Risk! ⚠️",
                        description = "Your ${streak + 1}-day streak for '${goal.title}' needs attention today!",
                        icon = "⚠️",
                        type = InsightType.WARNING,
                        category = InsightCategory.STREAK,
                        metric = goal.title,
                        metricValue = "$streak days at risk"
                    )
                )
            }
        }

        return insights
    }

    // ==================== PREDICTION GENERATORS ====================

    private fun generateGoalPredictions(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>
    ): List<AiPrediction> {
        val predictions = mutableListOf<AiPrediction>()

        goals.forEach { goal ->
            val goalProgress = progressData.filter { it.goalId == goal.id }
            if (goalProgress.isEmpty()) return@forEach

            val daysRemaining = AnalyticsUtils.calculateDaysRemaining(goal)
            val overallProgress = AnalyticsUtils.calculateOverallGoalProgress(goal, progressData)

            // Calculate average daily progress
            val today = AnalyticsUtils.getStartOfDay()
            val weekAgo = today - TimeUnit.DAYS.toMillis(7)
            val recentProgress = goalProgress.filter { it.date in weekAgo..today }
            val avgDailyMinutes = if (recentProgress.isNotEmpty()) {
                recentProgress.sumOf { it.minutesDone } / recentProgress.groupBy { it.date }.size.coerceAtLeast(1)
            } else 0

            // Predict completion
            val remainingTarget = ((1 - overallProgress) * 100).roundToInt()
            val daysNeeded = if (avgDailyMinutes > 0 && goal.dailyTargetMinutes > 0) {
                ((remainingTarget * goal.dailyTargetMinutes) / avgDailyMinutes / 100.0).roundToInt()
            } else daysRemaining * 2

            val willComplete = daysNeeded <= daysRemaining
            val confidence = when {
                avgDailyMinutes >= goal.dailyTargetMinutes * 1.2 -> PredictionConfidence.VERY_HIGH
                avgDailyMinutes >= goal.dailyTargetMinutes -> PredictionConfidence.HIGH
                avgDailyMinutes >= goal.dailyTargetMinutes * 0.7 -> PredictionConfidence.MEDIUM
                else -> PredictionConfidence.LOW
            }

            predictions.add(
                AiPrediction(
                    title = if (willComplete) "On Track: ${goal.title} ✓" else "At Risk: ${goal.title}",
                    description = if (willComplete) {
                        "At your current pace, you'll complete this goal ${daysRemaining - daysNeeded} days early!"
                    } else {
                        "You may need ${daysNeeded - daysRemaining} extra days at current pace."
                    },
                    icon = if (willComplete) "✅" else "⚠️",
                    confidence = confidence,
                    probability = if (willComplete) {
                        (80 + (avgDailyMinutes.toFloat() / goal.dailyTargetMinutes * 20).coerceAtMost(20f)).roundToInt()
                    } else {
                        (60 - (daysNeeded - daysRemaining).coerceAtMost(30)).coerceAtLeast(20)
                    },
                    timeframe = "$daysRemaining days remaining",
                    supportingData = "Avg: ${avgDailyMinutes}m/day | Target: ${goal.dailyTargetMinutes}m/day"
                )
            )
        }

        return predictions
    }

    private fun generateBehaviorPredictions(
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>
    ): List<AiPrediction> {
        val predictions = mutableListOf<AiPrediction>()
        val today = AnalyticsUtils.getStartOfDay()
        val monthAgo = today - TimeUnit.DAYS.toMillis(30)

        val recentProgress = progressData.filter { it.date in monthAgo..today }
        if (recentProgress.isEmpty()) return predictions

        // Predict best study time (based on which days have highest completion)
        val calendar = Calendar.getInstance()
        val byDayOfWeek = recentProgress.groupBy {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        val completionRates = byDayOfWeek.mapValues { (_, entries) ->
            val completed = entries.count { it.wasTargetMet }
            val total = entries.groupBy { it.date }.size
            if (total > 0) completed * 100 / total else 0
        }

        val bestDays = completionRates.filter { it.value >= 70 }.keys.map { getDayName(it) }
        if (bestDays.isNotEmpty()) {
            predictions.add(
                AiPrediction(
                    title = "Best Days for Learning 📅",
                    description = "You're most likely to hit targets on ${bestDays.joinToString(", ")}.",
                    icon = "📅",
                    confidence = PredictionConfidence.HIGH,
                    probability = 85,
                    timeframe = "Based on 30-day analysis"
                )
            )
        }

        // Predict phone impact
        val phoneData = phoneUsageData.filter { it.date in monthAgo..today }
        if (phoneData.isNotEmpty()) {
            val avgPhone = phoneData.sumOf { it.totalMinutesUsed } / phoneData.groupBy { it.date }.size.coerceAtLeast(1)
            val highPhoneDays = phoneData.groupBy { it.date }
                .filter { (_, usage) -> usage.sumOf { it.totalMinutesUsed } > avgPhone * 1.5 }
                .keys

            val studyOnHighPhoneDays = recentProgress.filter { it.date in highPhoneDays }
            val avgStudyHighPhone = if (studyOnHighPhoneDays.isNotEmpty()) {
                studyOnHighPhoneDays.sumOf { it.minutesDone } / studyOnHighPhoneDays.groupBy { it.date }.size.coerceAtLeast(1)
            } else 0

            val avgStudyNormal = recentProgress.filter { it.date !in highPhoneDays }
                .let { if (it.isNotEmpty()) it.sumOf { p -> p.minutesDone } / it.groupBy { p -> p.date }.size.coerceAtLeast(1) else 0 }

            if (avgStudyHighPhone < avgStudyNormal && avgStudyNormal > 0) {
                val drop = ((avgStudyNormal - avgStudyHighPhone).toFloat() / avgStudyNormal * 100).roundToInt()
                predictions.add(
                    AiPrediction(
                        title = "Phone Usage Impact 📱",
                        description = "High phone usage days see ~$drop% less study time.",
                        icon = "📱",
                        confidence = PredictionConfidence.MEDIUM,
                        probability = 75,
                        timeframe = "30-day correlation"
                    )
                )
            }
        }

        return predictions
    }

    private fun generateBurnoutPredictions(progressData: List<DailyProgressEntity>): List<AiPrediction> {
        val predictions = mutableListOf<AiPrediction>()
        val today = AnalyticsUtils.getStartOfDay()
        val twoWeeksAgo = today - TimeUnit.DAYS.toMillis(14)

        val recentProgress = progressData.filter { it.date in twoWeeksAgo..today }
        if (recentProgress.isEmpty()) return predictions

        // Check for declining trend over 2 weeks
        val firstWeek = recentProgress.filter { it.date < today - TimeUnit.DAYS.toMillis(7) }
        val secondWeek = recentProgress.filter { it.date >= today - TimeUnit.DAYS.toMillis(7) }

        val firstWeekAvg = if (firstWeek.isNotEmpty()) {
            firstWeek.sumOf { it.minutesDone } / firstWeek.groupBy { it.date }.size.coerceAtLeast(1)
        } else 0
        
        val secondWeekAvg = if (secondWeek.isNotEmpty()) {
            secondWeek.sumOf { it.minutesDone } / secondWeek.groupBy { it.date }.size.coerceAtLeast(1)
        } else 0

        // Burnout pattern: declining productivity + high initial output
        if (firstWeekAvg > 60 && secondWeekAvg < firstWeekAvg * 0.6) {
            val decline = ((firstWeekAvg - secondWeekAvg).toFloat() / firstWeekAvg * 100).roundToInt()
            predictions.add(
                AiPrediction(
                    title = "Potential Burnout Pattern 🔋",
                    description = "Your productivity dropped $decline% in the past week. Consider taking a rest day.",
                    icon = "🔋",
                    confidence = PredictionConfidence.MEDIUM,
                    probability = 70,
                    timeframe = "2-week analysis"
                )
            )
        }

        // Check for extended breaks
        val daysSinceStudy = progressData
            .filter { it.minutesDone > 0 }
            .maxByOrNull { it.date }
            ?.let { (today - it.date) / TimeUnit.DAYS.toMillis(1) }
            ?.toInt() ?: 0

        if (daysSinceStudy >= 3) {
            predictions.add(
                AiPrediction(
                    title = "Extended Break Detected 📆",
                    description = "It's been $daysSinceStudy days since your last study session. Ready to get back?",
                    icon = "📆",
                    confidence = PredictionConfidence.VERY_HIGH,
                    probability = 95,
                    timeframe = "Starting today"
                )
            )
        }

        // Overwork detection
        val dailyMinutes = recentProgress.groupBy { it.date }.mapValues { it.value.sumOf { p -> p.minutesDone } }
        val consecutiveHighDays = dailyMinutes.values.windowed(5).count { window ->
            window.all { it > 120 } // More than 2 hours daily for 5+ days
        }

        if (consecutiveHighDays >= 1) {
            predictions.add(
                AiPrediction(
                    title = "Overwork Warning ⚡",
                    description = "You've been studying intensively. A rest day could boost long-term performance.",
                    icon = "⚡",
                    confidence = PredictionConfidence.HIGH,
                    probability = 80,
                    timeframe = "Recommend rest within 2 days"
                )
            )
        }

        return predictions
    }

    // ==================== SUGGESTION GENERATORS ====================

    private fun generateTargetSuggestions(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>
    ): List<AiSuggestion> {
        val suggestions = mutableListOf<AiSuggestion>()

        goals.forEach { goal ->
            val goalProgress = progressData.filter { it.goalId == goal.id }
            if (goalProgress.isEmpty()) return@forEach

            val today = AnalyticsUtils.getStartOfDay()
            val weekAgo = today - TimeUnit.DAYS.toMillis(7)
            val recentProgress = goalProgress.filter { it.date in weekAgo..today }

            val avgDaily = if (recentProgress.isNotEmpty()) {
                recentProgress.sumOf { it.minutesDone } / recentProgress.groupBy { it.date }.size.coerceAtLeast(1)
            } else 0

            val targetDiff = avgDaily - goal.dailyTargetMinutes
            val completionRate = recentProgress.count { it.wasTargetMet } * 100 / recentProgress.groupBy { it.date }.size.coerceAtLeast(1)

            // Target too easy
            if (targetDiff > goal.dailyTargetMinutes * 0.3 && completionRate >= 90) {
                val newTarget = (avgDaily * 0.9).roundToInt()
                suggestions.add(
                    AiSuggestion(
                        title = "Increase Target for '${goal.title}'",
                        description = "You're exceeding your target consistently! Consider raising it from ${goal.dailyTargetMinutes}m to ${newTarget}m.",
                        icon = "📈",
                        priority = SuggestionPriority.MEDIUM,
                        actionLabel = "Adjust Target",
                        category = InsightCategory.GOALS
                    )
                )
            }

            // Target too hard
            if (completionRate < 30 && avgDaily > 0) {
                val newTarget = (avgDaily * 1.1).roundToInt().coerceAtLeast(10)
                suggestions.add(
                    AiSuggestion(
                        title = "Adjust Target for '${goal.title}'",
                        description = "Current target may be too ambitious. Try ${newTarget}m/day to build momentum.",
                        icon = "🎯",
                        priority = SuggestionPriority.HIGH,
                        actionLabel = "Lower Target",
                        category = InsightCategory.GOALS
                    )
                )
            }
        }

        return suggestions
    }

    private fun generateScheduleSuggestions(progressData: List<DailyProgressEntity>): List<AiSuggestion> {
        val suggestions = mutableListOf<AiSuggestion>()
        val today = AnalyticsUtils.getStartOfDay()
        val monthAgo = today - TimeUnit.DAYS.toMillis(30)

        val recentProgress = progressData.filter { it.date in monthAgo..today }
        if (recentProgress.isEmpty()) return suggestions

        val calendar = Calendar.getInstance()
        val byDayOfWeek = recentProgress.groupBy {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        // Find weak days
        val dayStats = byDayOfWeek.mapValues { (_, entries) ->
            val completed = entries.count { it.wasTargetMet }
            val total = entries.groupBy { it.date }.size
            if (total > 0) completed * 100 / total else 0
        }

        val weakDays = dayStats.filter { it.value < 40 }.keys.map { getDayName(it) }
        val strongDays = dayStats.filter { it.value >= 70 }.keys.map { getDayName(it) }

        if (weakDays.isNotEmpty() && strongDays.isNotEmpty()) {
            suggestions.add(
                AiSuggestion(
                    title = "Optimize Your Week",
                    description = "You're strong on ${strongDays.take(2).joinToString(", ")} but struggle on ${weakDays.take(2).joinToString(", ")}. Try scheduling lighter sessions on weak days.",
                    icon = "📅",
                    priority = SuggestionPriority.MEDIUM,
                    actionLabel = "Plan Week",
                    category = InsightCategory.HABITS
                )
            )
        }

        return suggestions
    }

    private fun generateRestSuggestions(progressData: List<DailyProgressEntity>): List<AiSuggestion> {
        val suggestions = mutableListOf<AiSuggestion>()
        val today = AnalyticsUtils.getStartOfDay()
        val weekAgo = today - TimeUnit.DAYS.toMillis(7)

        val recentProgress = progressData.filter { it.date in weekAgo..today }
        val dailyMinutes = recentProgress.groupBy { it.date }.mapValues { it.value.sumOf { p -> p.minutesDone } }

        // Check consecutive high-effort days
        val consecutiveHigh = dailyMinutes.values.count { it >= 90 }
        val totalMinutesWeek = dailyMinutes.values.sum()

        if (consecutiveHigh >= 5) {
            suggestions.add(
                AiSuggestion(
                    title = "Take a Rest Day 🧘",
                    description = "You've had $consecutiveHigh high-intensity days. Rest improves retention and prevents burnout.",
                    icon = "🧘",
                    priority = SuggestionPriority.HIGH,
                    actionLabel = "Schedule Rest",
                    category = InsightCategory.IMPROVEMENT
                )
            )
        }

        if (totalMinutesWeek >= 600) { // 10+ hours in a week
            suggestions.add(
                AiSuggestion(
                    title = "Amazing Week! Now Rest 🌟",
                    description = "You've studied ${totalMinutesWeek / 60}+ hours this week! A light day will help consolidate learning.",
                    icon = "🌟",
                    priority = SuggestionPriority.MEDIUM,
                    actionLabel = "Take It Easy",
                    category = InsightCategory.IMPROVEMENT
                )
            )
        }

        return suggestions
    }

    private fun generateGoalSuggestions(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>
    ): List<AiSuggestion> {
        val suggestions = mutableListOf<AiSuggestion>()

        // Suggest new goal if all current ones are nearly complete
        val nearlyComplete = goals.count { goal ->
            AnalyticsUtils.calculateOverallGoalProgress(goal, progressData) >= 0.8f
        }

        if (nearlyComplete >= goals.size && goals.isNotEmpty()) {
            suggestions.add(
                AiSuggestion(
                    title = "Time for New Challenges! 🚀",
                    description = "Your current goals are ${(nearlyComplete * 100 / goals.size.coerceAtLeast(1))}% near completion. Consider adding a new learning goal!",
                    icon = "🚀",
                    priority = SuggestionPriority.LOW,
                    actionLabel = "Add Goal",
                    category = InsightCategory.GOALS
                )
            )
        }

        // Suggest based on unused categories
        val usedCategories = goals.map { it.category.lowercase() }.toSet()
        val suggestedCategories = listOf("Language", "Programming", "Music", "Fitness", "Reading")
            .filter { it.lowercase() !in usedCategories }

        if (suggestedCategories.isNotEmpty() && goals.size < 5) {
            suggestions.add(
                AiSuggestion(
                    title = "Explore: ${suggestedCategories.first()}",
                    description = "Diversify your learning! How about adding a ${suggestedCategories.first()} goal?",
                    icon = "💡",
                    priority = SuggestionPriority.LOW,
                    actionLabel = "Explore",
                    category = InsightCategory.GOALS
                )
            )
        }

        return suggestions
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun calculateOverallScore(
        goals: List<GoalEntity>,
        progressData: List<DailyProgressEntity>,
        phoneUsageData: List<PhoneUsageEntity>
    ): Int {
        if (goals.isEmpty()) return 50

        val today = AnalyticsUtils.getStartOfDay()
        val weekAgo = today - TimeUnit.DAYS.toMillis(7)

        // Goal progress score (40%)
        val avgGoalProgress = goals.map { 
            AnalyticsUtils.calculateOverallGoalProgress(it, progressData) 
        }.average().takeIf { !it.isNaN() } ?: 0.0
        val goalScore = (avgGoalProgress * 40).roundToInt()

        // Consistency score (30%)
        val recentProgress = progressData.filter { it.date in weekAgo..today }
        val daysWithStudy = recentProgress.groupBy { it.date }.count { it.value.sumOf { p -> p.minutesDone } > 0 }
        val consistencyScore = (daysWithStudy.toFloat() / 7 * 30).roundToInt()

        // Target completion score (20%)
        val targetsMet = recentProgress.count { it.wasTargetMet }
        val totalEntries = recentProgress.size.coerceAtLeast(1)
        val targetScore = (targetsMet.toFloat() / totalEntries * 20).roundToInt()

        // Phone balance score (10%)
        val phoneData = phoneUsageData.filter { it.date in weekAgo..today }
        val avgPhone = if (phoneData.isNotEmpty()) {
            phoneData.sumOf { it.totalMinutesUsed } / phoneData.groupBy { it.date }.size.coerceAtLeast(1)
        } else 30
        val phoneScore = when {
            avgPhone < 30 -> 10
            avgPhone < 60 -> 7
            avgPhone < 90 -> 4
            else -> 0
        }

        return (goalScore + consistencyScore + targetScore + phoneScore).coerceIn(0, 100)
    }

    private fun generateSummary(
        insights: List<AiInsight>,
        predictions: List<AiPrediction>,
        score: Int
    ): String {
        val positive = insights.count { it.type == InsightType.POSITIVE || it.type == InsightType.ACHIEVEMENT }
        val warnings = insights.count { it.type == InsightType.WARNING || it.type == InsightType.CRITICAL }

        return when {
            score >= 80 -> "Outstanding performance! You're crushing your goals. 🏆"
            score >= 60 -> "Great progress! Keep up the momentum. 💪"
            score >= 40 -> "You're making progress. Focus on consistency to level up! 📈"
            else -> "Let's get back on track! Small steps lead to big results. 🎯"
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }
}
