package com.example.todoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.repository.DailyProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DailyProgressViewModel(private val repository: DailyProgressRepository) : ViewModel() {

    fun getProgressByDate(date: Long): Flow<List<DailyProgressEntity>> {
        return repository.getProgressByDate(date)
    }

    fun getProgressByGoal(goalId: Long): Flow<List<DailyProgressEntity>> {
        return repository.getProgressByGoal(goalId)
    }

    fun insert(progress: DailyProgressEntity) = viewModelScope.launch {
        repository.insertProgress(progress)
    }
}

class DailyProgressViewModelFactory(private val repository: DailyProgressRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DailyProgressViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
