package com.example.todoapp.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.local.TaskEntity
import com.example.todoapp.data.model.ActionTaken
import com.example.todoapp.data.model.ActionType
import com.example.todoapp.data.model.ChatMessage
import com.example.todoapp.data.model.ChatSender
import com.example.todoapp.llm.LocalAssistantRepository
import com.example.todoapp.llm.DeterministicParser
import com.example.todoapp.llm.AssistantAction
import com.example.todoapp.llm.GoalContext
import com.example.todoapp.llm.TaskContext
import com.example.todoapp.data.repository.AssistantMemoryRepository
import com.example.todoapp.data.repository.DailyProgressRepository
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// UI State for Voice Assistant
data class VoiceAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val speechAmplitude: Float = 0f,
    val currentSessionId: String = UUID.randomUUID().toString(),
    val showApiKeyDialog: Boolean = false,
    val apiKeyConfigured: Boolean = true,
    val error: String? = null,
    val userPreferences: Map<String, String> = emptyMap()
)

// Voice command result data
data class VoiceCmdResult(
    val message: String,
    val action: ActionTaken? = null,
    val shouldSpeak: Boolean = true
)

class VoiceAssistantViewModel(
    private val localAssistantRepository: LocalAssistantRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val memoryRepository: AssistantMemoryRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceAssistantUiState())
    val uiState: StateFlow<VoiceAssistantUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerInitialized = false

    private val assistantName = "Nova"
    private val greetings = listOf(
        "Hey there! 👋 I'm $assistantName, your productivity coach. What can I help you with today?",
        "Hi! Ready to crush some goals today? 💪",
        "Hello! Let's make today productive together! ✨"
    )

    private val encouragements = listOf(
        "You're doing amazing! 🌟",
        "Keep up the great work! 💪",
        "I believe in you! 🎯",
        "One step at a time - you've got this! 🚀"
    )

    init {
        loadUserPreferences()
        // Show welcome message for fresh session (chat clears on app restart)
        showWelcomeMessage()
    }
    
    private fun showWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = greetings.random()
        )
        _uiState.value = _uiState.value.copy(messages = listOf(welcomeMessage))
    }

    private fun loadUserPreferences() {
        viewModelScope.launch {
            val memories = memoryRepository.getAllMemoriesNow()
            val prefs = memories.associate { it.key to it.value }
            _uiState.value = _uiState.value.copy(userPreferences = prefs)
        }
    }



    fun initializeSpeechRecognizer() {
        if (!isRecognizerInitialized && SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            isRecognizerInitialized = true
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.value = _uiState.value.copy(isListening = true, speechAmplitude = 0.3f)
            }
            override fun onBeginningOfSpeech() {
                _uiState.value = _uiState.value.copy(speechAmplitude = 0.5f)
            }
            override fun onRmsChanged(rmsdB: Float) {
                val amplitude = ((rmsdB + 2) / 12f).coerceIn(0.1f, 1f)
                _uiState.value = _uiState.value.copy(speechAmplitude = amplitude)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _uiState.value = _uiState.value.copy(isListening = false, speechAmplitude = 0f)
            }
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    speechAmplitude = 0f,
                    error = if (error != SpeechRecognizer.ERROR_NO_MATCH) errorMessage else null
                )
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    sendMessage(text)
                }
                _uiState.value = _uiState.value.copy(isListening = false, speechAmplitude = 0f)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (!isRecognizerInitialized) initializeSpeechRecognizer()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to start speech recognition")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(isListening = false, speechAmplitude = 0f)
    }



    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(sender = ChatSender.USER, text = text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            memoryRepository.saveMessage("user", text, _uiState.value.currentSessionId)

            val commandResult = parseCommand(text)
            if (commandResult != null) {
                val assistantMessage = ChatMessage(
                    sender = ChatSender.ASSISTANT,
                    text = commandResult.message,
                    action = commandResult.action
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false
                )
                memoryRepository.saveMessage("assistant", commandResult.message, _uiState.value.currentSessionId)
                // TTS disabled - removed speak() call
                return@launch
            }

            try {
                val response = getAiResponse(text)
                val assistantMessage = ChatMessage(sender = ChatSender.ASSISTANT, text = response)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false
                )
                memoryRepository.saveMessage("assistant", response, _uiState.value.currentSessionId)
                learnFromConversation(text)
            } catch (e: Exception) {
                // Fallback with deterministic response
                val deterministicAction = DeterministicParser.parse(text)
                if (deterministicAction != null) {
                    handleDeterministicAction(deterministicAction)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to process: ${e.message}"
                    )
                }
            }
        }
    }
    
    private suspend fun handleDeterministicAction(action: AssistantAction) {
        when (action) {
            is AssistantAction.Reply -> {
                val msg = ChatMessage(sender = ChatSender.ASSISTANT, text = action.message)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + msg,
                    isLoading = false
                )
            }
            is AssistantAction.CreateGoal -> {
                val result = createGoalFromAction(action)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AssistantAction.CreateTask -> {
                val result = createTaskFromAction(action)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AssistantAction.CompleteTask -> {
                val result = completeTaskFromAction(action)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AssistantAction.DeleteGoal -> {
                val result = deleteGoalFromAction(action)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AssistantAction.DeleteTask -> {
                val result = deleteTaskFromAction(action)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AssistantAction.ShowProgress -> {
                val summary = getProgressSummary()
                val msg = ChatMessage(sender = ChatSender.ASSISTANT, text = summary.message, action = summary.action)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + msg,
                    isLoading = false
                )
            }
        }
    }
    
    private suspend fun createGoalFromAction(action: AssistantAction.CreateGoal) {
        val goal = GoalEntity(
            title = action.goalTitle,
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + (action.durationMonths * 30L * 86400000),
            dailyTargetMinutes = action.dailyMinutes,
            category = "General"
        )
        goalRepository.insertGoal(goal)
        
        val msg = ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = "🎯 Created **${action.goalTitle}**!\n📅 ${action.durationMonths} months • ⏱️ ${action.dailyMinutes} min/day",
            action = ActionTaken(ActionType.GOAL_CREATED, action.goalTitle, "${action.dailyMinutes} min/day")
        )
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
    }
    
    private suspend fun createTaskFromAction(action: AssistantAction.CreateTask) {
        val dueDate = when (action.dueDate.lowercase()) {
            "tomorrow" -> getStartOfDay() + 86400000
            else -> getStartOfDay()
        }
        val task = TaskEntity(goalId = null, title = action.taskTitle, description = "", dueDate = dueDate, priority = 2, isCompleted = false)
        taskRepository.insertTask(task)
        
        val msg = ChatMessage(
            sender = ChatSender.ASSISTANT,
            text = "✅ Added '**${action.taskTitle}**' for ${action.dueDate}!",
            action = ActionTaken(ActionType.TASK_CREATED, action.taskTitle, action.dueDate)
        )
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
    }
    
    private suspend fun completeTaskFromAction(action: AssistantAction.CompleteTask) {
        val tasks = taskRepository.getTasksByDate(getStartOfDay(), getStartOfDay() + 86400000).first()
        val match = tasks.find { it.title.lowercase().contains(action.taskTitle.lowercase()) }
        
        val msg = if (match != null) {
            taskRepository.updateTask(match.copy(isCompleted = true))
            ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "🎉 Completed '${match.title}'!",
                action = ActionTaken(ActionType.TASK_COMPLETED, match.title)
            )
        } else {
            ChatMessage(sender = ChatSender.ASSISTANT, text = "Couldn't find task '${action.taskTitle}'")
        }
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
    }
    
    private suspend fun deleteGoalFromAction(action: AssistantAction.DeleteGoal) {
        val goals = goalRepository.allActiveGoals.first()
        val match = goals.find { it.title.lowercase().contains(action.goalTitle.lowercase()) }
        
        val msg = if (match != null) {
            goalRepository.deleteGoalById(match.id)
            ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "🗑️ Deleted '${match.title}'",
                action = ActionTaken(ActionType.GOAL_DELETED, match.title)
            )
        } else {
            ChatMessage(sender = ChatSender.ASSISTANT, text = "Couldn't find goal '${action.goalTitle}'")
        }
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
    }
    
    private suspend fun deleteTaskFromAction(action: AssistantAction.DeleteTask) {
        val tasks = taskRepository.getTasksByDate(getStartOfDay(), getStartOfDay() + 86400000).first()
        val match = tasks.find { it.title.lowercase().contains(action.taskTitle.lowercase()) }
        
        val msg = if (match != null) {
            taskRepository.deleteTask(match)
            ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "🗑️ Deleted '${match.title}'",
                action = ActionTaken(ActionType.TASK_DELETED, match.title)
            )
        } else {
            ChatMessage(sender = ChatSender.ASSISTANT, text = "Couldn't find task '${action.taskTitle}'")
        }
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
    }

    private suspend fun getAiResponse(userMessage: String): String {
        val goals = goalRepository.allActiveGoals.first().take(5)
        val todayStart = getStartOfDay()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val tasks = taskRepository.getTasksByDate(todayStart, todayEnd).first()
        
        // Build context for local LLM
        val goalContexts = goals.map { GoalContext(it.title, it.dailyTargetMinutes, "") }
        val taskContexts = tasks.map { TaskContext(it.title, it.isCompleted, 30) }
        
        // Try local LLM
        val result = localAssistantRepository.generate(
            userMessage = userMessage,
            goals = goalContexts,
            tasks = taskContexts,
            maxTokens = 128
        )
        
        return result.fold(
            onSuccess = { action -> action.message },
            onFailure = { 
                // Fallback response
                "I understand! You can use commands like 'create goal' or 'add task' to get started. 😊"
            }
        )
    }

    private fun buildContextMessage(
        goals: List<GoalEntity>, 
        tasks: List<TaskEntity>,
        memories: List<com.example.todoapp.data.local.AssistantMemoryEntity>
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sb.append("Current: ${dateFormat.format(System.currentTimeMillis())}\n")
        
        if (memories.isNotEmpty()) {
            sb.append("USER INFO: ")
            memories.take(5).forEach { sb.append("${it.value}; ") }
            sb.append("\n")
        }
        
        if (goals.isNotEmpty()) {
            sb.append("GOALS: ")
            goals.forEach { sb.append("${it.title} (${it.dailyTargetMinutes}min/day); ") }
            sb.append("\n")
        }
        
        if (tasks.isNotEmpty()) {
            val done = tasks.count { it.isCompleted }
            sb.append("TODAY'S TASKS ($done/${tasks.size} done): ")
            tasks.take(5).forEach { sb.append("${if(it.isCompleted) "✅" else "⬜"}${it.title}; ") }
        }
        
        return sb.toString()
    }

    private suspend fun parseCommand(text: String): VoiceCmdResult? {
        val lowerText = text.lowercase().trim()
        
        if (lowerText == "help" || lowerText == "commands") {
            return VoiceCmdResult(
                message = """🤖 I can help you with:
• "create goal [name]" - Create a goal
• "add task [name]" - Add a task
• "complete task [name]" - Mark as done
• "list goals" / "list tasks" - See items
• "remember that [info]" - Save preference
• "how am I doing" - Progress summary
Just talk naturally! 😊""",
                shouldSpeak = true
            )
        }
        
        if (lowerText == "list goals" || lowerText == "show goals" || lowerText == "my goals") {
            val goals = goalRepository.allActiveGoals.first()
            return if (goals.isEmpty()) {
                VoiceCmdResult("📋 No active goals yet. Say 'create goal' to start! 🎯")
            } else {
                val goalList = goals.mapIndexed { i, g -> "${i+1}. **${g.title}** (${g.dailyTargetMinutes} min/day)" }.joinToString("\n")
                VoiceCmdResult(
                    message = "📋 **Your Goals:**\n$goalList\n\n${encouragements.random()}",
                    action = ActionTaken(ActionType.LIST_SHOWN, "Goals", "${goals.size} goals")
                )
            }
        }
        
        if (lowerText == "list tasks" || lowerText == "show tasks" || lowerText == "my tasks") {
            val todayStart = getStartOfDay()
            val tasks = taskRepository.getTasksByDate(todayStart, todayStart + 86400000).first()
            return if (tasks.isEmpty()) {
                VoiceCmdResult("📋 No tasks for today! Say 'add task' to create one! ✨")
            } else {
                val done = tasks.count { it.isCompleted }
                val taskList = tasks.mapIndexed { i, t -> "${if(t.isCompleted) "✅" else "⬜"} ${i+1}. ${t.title}" }.joinToString("\n")
                VoiceCmdResult(
                    message = "📋 **Today's Tasks** ($done/${tasks.size}):\n$taskList",
                    action = ActionTaken(ActionType.LIST_SHOWN, "Tasks", "${tasks.size} tasks")
                )
            }
        }
        
        if (lowerText.contains("how am i doing") || lowerText.contains("my progress")) {
            return getProgressSummary()
        }
        
        if (lowerText.startsWith("remember that") || lowerText.startsWith("remember i")) {
            val pref = text.replace(Regex("(?i)remember (that |i )"), "").trim()
            if (pref.isNotBlank()) {
                memoryRepository.rememberPreference("pref_${System.currentTimeMillis()}", pref)
                return VoiceCmdResult("Got it! I'll remember: \"$pref\" 🧠✨")
            }
        }
        
        if (lowerText.contains("what do you know") || lowerText.contains("what do you remember")) {
            val memories = memoryRepository.getAllMemoriesNow()
            return if (memories.isEmpty()) {
                VoiceCmdResult("I don't have any saved info yet! Tell me 'remember that...' to save something! 📝")
            } else {
                VoiceCmdResult("🧠 **What I Know:**\n${memories.map { "• ${it.value}" }.joinToString("\n")}")
            }
        }
        
        if (lowerText.contains("create goal") || lowerText.contains("add goal") || lowerText.contains("new goal")) {
            return createGoalCommand(text)
        }
        
        if (lowerText.startsWith("add task") || lowerText.startsWith("create task") || lowerText.startsWith("new task")) {
            return createTaskCommand(text)
        }
        
        if (lowerText.contains("complete task") || lowerText.contains("finish task") || lowerText.contains("done with")) {
            return completeTaskCommand(text)
        }
        
        if ((lowerText.contains("delete") || lowerText.contains("remove")) && lowerText.contains("goal")) {
            return deleteGoalCommand(text)
        }
        
        if ((lowerText.contains("delete") || lowerText.contains("remove")) && lowerText.contains("task")) {
            return deleteTaskCommand(text)
        }
        
        if (lowerText == "clear chat" || lowerText == "new conversation") {
            _uiState.value = _uiState.value.copy(
                messages = listOf(ChatMessage(sender = ChatSender.ASSISTANT, text = "Fresh start! 🌟 How can I help?")),
                currentSessionId = UUID.randomUUID().toString()
            )
            return VoiceCmdResult("", shouldSpeak = false)
        }
        
        return null
    }

    private suspend fun createGoalCommand(text: String): VoiceCmdResult {
        var name = text.replace(Regex("(?i)(create|add|new|set)\\s*(a)?\\s*goal\\s*(to)?"), "").trim()
        if (name.isBlank()) return VoiceCmdResult("What goal would you like to create? 🎯")
        
        val lowerText = text.lowercase()
        val months = Regex("(\\d+)\\s*month").find(lowerText)?.groupValues?.get(1)?.toIntOrNull() ?: 3
        val minutes = when {
            lowerText.contains("hour") -> (Regex("(\\d+)\\s*h").find(lowerText)?.groupValues?.get(1)?.toIntOrNull() ?: 1) * 60
            lowerText.contains("min") -> Regex("(\\d+)\\s*min").find(lowerText)?.groupValues?.get(1)?.toIntOrNull() ?: 30
            else -> 30
        }

        name = name.replace(Regex("(?i)\\s*(in|for)?\\s*\\d+\\s*(month|hour|min).*"), "").trim()
            .replaceFirstChar { it.uppercase() }

        val goal = GoalEntity(
            title = name,
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + (months * 30L * 86400000),
            dailyTargetMinutes = minutes,
            category = "General"
        )
        goalRepository.insertGoal(goal)
        
        return VoiceCmdResult(
            message = "🎯 Created **${goal.title}**!\n📅 $months months • ⏱️ $minutes min/day\n\n${encouragements.random()}",
            action = ActionTaken(ActionType.GOAL_CREATED, goal.title, "$minutes min/day")
        )
    }

    private suspend fun createTaskCommand(text: String): VoiceCmdResult {
        var name = text.replace(Regex("(?i)(add|create|new)\\s*(a)?\\s*task"), "").trim()
        if (name.isBlank()) return VoiceCmdResult("What task do you want to add? ✅")
        
        val tomorrow = text.lowercase().contains("tomorrow")
        val dueDate = getStartOfDay() + if (tomorrow) 86400000 else 0
        name = name.replace(Regex("(?i)\\s*(tomorrow|today|for)\\s*"), " ").trim()
            .replaceFirstChar { it.uppercase() }

        val task = TaskEntity(goalId = null, title = name, description = "", dueDate = dueDate, priority = 2, isCompleted = false)
        taskRepository.insertTask(task)
        
        return VoiceCmdResult(
            message = "✅ Added '**$name**' for ${if(tomorrow) "tomorrow" else "today"}! 💪",
            action = ActionTaken(ActionType.TASK_CREATED, name, if(tomorrow) "Tomorrow" else "Today")
        )
    }

    private suspend fun completeTaskCommand(text: String): VoiceCmdResult {
        val name = text.replace(Regex("(?i)(complete|finish|done with)\\s*(the)?\\s*task"), "").trim()
        if (name.isBlank()) return VoiceCmdResult("Which task did you complete? 🤔")
        
        val tasks = taskRepository.getTasksByDate(getStartOfDay(), getStartOfDay() + 86400000).first()
        val match = tasks.find { it.title.lowercase().contains(name.lowercase()) }
        
        return if (match != null) {
            taskRepository.updateTask(match.copy(isCompleted = true))
            VoiceCmdResult(
                message = "🎉 **AMAZING!** Completed '${match.title}'!\n\n${encouragements.random()}",
                action = ActionTaken(ActionType.TASK_COMPLETED, match.title)
            )
        } else {
            VoiceCmdResult("Couldn't find a task matching '$name'. Say 'list tasks' to see your tasks! 📋")
        }
    }

    private suspend fun deleteGoalCommand(text: String): VoiceCmdResult {
        val name = text.replace(Regex("(?i)(delete|remove)\\s*(the)?\\s*goal"), "").trim()
        if (name.isBlank()) return VoiceCmdResult("Which goal should I delete? 🤔")
        
        val goals = goalRepository.allActiveGoals.first()
        val match = goals.find { it.title.lowercase().contains(name.lowercase()) }
        
        return if (match != null) {
            goalRepository.deleteGoalById(match.id)
            VoiceCmdResult(
                message = "🗑️ Deleted '${match.title}'. Focus on what matters! 💙",
                action = ActionTaken(ActionType.GOAL_DELETED, match.title)
            )
        } else {
            VoiceCmdResult("Couldn't find a goal matching '$name'. 📋")
        }
    }

    private suspend fun deleteTaskCommand(text: String): VoiceCmdResult {
        val name = text.replace(Regex("(?i)(delete|remove)\\s*(the)?\\s*task"), "").trim()
        if (name.isBlank()) return VoiceCmdResult("Which task should I delete? 🤔")
        
        val tasks = taskRepository.getTasksByDate(getStartOfDay(), getStartOfDay() + 86400000).first()
        val match = tasks.find { it.title.lowercase().contains(name.lowercase()) }
        
        return if (match != null) {
            taskRepository.deleteTask(match)
            VoiceCmdResult(
                message = "🗑️ Deleted '${match.title}'.",
                action = ActionTaken(ActionType.TASK_DELETED, match.title)
            )
        } else {
            VoiceCmdResult("Couldn't find a task matching '$name'. 📋")
        }
    }

    private suspend fun getProgressSummary(): VoiceCmdResult {
        val goals = goalRepository.allActiveGoals.first()
        val tasks = taskRepository.getTasksByDate(getStartOfDay(), getStartOfDay() + 86400000).first()
        val done = tasks.count { it.isCompleted }
        val total = tasks.size
        val pct = if (total > 0) done * 100 / total else 0
        
        val sb = StringBuilder("📊 **Today's Progress:**\n\n")
        sb.append("✅ Tasks: $done/$total ($pct%)\n")
        sb.append("🎯 Active Goals: ${goals.size}\n\n")
        
        sb.append(when {
            pct >= 80 -> "🌟 **Outstanding!** You're crushing it!"
            pct >= 50 -> "💪 **Great progress!** Keep it up!"
            pct > 0 -> "🚀 **Good start!** Every step counts!"
            else -> "✨ **New day, new opportunities!**"
        })
        
        return VoiceCmdResult(sb.toString())
    }

    private fun learnFromConversation(userMessage: String) {
        viewModelScope.launch {
            val lower = userMessage.lowercase()
            if (lower.contains("morning") || lower.contains("early")) {
                memoryRepository.rememberPreference("preferred_time", "morning")
            } else if (lower.contains("evening") || lower.contains("night")) {
                memoryRepository.rememberPreference("preferred_time", "evening")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
