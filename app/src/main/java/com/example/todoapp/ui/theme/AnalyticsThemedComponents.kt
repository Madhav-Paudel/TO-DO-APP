package com.example.todoapp.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Themed Components for Analytics Dashboard
 * Components that automatically adapt to the current analytics theme
 */

// ==================== THEMED CARD ====================

@Composable
fun ThemedAnalyticsCard(
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    useGlassEffect: Boolean = theme.type == AnalyticsThemeType.GRADIENT_GLASSMORPHISM,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(theme.shapes.cardCornerRadius)
    
    val cardModifier = if (useGlassEffect) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.colors.cardBackground)
            .border(1.dp, theme.colors.cardBorder, shape)
    } else {
        modifier
            .fillMaxWidth()
            .shadow(theme.elevation.card, shape)
            .clip(shape)
            .background(theme.colors.cardBackground)
    }
    
    Column(
        modifier = cardModifier.padding(theme.spacing.cardPadding),
        content = content
    )
}

// ==================== THEMED SECTION HEADER ====================

@Composable
fun ThemedSectionHeader(
    emoji: String,
    title: String,
    subtitle: String? = null,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(theme.spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.colors.textSecondary
                )
            }
        }
        action?.invoke()
    }
}

// ==================== THEMED BUTTON ====================

@Composable
fun ThemedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val backgroundColor = if (enabled) theme.colors.buttonPrimary else theme.colors.buttonDisabled
    val textColor = if (enabled) theme.colors.textOnAccent else theme.colors.textTertiary
    
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(theme.shapes.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = theme.colors.buttonDisabled,
            disabledContentColor = theme.colors.textTertiary
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ThemedSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(theme.shapes.buttonCornerRadius),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                listOf(theme.colors.accentPrimary, theme.colors.accentSecondary)
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = theme.colors.accentPrimary
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

// ==================== THEMED CHIP / TAB ====================

@Composable
fun ThemedFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) theme.colors.accentPrimary else theme.colors.surfaceSecondary,
        animationSpec = tween(200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) theme.colors.textOnAccent else theme.colors.textSecondary,
        animationSpec = tween(200),
        label = "chipText"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.shapes.chipCornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun ThemedTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.chipCornerRadius))
            .background(theme.colors.surfaceSecondary)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) theme.colors.cardBackground else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) theme.colors.accentPrimary else theme.colors.textTertiary,
                animationSpec = tween(200),
                label = "tabText"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(theme.shapes.chipCornerRadius - 4.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

// ==================== THEMED PROGRESS BAR ====================

@Composable
fun ThemedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    useGradient: Boolean = true,
    showGlow: Boolean = theme.type == AnalyticsThemeType.DARK_NEON
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(theme.spacing.sm)
            .clip(RoundedCornerShape(theme.shapes.progressBarCornerRadius))
            .background(theme.colors.progressTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(theme.shapes.progressBarCornerRadius))
                .then(
                    if (showGlow) {
                        Modifier.shadow(4.dp, RoundedCornerShape(theme.shapes.progressBarCornerRadius), spotColor = theme.colors.progressGlow)
                    } else Modifier
                )
                .background(
                    if (useGradient) {
                        Brush.horizontalGradient(
                            listOf(theme.colors.accentGradientStart, theme.colors.accentGradientEnd)
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(theme.colors.progressIndicator, theme.colors.progressIndicator)
                        )
                    }
                )
        )
    }
}

// ==================== THEMED STAT CARD ====================

@Composable
fun ThemedStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    valueColor: Color = theme.colors.accentPrimary
) {
    ThemedAnalyticsCard(
        modifier = modifier,
        theme = theme
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(theme.spacing.sm))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = theme.colors.textSecondary
            )
        }
    }
}

// ==================== THEMED STATUS BADGE ====================

@Composable
fun ThemedStatusBadge(
    text: String,
    status: StatusType,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current
) {
    val (backgroundColor, textColor) = when (status) {
        StatusType.SUCCESS -> theme.colors.statusSuccessLight to theme.colors.statusSuccess
        StatusType.WARNING -> theme.colors.statusWarningLight to theme.colors.statusWarning
        StatusType.ERROR -> theme.colors.statusErrorLight to theme.colors.statusError
        StatusType.INFO -> theme.colors.statusInfoLight to theme.colors.statusInfo
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.shapes.chipCornerRadius))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

