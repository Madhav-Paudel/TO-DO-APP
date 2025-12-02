package com.example.todoapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todoapp.ToDoApplication
import com.example.todoapp.data.local.PhoneUsageEntity
import kotlinx.coroutines.flow.first
import java.util.Calendar

class PhoneUsageWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "phone_usage_channel"
        const val NOTIFICATION_ID = 2000
        const val PHONE_USAGE_THRESHOLD_MINUTES = 30
    }

    override suspend fun doWork(): Result {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return Result.failure()
            }
        }

        val container = (context.applicationContext as ToDoApplication).container
        val phoneUsageRepository = container.phoneUsageRepository
        val goalRepository = container.goalRepository
        val dailyProgressRepository = container.dailyProgressRepository

        val today = getStartOfDay()

        try {
            // Get phone usage from UsageStatsManager
            val usageMinutes = getPhoneUsageMinutes()

            // Update phone usage in database
            val usageEntity = PhoneUsageEntity(
                date = today,
                totalMinutesUsed = usageMinutes
            )
            phoneUsageRepository.insertUsage(usageEntity)

            // Check if we should show notification
            if (usageMinutes >= PHONE_USAGE_THRESHOLD_MINUTES) {
                // Check if any goal hasn't met target today
                val activeGoals = goalRepository.allActiveGoals.first()
                
                for (goal in activeGoals) {
                    val progressList = dailyProgressRepository.getProgressByGoal(goal.id).first()
                    val todayProgress = progressList.find { it.date == today }
                    val targetMet = todayProgress?.wasTargetMet ?: false

                    if (!targetMet) {
                        showNotification(
                            title = "Phone Usage Alert",
                            message = "You've used your phone for $usageMinutes+ min. Maybe study '${goal.title}' now? 📚"
                        )
                        break // Only show one notification
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    private fun getPhoneUsageMinutes(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return 0
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0

            val today = getStartOfDay()
            val now = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                today,
                now
            )

            if (stats.isNullOrEmpty()) {
                return 0
            }

            val totalTimeMs = stats.sumOf { it.totalTimeInForeground }
            return (totalTimeMs / 1000 / 60).toInt()
        } catch (e: SecurityException) {
            // Permission not granted
            return 0
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phone Usage Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when phone usage exceeds limit"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
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
