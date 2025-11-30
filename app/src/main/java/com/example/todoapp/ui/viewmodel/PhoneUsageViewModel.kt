package com.example.todoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.PhoneUsageEntity
import com.example.todoapp.data.repository.PhoneUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PhoneUsageViewModel(private val repository: PhoneUsageRepository) : ViewModel() {

    fun getUsageByDate(date: Long): Flow<PhoneUsageEntity?> {
        return repository.getUsageByDate(date)
    }

    fun insert(usage: PhoneUsageEntity) = viewModelScope.launch {
        repository.insertUsage(usage)
    }
}

class PhoneUsageViewModelFactory(private val repository: PhoneUsageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhoneUsageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhoneUsageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
