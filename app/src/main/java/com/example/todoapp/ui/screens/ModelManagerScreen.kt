package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.llm.*
import kotlinx.coroutines.launch

// Color palette
private val PrimaryPurple = Color(0xFF7C4DFF)
private val PrimaryBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFF9800)
private val AccentRed = Color(0xFFE53935)
private val CardBackground = Color(0xFFF8F9FF)
private val GradientStart = Color(0xFF667eea)
private val GradientEnd = Color(0xFF764ba2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    modelManager: ModelManager = remember { 
        // This should ideally come from DI/ViewModel
        throw IllegalStateException("ModelManager must be provided")
    },
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val downloadState by modelManager.downloadState.collectAsState()
    val installedModels by modelManager.installedModels.collectAsState()
    val selectedModelId by modelManager.selectedModelId.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showSpaceWarning by remember { mutableStateOf<ModelInfo?>(null) }
    
    val availableSpace = remember { modelManager.getAvailableSpaceBytes() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "On-Device Models",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "llama.cpp inference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Storage info card
            item {
                StorageInfoCard(
                    availableSpace = availableSpace,
                    modelManager = modelManager
                )
            }
            
            // Download progress card (if downloading)
            item {
                AnimatedVisibility(
                    visible = downloadState is DownloadState.Downloading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val state = downloadState as? DownloadState.Downloading
                    if (state != null) {
                        DownloadProgressCard(
                            state = state,
                            modelManager = modelManager,
                            onCancel = { modelManager.cancelDownload() }
                        )
                    }
                }
            }
            
            // Section header - Installed Models
            if (installedModels.isNotEmpty()) {
                item {
                    SectionHeader(
                        emoji = "✅",
                        title = "Installed Models",
                        subtitle = "${installedModels.size} model(s) ready"
                    )
                }
                
                items(installedModels) { installed ->
                    InstalledModelCard(
                        installed = installed,
                        isSelected = installed.info.id == selectedModelId,
                        modelManager = modelManager,
                        onSelect = { modelManager.selectModel(installed.info.id) },
                        onDelete = { showDeleteDialog = installed.info.id }
                    )
                }
            }
            
            // Section header - Available Models
            item {
                SectionHeader(
                    emoji = "📦",
                    title = "Available Models",
                    subtitle = "Download to enable on-device inference"
                )
            }
            
            // Available models (not installed)
            val notInstalled = ModelManager.AVAILABLE_MODELS.filter { model ->
                installedModels.none { it.info.id == model.id }
            }
            
            items(notInstalled) { modelInfo ->
                AvailableModelCard(
                    modelInfo = modelInfo,
                    modelManager = modelManager,
                    downloadState = downloadState,
                    onDownload = {
                        if (modelManager.hasEnoughSpace(modelInfo.sizeBytes)) {
                            scope.launch {
                                modelManager.downloadModel(modelInfo)
                            }
                        } else {
                            showSpaceWarning = modelInfo
                        }
                    }
                )
            }
            
            // Info card about on-device inference
            item {
                InfoCard()
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { modelId ->
        val modelInfo = modelManager.getModelInfo(modelId)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed) },
            title = { Text("Delete Model?") },
            text = {
                Text("Delete ${modelInfo?.name ?: "this model"}? You can download it again later.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        modelManager.deleteModel(modelId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Space warning dialog
    showSpaceWarning?.let { modelInfo ->
        AlertDialog(
            onDismissRequest = { showSpaceWarning = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange) },
            title = { Text("Insufficient Storage") },
            text = {
                Column {
                    Text("Not enough free space to download ${modelInfo.name}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Required: ${modelManager.formatSize(modelInfo.sizeBytes)}")
                    Text("Available: ${modelManager.formatSize(availableSpace)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpaceWarning = null }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Download completed snackbar effect
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Completed) {
            // Auto-reset after showing completion
            kotlinx.coroutines.delay(2000)
            modelManager.resetDownloadState()
        }
    }
}

@Composable
private fun StorageInfoCard(
    availableSpace: Long,
    modelManager: ModelManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryBlue.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Available Storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = modelManager.formatSize(availableSpace),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    state: DownloadState.Downloading,
    modelManager: ModelManager,
    onCancel: () -> Unit
) {
    val modelInfo = modelManager.getModelInfo(state.modelId)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryPurple.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloading ${modelInfo?.name ?: "model"}...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = state.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryPurple,
                trackColor = PrimaryPurple.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${modelManager.formatSize(state.downloadedBytes)} / ${modelManager.formatSize(state.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    emoji: String,
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "$emoji $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InstalledModelCard(
    installed: InstalledModel,
    isSelected: Boolean,
    modelManager: ModelManager,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    AccentGreen,
                    RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentGreen.copy(alpha = 0.1f) else CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AccentGreen.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = installed.info.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = installed.info.parameters + " parameters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = modelManager.formatSize(installed.sizeOnDisk),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = AccentRed.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AvailableModelCard(
    modelInfo: ModelInfo,
    modelManager: ModelManager,
    downloadState: DownloadState,
    onDownload: () -> Unit
) {
    val isDownloading = downloadState is DownloadState.Downloading && 
            (downloadState as DownloadState.Downloading).modelId == modelInfo.id
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelInfo.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = modelInfo.parameters + " parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Size badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = modelManager.formatSize(modelInfo.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = modelInfo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onDownload,
                enabled = !isDownloading && downloadState !is DownloadState.Downloading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple
                )
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AccentOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About On-Device Inference",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "• Models run entirely on your device using llama.cpp\n" +
                       "• No internet required after download\n" +
                       "• Your conversations stay private\n" +
                       "• Performance depends on device capabilities\n" +
                       "• Recommended: 4GB+ RAM for 1B models",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Preview-safe version that creates its own ModelManager
 */
@Composable
fun ModelManagerScreenWithContext(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    
    ModelManagerScreen(
        modelManager = modelManager,
        onNavigateBack = onNavigateBack
    )
}
