package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.data.local.ModelQuality
import com.example.todoapp.data.local.SyncOption
import com.example.todoapp.data.local.ThemeMode
import com.example.todoapp.ui.viewmodels.ExportStatus
import com.example.todoapp.ui.viewmodels.ImportStatus
import com.example.todoapp.ui.viewmodels.SettingsViewModel

// Color palette
private val AccentPurple = Color(0xFF7C4DFF)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFF9800)
private val AccentRed = Color(0xFFE53935)
private val AccentTeal = Color(0xFF00BCD4)
private val HeaderGradientStart = Color(0xFF667eea)
private val HeaderGradientEnd = Color(0xFF764ba2)
private val CardBackground = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            SettingsHeader(
                userName = settings.userName.ifEmpty { "Guest User" },
                userEmail = settings.userEmail.ifEmpty { "Not signed in" }
            )
        }
        
        // Notifications Section
        item {
            SettingsSectionTitle(
                title = "Notifications",
                icon = Icons.Outlined.Notifications,
                color = AccentOrange
            )
        }
        
        item {
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Push Notifications",
                    subtitle = "Get reminders for your goals and tasks",
                    checked = settings.pushNotificationsEnabled,
                    onCheckedChange = { viewModel.togglePushNotifications(it) }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsToggleItem(
                    icon = Icons.Outlined.Email,
                    title = "Email Reminders",
                    subtitle = "Receive weekly progress summaries",
                    checked = settings.emailRemindersEnabled,
                    onCheckedChange = { viewModel.toggleEmailReminders(it) }
                )
            }
        }
        
        // Appearance Section
        item {
            SettingsSectionTitle(
                title = "Appearance",
                icon = Icons.Outlined.Palette,
                color = AccentPurple
            )
        }
        
        item {
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = when (settings.themeMode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "System default"
                    },
                    onClick = { viewModel.showThemeDialog() }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = getLanguageName(settings.language),
                    onClick = { viewModel.showLanguageDialog() }
                )
            }
        }
        
        // AI Assistant Section
        item {
            SettingsSectionTitle(
                title = "AI Assistant",
                icon = Icons.Outlined.Psychology,
                color = AccentTeal
            )
        }
        
        item {
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Enable AI Coach",
                    subtitle = "Get personalized productivity tips",
                    checked = settings.aiAssistantEnabled,
                    onCheckedChange = { viewModel.toggleAiEnabled(it) }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Local Model Status
                val hasModel = uiState.installedModels.isNotEmpty()
                val modelStatusText = if (hasModel) {
                    val model = uiState.installedModels.firstOrNull { it.info.id == uiState.selectedModelId }
                        ?: uiState.installedModels.first()
                    "Model: ${model.info.name}"
                } else {
                    "No model installed"
                }
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Memory,
                    title = "On-Device Model",
                    subtitle = modelStatusText,
                    onClick = { /* Navigate to model management or show info */ },
                    enabled = settings.aiAssistantEnabled
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Speed,
                    title = "Model Quality",
                    subtitle = when (settings.modelQuality) {
                        ModelQuality.FAST -> "Fast (smaller model)"
                        ModelQuality.BALANCED -> "Balanced (recommended)"
                    },
                    onClick = { viewModel.showModelQualityDialog() },
                    enabled = settings.aiAssistantEnabled
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Tune,
                    title = "AI Settings",
                    subtitle = "Response style: ${settings.aiResponseStyle.replaceFirstChar { it.uppercase() }}",
                    onClick = { viewModel.showAiSettingsDialog() },
                    enabled = settings.aiAssistantEnabled
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Storage info
                SettingsInfoItem(
                    icon = Icons.Outlined.Storage,
                    title = "Available Storage",
                    subtitle = viewModel.getStorageInfo()
                )
            }
        }
        
        // Data & Sync Section
        item {
            SettingsSectionTitle(
                title = "Data & Sync",
                icon = Icons.Outlined.CloudSync,
                color = AccentBlue
            )
        }
        
        item {
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Outlined.Cloud,
                    title = "Sync Option",
                    subtitle = when (settings.syncOption) {
                        SyncOption.GOOGLE_DRIVE -> "Google Drive"
                        SyncOption.LOCAL_STORAGE -> "Local Storage Only"
                    },
                    onClick = { viewModel.showSyncDialog() }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsToggleItem(
                    icon = Icons.Outlined.Sync,
                    title = "Auto Sync",
                    subtitle = "Sync data automatically",
                    checked = settings.autoSyncEnabled,
                    onCheckedChange = { viewModel.toggleAutoSync(it) },
                    enabled = settings.syncOption == SyncOption.GOOGLE_DRIVE
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsActionItem(
                    icon = Icons.Outlined.Upload,
                    title = "Export Data",
                    subtitle = "Save your data to a file",
                    actionStatus = when (uiState.exportStatus) {
                        ExportStatus.Idle -> null
                        ExportStatus.Exporting -> "Exporting..."
                        ExportStatus.Success -> "Exported successfully!"
                        ExportStatus.Error -> "Export failed"
                    },
                    isLoading = uiState.exportStatus == ExportStatus.Exporting,
                    onClick = { viewModel.exportData() }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsActionItem(
                    icon = Icons.Outlined.Download,
                    title = "Import Data",
                    subtitle = "Restore from a backup file",
                    actionStatus = when (uiState.importStatus) {
                        ImportStatus.Idle -> null
                        ImportStatus.Importing -> "Importing..."
                        ImportStatus.Success -> "Imported successfully!"
                        ImportStatus.Error -> "Import failed"
                    },
                    isLoading = uiState.importStatus == ImportStatus.Importing,
                    onClick = { viewModel.importData() }
                )
            }
        }
        
        // Account Section
        item {
            SettingsSectionTitle(
                title = "Account",
                icon = Icons.Outlined.Person,
                color = AccentGreen
            )
        }
        
        item {
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Outlined.AccountCircle,
                    title = "Profile",
                    subtitle = if (settings.userName.isNotEmpty()) settings.userName else "Set up your profile",
                    onClick = { viewModel.showProfileDialog() }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Logout,
                    title = "Sign Out",
                    subtitle = "Log out of your account",
                    onClick = { viewModel.showLogoutDialog() },
                    textColor = AccentRed
                )
            }
        }
        
        // Support Section
        item {
            SettingsSectionTitle(
                title = "Help & Support",
                icon = Icons.Outlined.Help,
                color = AccentOrange
            )
        }
        
        item {
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Outlined.QuestionAnswer,
                    title = "FAQ",
                    subtitle = "Frequently asked questions",
                    onClick = { /* Open FAQ */ }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Chat,
                    title = "Contact Support",
                    subtitle = "Get help from our team",
                    onClick = { /* Open support */ }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Star,
                    title = "Rate the App",
                    subtitle = "Leave a review on the Play Store",
                    onClick = { /* Open play store */ }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Share,
                    title = "Share App",
                    subtitle = "Invite friends to be productive",
                    onClick = { /* Share app */ }
                )
            }
        }
        
        // Danger Zone
        item {
            SettingsSectionTitle(
                title = "Danger Zone",
                icon = Icons.Outlined.Warning,
                color = AccentRed
            )
        }
        
        item {
            SettingsCard(
                borderColor = AccentRed.copy(alpha = 0.3f)
            ) {
                SettingsClickableItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Delete All Data",
                    subtitle = "Permanently remove all your data",
                    onClick = { viewModel.showDeleteDataDialog() },
                    textColor = AccentRed
                )
            }
        }
        
        // App Info
        item {
            AppInfoFooter()
        }
    }
    
    // Dialogs
    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = settings.themeMode,
            onThemeSelected = { viewModel.updateTheme(it) },
            onDismiss = { viewModel.dismissThemeDialog() }
        )
    }
    
    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = settings.language,
            onLanguageSelected = { viewModel.updateLanguage(it) },
            onDismiss = { viewModel.dismissLanguageDialog() }
        )
    }
    
    if (uiState.showAiSettingsDialog) {
        AiSettingsDialog(
            currentStyle = settings.aiResponseStyle,
            onStyleSelected = { viewModel.updateAiResponseStyle(it) },
            onDismiss = { viewModel.dismissAiSettingsDialog() }
        )
    }
    
    if (uiState.showModelQualityDialog) {
        ModelQualityDialog(
            currentQuality = settings.modelQuality,
            onQualitySelected = { viewModel.updateModelQuality(it) },
            onDismiss = { viewModel.dismissModelQualityDialog() }
        )
    }
    
    if (uiState.showSyncDialog) {
        SyncOptionsDialog(
            currentOption = settings.syncOption,
            onOptionSelected = { viewModel.updateSyncOption(it) },
            onDismiss = { viewModel.dismissSyncDialog() }
        )
    }
    
    if (uiState.showProfileDialog) {
        ProfileEditDialog(
            currentName = settings.userName,
            currentEmail = settings.userEmail,
            onSave = { name, email -> viewModel.updateProfile(name, email) },
            onDismiss = { viewModel.dismissProfileDialog() }
        )
    }
    
    if (uiState.showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out? Your local data will be preserved.",
            confirmText = "Sign Out",
            confirmColor = AccentRed,
            onConfirm = { viewModel.logout() },
            onDismiss = { viewModel.dismissLogoutDialog() }
        )
    }
    
    if (uiState.showDeleteDataDialog) {
        ConfirmationDialog(
            title = "Delete All Data",
            message = "This action cannot be undone. All your goals, tasks, and progress will be permanently deleted.",
            confirmText = "Delete Everything",
            confirmColor = AccentRed,
            onConfirm = { viewModel.deleteAllData() },
            onDismiss = { viewModel.dismissDeleteDataDialog() }
        )
    }
}

