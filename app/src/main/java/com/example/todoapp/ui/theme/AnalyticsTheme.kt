package com.example.todoapp.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Analytics Dashboard Theme System
 * Provides multiple attractive themes with color palettes, typography, shapes, and chart styles
 */

// ==================== THEME TYPES ====================

enum class AnalyticsThemeType {
    MINIMAL_WHITE,
    DARK_NEON,
    PASTEL_PRODUCTIVITY,
    GRADIENT_GLASSMORPHISM
}

// ==================== COLOR SCHEMES ====================

data class AnalyticsColorScheme(
    // Background colors
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val backgroundGradientMid: Color? = null,
    
    // Surface colors
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceElevated: Color,
    val surfaceOverlay: Color,
    
    // Card colors
    val cardBackground: Color,
    val cardBackgroundElevated: Color,
    val cardBorder: Color,
    val cardShadow: Color,
    
    // Text colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnAccent: Color,
    val textMuted: Color,
    
    // Accent colors
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,
    val accentGradientStart: Color,
    val accentGradientEnd: Color,
    
    // Status colors
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val statusInfo: Color,
    val statusSuccessLight: Color,
    val statusWarningLight: Color,
    val statusErrorLight: Color,
    val statusInfoLight: Color,
    
    // Chart colors
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
    val chartQuaternary: Color,
    val chartBackground: Color,
    val chartGridLines: Color,
    val chartAxisLabels: Color,
    val chartTooltipBackground: Color,
    
    // Chart palette (for multiple data series)
    val chartPalette: List<Color>,
    
    // Heatmap colors (5 levels)
    val heatmapEmpty: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
    
    // Progress colors
    val progressTrack: Color,
    val progressIndicator: Color,
    val progressGlow: Color,
    
    // Interactive elements
    val buttonPrimary: Color,
    val buttonSecondary: Color,
    val buttonDisabled: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconAccent: Color,
    
    // Dividers and borders
    val divider: Color,
    val border: Color,
    val borderFocused: Color,
    
    // Special effects
    val glassOverlay: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val neonGlow: Color? = null,
    
    // Is dark theme
    val isDark: Boolean
)

// ==================== SHAPE SCHEME ====================

data class AnalyticsShapeScheme(
    val cardCornerRadius: Dp,
    val buttonCornerRadius: Dp,
    val chipCornerRadius: Dp,
    val chartCornerRadius: Dp,
    val dialogCornerRadius: Dp,
    val bottomSheetCornerRadius: Dp,
    val inputCornerRadius: Dp,
    val avatarCornerRadius: Dp,
    val heatmapCellCornerRadius: Dp,
    val progressBarCornerRadius: Dp
)

// ==================== SPACING SCHEME ====================

data class AnalyticsSpacingScheme(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val cardPadding: Dp = 20.dp,
    val screenPadding: Dp = 16.dp,
    val sectionSpacing: Dp = 20.dp,
    val itemSpacing: Dp = 12.dp
)

// ==================== ELEVATION SCHEME ====================

data class AnalyticsElevationScheme(
    val none: Dp = 0.dp,
    val low: Dp = 2.dp,
    val medium: Dp = 4.dp,
    val high: Dp = 8.dp,
    val extraHigh: Dp = 16.dp,
    val card: Dp = 8.dp,
    val dialog: Dp = 24.dp,
    val bottomSheet: Dp = 16.dp
)

// ==================== CHART STYLE ====================

data class AnalyticsChartStyle(
    val lineStrokeWidth: Float = 3f,
    val lineStrokeWidthThick: Float = 4f,
    val dotRadius: Float = 6f,
    val dotRadiusSmall: Float = 4f,
    val barCornerRadius: Float = 8f,
    val gridLineWidth: Float = 1f,
    val axisLineWidth: Float = 2f,
    val areaFillAlpha: Float = 0.2f,
    val tooltipCornerRadius: Dp = 12.dp,
    val animationDuration: Int = 800,
    val showGridLines: Boolean = true,
    val showDataPoints: Boolean = true,
    val showAreaFill: Boolean = true,
    val useGradientFill: Boolean = true,
    val useShadow: Boolean = true
)

