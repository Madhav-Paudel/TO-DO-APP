package com.example.todoapp.data.repository

import com.example.todoapp.data.local.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AssistantMemoryRepository(
    private val memoryDao: AssistantMemoryDao
) {
    // Session management
    fun generateSessionId(): String = UUID.randomUUID().toString()
    
    // Conversation methods
    suspend fun saveMessage(role: String, content: String, sessionId: String) {
        memoryDao.insertConversation(
            ConversationEntity(
                role = role,
                content = content,
                sessionId = sessionId
            )
        )
    }
    
    fun getSessionConversations(sessionId: String): Flow<List<ConversationEntity>> =
        memoryDao.getConversationsBySession(sessionId)
    
    suspend fun getRecentConversations(limit: Int = 50): List<ConversationEntity> =
        memoryDao.getRecentConversations(limit)
    
    suspend fun getRecentSessions(): List<String> =
        memoryDao.getRecentSessionIds()
    
    suspend fun clearOldConversations(daysOld: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        memoryDao.deleteOldConversations(cutoffTime)
    }
    
    suspend fun clearAllConversations() = memoryDao.clearAllConversations()
    
    // Memory methods (for learning user preferences)
    suspend fun rememberPreference(key: String, value: String) {
        memoryDao.insertMemory(
            AssistantMemoryEntity(
                key = key,
                value = value,
                category = "preference"
            )
        )
    }
    
    suspend fun rememberHabit(key: String, value: String) {
        memoryDao.insertMemory(
            AssistantMemoryEntity(
                key = key,
                value = value,
                category = "habit"
            )
        )
    }
    
    suspend fun rememberContext(key: String, value: String) {
        memoryDao.insertMemory(
            AssistantMemoryEntity(
                key = key,
                value = value,
                category = "context"
            )
        )
    }
    
    suspend fun recall(key: String): String? =
        memoryDao.getMemory(key)?.value
    
    fun getAllMemories(): Flow<List<AssistantMemoryEntity>> =
        memoryDao.getAllMemories()
    
    suspend fun getAllMemoriesNow(): List<AssistantMemoryEntity> =
        memoryDao.getAllMemoriesNow()
    
    fun getPreferences(): Flow<List<AssistantMemoryEntity>> =
        memoryDao.getMemoriesByCategory("preference")
    
    fun getHabits(): Flow<List<AssistantMemoryEntity>> =
        memoryDao.getMemoriesByCategory("habit")
    
    suspend fun forgetMemory(key: String) =
        memoryDao.deleteMemory(key)
    
    suspend fun clearAllMemories() = memoryDao.clearAllMemories()
    
    // Reminder methods
    suspend fun createReminder(
        title: String,
        message: String,
        triggerTime: Long,
        goalId: Long? = null,
        taskId: Long? = null,
        isRepeating: Boolean = false,
        repeatInterval: Long? = null
    ): Long {
        return memoryDao.insertReminder(
            AssistantReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                relatedGoalId = goalId,
                relatedTaskId = taskId,
                isRepeating = isRepeating,
                repeatInterval = repeatInterval
            )
        )
    }
    
    fun getActiveReminders(): Flow<List<AssistantReminderEntity>> =
        memoryDao.getActiveReminders()
    
    suspend fun getDueReminders(): List<AssistantReminderEntity> =
        memoryDao.getDueReminders(System.currentTimeMillis())
    
    suspend fun completeReminder(id: Long) =
        memoryDao.deactivateReminder(id)
    
    suspend fun deleteReminder(id: Long) =
        memoryDao.deleteReminder(id)
    
    suspend fun cleanupOldReminders(daysOld: Int = 7) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        memoryDao.deleteOldInactiveReminders(cutoffTime)
    }
}