@Composable
private fun SettingsHeader(
    userName: String,
    userEmail: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(HeaderGradientStart, HeaderGradientEnd)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsCard(
    borderColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) AccentPurple else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentPurple
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) {
                if (textColor != MaterialTheme.colorScheme.onSurface) textColor else AccentPurple
            } else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) textColor else Color.Gray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.6f)
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionStatus: String?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = actionStatus ?: subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    actionStatus?.contains("success", ignoreCase = true) == true -> AccentGreen
                    actionStatus?.contains("fail", ignoreCase = true) == true -> AccentRed
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AccentBlue
            )
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AppInfoFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AccentPurple,
            modifier = Modifier.size(40.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "ProductivityPro",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Version 1.0.0 (Build 1)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Made with ❤️ for productivity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

// ===== DIALOGS =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Choose Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                ThemeOption(
                    icon = Icons.Outlined.LightMode,
                    title = "Light",
                    subtitle = "Always use light theme",
                    selected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )
                
                ThemeOption(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark",
                    subtitle = "Always use dark theme",
                    selected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )
                
                ThemeOption(
                    icon = Icons.Outlined.Smartphone,
                    title = "System",
                    subtitle = "Follow system settings",
                    selected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(animatedScale)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AccentPurple.copy(alpha = 0.1f) else Color.Transparent
        ),
        border = if (selected) BorderStroke(2.dp, AccentPurple) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) AccentPurple else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "hi" to "हिन्दी"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Select Language",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                languages.forEach { (code, name) ->
                    LanguageOption(
                        name = name,
                        selected = currentLanguage == code,
                        onClick = { onLanguageSelected(code) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(if (selected) AccentPurple.copy(alpha = 0.1f) else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = AccentPurple,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AiSettingsDialog(
    currentStyle: String,
    onStyleSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val styles = listOf(
        "concise" to Pair("Concise", "Short, to-the-point responses"),
        "balanced" to Pair("Balanced", "Moderate detail level"),
        "detailed" to Pair("Detailed", "Comprehensive explanations")
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI Response Style",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                styles.forEach { (key, value) ->
                    val (title, description) = value
                    ThemeOption(
                        icon = when (key) {
                            "concise" -> Icons.Outlined.ShortText
                            "balanced" -> Icons.Outlined.Notes
                            else -> Icons.Outlined.Article
                        },
                        title = title,
                        subtitle = description,
                        selected = currentStyle == key,
                        onClick = { onStyleSelected(key) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelQualityDialog(
    currentQuality: ModelQuality,
    onQualitySelected: (ModelQuality) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Model Quality",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose between faster responses or higher quality",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                ThemeOption(
                    icon = Icons.Outlined.FlashOn,
                    title = "Fast",
                    subtitle = "Smaller model, quicker responses (~500MB)",
                    selected = currentQuality == ModelQuality.FAST,
                    onClick = { onQualitySelected(ModelQuality.FAST) }
                )
                
                ThemeOption(
                    icon = Icons.Outlined.Balance,
                    title = "Balanced",
                    subtitle = "Better quality, moderate speed (~1.5GB)",
                    selected = currentQuality == ModelQuality.BALANCED,
                    onClick = { onQualitySelected(ModelQuality.BALANCED) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncOptionsDialog(
    currentOption: SyncOption,
    onOptionSelected: (SyncOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Data Storage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                ThemeOption(
                    icon = Icons.Outlined.CloudQueue,
                    title = "Google Drive",
                    subtitle = "Sync across all your devices",
                    selected = currentOption == SyncOption.GOOGLE_DRIVE,
                    onClick = { onOptionSelected(SyncOption.GOOGLE_DRIVE) }
                )
                
                ThemeOption(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Local Storage",
                    subtitle = "Keep data on this device only",
                    selected = currentOption == SyncOption.LOCAL_STORAGE,
                    onClick = { onOptionSelected(SyncOption.LOCAL_STORAGE) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditDialog(
    currentName: String,
    currentEmail: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, email) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = confirmColor,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

// Helper function
private fun getLanguageName(code: String): String {
    return when (code) {
        "en" -> "English"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "zh" -> "中文"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "hi" -> "हिन्दी"
        else -> "English"
    }
}
