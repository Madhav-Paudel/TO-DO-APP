package com.example.todoapp.data.repository

import com.example.todoapp.data.local.PhoneUsageDao
import com.example.todoapp.data.local.PhoneUsageEntity
import kotlinx.coroutines.flow.Flow

class PhoneUsageRepository(private val phoneUsageDao: PhoneUsageDao) {

    fun getUsageByDate(date: Long): Flow<PhoneUsageEntity?> {
        return phoneUsageDao.getUsageByDate(date)
    }

    fun getUsageBetweenDates(start: Long, end: Long): Flow<List<PhoneUsageEntity>> {
        return phoneUsageDao.getUsageBetweenDates(start, end)
    }

    suspend fun insertUsage(usage: PhoneUsageEntity) {
        phoneUsageDao.insertUsage(usage)
    }
}
