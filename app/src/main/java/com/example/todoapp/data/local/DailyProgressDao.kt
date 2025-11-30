package com.example.todoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyProgressDao {
    @Query("SELECT * FROM daily_progress WHERE date = :date")
    fun getProgressByDate(date: Long): Flow<List<DailyProgressEntity>>

    @Query("SELECT * FROM daily_progress WHERE goalId = :goalId")
    fun getProgressByGoal(goalId: Long): Flow<List<DailyProgressEntity>>

    @Query("SELECT * FROM daily_progress WHERE date >= :start AND date <= :end")
    fun getProgressBetweenDates(start: Long, end: Long): Flow<List<DailyProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DailyProgressEntity)
}
