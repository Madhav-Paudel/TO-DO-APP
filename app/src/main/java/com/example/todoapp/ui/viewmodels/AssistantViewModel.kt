package com.example.todoapp.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CommandResult(
    val message: String,
    val action: ActionTaken? = null
)

/**
 * AssistantViewModel - ViewModel for on-device AI assistant
 * 
 * Uses local LLM inference via llama.cpp with JSON-first responses
 * and deterministic command parsing as fallback.
 */
class AssistantViewModel(
    private val localAssistantRepository: LocalAssistantRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val maxTokens: Int = 128,
    private val timeoutSeconds: Int = 30
) : ViewModel() {

    companion object {
        private const val TAG = "AssistantViewModel"
    }

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()
    
    private val _modelInfo = MutableStateFlow<ModelInfo?>(null)
    val modelInfo: StateFlow<ModelInfo?> = _modelInfo.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Check model status on init
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
            _modelInfo.value = localAssistantRepository.getModelInfo()
        }
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(sender = ChatSender.USER, text = text)
        _chatHistory.value += userMessage

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // First try deterministic parser for commands
                val commandResult = parseCommands(text)
                if (commandResult != null) {
                    _chatHistory.value += ChatMessage(
                        sender = ChatSender.ASSISTANT, 
                        text = commandResult.message,
                        action = commandResult.action
                    )
                    _isLoading.value = false
                    return@launch
                }

                // Try LLM if model is loaded
                if (_isModelLoaded.value) {
                    try {
                        val action = withTimeout(timeoutSeconds * 1000L) {
                            val goals = getGoalContext()
                            val tasks = getTaskContext()
                            
                            localAssistantRepository.generate(
                                userMessage = text,
                                goals = goals,
                                tasks = tasks,
                                maxTokens = maxTokens
                            )
                        }
                        
                        action.fold(
                            onSuccess = { assistantAction ->
                                handleAssistantAction(assistantAction)
                            },
                            onFailure = { error ->
                                Log.e(TAG, "LLM generation failed: ${error.message}")
                                addFallbackResponse(text)
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "LLM timeout or error: ${e.message}")
                        addFallbackResponse(text)
                    }
                } else {
                    // No model loaded - use deterministic parser fallback
                    val deterministicAction = DeterministicParser.parse(text)
                    if (deterministicAction != null) {
                        handleAssistantAction(deterministicAction)
                    } else {
                        // Show model not loaded message with helpful commands
                        _chatHistory.value += ChatMessage(
                            sender = ChatSender.ASSISTANT, 
                            text = """🤖 I don't have a model loaded yet, but I can still help with commands!

Try these:
• "create goal Learn Python in 3 months 30 minutes per day"
• "add task Review notes tomorrow"
• "complete task [name]"
• "show progress"
• "list goals" or "list tasks"

To enable AI responses, download a model in Settings → Model Manager."""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
                _chatHistory.value += ChatMessage(
                    sender = ChatSender.ASSISTANT,
                    text = "Sorry, something went wrong. Please try again."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Handle an AssistantAction from LLM or deterministic parser
     */
    private suspend fun handleAssistantAction(action: AssistantAction) {
        when (action) {
            is AssistantAction.Reply -> {
                _chatHistory.value += ChatMessage(
                    sender = ChatSender.ASSISTANT,
                    text = action.message
                )
            }
            
            is AssistantAction.CreateGoal -> {
                createGoalFromAction(action)
            }
            
            is AssistantAction.CreateTask -> {
                createTaskFromAction(action)
            }
            
            is AssistantAction.CompleteTask -> {
                completeTaskFromAction(action)
            }
            
            is AssistantAction.DeleteGoal -> {
                deleteGoalFromAction(action)
            }
            
            is AssistantAction.DeleteTask -> {
                deleteTaskFromAction(action)
            }
            
            is AssistantAction.ShowProgress -> {
                showProgressSummary()
            }
        }
    }
    
    private suspend fun createGoalFromAction(action: AssistantAction.CreateGoal) {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (action.durationMonths * 30L * 24 * 60 * 60 * 1000)
        
        val goal = GoalEntity(
            title = action.goalTitle,
            startDate = startDate,
            endDate = endDate,
            dailyTargetMinutes = action.dailyMinutes,
            category = "General"
        )
        val goalId = goalRepository.insertGoal(goal)
        
        // Also create a daily task
        val todayTask = TaskEntity(
            goalId = goalId,
            title = "Study ${action.goalTitle} (${action.dailyMinutes} min)",
            description = "Daily study task",
            dueDate = getStartOfDay(),
            priority = 1,
            isCompleted = false
        )
        taskRepository.insertTask(todayTask)
        
        _chatHistory.value += ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = "✅ ${action.message}\n\n📎 Goal: ${action.goalTitle}\n📅 Duration: ${action.durationMonths} months\n⏱️ Daily target: ${action.dailyMinutes} minutes\n\n📝 Also added today's study task!",
            action = ActionTaken(
                type = ActionType.GOAL_CREATED,
                itemName = action.goalTitle,
                details = "${action.dailyMinutes} min/day for ${action.durationMonths} months"
            )
        )
    }
    
    private suspend fun createTaskFromAction(action: AssistantAction.CreateTask) {
        val dueDate = when (action.dueDate.lowercase()) {
            "today" -> getStartOfDay()
            "tomorrow" -> getStartOfDay() + 24 * 60 * 60 * 1000
            "next_week" -> getStartOfDay() + 7 * 24 * 60 * 60 * 1000
            else -> {
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
        taskRepository.insertTask(task)
        
        val dayText = when {
            dueDate == getStartOfDay() -> "today"
            dueDate == getStartOfDay() + 24 * 60 * 60 * 1000 -> "tomorrow"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueDate))
        }
        
        _chatHistory.value += ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = "✅ ${action.message}\n\n📝 Task: ${action.taskTitle}\n📅 Due: $dayText",
            action = ActionTaken(ActionType.TASK_CREATED, action.taskTitle, "Due: $dayText")
        )
    }
    
    private suspend fun completeTaskFromAction(action: AssistantAction.CompleteTask) {
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        val matchingTask = tasks.find { it.title.lowercase().contains(action.taskTitle.lowercase()) }
        
        if (matchingTask != null) {
            taskRepository.updateTask(matchingTask.copy(isCompleted = true))
            _chatHistory.value += ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "🎉 ${action.message}",
                action = ActionTaken(ActionType.TASK_COMPLETED, matchingTask.title)
            )
        } else {
            _chatHistory.value += ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "❌ I couldn't find a task matching '${action.taskTitle}'. Please check the task name."
            )
        }
    }
    
    private suspend fun deleteGoalFromAction(action: AssistantAction.DeleteGoal) {
        val goals = goalRepository.allActiveGoals.first()
        val matchingGoal = goals.find { it.title.lowercase().contains(action.goalTitle.lowercase()) }
        
        if (matchingGoal != null) {
            goalRepository.deleteGoalById(matchingGoal.id)
            _chatHistory.value += ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "🗑️ ${action.message}",
                action = ActionTaken(ActionType.GOAL_DELETED, matchingGoal.title)
            )
        } else {
            _chatHistory.value += ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "❌ I couldn't find a goal matching '${action.goalTitle}'."
            )
        }
    }
    
    private suspend fun deleteTaskFromAction(action: AssistantAction.DeleteTask) {
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        val matchingTask = tasks.find { it.title.lowercase().contains(action.taskTitle.lowercase()) }
        
        if (matchingTask != null) {
            taskRepository.deleteTask(matchingTask)
            _chatHistory.value += ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "🗑️ ${action.message}",
                action = ActionTaken(ActionType.TASK_DELETED, matchingTask.title)
            )
        } else {
            _chatHistory.value += ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "❌ I couldn't find a task matching '${action.taskTitle}'."
            )
        }
    }
    
    private suspend fun showProgressSummary() {
        val goals = goalRepository.allActiveGoals.first()
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        
        val completedTasks = tasks.count { it.isCompleted }
        val totalTasks = tasks.size
        val percentage = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
        
        val message = buildString {
            appendLine("📊 **Your Progress Summary**\n")
            appendLine("**Today's Tasks:** $completedTasks/$totalTasks completed ($percentage%)")
            appendLine("**Active Goals:** ${goals.size}")
            
            if (goals.isNotEmpty()) {
                appendLine("\n**Goals:**")
                goals.take(5).forEach { goal ->
                    appendLine("• ${goal.title} - ${goal.dailyTargetMinutes} min/day")
                }
            }
            
            appendLine("\n" + when {
                percentage >= 80 -> "🌟 Outstanding work! You're crushing it!"
                percentage >= 50 -> "💪 Great progress! Keep it up!"
                percentage > 0 -> "🚀 Good start! Every step counts!"
                else -> "✨ Let's make today productive!"
            })
        }
        
        _chatHistory.value += ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = message,
            action = ActionTaken(ActionType.LIST_SHOWN, "Progress", "$completedTasks/$totalTasks tasks")
        )
    }
    
    private fun addFallbackResponse(originalText: String) {
        _chatHistory.value += ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = """I couldn't parse my response properly. Let me try to help differently!

You can use these commands:
• "create goal [name] in [X] months [Y] minutes per day"
• "add task [name] tomorrow"
• "complete task [name]"
• "show progress"

Or try rephrasing your question."""
        )
    }

    private suspend fun parseCommands(text: String): CommandResult? {
        val lowerText = text.lowercase().trim()
        
        // ===== HELP COMMAND =====
        if (lowerText == "help" || lowerText == "commands" || lowerText == "what can you do") {
            return CommandResult(
                message = """🤖 Here's what I can do:

📋 **Goals:**
• "create goal Learn Python" - Create a goal
• "create goal Learn Python 6 months 1 hour" - With duration & daily target
• "delete goal Python" - Delete a goal
• "list goals" - Show all goals

✅ **Tasks:**
• "add task Review notes" - Add a task
• "delete task Review" - Delete a task
• "complete task Review" - Mark as done
• "list tasks" - Show today's tasks

📊 **Progress:**
• "show progress" - See your summary

Just type naturally - I'll understand! 😊"""
            )
        }
        
        // ===== LIST COMMANDS =====
        if (lowerText == "list goals" || lowerText == "show goals" || lowerText == "my goals" || 
            lowerText == "goals" || lowerText.contains("show me my goals") || lowerText.contains("what are my goals")) {
            val goals = goalRepository.allActiveGoals.first()
            return if (goals.isEmpty()) {
                CommandResult("📋 You don't have any active goals yet. Try saying 'create goal Learn Python' to get started!")
            } else {
                val goalList = goals.mapIndexed { index, goal -> 
                    "${index + 1}. ${goal.title} (${goal.dailyTargetMinutes} min/day)"
                }.joinToString("\n")
                CommandResult(
                    message = "📋 Your Active Goals:\n$goalList",
                    action = ActionTaken(ActionType.LIST_SHOWN, "Goals", "${goals.size} goals")
                )
            }
        }
        
        if (lowerText == "list tasks" || lowerText == "show tasks" || lowerText == "my tasks" ||
            lowerText == "tasks" || lowerText.contains("show me my tasks") || lowerText.contains("what are my tasks")) {
            val todayStart = getStartOfDay()
            val todayEnd = todayStart + 24 * 60 * 60 * 1000
            val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
            return if (tasks.isEmpty()) {
                CommandResult("📋 No tasks for today. Try saying 'add task Review notes' to add one!")
            } else {
                val taskList = tasks.mapIndexed { index, task ->
                    val status = if (task.isCompleted) "✅" else "⬜"
                    "$status ${index + 1}. ${task.title}"
                }.joinToString("\n")
                CommandResult(
                    message = "📋 Today's Tasks:\n$taskList",
                    action = ActionTaken(ActionType.LIST_SHOWN, "Tasks", "${tasks.size} tasks")
                )
            }
        }

        // Try deterministic parser for other commands
        val deterministicAction = DeterministicParser.parse(text)
        if (deterministicAction != null) {
            // Convert to CommandResult for backward compatibility
            return when (deterministicAction) {
                is AssistantAction.CreateGoal -> null // Let handleAssistantAction process it
                is AssistantAction.CreateTask -> null
                is AssistantAction.CompleteTask -> null
                is AssistantAction.DeleteGoal -> null
                is AssistantAction.DeleteTask -> null
                is AssistantAction.ShowProgress -> null
                is AssistantAction.Reply -> CommandResult(deterministicAction.message)
            }
        }

        // No command matched
        return null
    }
    
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
    
    private suspend fun getTaskContext(): List<TaskContext> {
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        return taskRepository.getTasksByDate(todayStart, todayEnd).first().take(5).map { task ->
            TaskContext(
                title = task.title,
                isCompleted = task.isCompleted,
                minutes = 30
            )
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
    
    fun clearError() {
        _error.value = null
    }
    
    fun reloadModel() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = localAssistantRepository.reloadModel()
            _isModelLoaded.value = result.isSuccess
            _modelInfo.value = localAssistantRepository.getModelInfo()
            _isLoading.value = false
            
            if (result.isSuccess) {
                _chatHistory.value += ChatMessage(
                    sender = ChatSender.ASSISTANT,
                    text = "✅ Model reloaded successfully!"
                )
            }
        }
    }
    
    fun clearChat() {
        _chatHistory.value = emptyList()
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            localAssistantRepository.close()
        }
    }
}
