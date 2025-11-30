package com.example.todoapp.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.AppSettings
import com.example.todoapp.data.local.ModelQuality
import com.example.todoapp.data.local.SettingsDataStore
import com.example.todoapp.data.local.SyncOption
import com.example.todoapp.data.local.ThemeMode
import com.example.todoapp.llm.InstalledModel
import com.example.todoapp.llm.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val exportStatus: ExportStatus = ExportStatus.Idle,
    val importStatus: ImportStatus = ImportStatus.Idle,
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showAiSettingsDialog: Boolean = false,
    val showSyncDialog: Boolean = false,
    val showProfileDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showDeleteDataDialog: Boolean = false,
    val showModelQualityDialog: Boolean = false,
    // Model info
    val installedModels: List<InstalledModel> = emptyList(),
    val selectedModelId: String? = null,
    val availableStorageBytes: Long = 0L
)

enum class ExportStatus {
    Idle, Exporting, Success, Error
}

enum class ImportStatus {
    Idle, Importing, Success, Error
}

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val modelManager: ModelManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        loadModelInfo()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }
    
    private fun loadModelInfo() {
        viewModelScope.launch {
            modelManager.installedModels.collect { models ->
                _uiState.value = _uiState.value.copy(installedModels = models)
            }
        }
        viewModelScope.launch {
            modelManager.selectedModelId.collect { id ->
                _uiState.value = _uiState.value.copy(selectedModelId = id)
            }
        }
        _uiState.value = _uiState.value.copy(
            availableStorageBytes = modelManager.getAvailableSpaceBytes()
        )
    }
    
    fun refreshModelInfo() {
        modelManager.refreshInstalledModels()
        _uiState.value = _uiState.value.copy(
            availableStorageBytes = modelManager.getAvailableSpaceBytes()
        )
    }
    
    // Notification Settings
    fun togglePushNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updatePushNotifications(enabled)
        }
    }
    
    fun toggleEmailReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateEmailReminders(enabled)
        }
    }
    
    // Theme Settings
    fun showThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = true)
    }
    
    fun dismissThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = false)
    }
    
    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.updateThemeMode(mode)
            _uiState.value = _uiState.value.copy(showThemeDialog = false)
        }
    }
    
    // Language Settings
    fun showLanguageDialog() {
        _uiState.value = _uiState.value.copy(showLanguageDialog = true)
    }
    
    fun dismissLanguageDialog() {
        _uiState.value = _uiState.value.copy(showLanguageDialog = false)
    }
    
    fun updateLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.updateLanguage(language)
            _uiState.value = _uiState.value.copy(showLanguageDialog = false)
        }
    }
    
    // AI Settings
    fun showAiSettingsDialog() {
        _uiState.value = _uiState.value.copy(showAiSettingsDialog = true)
    }
    
    fun dismissAiSettingsDialog() {
        _uiState.value = _uiState.value.copy(showAiSettingsDialog = false)
    }
    
    fun toggleAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAiEnabled(enabled)
        }
    }
    
    fun updateAiResponseStyle(style: String) {
        viewModelScope.launch {
            settingsDataStore.updateAiResponseStyle(style)
        }
    }
    
    // Local Assistant Settings
    fun toggleUseLocalAssistant(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateUseLocalAssistant(enabled)
        }
    }
    
    fun showModelQualityDialog() {
        _uiState.value = _uiState.value.copy(showModelQualityDialog = true)
    }
    
    fun dismissModelQualityDialog() {
        _uiState.value = _uiState.value.copy(showModelQualityDialog = false)
    }
    
    fun updateModelQuality(quality: ModelQuality) {
        viewModelScope.launch {
            settingsDataStore.updateModelQuality(quality)
            _uiState.value = _uiState.value.copy(showModelQualityDialog = false)
        }
    }
    
    fun updateMaxTokens(tokens: Int) {
        viewModelScope.launch {
            settingsDataStore.updateMaxTokens(tokens)
        }
    }
    
    fun updateGenerationTimeout(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.updateGenerationTimeout(seconds)
        }
    }
    
    fun hasModelInstalled(): Boolean {
        return _uiState.value.installedModels.isNotEmpty()
    }
    
    fun getStorageInfo(): String {
        val available = modelManager.getAvailableSpaceBytes()
        return modelManager.formatSize(available)
    }
    
    // Sync Settings
    fun showSyncDialog() {
        _uiState.value = _uiState.value.copy(showSyncDialog = true)
    }
    
    fun dismissSyncDialog() {
        _uiState.value = _uiState.value.copy(showSyncDialog = false)
    }
    
    fun updateSyncOption(option: SyncOption) {
        viewModelScope.launch {
            settingsDataStore.updateSyncOption(option)
        }
    }
    
    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoSync(enabled)
        }
    }
    
    // Export/Import
    fun exportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exportStatus = ExportStatus.Exporting)
            try {
                // Simulate export - in real app, this would export to file
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(exportStatus = ExportStatus.Success)
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(exportStatus = ExportStatus.Idle)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(exportStatus = ExportStatus.Error)
            }
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importStatus = ImportStatus.Importing)
            try {
                // Simulate import - in real app, this would import from file
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(importStatus = ImportStatus.Success)
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(importStatus = ImportStatus.Idle)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(importStatus = ImportStatus.Error)
            }
        }
    }
    
    // Profile Settings
    fun showProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = true)
    }
    
    fun dismissProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = false)
    }
    
    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            settingsDataStore.updateUserProfile(name, email)
            _uiState.value = _uiState.value.copy(showProfileDialog = false)
        }
    }
    
    // Logout
    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }
    
    fun dismissLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }
    
    fun logout() {
        viewModelScope.launch {
            settingsDataStore.updateUserProfile("", "")
            _uiState.value = _uiState.value.copy(showLogoutDialog = false)
        }
    }
    
    // Delete Data
    fun showDeleteDataDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDataDialog = true)
    }
    
    fun dismissDeleteDataDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDataDialog = false)
    }
    
    fun deleteAllData() {
        viewModelScope.launch {
            settingsDataStore.clearAllData()
            _uiState.value = _uiState.value.copy(showDeleteDataDialog = false)
        }
    }
}
