package com.example.todoapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timer_sessions",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class TimerSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val mode: String, // "stopwatch" or "countdown"
    val wasCompleted: Boolean, // For countdown - did they complete full duration?
    val date: Long // Midnight timestamp for that day
)