// ==================== COMPLETE THEME ====================

data class AnalyticsTheme(
    val type: AnalyticsThemeType,
    val colors: AnalyticsColorScheme,
    val shapes: AnalyticsShapeScheme,
    val spacing: AnalyticsSpacingScheme,
    val elevation: AnalyticsElevationScheme,
    val chartStyle: AnalyticsChartStyle,
    val name: String,
    val description: String
)

// ==================== THEME DEFINITIONS ====================

object AnalyticsThemes {
    
    // ========== 1. MINIMAL WHITE THEME ==========
    val MinimalWhite = AnalyticsTheme(
        type = AnalyticsThemeType.MINIMAL_WHITE,
        name = "Minimal White",
        description = "Clean, professional look with subtle shadows",
        colors = AnalyticsColorScheme(
            // Backgrounds
            backgroundPrimary = Color(0xFFFAFAFA),
            backgroundSecondary = Color(0xFFF5F5F5),
            backgroundGradientStart = Color(0xFFFFFFFF),
            backgroundGradientEnd = Color(0xFFF8F9FA),
            
            // Surfaces
            surfacePrimary = Color(0xFFFFFFFF),
            surfaceSecondary = Color(0xFFF8F9FA),
            surfaceElevated = Color(0xFFFFFFFF),
            surfaceOverlay = Color(0x0A000000),
            
            // Cards
            cardBackground = Color(0xFFFFFFFF),
            cardBackgroundElevated = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE8E8E8),
            cardShadow = Color(0x1A000000),
            
            // Text
            textPrimary = Color(0xFF1A1A1A),
            textSecondary = Color(0xFF666666),
            textTertiary = Color(0xFF999999),
            textOnAccent = Color(0xFFFFFFFF),
            textMuted = Color(0xFFBBBBBB),
            
            // Accents
            accentPrimary = Color(0xFF2563EB),
            accentSecondary = Color(0xFF7C3AED),
            accentTertiary = Color(0xFF059669),
            accentGradientStart = Color(0xFF3B82F6),
            accentGradientEnd = Color(0xFF8B5CF6),
            
            // Status
            statusSuccess = Color(0xFF10B981),
            statusWarning = Color(0xFFF59E0B),
            statusError = Color(0xFFEF4444),
            statusInfo = Color(0xFF3B82F6),
            statusSuccessLight = Color(0xFFD1FAE5),
            statusWarningLight = Color(0xFFFEF3C7),
            statusErrorLight = Color(0xFFFEE2E2),
            statusInfoLight = Color(0xFFDBEAFE),
            
            // Charts
            chartPrimary = Color(0xFF2563EB),
            chartSecondary = Color(0xFF7C3AED),
            chartTertiary = Color(0xFF059669),
            chartQuaternary = Color(0xFFF59E0B),
            chartBackground = Color(0xFFF8FAFC),
            chartGridLines = Color(0xFFE2E8F0),
            chartAxisLabels = Color(0xFF64748B),
            chartTooltipBackground = Color(0xFF1E293B),
            chartPalette = listOf(
                Color(0xFF2563EB),
                Color(0xFF7C3AED),
                Color(0xFF059669),
                Color(0xFFF59E0B),
                Color(0xFFEC4899),
                Color(0xFF06B6D4)
            ),
            
            // Heatmap
            heatmapEmpty = Color(0xFFEBEDF0),
            heatmapLevel1 = Color(0xFFC6E48B),
            heatmapLevel2 = Color(0xFF7BC96F),
            heatmapLevel3 = Color(0xFF239A3B),
            heatmapLevel4 = Color(0xFF196127),
            
            // Progress
            progressTrack = Color(0xFFE2E8F0),
            progressIndicator = Color(0xFF2563EB),
            progressGlow = Color(0x402563EB),
            
            // Interactive
            buttonPrimary = Color(0xFF2563EB),
            buttonSecondary = Color(0xFFF1F5F9),
            buttonDisabled = Color(0xFFE2E8F0),
            iconPrimary = Color(0xFF334155),
            iconSecondary = Color(0xFF94A3B8),
            iconAccent = Color(0xFF2563EB),
            
            // Dividers
            divider = Color(0xFFE2E8F0),
            border = Color(0xFFE2E8F0),
            borderFocused = Color(0xFF2563EB),
            
            // Effects
            glassOverlay = Color(0x0AFFFFFF),
            shimmerBase = Color(0xFFF1F5F9),
            shimmerHighlight = Color(0xFFFFFFFF),
            
            isDark = false
        ),
        shapes = AnalyticsShapeScheme(
            cardCornerRadius = 16.dp,
            buttonCornerRadius = 12.dp,
            chipCornerRadius = 20.dp,
            chartCornerRadius = 12.dp,
            dialogCornerRadius = 24.dp,
            bottomSheetCornerRadius = 28.dp,
            inputCornerRadius = 12.dp,
            avatarCornerRadius = 50.dp,
            heatmapCellCornerRadius = 3.dp,
            progressBarCornerRadius = 8.dp
        ),
        spacing = AnalyticsSpacingScheme(),
        elevation = AnalyticsElevationScheme(
            card = 4.dp
        ),
        chartStyle = AnalyticsChartStyle(
            lineStrokeWidth = 2.5f,
            areaFillAlpha = 0.15f,
            useShadow = false
        )
    )
    
    // ========== 2. DARK NEON THEME ==========
    val DarkNeon = AnalyticsTheme(
        type = AnalyticsThemeType.DARK_NEON,
        name = "Dark Neon",
        description = "Bold, vibrant colors on dark background",
        colors = AnalyticsColorScheme(
            // Backgrounds
            backgroundPrimary = Color(0xFF0F0F1A),
            backgroundSecondary = Color(0xFF1A1A2E),
            backgroundGradientStart = Color(0xFF0F0F1A),
            backgroundGradientEnd = Color(0xFF16162A),
            backgroundGradientMid = Color(0xFF1A1A2E),
            
            // Surfaces
            surfacePrimary = Color(0xFF1E1E32),
            surfaceSecondary = Color(0xFF252540),
            surfaceElevated = Color(0xFF2A2A45),
            surfaceOverlay = Color(0x20FFFFFF),
            
            // Cards
            cardBackground = Color(0xFF1E1E32),
            cardBackgroundElevated = Color(0xFF252540),
            cardBorder = Color(0xFF3D3D5C),
            cardShadow = Color(0x40000000),
            
            // Text
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB8B8D0),
            textTertiary = Color(0xFF8080A0),
            textOnAccent = Color(0xFF0F0F1A),
            textMuted = Color(0xFF606080),
            
            // Accents - Neon colors
            accentPrimary = Color(0xFF00F5FF),
            accentSecondary = Color(0xFFFF00FF),
            accentTertiary = Color(0xFF00FF88),
            accentGradientStart = Color(0xFF00F5FF),
            accentGradientEnd = Color(0xFFFF00FF),
            
            // Status - Neon variants
            statusSuccess = Color(0xFF00FF88),
            statusWarning = Color(0xFFFFD700),
            statusError = Color(0xFFFF4466),
            statusInfo = Color(0xFF00BFFF),
            statusSuccessLight = Color(0x3000FF88),
            statusWarningLight = Color(0x30FFD700),
            statusErrorLight = Color(0x30FF4466),
            statusInfoLight = Color(0x3000BFFF),
            
            // Charts - Neon palette
            chartPrimary = Color(0xFF00F5FF),
            chartSecondary = Color(0xFFFF00FF),
            chartTertiary = Color(0xFF00FF88),
            chartQuaternary = Color(0xFFFFD700),
            chartBackground = Color(0xFF1A1A2E),
            chartGridLines = Color(0xFF2D2D48),
            chartAxisLabels = Color(0xFF8080A0),
            chartTooltipBackground = Color(0xFF252540),
            chartPalette = listOf(
                Color(0xFF00F5FF),
                Color(0xFFFF00FF),
                Color(0xFF00FF88),
                Color(0xFFFFD700),
                Color(0xFFFF6B6B),
                Color(0xFF4ECDC4)
            ),
            
            // Heatmap - Neon green
            heatmapEmpty = Color(0xFF252540),
            heatmapLevel1 = Color(0xFF004D40),
            heatmapLevel2 = Color(0xFF00796B),
            heatmapLevel3 = Color(0xFF00BFA5),
            heatmapLevel4 = Color(0xFF00FF88),
            
            // Progress
            progressTrack = Color(0xFF2D2D48),
            progressIndicator = Color(0xFF00F5FF),
            progressGlow = Color(0x6000F5FF),
            
            // Interactive
            buttonPrimary = Color(0xFF00F5FF),
            buttonSecondary = Color(0xFF2D2D48),
            buttonDisabled = Color(0xFF3D3D5C),
            iconPrimary = Color(0xFFFFFFFF),
            iconSecondary = Color(0xFF8080A0),
            iconAccent = Color(0xFF00F5FF),
            
            // Dividers
            divider = Color(0xFF2D2D48),
            border = Color(0xFF3D3D5C),
            borderFocused = Color(0xFF00F5FF),
            
            // Effects
            glassOverlay = Color(0x15FFFFFF),
            shimmerBase = Color(0xFF252540),
            shimmerHighlight = Color(0xFF3D3D5C),
            neonGlow = Color(0xFF00F5FF),
            
            isDark = true
        ),
        shapes = AnalyticsShapeScheme(
            cardCornerRadius = 20.dp,
            buttonCornerRadius = 14.dp,
            chipCornerRadius = 24.dp,
            chartCornerRadius = 16.dp,
            dialogCornerRadius = 28.dp,
            bottomSheetCornerRadius = 32.dp,
            inputCornerRadius = 14.dp,
            avatarCornerRadius = 50.dp,
            heatmapCellCornerRadius = 4.dp,
            progressBarCornerRadius = 10.dp
        ),
        spacing = AnalyticsSpacingScheme(),
        elevation = AnalyticsElevationScheme(
            card = 12.dp
        ),
        chartStyle = AnalyticsChartStyle(
            lineStrokeWidth = 3f,
            lineStrokeWidthThick = 4f,
            dotRadius = 7f,
            areaFillAlpha = 0.25f,
            useShadow = true
        )
    )
    
    // ========== 3. PASTEL PRODUCTIVITY THEME ==========
    val PastelProductivity = AnalyticsTheme(
        type = AnalyticsThemeType.PASTEL_PRODUCTIVITY,
        name = "Pastel Productivity",
        description = "Soft, calming colors for focused work",
        colors = AnalyticsColorScheme(
            // Backgrounds
            backgroundPrimary = Color(0xFFFDF6F0),
            backgroundSecondary = Color(0xFFF8F0EA),
            backgroundGradientStart = Color(0xFFFDF6F0),
            backgroundGradientEnd = Color(0xFFF0E6FF),
            backgroundGradientMid = Color(0xFFE8F4F8),
            
            // Surfaces
            surfacePrimary = Color(0xFFFFFFFF),
            surfaceSecondary = Color(0xFFFDF6F0),
            surfaceElevated = Color(0xFFFFFFFF),
            surfaceOverlay = Color(0x08000000),
            
            // Cards
            cardBackground = Color(0xFFFFFFFF),
            cardBackgroundElevated = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE8DDD4),
            cardShadow = Color(0x15B8A090),
            
            // Text
            textPrimary = Color(0xFF3D3D3D),
            textSecondary = Color(0xFF6B6B6B),
            textTertiary = Color(0xFF9B9B9B),
            textOnAccent = Color(0xFFFFFFFF),
            textMuted = Color(0xFFC0C0C0),
            
            // Accents - Soft pastels
            accentPrimary = Color(0xFFB8A4E3),
            accentSecondary = Color(0xFFF8B4C4),
            accentTertiary = Color(0xFF9DD5C0),
            accentGradientStart = Color(0xFFB8A4E3),
            accentGradientEnd = Color(0xFFF8B4C4),
            
            // Status - Pastel variants
            statusSuccess = Color(0xFF7EC8A3),
            statusWarning = Color(0xFFF5C77E),
            statusError = Color(0xFFE8A0A0),
            statusInfo = Color(0xFF8FBCE8),
            statusSuccessLight = Color(0xFFE0F5EA),
            statusWarningLight = Color(0xFFFFF3E0),
            statusErrorLight = Color(0xFFFDE8E8),
            statusInfoLight = Color(0xFFE3F2FD),
            
            // Charts - Pastel palette
            chartPrimary = Color(0xFFB8A4E3),
            chartSecondary = Color(0xFFF8B4C4),
            chartTertiary = Color(0xFF9DD5C0),
            chartQuaternary = Color(0xFFF5C77E),
            chartBackground = Color(0xFFFAF8F5),
            chartGridLines = Color(0xFFE8E0D8),
            chartAxisLabels = Color(0xFF8B8B8B),
            chartTooltipBackground = Color(0xFF4A4A4A),
            chartPalette = listOf(
                Color(0xFFB8A4E3),
                Color(0xFFF8B4C4),
                Color(0xFF9DD5C0),
                Color(0xFFF5C77E),
                Color(0xFF8FBCE8),
                Color(0xFFE8C4A0)
            ),
            
            // Heatmap - Soft purple
            heatmapEmpty = Color(0xFFF0EAF8),
            heatmapLevel1 = Color(0xFFDDD0F0),
            heatmapLevel2 = Color(0xFFC4B0E3),
            heatmapLevel3 = Color(0xFFA890D5),
            heatmapLevel4 = Color(0xFF8B70C8),
            
            // Progress
            progressTrack = Color(0xFFE8E0D8),
            progressIndicator = Color(0xFFB8A4E3),
            progressGlow = Color(0x40B8A4E3),
            
            // Interactive
            buttonPrimary = Color(0xFFB8A4E3),
            buttonSecondary = Color(0xFFF8F0EA),
            buttonDisabled = Color(0xFFE8E0D8),
            iconPrimary = Color(0xFF5A5A5A),
            iconSecondary = Color(0xFFA0A0A0),
            iconAccent = Color(0xFFB8A4E3),
            
            // Dividers
            divider = Color(0xFFE8E0D8),
            border = Color(0xFFE8DDD4),
            borderFocused = Color(0xFFB8A4E3),
            
            // Effects
            glassOverlay = Color(0x08FFFFFF),
            shimmerBase = Color(0xFFF5F0EA),
            shimmerHighlight = Color(0xFFFFFFFF),
            
            isDark = false
        ),
        shapes = AnalyticsShapeScheme(
            cardCornerRadius = 24.dp,
            buttonCornerRadius = 16.dp,
            chipCornerRadius = 28.dp,
            chartCornerRadius = 18.dp,
            dialogCornerRadius = 32.dp,
            bottomSheetCornerRadius = 36.dp,
            inputCornerRadius = 16.dp,
            avatarCornerRadius = 50.dp,
            heatmapCellCornerRadius = 5.dp,
            progressBarCornerRadius = 12.dp
        ),
        spacing = AnalyticsSpacingScheme(
            cardPadding = 24.dp,
            sectionSpacing = 24.dp
        ),
        elevation = AnalyticsElevationScheme(
            card = 6.dp
        ),
        chartStyle = AnalyticsChartStyle(
            lineStrokeWidth = 3f,
            dotRadius = 6f,
            areaFillAlpha = 0.2f,
            useShadow = true
        )
    )
    
    // ========== 4. GRADIENT GLASSMORPHISM THEME ==========
    val GradientGlassmorphism = AnalyticsTheme(
        type = AnalyticsThemeType.GRADIENT_GLASSMORPHISM,
        name = "Gradient Glass",
        description = "Modern glass effect with vibrant gradients",
        colors = AnalyticsColorScheme(
            // Backgrounds - Rich gradients
            backgroundPrimary = Color(0xFF1A1A2E),
            backgroundSecondary = Color(0xFF16213E),
            backgroundGradientStart = Color(0xFF0F0C29),
            backgroundGradientEnd = Color(0xFF24243E),
            backgroundGradientMid = Color(0xFF302B63),
            
            // Surfaces - Glass effect
            surfacePrimary = Color(0x25FFFFFF),
            surfaceSecondary = Color(0x18FFFFFF),
            surfaceElevated = Color(0x30FFFFFF),
            surfaceOverlay = Color(0x15FFFFFF),
            
            // Cards - Glass cards
            cardBackground = Color(0x20FFFFFF),
            cardBackgroundElevated = Color(0x28FFFFFF),
            cardBorder = Color(0x30FFFFFF),
            cardShadow = Color(0x30000000),
            
            // Text
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD0D0E0),
            textTertiary = Color(0xFFA0A0B8),
            textOnAccent = Color(0xFFFFFFFF),
            textMuted = Color(0xFF707090),
            
            // Accents - Vibrant gradients
            accentPrimary = Color(0xFF667EEA),
            accentSecondary = Color(0xFFEC4899),
            accentTertiary = Color(0xFF10B981),
            accentGradientStart = Color(0xFF667EEA),
            accentGradientEnd = Color(0xFFEC4899),
            
            // Status - Vibrant
            statusSuccess = Color(0xFF34D399),
            statusWarning = Color(0xFFFBBF24),
            statusError = Color(0xFFF87171),
            statusInfo = Color(0xFF60A5FA),
            statusSuccessLight = Color(0x3034D399),
            statusWarningLight = Color(0x30FBBF24),
            statusErrorLight = Color(0x30F87171),
            statusInfoLight = Color(0x3060A5FA),
            
            // Charts - Gradient palette
            chartPrimary = Color(0xFF667EEA),
            chartSecondary = Color(0xFFEC4899),
            chartTertiary = Color(0xFF34D399),
            chartQuaternary = Color(0xFFFBBF24),
            chartBackground = Color(0x15FFFFFF),
            chartGridLines = Color(0x20FFFFFF),
            chartAxisLabels = Color(0xFFA0A0B8),
            chartTooltipBackground = Color(0xE0302B63),
            chartPalette = listOf(
                Color(0xFF667EEA),
                Color(0xFFEC4899),
                Color(0xFF34D399),
                Color(0xFFFBBF24),
                Color(0xFFF87171),
                Color(0xFF60A5FA)
            ),
            
            // Heatmap - Purple to pink
            heatmapEmpty = Color(0x15FFFFFF),
            heatmapLevel1 = Color(0xFF4C3D99),
            heatmapLevel2 = Color(0xFF7653C8),
            heatmapLevel3 = Color(0xFFA855F7),
            heatmapLevel4 = Color(0xFFD946EF),
            
            // Progress
            progressTrack = Color(0x20FFFFFF),
            progressIndicator = Color(0xFF667EEA),
            progressGlow = Color(0x60667EEA),
            
            // Interactive
            buttonPrimary = Color(0xFF667EEA),
            buttonSecondary = Color(0x25FFFFFF),
            buttonDisabled = Color(0x15FFFFFF),
            iconPrimary = Color(0xFFFFFFFF),
            iconSecondary = Color(0xFFA0A0B8),
            iconAccent = Color(0xFF667EEA),
            
            // Dividers
            divider = Color(0x20FFFFFF),
            border = Color(0x30FFFFFF),
            borderFocused = Color(0xFF667EEA),
            
            // Effects - Glass
            glassOverlay = Color(0x15FFFFFF),
            shimmerBase = Color(0x15FFFFFF),
            shimmerHighlight = Color(0x30FFFFFF),
            neonGlow = Color(0xFF667EEA),
            
            isDark = true
        ),
        shapes = AnalyticsShapeScheme(
            cardCornerRadius = 24.dp,
            buttonCornerRadius = 16.dp,
            chipCornerRadius = 24.dp,
            chartCornerRadius = 20.dp,
            dialogCornerRadius = 32.dp,
            bottomSheetCornerRadius = 36.dp,
            inputCornerRadius = 16.dp,
            avatarCornerRadius = 50.dp,
            heatmapCellCornerRadius = 4.dp,
            progressBarCornerRadius = 10.dp
        ),
        spacing = AnalyticsSpacingScheme(
            cardPadding = 24.dp,
            sectionSpacing = 24.dp
        ),
        elevation = AnalyticsElevationScheme(
            card = 0.dp // Glass cards don't use elevation shadows
        ),
        chartStyle = AnalyticsChartStyle(
            lineStrokeWidth = 3.5f,
            dotRadius = 6f,
            areaFillAlpha = 0.3f,
            useGradientFill = true,
            useShadow = true
        )
    )
    
    // Get theme by type
    fun getTheme(type: AnalyticsThemeType): AnalyticsTheme {
        return when (type) {
            AnalyticsThemeType.MINIMAL_WHITE -> MinimalWhite
            AnalyticsThemeType.DARK_NEON -> DarkNeon
            AnalyticsThemeType.PASTEL_PRODUCTIVITY -> PastelProductivity
            AnalyticsThemeType.GRADIENT_GLASSMORPHISM -> GradientGlassmorphism
        }
    }
    
    // Get all themes
    fun getAllThemes(): List<AnalyticsTheme> {
        return listOf(MinimalWhite, DarkNeon, PastelProductivity, GradientGlassmorphism)
    }
}

