package com.example.todoapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_usage")
data class PhoneUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Midnight timestamp
    val totalMinutesUsed: Int
)
