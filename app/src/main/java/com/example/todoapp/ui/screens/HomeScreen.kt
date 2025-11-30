package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.viewmodels.HomeGoalItem
import com.example.todoapp.ui.viewmodels.HomeTaskItem
import com.example.todoapp.ui.viewmodels.HomeUiState
import com.example.todoapp.ui.viewmodels.HomeViewModel
import com.example.todoapp.ui.viewmodels.TodayOverviewData
import com.example.todoapp.util.MotivationalQuotes
import kotlinx.coroutines.delay
import kotlin.random.Random

// Color palette
private val StudyGreen = Color(0xFF4CAF50)
private val StudyGreenLight = Color(0xFF81C784)
private val PhoneOrange = Color(0xFFFF9800)
private val PhoneOrangeLight = Color(0xFFFFB74D)
private val TaskBlue = Color(0xFF2196F3)
private val TaskBlueLight = Color(0xFF64B5F6)
private val StreakFire = Color(0xFFFF5722)
private val PriorityHigh = Color(0xFFE53935)
private val PriorityMedium = Color(0xFFFFA726)
private val PriorityLow = Color(0xFF66BB6A)
private val ConfettiColors = listOf(
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
    Color(0xFFFFEB3B)
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToGoals: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToTimer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val homeState by viewModel.homeState.collectAsState()
    val greeting = remember { MotivationalQuotes.getGreeting() }
    
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            // Header with greeting
            item {
                GreetingHeader(
                    greeting = greeting,
                    onSettingsClick = onNavigateToSettings
                )
            }

            // Today Overview Banner
            item {
                TodayOverviewBanner(
                    overview = homeState.todayOverview,
                    onTimerClick = onNavigateToTimer
                )
            }

            // Today's Goals Section
            item {
                SectionHeader(
                    title = "🎯 Today's Goals",
                    subtitle = if (homeState.goals.isNotEmpty()) 
                        "${homeState.goals.count { it.isCompletedToday }}/${homeState.goals.size} completed" 
                    else null,
                    actionLabel = "View All",
                    onActionClick = onNavigateToGoals
                )
            }

            if (homeState.goals.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Flag,
                        title = "No Active Goals",
                        subtitle = "Set a learning goal to track your progress!",
                        actionLabel = "Add Goal",
                        onActionClick = { showAddGoalDialog = true }
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(homeState.goals, key = { it.id }) { goal ->
                            GoalDashboardCard(
                                goal = goal,
                                onStartSession = onNavigateToTimer
                            )
                        }
                    }
                }
            }

            // Today's Tasks Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "✅ Today's Tasks",
                    subtitle = if (homeState.tasks.isNotEmpty())
                        "${homeState.todayOverview.completedTasks}/${homeState.todayOverview.totalTasks} done"
                    else null,
                    actionLabel = "View All",
                    onActionClick = onNavigateToTasks
                )
            }

            if (homeState.tasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Assignment,
                        title = "No Tasks Today",
                        subtitle = "Add tasks to organize your day!",
                        actionLabel = "Add Task",
                        onActionClick = { showAddTaskDialog = true }
                    )
                }
            } else {
                items(homeState.tasks, key = { it.id }) { task ->
                    TaskDashboardCard(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task.id) }
                    )
                }
            }

            // Quick Tips Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                QuickTipCard()
            }
        }

        // Floating Action Buttons
        FloatingActionButtons(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            onAddGoal = onNavigateToGoals,
            onAddTask = onNavigateToTasks
        )

        // Confetti Animation
        if (homeState.showConfetti && homeState.allTasksCompleted) {
            ConfettiAnimation()
        }
    }
}

