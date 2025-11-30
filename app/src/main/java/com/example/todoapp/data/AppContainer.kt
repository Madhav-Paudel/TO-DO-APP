package com.example.todoapp.data

import android.content.Context
import android.util.Log
import com.example.todoapp.data.local.AppDatabase
import com.example.todoapp.data.local.SettingsDataStore
import com.example.todoapp.data.repository.AssistantMemoryRepository
import com.example.todoapp.data.repository.DailyProgressRepository
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.PhoneUsageRepository
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.data.repository.TimerSessionRepository
import com.example.todoapp.llm.LocalAssistantRepository
import com.example.todoapp.llm.ModelManager

interface AppContainer {
    val goalRepository: GoalRepository
    val taskRepository: TaskRepository
    val dailyProgressRepository: DailyProgressRepository
    val phoneUsageRepository: PhoneUsageRepository
    val settingsDataStore: SettingsDataStore
    val timerSessionRepository: TimerSessionRepository
    val assistantMemoryRepository: AssistantMemoryRepository
    val modelManager: ModelManager
    val localAssistantRepository: LocalAssistantRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val goalRepository: GoalRepository by lazy {
        GoalRepository(AppDatabase.getDatabase(context).goalDao())
    }
    override val taskRepository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getDatabase(context).taskDao())
    }
    override val dailyProgressRepository: DailyProgressRepository by lazy {
        DailyProgressRepository(AppDatabase.getDatabase(context).dailyProgressDao())
    }
    override val phoneUsageRepository: PhoneUsageRepository by lazy {
        PhoneUsageRepository(AppDatabase.getDatabase(context).phoneUsageDao())
    }
    override val timerSessionRepository: TimerSessionRepository by lazy {
        TimerSessionRepository(AppDatabase.getDatabase(context).timerSessionDao())
    }
    override val assistantMemoryRepository: AssistantMemoryRepository by lazy {
        AssistantMemoryRepository(AppDatabase.getDatabase(context).assistantMemoryDao())
    }

    override val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }
    
    // On-device LLM components
    override val modelManager: ModelManager by lazy {
        ModelManager(context)
    }
    
    override val localAssistantRepository: LocalAssistantRepository by lazy {
        LocalAssistantRepository(context, modelManager)
    }
}