// ==================== THEME PROVIDER ====================

val LocalAnalyticsTheme = compositionLocalOf { AnalyticsThemes.MinimalWhite }

@Composable
fun AnalyticsThemeProvider(
    theme: AnalyticsTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAnalyticsTheme provides theme,
        content = content
    )
}

// Extension to get current theme
object AnalyticsThemeAccessor {
    val current: AnalyticsTheme
        @Composable
        get() = LocalAnalyticsTheme.current
}

// ==================== ANIMATED COLOR EXTENSIONS ====================

@Composable
fun animatedAnalyticsColor(
    targetColor: Color,
    durationMillis: Int = 400
): Color {
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis),
        label = "analyticsColorAnimation"
    )
    return animatedColor
}

// ==================== BRUSH HELPERS ====================

fun AnalyticsColorScheme.backgroundGradient(): Brush {
    return if (backgroundGradientMid != null) {
        Brush.verticalGradient(
            colors = listOf(backgroundGradientStart, backgroundGradientMid!!, backgroundGradientEnd)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(backgroundGradientStart, backgroundGradientEnd)
        )
    }
}

fun AnalyticsColorScheme.accentGradient(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(accentGradientStart, accentGradientEnd)
    )
}

fun AnalyticsColorScheme.cardGradient(): Brush {
    return Brush.verticalGradient(
        colors = listOf(cardBackground, cardBackgroundElevated)
    )
}
