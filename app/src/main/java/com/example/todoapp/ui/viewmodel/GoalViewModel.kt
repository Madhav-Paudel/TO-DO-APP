package com.example.todoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GoalViewModel(private val repository: GoalRepository) : ViewModel() {

    val allActiveGoals: Flow<List<GoalEntity>> = repository.allActiveGoals

    fun insert(goal: GoalEntity) = viewModelScope.launch {
        repository.insertGoal(goal)
    }

    fun update(goal: GoalEntity) = viewModelScope.launch {
        repository.updateGoal(goal)
    }

    fun delete(goal: GoalEntity) = viewModelScope.launch {
        repository.deleteGoal(goal)
    }
}

class GoalViewModelFactory(private val repository: GoalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
