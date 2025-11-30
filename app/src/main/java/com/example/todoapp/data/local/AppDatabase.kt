package com.example.todoapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GoalEntity::class, 
        TaskEntity::class, 
        DailyProgressEntity::class, 
        PhoneUsageEntity::class, 
        TimerSessionEntity::class,
        ConversationEntity::class,
        AssistantMemoryEntity::class,
        AssistantReminderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun dailyProgressDao(): DailyProgressDao
    abstract fun phoneUsageDao(): PhoneUsageDao
    abstract fun timerSessionDao(): TimerSessionDao
    abstract fun assistantMemoryDao(): AssistantMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS timer_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        mode TEXT NOT NULL,
                        wasCompleted INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_timer_sessions_goalId ON timer_sessions(goalId)")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create assistant_conversations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS assistant_conversations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        sessionId TEXT NOT NULL
                    )
                """)
                
                // Create assistant_memory table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS assistant_memory (
                        `key` TEXT PRIMARY KEY NOT NULL,
                        value TEXT NOT NULL,
                        category TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """)
                
                // Create assistant_reminders table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS assistant_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        triggerTime INTEGER NOT NULL,
                        relatedGoalId INTEGER,
                        relatedTaskId INTEGER,
                        isRepeating INTEGER NOT NULL,
                        repeatInterval INTEGER,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
