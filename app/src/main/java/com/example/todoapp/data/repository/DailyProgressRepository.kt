package com.example.todoapp.data.repository

import com.example.todoapp.data.local.DailyProgressDao
import com.example.todoapp.data.local.DailyProgressEntity
import kotlinx.coroutines.flow.Flow

class DailyProgressRepository(private val dailyProgressDao: DailyProgressDao) {

    fun getProgressByDate(date: Long): Flow<List<DailyProgressEntity>> {
        return dailyProgressDao.getProgressByDate(date)
    }

    fun getProgressByGoal(goalId: Long): Flow<List<DailyProgressEntity>> {
        return dailyProgressDao.getProgressByGoal(goalId)
    }

    fun getProgressBetweenDates(start: Long, end: Long): Flow<List<DailyProgressEntity>> {
        return dailyProgressDao.getProgressBetweenDates(start, end)
    }

    suspend fun insertProgress(progress: DailyProgressEntity) {
        dailyProgressDao.insertProgress(progress)
    }
}
