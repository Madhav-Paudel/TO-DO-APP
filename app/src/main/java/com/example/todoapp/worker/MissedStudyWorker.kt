package com.example.todoapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todoapp.ToDoApplication
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MissedStudyWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "missed_study_channel"
        const val NOTIFICATION_ID_BASE = 1000
    }

    override suspend fun doWork(): Result {
        val container = (context.applicationContext as ToDoApplication).container
        val goalRepository = container.goalRepository
        val dailyProgressRepository = container.dailyProgressRepository

        val today = getStartOfDay()

        try {
            val activeGoals = goalRepository.allActiveGoals.first()

            for (goal in activeGoals) {
                val progressList = dailyProgressRepository.getProgressByGoal(goal.id).first()
                val todayProgress = progressList.find { it.date == today }

                val minutesDone = todayProgress?.minutesDone ?: 0
                val targetMet = todayProgress?.wasTargetMet ?: false

                if (!targetMet && minutesDone < goal.dailyTargetMinutes) {
                    val remainingMinutes = goal.dailyTargetMinutes - minutesDone
                    showNotification(
                        goalId = goal.id.toInt(),
                        title = "Study Reminder",
                        message = "You haven't completed '${goal.title}' today. $remainingMinutes minutes remaining! ✅"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    private fun showNotification(goalId: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for missed study sessions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + goalId, notification)
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
