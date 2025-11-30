package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.viewmodels.GoalDetailItem
import com.example.todoapp.ui.viewmodels.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Color palette
private val AccentBlue = Color(0xFF2196F3)
private val AccentPurple = Color(0xFF9C27B0)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFF9800)
private val AccentPink = Color(0xFFE91E63)
private val AccentTeal = Color(0xFF009688)
private val StreakFire = Color(0xFFFF5722)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = viewModel(),
    onStartSession: (Long) -> Unit,
    onGoalClick: ((Long) -> Unit)? = null
) {
    val goals by viewModel.goals.collectAsState()
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddBottomSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Goal") },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "🎯 My Goals",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track your learning journey",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Goals summary card
            if (goals.isNotEmpty()) {
                item {
                    GoalsSummaryCard(goals = goals)
                }
            }

            // Empty state
            if (goals.isEmpty()) {
                item {
                    EmptyGoalsState(onAddGoal = { showAddBottomSheet = true })
                }
            }

            // Goal cards
            items(goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onStartSession = { onStartSession(goal.id) },
                    onClick = { onGoalClick?.invoke(goal.id) },
                    onDelete = { goalToDelete = goal.id }
                )
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Goal Bottom Sheet
    if (showAddBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddBottomSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AddGoalBottomSheet(
                onDismiss = { showAddBottomSheet = false },
                onConfirm = { title, dailyTarget, category, duration ->
                    viewModel.addGoal(title, dailyTarget, category, duration)
                    showAddBottomSheet = false
                }
            )
        }
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goalId ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Goal?") },
            text = { Text("This will permanently delete this goal and all its progress. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goalId)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { goalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoalsSummaryCard(goals: List<GoalDetailItem>) {
    val totalGoals = goals.size
    val completedToday = goals.count { it.isCompletedToday }
    val totalStreak = goals.maxOfOrNull { it.streak } ?: 0
    val avgProgress = goals.map { it.overallProgress }.average().toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    value = "$completedToday/$totalGoals",
                    label = "Today",
                    icon = Icons.Default.CheckCircle
                )
                SummaryItem(
                    value = "🔥 $totalStreak",
                    label = "Best Streak",
                    icon = null
                )
                SummaryItem(
                    value = "${(avgProgress * 100).toInt()}%",
                    label = "Overall",
                    icon = Icons.Default.TrendingUp
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String,
    icon: ImageVector?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalDetailItem,
    onStartSession: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.overallProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    val animatedDailyProgress by animateFloatAsState(
        targetValue = goal.progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "dailyProgress"
    )
    
    val categoryColor = getCategoryColor(goal.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Title and Category
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = goal.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Streak Badge
                if (goal.streak > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StreakFire.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${goal.streak} days",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StreakFire
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Progress Section
            Text(
                text = "Overall Progress",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = categoryColor,
                    trackColor = categoryColor.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Outlined.CalendarToday,
                    text = goal.endDate,
                    label = "End Date"
                )
                InfoChip(
                    icon = Icons.Outlined.Timer,
                    text = "${goal.dailyTargetMinutes} min",
                    label = "Daily Target"
                )
                InfoChip(
                    icon = Icons.Outlined.Schedule,
                    text = "${goal.daysRemaining}",
                    label = "Days Left"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Today's Progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (goal.isCompletedToday) 
                        AccentGreen.copy(alpha = 0.1f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${goal.totalMinutesToday}/${goal.dailyTargetMinutes} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (goal.isCompletedToday) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Circular progress
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicatorCustom(
                            progress = animatedDailyProgress,
                            color = if (goal.isCompletedToday) AccentGreen else categoryColor
                        )
                        if (goal.isCompletedToday) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "${(animatedDailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                
                // Start Session button
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (goal.isCompletedToday) AccentGreen else categoryColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (goal.isCompletedToday) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (goal.isCompletedToday) "Completed!" else "Start Session")
                }
            }
        }
    }
}

@Composable
private fun CircularProgressIndicatorCustom(
    progress: Float,
    color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        
        // Background track
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        
        // Progress arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyGoalsState(onAddGoal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Flag,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Goals Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start your learning journey by creating your first goal!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddGoal,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create First Goal")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dailyTarget by remember { mutableStateOf("30") }
    var category by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(1) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    val categories = listOf("Programming", "Languages", "Math", "Science", "Reading", "Music", "Fitness", "Other")
    val durations = listOf(
        1 to "1 Month",
        3 to "3 Months", 
        6 to "6 Months",
        12 to "1 Year"
    )
    
    // Calculate end date
    val endDateText = remember(selectedDuration) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, selectedDuration)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎯 New Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Goal Title") },
            placeholder = { Text("e.g., Learn Python") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("Select or type category") },
                singleLine = true,
                readOnly = false,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null) }
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(option))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(option) 
                            }
                        },
                        onClick = {
                            category = option
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Duration selection
        Text(
            text = "Duration",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durations.forEach { (months, label) ->
                FilterChip(
                    selected = selectedDuration == months,
                    onClick = { selectedDuration = months },
                    label = { Text(label, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        // Auto-filled end date
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "End Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = endDateText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Daily target
        OutlinedTextField(
            value = dailyTarget,
            onValueChange = { dailyTarget = it.filter { c -> c.isDigit() } },
            label = { Text("Daily Target (minutes)") },
            placeholder = { Text("30") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
            suffix = { Text("min/day") }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Create button
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onConfirm(title, dailyTarget.toIntOrNull() ?: 30, category, selectedDuration)
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Goal", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "programming", "coding", "development" -> AccentBlue
        "languages", "language" -> AccentPurple
        "math", "mathematics" -> AccentOrange
        "science" -> AccentTeal
        "reading", "books" -> Color(0xFF795548)
        "music", "art" -> AccentPink
        "fitness", "health", "exercise" -> AccentGreen
        else -> Color(0xFF607D8B)
    }
}
