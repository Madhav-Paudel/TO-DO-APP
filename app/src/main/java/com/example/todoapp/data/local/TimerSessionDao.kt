package com.example.todoapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimerSessionEntity): Long

    @Query("SELECT * FROM timer_sessions WHERE goalId = :goalId ORDER BY startTime DESC")
    fun getSessionsByGoal(goalId: Long): Flow<List<TimerSessionEntity>>

    @Query("SELECT * FROM timer_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsByDate(date: Long): Flow<List<TimerSessionEntity>>

    @Query("SELECT * FROM timer_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getSessionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TimerSessionEntity>>

    @Query("SELECT SUM(durationMinutes) FROM timer_sessions WHERE goalId = :goalId AND date = :date")
    suspend fun getTotalMinutesForGoalToday(goalId: Long, date: Long): Int?

    @Query("SELECT SUM(durationMinutes) FROM timer_sessions WHERE date = :date")
    suspend fun getTotalMinutesToday(date: Long): Int?

    @Query("SELECT * FROM timer_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<TimerSessionEntity>>

    @Delete
    suspend fun deleteSession(session: TimerSessionEntity)

    @Query("DELETE FROM timer_sessions WHERE goalId = :goalId")
    suspend fun deleteSessionsByGoal(goalId: Long)
}
