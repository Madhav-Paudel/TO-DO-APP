package com.example.todoapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.local.TaskEntity
import com.example.todoapp.data.model.ActionTaken
import com.example.todoapp.data.model.ActionType
import com.example.todoapp.data.model.ChatMessage
import com.example.todoapp.data.model.ChatSender
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.llm.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * LocalAssistantViewModel - ViewModel for on-device AI assistant
 * 
 * Manages chat state and handles LLM-generated actions like creating
 * goals and tasks. Uses JSON-first responses with deterministic fallback.
 */
class LocalAssistantViewModel(
    private val localAssistantRepository: LocalAssistantRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    application: Application
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "LocalAssistantViewModel"
    }
    
    // Chat state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()
    
    private val _modelInfo = MutableStateFlow<ModelInfo?>(null)
    val modelInfo: StateFlow<ModelInfo?> = _modelInfo.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Pending action for confirmation
    private val _pendingAction = MutableStateFlow<AssistantAction?>(null)
    val pendingAction: StateFlow<AssistantAction?> = _pendingAction.asStateFlow()
    
    init {
        // Show welcome message
        val welcomeMessage = ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = "👋 Hi! I'm your local AI assistant running entirely on your device. " +
                   "I can help you create goals, add tasks, and track your progress. " +
                   "What would you like to do?"
        )
        _messages.value = listOf(welcomeMessage)
        
        // Check model status
        viewModelScope.launch {
            checkModelStatus()
        }
    }
    
    /**
     * Check and update model status
     */
    private suspend fun checkModelStatus() {
        _isModelLoaded.value = localAssistantRepository.isReady()
        _modelInfo.value = localAssistantRepository.getModelInfo()
        
        if (!_isModelLoaded.value && localAssistantRepository.isNativeAvailable) {
            // Try to initialize model
            val result = localAssistantRepository.initializeModel()
            _isModelLoaded.value = result.isSuccess
        }
    }
    
    /**
     * Send a user message and get AI response
     */
    fun sendUserMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        
        // Add user message
        val userMessage = ChatMessage(sender = ChatSender.USER, text = text)
        _messages.value = _messages.value + userMessage
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Get context
                val goals = getGoalContext()
                val tasks = getTaskContext()
                
                // First try deterministic parser for commands
                val deterministicAction = DeterministicParser.parse(text)
                
                val action: AssistantAction = if (deterministicAction != null) {
                    Log.d(TAG, "Using deterministic parser result")
                    deterministicAction
                } else if (_isModelLoaded.value) {
                    // Use LLM
                    val result = localAssistantRepository.generate(
                        userMessage = text,
                        goals = goals,
                        tasks = tasks
                    )
                    
                    result.getOrElse { error ->
                        Log.e(TAG, "LLM generation failed: ${error.message}")
                        AssistantAction.Reply(
                            "I'm having trouble processing that. Could you try rephrasing? " +
                            "You can also use commands like 'create goal X' or 'add task Y'."
                        )
                    }
                } else {
                    // No model loaded - provide helpful fallback
                    AssistantAction.Reply(
                        "I don't have a model loaded yet, but I can still understand basic commands! " +
                        "Try: 'create goal Learn Python in 3 months 30 minutes per day' " +
                        "or 'add task Review notes tomorrow 20 minutes'"
                    )
                }
                
                // Handle the action
                handleAction(action)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
                addAssistantMessage(
                    "Sorry, something went wrong. Please try again.",
                    null
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Handle an AssistantAction - execute it or ask for confirmation
     */
    private suspend fun handleAction(action: AssistantAction) {
        when (action) {
            is AssistantAction.Reply -> {
                addAssistantMessage(action.message, null)
            }
            
            is AssistantAction.CreateGoal -> {
                // Execute immediately
                val goalId = createGoal(action)
                addAssistantMessage(
                    "✅ ${action.message}\n\n" +
                    "📎 Goal: ${action.goalTitle}\n" +
                    "📅 Duration: ${action.durationMonths} months\n" +
                    "⏱️ Daily target: ${action.dailyMinutes} minutes",
                    ActionTaken(ActionType.GOAL_CREATED, action.goalTitle, "${action.dailyMinutes} min/day")
                )
            }
            
            is AssistantAction.CreateTask -> {
                // Execute immediately
                createTask(action)
                addAssistantMessage(
                    "✅ ${action.message}\n\n" +
                    "📝 Task: ${action.taskTitle}\n" +
                    "📅 Due: ${action.dueDate}\n" +
                    "⏱️ Duration: ${action.minutes} minutes",
                    ActionTaken(ActionType.TASK_CREATED, action.taskTitle, "Due: ${action.dueDate}")
                )
            }
            
            is AssistantAction.CompleteTask -> {
                val completed = completeTask(action.taskTitle)
                if (completed) {
                    addAssistantMessage(
                        "🎉 ${action.message}",
                        ActionTaken(ActionType.TASK_COMPLETED, action.taskTitle)
                    )
                } else {
                    addAssistantMessage(
                        "I couldn't find a task matching \"${action.taskTitle}\". " +
                        "Please check the task name and try again.",
                        null
                    )
                }
            }
            
            is AssistantAction.DeleteGoal -> {
                val deleted = deleteGoal(action.goalTitle)
                if (deleted) {
                    addAssistantMessage(
                        "🗑️ ${action.message}",
                        ActionTaken(ActionType.GOAL_DELETED, action.goalTitle)
                    )
                } else {
                    addAssistantMessage(
                        "I couldn't find a goal matching \"${action.goalTitle}\".",
                        null
                    )
                }
            }
            
            is AssistantAction.DeleteTask -> {
                val deleted = deleteTask(action.taskTitle)
                if (deleted) {
                    addAssistantMessage(
                        "🗑️ ${action.message}",
                        ActionTaken(ActionType.TASK_DELETED, action.taskTitle)
                    )
                } else {
                    addAssistantMessage(
                        "I couldn't find a task matching \"${action.taskTitle}\".",
                        null
                    )
                }
            }
            
            is AssistantAction.ShowProgress -> {
                val summary = getProgressSummary()
                addAssistantMessage(summary, null)
            }
        }
    }
    
    /**
     * Create a goal from action
     */
    private suspend fun createGoal(action: AssistantAction.CreateGoal): Long {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (action.durationMonths * 30L * 24 * 60 * 60 * 1000)
        
        val goal = GoalEntity(
            title = action.goalTitle,
            startDate = startDate,
            endDate = endDate,
            dailyTargetMinutes = action.dailyMinutes,
            category = "General"
        )
        
        return goalRepository.insertGoal(goal)
    }
    
    /**
     * Create a task from action
     */
    private suspend fun createTask(action: AssistantAction.CreateTask): Long {
        val dueDate = when (action.dueDate.lowercase()) {
            "today" -> getStartOfDay()
            "tomorrow" -> getStartOfDay() + 24 * 60 * 60 * 1000
            "next_week" -> getStartOfDay() + 7 * 24 * 60 * 60 * 1000
            else -> {
                // Try to parse YYYY-MM-DD
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(action.dueDate)?.time ?: getStartOfDay()
                } catch (e: Exception) {
                    getStartOfDay()
                }
            }
        }
        
        // Find matching goal if specified
        var goalId: Long? = null
        if (!action.goalTitle.isNullOrBlank()) {
            val goals = goalRepository.allActiveGoals.first()
            val matchingGoal = goals.find { 
                it.title.lowercase().contains(action.goalTitle.lowercase()) 
            }
            goalId = matchingGoal?.id
        }
        
        val task = TaskEntity(
            goalId = goalId,
            title = action.taskTitle,
            description = "",
            dueDate = dueDate,
            priority = 2,
            isCompleted = false
        )
        
        return taskRepository.insertTask(task)
    }
    
    /**
     * Complete a task by title (fuzzy match)
     */
    private suspend fun completeTask(taskTitle: String): Boolean {
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        
        val matchingTask = tasks.find {
            it.title.lowercase().contains(taskTitle.lowercase())
        }
        
        return if (matchingTask != null) {
            taskRepository.updateTask(matchingTask.copy(isCompleted = true))
            true
        } else {
            false
        }
    }
    
    /**
     * Delete a goal by title (fuzzy match)
     */
    private suspend fun deleteGoal(goalTitle: String): Boolean {
        val goals = goalRepository.allActiveGoals.first()
        val matchingGoal = goals.find {
            it.title.lowercase().contains(goalTitle.lowercase())
        }
        
        return if (matchingGoal != null) {
            goalRepository.deleteGoalById(matchingGoal.id)
            true
        } else {
            false
        }
    }
    
    /**
     * Delete a task by title (fuzzy match)
     */
    private suspend fun deleteTask(taskTitle: String): Boolean {
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        
        val matchingTask = tasks.find {
            it.title.lowercase().contains(taskTitle.lowercase())
        }
        
        return if (matchingTask != null) {
            taskRepository.deleteTask(matchingTask)
            true
        } else {
            false
        }
    }
    
    /**
     * Get progress summary
     */
    private suspend fun getProgressSummary(): String {
        val goals = goalRepository.allActiveGoals.first()
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        
        val completedTasks = tasks.count { it.isCompleted }
        val totalTasks = tasks.size
        val percentage = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
        
        val sb = StringBuilder()
        sb.appendLine("📊 **Your Progress Summary**\n")
        
        sb.appendLine("**Today's Tasks:** $completedTasks/$totalTasks completed ($percentage%)")
        sb.appendLine("**Active Goals:** ${goals.size}")
        
        if (goals.isNotEmpty()) {
            sb.appendLine("\n**Goals:**")
            goals.take(5).forEach { goal ->
                sb.appendLine("• ${goal.title} - ${goal.dailyTargetMinutes} min/day")
            }
        }
        
        sb.appendLine("\n" + when {
            percentage >= 80 -> "🌟 Outstanding work! You're crushing it!"
            percentage >= 50 -> "💪 Great progress! Keep it up!"
            percentage > 0 -> "🚀 Good start! Every step counts!"
            else -> "✨ Let's make today productive!"
        })
        
        return sb.toString()
    }
    
    /**
     * Build goal context for prompts
     */
    private suspend fun getGoalContext(): List<GoalContext> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return goalRepository.allActiveGoals.first().take(5).map { goal ->
            GoalContext(
                title = goal.title,
                dailyMinutes = goal.dailyTargetMinutes,
                endDate = dateFormat.format(Date(goal.endDate))
            )
        }
    }
    
    /**
     * Build task context for prompts
     */
    private suspend fun getTaskContext(): List<TaskContext> {
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        return taskRepository.getTasksByDate(todayStart, todayEnd).first().take(5).map { task ->
            TaskContext(
                title = task.title,
                isCompleted = task.isCompleted,
                minutes = 30 // Default, could be stored in task
            )
        }
    }
    
    /**
     * Add assistant message to chat
     */
    private fun addAssistantMessage(text: String, action: ActionTaken?) {
        val message = ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = text,
            action = action
        )
        _messages.value = _messages.value + message
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Reload model
     */
    fun reloadModel() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = localAssistantRepository.reloadModel()
            _isModelLoaded.value = result.isSuccess
            _modelInfo.value = localAssistantRepository.getModelInfo()
            _isLoading.value = false
            
            if (result.isSuccess) {
                addAssistantMessage("✅ Model reloaded successfully!", null)
            }
        }
    }
    
    /**
     * Clear chat history
     */
    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "Chat cleared! How can I help you?"
            )
        )
    }
    
    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            localAssistantRepository.close()
        }
    }
}
