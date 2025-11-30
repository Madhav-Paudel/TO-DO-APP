package com.example.todoapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val startDate: Long,
    val endDate: Long,
    val dailyTargetMinutes: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
