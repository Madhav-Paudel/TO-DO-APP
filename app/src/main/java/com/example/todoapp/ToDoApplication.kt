package com.example.todoapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.todoapp.data.AppContainer
import com.example.todoapp.data.AppDataContainer
import com.example.todoapp.worker.MissedStudyWorker
import com.example.todoapp.worker.PhoneUsageWorker
import java.util.concurrent.TimeUnit

class ToDoApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Schedule MissedStudyWorker - every 2 hours
        val missedStudyRequest = PeriodicWorkRequestBuilder<MissedStudyWorker>(
            2, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "missed_study_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            missedStudyRequest
        )

        // Schedule PhoneUsageWorker - every 15 minutes
        val phoneUsageRequest = PeriodicWorkRequestBuilder<PhoneUsageWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "phone_usage_tracker",
            ExistingPeriodicWorkPolicy.KEEP,
            phoneUsageRequest
        )
    }
}
