package com.example.todoapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.TaskEntity
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TaskDetailItem(
    val id: Long,
    val title: String,
    val description: String,
    val dueDate: String,
    val dueDateTimestamp: Long,
    val isCompleted: Boolean,
    val priority: String,
    val priorityInt: Int,
    val goalId: Long?,
    val goalTitle: String?,
    val isOverdue: Boolean
)

data class GoalOption(
    val id: Long,
    val title: String
)

enum class TaskFilter {
    TODAY, WEEK, OVERDUE
}

class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository? = null
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.TODAY)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()
    
    private val _availableGoals = MutableStateFlow<List<GoalOption>>(emptyList())
    val availableGoals: StateFlow<List<GoalOption>> = _availableGoals.asStateFlow()

    init {
        loadGoals()
    }
    
    private fun loadGoals() {
        viewModelScope.launch {
            goalRepository?.allActiveGoals?.collect { goals ->
                _availableGoals.value = goals.map { GoalOption(it.id, it.title) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskDetailItem>> = _filter.flatMapLatest { filter ->
        val today = getStartOfDay()
        val endOfToday = today + 24 * 60 * 60 * 1000
        val endOfWeek = today + 7 * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        when (filter) {
            TaskFilter.TODAY -> taskRepository.getTasksByDate(today, endOfToday)
            TaskFilter.WEEK -> taskRepository.getTasksByDate(today, endOfWeek)
            TaskFilter.OVERDUE -> taskRepository.getOverdueTasks(now)
        }
    }.map { entities ->
        val now = System.currentTimeMillis()
        entities.map { task ->
            val goalTitle = task.goalId?.let { goalId ->
                _availableGoals.value.find { it.id == goalId }?.title
            }
            val isOverdue = task.dueDate < now && !task.isCompleted
            
            TaskDetailItem(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = formatDueDate(task.dueDate),
                dueDateTimestamp = task.dueDate,
                isCompleted = task.isCompleted,
                priority = when (task.priority) {
                    1 -> "Low"
                    2 -> "Medium"
                    3 -> "High"
                    else -> "Medium"
                },
                priorityInt = task.priority,
                goalId = task.goalId,
                goalTitle = goalTitle,
                isOverdue = isOverdue
            )
        }.sortedWith(
            compareBy(
                { it.isCompleted },
                { -it.priorityInt },
                { it.dueDateTimestamp }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(newFilter: TaskFilter) {
        _filter.value = newFilter
    }

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
            }
        }
    }
    
    fun addTask(title: String, description: String, priority: Int, dueDate: Long = System.currentTimeMillis(), goalId: Long? = null) {
         viewModelScope.launch {
            val task = TaskEntity(
                goalId = goalId,
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                isCompleted = false
            )
            taskRepository.insertTask(task)
        }
    }

    fun updateTask(taskId: Long, title: String, description: String, priority: Int, dueDate: Long? = null, goalId: Long? = null) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                taskRepository.updateTask(
                    task.copy(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate ?: task.dueDate,
                        goalId = goalId
                    )
                )
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                taskRepository.deleteTask(task)
            }
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun formatDueDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val today = getStartOfDay()
        val tomorrow = today + TimeUnit.DAYS.toMillis(1)
        val dayAfterTomorrow = today + TimeUnit.DAYS.toMillis(2)
        
        return when {
            timestamp < today -> {
                val daysAgo = TimeUnit.MILLISECONDS.toDays(today - timestamp).toInt()
                if (daysAgo == 1) "Yesterday" else "$daysAgo days ago"
            }
            timestamp < tomorrow -> "Today"
            timestamp < dayAfterTomorrow -> "Tomorrow"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
