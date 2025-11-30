package com.example.todoapp.data.repository

import com.example.todoapp.data.local.TimerSessionDao
import com.example.todoapp.data.local.TimerSessionEntity
import kotlinx.coroutines.flow.Flow

class TimerSessionRepository(private val timerSessionDao: TimerSessionDao) {
    
    suspend fun insertSession(session: TimerSessionEntity): Long {
        return timerSessionDao.insertSession(session)
    }
    
    fun getSessionsByGoal(goalId: Long): Flow<List<TimerSessionEntity>> {
        return timerSessionDao.getSessionsByGoal(goalId)
    }
    
    fun getSessionsByDate(date: Long): Flow<List<TimerSessionEntity>> {
        return timerSessionDao.getSessionsByDate(date)
    }
    
    fun getSessionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TimerSessionEntity>> {
        return timerSessionDao.getSessionsBetweenDates(startDate, endDate)
    }
    
    suspend fun getTotalMinutesForGoalToday(goalId: Long, date: Long): Int {
        return timerSessionDao.getTotalMinutesForGoalToday(goalId, date) ?: 0
    }
    
    suspend fun getTotalMinutesToday(date: Long): Int {
        return timerSessionDao.getTotalMinutesToday(date) ?: 0
    }
    
    fun getRecentSessions(limit: Int = 10): Flow<List<TimerSessionEntity>> {
        return timerSessionDao.getRecentSessions(limit)
    }
    
    suspend fun deleteSession(session: TimerSessionEntity) {
        timerSessionDao.deleteSession(session)
    }
    
    suspend fun deleteSessionsByGoal(goalId: Long) {
        timerSessionDao.deleteSessionsByGoal(goalId)
    }
}
