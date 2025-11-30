package com.example.todoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.TaskEntity
import com.example.todoapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    fun getTasksByGoal(goalId: Long): Flow<List<TaskEntity>> {
        return repository.getTasksByGoal(goalId)
    }

    fun getTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>> {
        return repository.getTasksByDate(startOfDay, endOfDay)
    }

    fun getOverdueTasks(currentTime: Long): Flow<List<TaskEntity>> {
        return repository.getOverdueTasks(currentTime)
    }

    fun insert(task: TaskEntity) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun update(task: TaskEntity) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun delete(task: TaskEntity) = viewModelScope.launch {
        repository.deleteTask(task)
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