enum class StatusType {
    SUCCESS, WARNING, ERROR, INFO
}

// ==================== THEMED ICON BUTTON ====================

@Composable
fun ThemedIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    tint: Color = theme.colors.iconPrimary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
    }
}

// ==================== THEMED DIVIDER ====================

@Composable
fun ThemedDivider(
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current
) {
    Divider(
        modifier = modifier,
        color = theme.colors.divider,
        thickness = 1.dp
    )
}

// ==================== THEMED EMPTY STATE ====================

@Composable
fun ThemedEmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    action: @Composable (() -> Unit)? = null
) {
    ThemedAnalyticsCard(
        modifier = modifier,
        theme = theme
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(theme.spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(theme.spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.colors.textSecondary
            )
            action?.let {
                Spacer(modifier = Modifier.height(theme.spacing.lg))
                it()
            }
        }
    }
}

// ==================== THEME SELECTOR ====================

@Composable
fun AnalyticsThemeSelector(
    currentTheme: AnalyticsThemeType,
    onThemeSelected: (AnalyticsThemeType) -> Unit,
    modifier: Modifier = Modifier
) {
    val themes = AnalyticsThemes.getAllThemes()
    val currentThemeObj = LocalAnalyticsTheme.current
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Dashboard Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = currentThemeObj.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        themes.forEach { theme ->
            val isSelected = currentTheme == theme.type
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(currentThemeObj.shapes.cardCornerRadius))
                    .background(
                        if (isSelected) currentThemeObj.colors.accentPrimary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) currentThemeObj.colors.accentPrimary else currentThemeObj.colors.border,
                        shape = RoundedCornerShape(currentThemeObj.shapes.cardCornerRadius)
                    )
                    .clickable { onThemeSelected(theme.type) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme color preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        theme.colors.accentPrimary,
                        theme.colors.accentSecondary,
                        theme.colors.statusSuccess,
                        theme.colors.chartPrimary
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = currentThemeObj.colors.textPrimary
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = currentThemeObj.colors.textSecondary
                    )
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = currentThemeObj.colors.accentPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ==================== ANIMATED BACKGROUND ====================

@Composable
fun ThemedAnimatedBackground(
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    content: @Composable () -> Unit
) {
    val animatedStart by animateColorAsState(
        targetValue = theme.colors.backgroundGradientStart,
        animationSpec = tween(600),
        label = "bgStart"
    )
    val animatedEnd by animateColorAsState(
        targetValue = theme.colors.backgroundGradientEnd,
        animationSpec = tween(600),
        label = "bgEnd"
    )
    val animatedMid by animateColorAsState(
        targetValue = theme.colors.backgroundGradientMid ?: theme.colors.backgroundGradientStart,
        animationSpec = tween(600),
        label = "bgMid"
    )
    
    val gradient = if (theme.colors.backgroundGradientMid != null) {
        Brush.verticalGradient(listOf(animatedStart, animatedMid, animatedEnd))
    } else {
        Brush.verticalGradient(listOf(animatedStart, animatedEnd))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        content()
    }
}

// ==================== NEON GLOW BOX ====================

@Composable
fun NeonGlowBox(
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    glowColor: Color = theme.colors.neonGlow ?: theme.colors.accentPrimary,
    glowRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (theme.type == AnalyticsThemeType.DARK_NEON && theme.colors.neonGlow != null) {
        Box(
            modifier = modifier
        ) {
            // Glow layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(glowRadius)
                    .background(glowColor.copy(alpha = 0.3f))
            )
            // Content
            Box(content = content)
        }
    } else {
        Box(
            modifier = modifier,
            content = content
        )
    }
}

// ==================== GLASS CARD ====================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme = LocalAnalyticsTheme.current,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(theme.shapes.cardCornerRadius)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.colors.glassOverlay)
            .border(1.dp, theme.colors.cardBorder, shape)
            .padding(theme.spacing.cardPadding),
        content = content
    )
}
