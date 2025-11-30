package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.viewmodels.GoalOption
import com.example.todoapp.ui.viewmodels.TaskDetailItem
import com.example.todoapp.ui.viewmodels.TaskFilter
import com.example.todoapp.ui.viewmodels.TasksViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Color palette
private val PriorityHigh = Color(0xFFE53935)
private val PriorityMedium = Color(0xFFFFA726)
private val PriorityLow = Color(0xFF66BB6A)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val DeleteRed = Color(0xFFE53935)
private val EditBlue = Color(0xFF2196F3)
private val OverdueRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val availableGoals by viewModel.availableGoals.collectAsState()
    
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskDetailItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddBottomSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Task") },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "✅ My Tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stay organized and productive",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filter Tabs
            FilterTabRow(
                currentFilter = currentFilter,
                onFilterChange = { viewModel.setFilter(it) },
                taskCounts = mapOf(
                    TaskFilter.TODAY to tasks.count { !it.isCompleted },
                    TaskFilter.WEEK to tasks.size,
                    TaskFilter.OVERDUE to tasks.count { it.isOverdue }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task List
            if (tasks.isEmpty()) {
                EmptyTasksState(
                    filter = currentFilter,
                    onAddTask = { showAddBottomSheet = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pending tasks
                    val pendingTasks = tasks.filter { !it.isCompleted }
                    if (pendingTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending (${pendingTasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        items(pendingTasks, key = { it.id }) { task ->
                            SwipeableTaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
                                onEdit = { taskToEdit = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                    
                    // Completed tasks
                    val completedTasks = tasks.filter { it.isCompleted }
                    if (completedTasks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Completed (${completedTasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        items(completedTasks, key = { it.id }) { task ->
                            SwipeableTaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task.id) },
                                onEdit = { taskToEdit = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                    
                    // Bottom spacer
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add Task Bottom Sheet
    if (showAddBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddBottomSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AddTaskBottomSheet(
                availableGoals = availableGoals,
                onDismiss = { showAddBottomSheet = false },
                onConfirm = { title, description, priority, dueDate, goalId ->
                    viewModel.addTask(title, description, priority, dueDate, goalId)
                    showAddBottomSheet = false
                }
            )
        }
    }

    // Edit Task Bottom Sheet
    taskToEdit?.let { task ->
        ModalBottomSheet(
            onDismissRequest = { taskToEdit = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            EditTaskBottomSheet(
                task = task,
                availableGoals = availableGoals,
                onDismiss = { taskToEdit = null },
                onConfirm = { id, title, description, priority, dueDate, goalId ->
                    viewModel.updateTask(id, title, description, priority, dueDate, goalId)
                    taskToEdit = null
                }
            )
        }
    }
}

@Composable
private fun FilterTabRow(
    currentFilter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    taskCounts: Map<TaskFilter, Int>
) {
    val filters = listOf(
        TaskFilter.TODAY to "Today",
        TaskFilter.WEEK to "This Week",
        TaskFilter.OVERDUE to "Overdue"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = currentFilter == filter
            val count = taskCounts[filter] ?: 0
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterChange(filter) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    when (filter) {
                        TaskFilter.OVERDUE -> OverdueRed
                        else -> AccentBlue
                    }
                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (count > 0 || filter == TaskFilter.OVERDUE) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White 
                                   else if (filter == TaskFilter.OVERDUE && count > 0) OverdueRed
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableTaskCard(
    task: TaskDetailItem,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swipeOffset"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Edit action (swipe right)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(EditBlue)
                    .clickable { 
                        offsetX = 0f
                        onEdit() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White
                    )
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            // Delete action (swipe left)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(DeleteRed)
                    .clickable { 
                        offsetX = 0f
                        onDelete() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        // Task Card (foreground)
        TaskCard(
            task = task,
            onToggleComplete = onToggleComplete,
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = when {
                                offsetX > swipeThreshold -> swipeThreshold
                                offsetX < -swipeThreshold -> -swipeThreshold
                                else -> 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-swipeThreshold, swipeThreshold)
                        }
                    )
                }
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskDetailItem,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priorityInt) {
        3 -> PriorityHigh
        2 -> PriorityMedium
        else -> PriorityLow
    }
    
    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (task.isCompleted) 0.dp else 4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Priority/Overdue indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Max)
                    .fillMaxHeight()
                    .background(
                        if (task.isOverdue && !task.isCompleted) OverdueRed
                        else if (task.isCompleted) priorityColor.copy(alpha = 0.3f)
                        else priorityColor
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom Checkbox
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) AccentGreen
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onToggleComplete() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Task Content
                Column(modifier = Modifier.weight(1f)) {
                    // Title with overdue badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isCompleted)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        if (task.isOverdue && !task.isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = OverdueRed.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "⚠️ Overdue",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OverdueRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Description
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (task.isCompleted) 0.5f else 0.7f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Meta row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Due date
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (task.isOverdue && !task.isCompleted) OverdueRed
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.dueDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.isOverdue && !task.isCompleted) OverdueRed
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Priority badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityColor.copy(alpha = if (task.isCompleted) 0.1f else 0.15f)
                        ) {
                            Text(
                                text = task.priority,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.isCompleted) priorityColor.copy(alpha = 0.5f) else priorityColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Goal link
                        task.goalTitle?.let { goalTitle ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentBlue.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = AccentBlue
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = goalTitle.take(15) + if (goalTitle.length > 15) "..." else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState(
    filter: TaskFilter,
    onAddTask: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val (icon, title, subtitle) = when (filter) {
                TaskFilter.TODAY -> Triple(
                    Icons.Outlined.Today,
                    "No tasks for today",
                    "Add a task to start your productive day!"
                )
                TaskFilter.WEEK -> Triple(
                    Icons.Outlined.DateRange,
                    "No tasks this week",
                    "Plan ahead by adding tasks for the week."
                )
                TaskFilter.OVERDUE -> Triple(
                    Icons.Outlined.CheckCircle,
                    "All caught up! 🎉",
                    "No overdue tasks. Great job staying on track!"
                )
            }
            
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            if (filter != TaskFilter.OVERDUE) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddTask,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Task")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskBottomSheet(
    availableGoals: List<GoalOption>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Long, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(2) }
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    var goalDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Default to today
    var selectedDate by remember { 
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis) 
    }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

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
                text = "✨ New Task",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title") },
            placeholder = { Text("What needs to be done?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Outlined.Task, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            placeholder = { Text("Add details...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Due Date Picker
        Text(
            text = "Due Date",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick date options
            listOf(
                "Today" to 0,
                "Tomorrow" to 1,
                "Next Week" to 7
            ).forEach { (label, daysToAdd) ->
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, daysToAdd)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                val targetDate = calendar.timeInMillis
                val isSelected = isSameDay(selectedDate, targetDate)
                
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDate = targetDate },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
            
            // Custom date button
            FilterChip(
                selected = false,
                onClick = { showDatePicker = true },
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick", fontSize = 12.sp)
                    }
                }
            )
        }
        
        // Show selected date
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
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatFullDate(selectedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Priority Selection
        Text(
            text = "Priority",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(1, "Low", PriorityLow),
                Triple(2, "Medium", PriorityMedium),
                Triple(3, "High", PriorityHigh)
            ).forEach { (value, label, color) ->
                val isSelected = selectedPriority == value
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPriority = value },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) color else color.copy(alpha = 0.1f),
                    border = if (!isSelected) null else null
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else color,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Goal Link Dropdown
        if (availableGoals.isNotEmpty()) {
            Text(
                text = "Link to Goal (optional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = goalDropdownExpanded,
                onExpandedChange = { goalDropdownExpanded = !goalDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = availableGoals.find { it.id == selectedGoalId }?.title ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = goalDropdownExpanded,
                    onDismissRequest = { goalDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            selectedGoalId = null
                            goalDropdownExpanded = false
                        }
                    )
                    availableGoals.forEach { goal ->
                        DropdownMenuItem(
                            text = { Text(goal.title) },
                            onClick = {
                                selectedGoalId = goal.id
                                goalDropdownExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Flag,
                                    contentDescription = null,
                                    tint = AccentBlue
                                )
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Create Button
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onConfirm(title, description, selectedPriority, selectedDate, selectedGoalId)
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
            Text("Create Task", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskBottomSheet(
    task: TaskDetailItem,
    availableGoals: List<GoalOption>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, Int, Long, Long?) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var selectedPriority by remember { mutableStateOf(task.priorityInt) }
    var selectedGoalId by remember { mutableStateOf(task.goalId) }
    var goalDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(task.dueDateTimestamp) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

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
                text = "✏️ Edit Task",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Outlined.Task, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Due Date
        Text(
            text = "Due Date",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Event,
                    contentDescription = null,
                    tint = AccentBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatFullDate(selectedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Priority Selection
        Text(
            text = "Priority",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(1, "Low", PriorityLow),
                Triple(2, "Medium", PriorityMedium),
                Triple(3, "High", PriorityHigh)
            ).forEach { (value, label, color) ->
                val isSelected = selectedPriority == value
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPriority = value },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) color else color.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else color,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Goal Link Dropdown
        if (availableGoals.isNotEmpty()) {
            Text(
                text = "Link to Goal (optional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = goalDropdownExpanded,
                onExpandedChange = { goalDropdownExpanded = !goalDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = availableGoals.find { it.id == selectedGoalId }?.title ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = goalDropdownExpanded,
                    onDismissRequest = { goalDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            selectedGoalId = null
                            goalDropdownExpanded = false
                        }
                    )
                    availableGoals.forEach { goal ->
                        DropdownMenuItem(
                            text = { Text(goal.title) },
                            onClick = {
                                selectedGoalId = goal.id
                                goalDropdownExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Flag,
                                    contentDescription = null,
                                    tint = AccentBlue
                                )
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Save Button
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onConfirm(task.id, title, description, selectedPriority, selectedDate, selectedGoalId)
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Changes", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
