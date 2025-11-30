package com.example.todoapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantMemoryDao {
    // Conversation methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)
    
    @Query("SELECT * FROM assistant_conversations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getConversationsBySession(sessionId: String): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM assistant_conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConversations(limit: Int = 50): List<ConversationEntity>
    
    @Query("SELECT DISTINCT sessionId FROM assistant_conversations ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentSessionIds(): List<String>
    
    @Query("DELETE FROM assistant_conversations WHERE timestamp < :olderThan")
    suspend fun deleteOldConversations(olderThan: Long)
    
    @Query("DELETE FROM assistant_conversations")
    suspend fun clearAllConversations()
    
    // Memory methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AssistantMemoryEntity)
    
    @Query("SELECT * FROM assistant_memory WHERE key = :key")
    suspend fun getMemory(key: String): AssistantMemoryEntity?
    
    @Query("SELECT * FROM assistant_memory WHERE category = :category")
    fun getMemoriesByCategory(category: String): Flow<List<AssistantMemoryEntity>>
    
    @Query("SELECT * FROM assistant_memory")
    fun getAllMemories(): Flow<List<AssistantMemoryEntity>>
    
    @Query("SELECT * FROM assistant_memory")
    suspend fun getAllMemoriesNow(): List<AssistantMemoryEntity>
    
    @Query("DELETE FROM assistant_memory WHERE key = :key")
    suspend fun deleteMemory(key: String)
    
    @Query("DELETE FROM assistant_memory")
    suspend fun clearAllMemories()
    
    // Reminder methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: AssistantReminderEntity): Long
    
    @Update
    suspend fun updateReminder(reminder: AssistantReminderEntity)
    
    @Query("SELECT * FROM assistant_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): AssistantReminderEntity?
    
    @Query("SELECT * FROM assistant_reminders WHERE isActive = 1 ORDER BY triggerTime ASC")
    fun getActiveReminders(): Flow<List<AssistantReminderEntity>>
    
    @Query("SELECT * FROM assistant_reminders WHERE isActive = 1 AND triggerTime <= :currentTime ORDER BY triggerTime ASC")
    suspend fun getDueReminders(currentTime: Long): List<AssistantReminderEntity>
    
    @Query("SELECT * FROM assistant_reminders WHERE relatedGoalId = :goalId")
    fun getRemindersByGoal(goalId: Long): Flow<List<AssistantReminderEntity>>
    
    @Query("UPDATE assistant_reminders SET isActive = 0 WHERE id = :id")
    suspend fun deactivateReminder(id: Long)
    
    @Query("DELETE FROM assistant_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)
    
    @Query("DELETE FROM assistant_reminders WHERE isActive = 0 AND triggerTime < :olderThan")
    suspend fun deleteOldInactiveReminders(olderThan: Long)
}
