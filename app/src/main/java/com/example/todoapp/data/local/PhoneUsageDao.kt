package com.example.todoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneUsageDao {
    @Query("SELECT * FROM phone_usage WHERE date = :date")
    fun getUsageByDate(date: Long): Flow<PhoneUsageEntity?>

    @Query("SELECT * FROM phone_usage WHERE date >= :start AND date <= :end")
    fun getUsageBetweenDates(start: Long, end: Long): Flow<List<PhoneUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: PhoneUsageEntity)
}
