package com.example.todoapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class SyncOption {
    GOOGLE_DRIVE, LOCAL_STORAGE
}

enum class ModelQuality {
    FAST,      // 1B models - fastest inference
    BALANCED   // 3B models - better quality
}

data class AppSettings(
    val pushNotificationsEnabled: Boolean = true,
    val emailRemindersEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "en",
    val aiAssistantEnabled: Boolean = true,
    val aiResponseStyle: String = "balanced", // concise, balanced, detailed
    val syncOption: SyncOption = SyncOption.LOCAL_STORAGE,
    val autoSyncEnabled: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    // On-device LLM settings
    val useLocalAssistant: Boolean = true,      // Default to local AI
    val modelQuality: ModelQuality = ModelQuality.FAST,  // Default to fast (1B)
    val maxTokens: Int = 128,                   // Limit output length
    val generationTimeoutSeconds: Int = 30      // Abort after timeout
)

class SettingsDataStore(private val context: Context) {
    
    companion object {
        private val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
        private val EMAIL_REMINDERS = booleanPreferencesKey("email_reminders")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        private val AI_RESPONSE_STYLE = stringPreferencesKey("ai_response_style")
        private val SYNC_OPTION = stringPreferencesKey("sync_option")
        private val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        // On-device LLM settings
        private val USE_LOCAL_ASSISTANT = booleanPreferencesKey("use_local_assistant")
        private val MODEL_QUALITY = stringPreferencesKey("model_quality")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val GENERATION_TIMEOUT = intPreferencesKey("generation_timeout")
    }
    
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            pushNotificationsEnabled = preferences[PUSH_NOTIFICATIONS] ?: true,
            emailRemindersEnabled = preferences[EMAIL_REMINDERS] ?: false,
            themeMode = ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name),
            language = preferences[LANGUAGE] ?: "en",
            aiAssistantEnabled = preferences[AI_ENABLED] ?: true,
            aiResponseStyle = preferences[AI_RESPONSE_STYLE] ?: "balanced",
            syncOption = SyncOption.valueOf(preferences[SYNC_OPTION] ?: SyncOption.LOCAL_STORAGE.name),
            autoSyncEnabled = preferences[AUTO_SYNC] ?: false,
            userName = preferences[USER_NAME] ?: "",
            userEmail = preferences[USER_EMAIL] ?: "",
            // On-device LLM settings
            useLocalAssistant = preferences[USE_LOCAL_ASSISTANT] ?: true,
            modelQuality = ModelQuality.valueOf(preferences[MODEL_QUALITY] ?: ModelQuality.FAST.name),
            maxTokens = preferences[MAX_TOKENS] ?: 128,
            generationTimeoutSeconds = preferences[GENERATION_TIMEOUT] ?: 30
        )
    }
    
    suspend fun updatePushNotifications(enabled: Boolean) {
        context.dataStore.edit { it[PUSH_NOTIFICATIONS] = enabled }
    }
    
    suspend fun updateEmailReminders(enabled: Boolean) {
        context.dataStore.edit { it[EMAIL_REMINDERS] = enabled }
    }
    
    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }
    
    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[LANGUAGE] = language }
    }
    
    suspend fun updateAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AI_ENABLED] = enabled }
    }
    
    suspend fun updateAiResponseStyle(style: String) {
        context.dataStore.edit { it[AI_RESPONSE_STYLE] = style }
    }
    
    suspend fun updateSyncOption(option: SyncOption) {
        context.dataStore.edit { it[SYNC_OPTION] = option.name }
    }
    
    suspend fun updateAutoSync(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_SYNC] = enabled }
    }
    
    suspend fun updateUserProfile(name: String, email: String) {
        context.dataStore.edit { 
            it[USER_NAME] = name
            it[USER_EMAIL] = email
        }
    }
    
    // On-device LLM settings
    suspend fun updateUseLocalAssistant(enabled: Boolean) {
        context.dataStore.edit { it[USE_LOCAL_ASSISTANT] = enabled }
    }
    
    suspend fun updateModelQuality(quality: ModelQuality) {
        context.dataStore.edit { it[MODEL_QUALITY] = quality.name }
    }
    
    suspend fun updateMaxTokens(tokens: Int) {
        context.dataStore.edit { it[MAX_TOKENS] = tokens }
    }
    
    suspend fun updateGenerationTimeout(seconds: Int) {
        context.dataStore.edit { it[GENERATION_TIMEOUT] = seconds }
    }
    
    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }
}
