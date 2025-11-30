package com.example.todoapp.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Analytics Theme Manager
 * Handles theme persistence and state management
 */
object AnalyticsThemeManager {
    
    private const val PREFS_NAME = "analytics_theme_prefs"
    private const val KEY_THEME_TYPE = "selected_theme"
    
    private var prefs: SharedPreferences? = null
    
    private val _currentThemeType = MutableStateFlow(AnalyticsThemeType.MINIMAL_WHITE)
    val currentThemeType: StateFlow<AnalyticsThemeType> = _currentThemeType.asStateFlow()
    
    private val _currentTheme = MutableStateFlow(AnalyticsThemes.MinimalWhite)
    val currentTheme: StateFlow<AnalyticsTheme> = _currentTheme.asStateFlow()
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedTheme()
    }
    
    private fun loadSavedTheme() {
        val savedThemeName = prefs?.getString(KEY_THEME_TYPE, AnalyticsThemeType.MINIMAL_WHITE.name)
        val themeType = try {
            AnalyticsThemeType.valueOf(savedThemeName ?: AnalyticsThemeType.MINIMAL_WHITE.name)
        } catch (e: Exception) {
            AnalyticsThemeType.MINIMAL_WHITE
        }
        setTheme(themeType, save = false)
    }
    
    fun setTheme(type: AnalyticsThemeType, save: Boolean = true) {
        _currentThemeType.value = type
        _currentTheme.value = AnalyticsThemes.getTheme(type)
        
        if (save) {
            prefs?.edit()?.putString(KEY_THEME_TYPE, type.name)?.apply()
        }
    }
    
    fun nextTheme() {
        val currentIndex = AnalyticsThemeType.values().indexOf(_currentThemeType.value)
        val nextIndex = (currentIndex + 1) % AnalyticsThemeType.values().size
        setTheme(AnalyticsThemeType.values()[nextIndex])
    }
    
    fun getThemeInfo(type: AnalyticsThemeType): ThemeInfo {
        val theme = AnalyticsThemes.getTheme(type)
        return ThemeInfo(
            type = type,
            name = theme.name,
            description = theme.description,
            isDark = theme.colors.isDark,
            previewColors = listOf(
                theme.colors.accentPrimary,
                theme.colors.accentSecondary,
                theme.colors.statusSuccess,
                theme.colors.chartPrimary
            )
        )
    }
    
    fun getAllThemeInfos(): List<ThemeInfo> {
        return AnalyticsThemeType.values().map { getThemeInfo(it) }
    }
    
    data class ThemeInfo(
        val type: AnalyticsThemeType,
        val name: String,
        val description: String,
        val isDark: Boolean,
        val previewColors: List<androidx.compose.ui.graphics.Color>
    )
}