@Composable
private fun GreetingHeader(
    greeting: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Let's make today productive! ✨",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayOverviewBanner(
    overview: TodayOverviewData,
    onTimerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
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
                            Color(0xFF764ba2),
                            Color(0xFFF093fb)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onTimerClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start Timer",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress Rings Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Study Progress Ring
                    ProgressRingItem(
                        progress = overview.studyProgress,
                        value = "${overview.totalStudyMinutes}m",
                        label = "Study",
                        goal = "${overview.studyGoalMinutes}m goal",
                        color = StudyGreenLight,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // Phone Usage Ring
                    ProgressRingItem(
                        progress = (1f - overview.phoneProgress).coerceIn(0f, 1f),
                        value = "${overview.phoneUsageMinutes}m",
                        label = "Phone",
                        goal = "${overview.phoneUsageLimit}m limit",
                        color = if (overview.phoneProgress > 1f) PriorityHigh else PhoneOrangeLight,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // Tasks Progress Ring
                    ProgressRingItem(
                        progress = overview.taskProgress,
                        value = "${overview.completedTasks}/${overview.totalTasks}",
                        label = "Tasks",
                        goal = "completed",
                        color = TaskBlueLight,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Motivational Line
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = overview.motivationalLine,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRingItem(
    progress: Float,
    value: String,
    label: String,
    goal: String,
    color: Color,
    trackColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Progress
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = goal,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String?,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.contains("/") && it.split("/")[0] == it.split("/").getOrNull(1)?.split(" ")?.get(0))
                        StudyGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onActionClick) {
            Text(actionLabel)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GoalDashboardCard(
    goal: HomeGoalItem,
    onStartSession: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.todayProgress,
        animationSpec = tween(durationMillis = 800),
        label = "goalProgress"
    )

    Card(
        modifier = Modifier
            .width(180.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (goal.isCompletedToday) 
                StudyGreen.copy(alpha = 0.1f) 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with category and streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getCategoryColor(goal.category).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = goal.category.take(10),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = getCategoryColor(goal.category),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Streak badge
                if (goal.currentStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${goal.currentStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StreakFire
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Goal title
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goal.todayMinutes}m",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (goal.isCompletedToday) StudyGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " / ${goal.dailyTargetMinutes}m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            @Suppress("DEPRECATION")
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.isCompletedToday) StudyGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Start Session Button
            if (!goal.isCompletedToday) {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Session", fontSize = 12.sp)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = StudyGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StudyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Done for today!",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudyGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskDashboardCard(
    task: HomeTaskItem,
    onToggle: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "taskScale"
    )
    
    val priorityColor = when (task.priority) {
        3 -> PriorityHigh
        2 -> PriorityMedium
        else -> PriorityLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.isCompleted) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (task.isCompleted) priorityColor.copy(alpha = 0.3f)
                        else priorityColor
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Checkbox with animation
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = StudyGreen,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Task content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Priority label
                Text(
                    text = when (task.priority) {
                        3 -> "🔴 High Priority"
                        2 -> "🟠 Medium"
                        else -> "🟢 Low"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else priorityColor
                )
            }
            
            // Completion icon
            AnimatedVisibility(
                visible = task.isCompleted,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StudyGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun QuickTipCard() {
    val tip = remember { MotivationalQuotes.getRandomTip() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "💡 Quick Tip",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FloatingActionButtons(
    modifier: Modifier = Modifier,
    onAddGoal: () -> Unit,
    onAddTask: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fabRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub FABs
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add Goal FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onAddGoal()
                    },
                    containerColor = StudyGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Goal")
                }
                
                // Add Task FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onAddTask()
                    },
                    containerColor = TaskBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Task")
                }
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun ConfettiAnimation() {
    val particles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                color = ConfettiColors.random(),
                speed = Random.nextFloat() * 2f + 1f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }
    
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val duration = 3000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            animationProgress = (System.currentTimeMillis() - startTime).toFloat() / duration
            delay(16)
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val currentY = particle.y + animationProgress * particle.speed
            if (currentY <= 1f) {
                val x = particle.x * size.width + 
                    kotlin.math.sin((animationProgress * 10 + particle.rotation).toDouble()).toFloat() * 30
                val y = currentY * size.height
                
                drawCircle(
                    color = particle.color.copy(alpha = 1f - animationProgress),
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val rotation: Float
)

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "programming", "coding", "development" -> Color(0xFF2196F3)
        "language", "languages" -> Color(0xFF9C27B0)
        "music", "art" -> Color(0xFFE91E63)
        "fitness", "health", "exercise" -> Color(0xFF4CAF50)
        "reading", "books" -> Color(0xFF795548)
        "math", "science" -> Color(0xFF00BCD4)
        else -> Color(0xFF607D8B)
    }
}
