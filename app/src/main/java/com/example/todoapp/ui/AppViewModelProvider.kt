package com.example.todoapp.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.todoapp.ToDoApplication
import com.example.todoapp.ui.viewmodels.AnalyticsViewModel
import com.example.todoapp.ui.viewmodels.AssistantViewModel
import com.example.todoapp.ui.viewmodels.GoalsViewModel
import com.example.todoapp.ui.viewmodels.HomeViewModel
import com.example.todoapp.ui.viewmodels.ModelManagerViewModel
import com.example.todoapp.ui.viewmodels.SettingsViewModel
import com.example.todoapp.ui.viewmodels.TasksViewModel
import com.example.todoapp.ui.viewmodels.TimerViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            GoalsViewModel(
                toDoApplication().container.goalRepository,
                toDoApplication().container.dailyProgressRepository
            )
        }
        initializer {
            TasksViewModel(
                toDoApplication().container.taskRepository,
                toDoApplication().container.goalRepository
            )
        }
        initializer {
            HomeViewModel(
                toDoApplication().container.taskRepository,
                toDoApplication().container.goalRepository,
                toDoApplication().container.dailyProgressRepository,
                toDoApplication().container.phoneUsageRepository
            )
        }
        initializer {
            AnalyticsViewModel(
                toDoApplication().container.dailyProgressRepository,
                toDoApplication().container.phoneUsageRepository,
                toDoApplication().container.goalRepository,
                toDoApplication().container.taskRepository
            )
        }
        initializer {
            TimerViewModel(
                toDoApplication().container.dailyProgressRepository,
                toDoApplication().container.goalRepository,
                toDoApplication().container.timerSessionRepository,
                toDoApplication()
            )
        }
        initializer {
            AssistantViewModel(
                toDoApplication().container.localAssistantRepository,
                toDoApplication().container.goalRepository,
                toDoApplication().container.taskRepository
            )
        }
        initializer {
            SettingsViewModel(
                toDoApplication().container.settingsDataStore,
                toDoApplication().container.modelManager
            )
        }
        initializer {
            ModelManagerViewModel(
                toDoApplication()
            )
        }
    }
}

fun CreationExtras.toDoApplication(): ToDoApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ToDoApplication)
