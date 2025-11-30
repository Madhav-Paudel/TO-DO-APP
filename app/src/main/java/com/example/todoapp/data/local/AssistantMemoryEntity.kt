package com.example.todoapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store conversation history and context
 */
@Entity(tableName = "assistant_conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String // Groups messages by session
)

/**
 * Entity to store user preferences learned by the assistant
 */
@Entity(tableName = "assistant_memory")
data class AssistantMemoryEntity(
    @PrimaryKey val key: String, // e.g., "preferred_study_time", "motivation_style"
    val value: String,
    val category: String, // "preference", "habit", "context"
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entity to store scheduled reminders
 */
@Entity(tableName = "assistant_reminders")
data class AssistantReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val triggerTime: Long,
    val relatedGoalId: Long? = null,
    val relatedTaskId: Long? = null,
    val isRepeating: Boolean = false,
    val repeatInterval: Long? = null, // in milliseconds
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
