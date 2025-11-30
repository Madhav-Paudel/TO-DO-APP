package com.example.todoapp.llm

import android.util.Log

/**
 * DeterministicParser - Regex-based fallback parser for user commands
 * 
 * When the LLM fails to return valid JSON or is unavailable, this parser
 * can extract structured commands from natural language using regex patterns.
 * 
 * Supported patterns:
 * - Create goal: "create goal X in Y months Z minutes/hours per day"
 * - Add task: "add task X tomorrow/today for Z minutes"
 * - Complete task: "complete/finish/done X"
 * - Delete goal: "delete goal X"
 * - Delete task: "delete task X"
 */
object DeterministicParser {
    
    private const val TAG = "DeterministicParser"
    
    // ========================================================================
    // Regex Patterns
    // ========================================================================
    
    // Create goal patterns
    private val CREATE_GOAL_FULL = Regex(
        """(?i)(?:create|add|new|set)\s+(?:a\s+)?goal\s+(?:to\s+)?["']?(.+?)["']?\s+(?:in|for)\s+(\d+)\s*months?.*?(\d+)\s*(?:hours?|hr|h|minutes?|min|m)\s*(?:per|a|each)?\s*(?:day)?""",
        RegexOption.IGNORE_CASE
    )
    
    private val CREATE_GOAL_SIMPLE = Regex(
        """(?i)(?:create|add|new|set)\s+(?:a\s+)?goal\s+(?:to\s+)?["']?(.+?)["']?(?:\s*$|\s+(?:in|for|at|with))""",
        RegexOption.IGNORE_CASE
    )
    
    private val CREATE_GOAL_DURATION = Regex(
        """(\d+)\s*months?""",
        RegexOption.IGNORE_CASE
    )
    
    private val CREATE_GOAL_TIME = Regex(
        """(\d+)\s*(?:hours?|hr|h)\s*(?:per|a|each)?\s*day|(\d+)\s*(?:minutes?|min|m)\s*(?:per|a|each)?\s*day""",
        RegexOption.IGNORE_CASE
    )
    
    // Add task patterns
    private val ADD_TASK_FULL = Regex(
        """(?i)(?:add|create|new)\s+(?:a\s+)?task\s+["']?(.+?)["']?\s+(?:for\s+)?(today|tomorrow|next\s+week)(?:.*?(\d+)\s*(?:min(?:utes?)?|m|hours?|hr|h))?""",
        RegexOption.IGNORE_CASE
    )
    
    private val ADD_TASK_SIMPLE = Regex(
        """(?i)(?:add|create|new)\s+(?:a\s+)?task\s+["']?(.+?)["']?(?:\s*$|\s+(?:for|on|due|tomorrow|today))""",
        RegexOption.IGNORE_CASE
    )
    
    private val TASK_DURATION = Regex(
        """(\d+)\s*(?:min(?:utes?)?|m)""",
        RegexOption.IGNORE_CASE
    )
    
    private val TASK_DURATION_HOURS = Regex(
        """(\d+)\s*(?:hours?|hr|h)""",
        RegexOption.IGNORE_CASE
    )
    
    private val TASK_DUE_DATE = Regex(
        """(today|tomorrow|next\s+week)""",
        RegexOption.IGNORE_CASE
    )
    
    // Complete task patterns
    private val COMPLETE_TASK = Regex(
        """(?i)(?:complete|finish|done\s+with|mark\s+(?:as\s+)?(?:done|complete))\s+(?:the\s+)?(?:task\s+)?["']?(.+?)["']?(?:\s+task)?(?:\s*$|\s+(?:please|thanks))""",
        RegexOption.IGNORE_CASE
    )
    
    // Delete patterns
    private val DELETE_GOAL = Regex(
        """(?i)(?:delete|remove)\s+(?:the\s+)?goal\s+["']?(.+?)["']?(?:\s*$|\s+(?:please|thanks))""",
        RegexOption.IGNORE_CASE
    )
    
    private val DELETE_TASK = Regex(
        """(?i)(?:delete|remove)\s+(?:the\s+)?task\s+["']?(.+?)["']?(?:\s*$|\s+(?:please|thanks))""",
        RegexOption.IGNORE_CASE
    )
    
    // List/show patterns
    private val LIST_GOALS = Regex(
        """(?i)(?:list|show|what\s+are)\s+(?:my\s+)?goals?""",
        RegexOption.IGNORE_CASE
    )
    
    private val LIST_TASKS = Regex(
        """(?i)(?:list|show|what\s+are)\s+(?:my\s+)?(?:today'?s?\s+)?tasks?""",
        RegexOption.IGNORE_CASE
    )
    
    private val SHOW_PROGRESS = Regex(
        """(?i)(?:how\s+am\s+i\s+doing|my\s+progress|show\s+progress|status|summary)""",
        RegexOption.IGNORE_CASE
    )
    
    // ========================================================================
    // Parsing Functions
    // ========================================================================
    
    /**
     * Parse user input and return an AssistantAction if a pattern matches
     * 
     * @param input User's message
     * @return Parsed AssistantAction or null if no pattern matches
     */
    fun parse(input: String): AssistantAction? {
        val trimmed = input.trim()
        
        // Try each pattern in order of specificity
        return tryParseCreateGoal(trimmed)
            ?: tryParseAddTask(trimmed)
            ?: tryParseCompleteTask(trimmed)
            ?: tryParseDeleteGoal(trimmed)
            ?: tryParseDeleteTask(trimmed)
            ?: tryParseListOrProgress(trimmed)
    }
    
    /**
     * Parse create goal command
     */
    private fun tryParseCreateGoal(input: String): AssistantAction.CreateGoal? {
        // Try full pattern first
        CREATE_GOAL_FULL.find(input)?.let { match ->
            val goalName = match.groupValues[1].trim()
            val months = match.groupValues[2].toIntOrNull() ?: 3
            val timeValue = match.groupValues[3].toIntOrNull() ?: 30
            
            // Determine if hours or minutes based on context
            val dailyMinutes = if (input.lowercase().contains("hour")) {
                timeValue * 60
            } else {
                timeValue
            }
            
            Log.d(TAG, "Parsed create_goal (full): $goalName, $months months, $dailyMinutes min/day")
            return AssistantAction.CreateGoal(
                message = "I'll create a goal for \"$goalName\" - $months months at $dailyMinutes minutes per day.",
                goalTitle = goalName,
                durationMonths = months,
                dailyMinutes = dailyMinutes
            )
        }
        
        // Try simple pattern
        CREATE_GOAL_SIMPLE.find(input)?.let { match ->
            var goalName = match.groupValues[1].trim()
            
            // Clean up the goal name
            goalName = goalName
                .replace(Regex("""(?i)\s*(in|for|at|with)\s*\d+.*$"""), "")
                .trim()
            
            // Extract duration if present elsewhere
            val months = CREATE_GOAL_DURATION.find(input)?.groupValues?.get(1)?.toIntOrNull() ?: 3
            
            // Extract daily time if present
            var dailyMinutes = 30
            CREATE_GOAL_TIME.find(input)?.let { timeMatch ->
                val hours = timeMatch.groupValues[1].toIntOrNull()
                val minutes = timeMatch.groupValues[2].toIntOrNull()
                dailyMinutes = when {
                    hours != null -> hours * 60
                    minutes != null -> minutes
                    else -> 30
                }
            }
            
            if (goalName.isNotEmpty()) {
                Log.d(TAG, "Parsed create_goal (simple): $goalName")
                return AssistantAction.CreateGoal(
                    message = "I'll create a goal for \"$goalName\".",
                    goalTitle = goalName,
                    durationMonths = months,
                    dailyMinutes = dailyMinutes
                )
            }
        }
        
        return null
    }
    
    /**
     * Parse add task command
     */
    private fun tryParseAddTask(input: String): AssistantAction.CreateTask? {
        // Try full pattern
        ADD_TASK_FULL.find(input)?.let { match ->
            val taskName = match.groupValues[1].trim()
            val dueDate = match.groupValues[2].lowercase().replace("\\s+".toRegex(), "_")
            val minutes = match.groupValues[3].toIntOrNull() ?: 30
            
            Log.d(TAG, "Parsed create_task (full): $taskName, due: $dueDate, $minutes min")
            return AssistantAction.CreateTask(
                message = "I'll add the task \"$taskName\" for $dueDate.",
                taskTitle = taskName,
                dueDate = normalizeDueDate(dueDate),
                minutes = minutes
            )
        }
        
        // Try simple pattern
        ADD_TASK_SIMPLE.find(input)?.let { match ->
            var taskName = match.groupValues[1].trim()
            
            // Clean up task name
            taskName = taskName
                .replace(Regex("""(?i)\s*(for|on|due|tomorrow|today)\s*.*$"""), "")
                .trim()
            
            // Extract due date
            val dueDate = TASK_DUE_DATE.find(input)?.value?.lowercase() ?: "today"
            
            // Extract duration
            var minutes = 30
            TASK_DURATION.find(input)?.let { 
                minutes = it.groupValues[1].toIntOrNull() ?: 30
            }
            TASK_DURATION_HOURS.find(input)?.let {
                minutes = (it.groupValues[1].toIntOrNull() ?: 1) * 60
            }
            
            if (taskName.isNotEmpty()) {
                Log.d(TAG, "Parsed create_task (simple): $taskName")
                return AssistantAction.CreateTask(
                    message = "I'll add the task \"$taskName\".",
                    taskTitle = taskName,
                    dueDate = normalizeDueDate(dueDate),
                    minutes = minutes
                )
            }
        }
        
        return null
    }
    
    /**
     * Parse complete task command
     */
    private fun tryParseCompleteTask(input: String): AssistantAction.CompleteTask? {
        COMPLETE_TASK.find(input)?.let { match ->
            val taskName = match.groupValues[1].trim()
            if (taskName.isNotEmpty()) {
                Log.d(TAG, "Parsed complete_task: $taskName")
                return AssistantAction.CompleteTask(
                    message = "Great job! I'll mark \"$taskName\" as complete.",
                    taskTitle = taskName
                )
            }
        }
        return null
    }
    
    /**
     * Parse delete goal command
     */
    private fun tryParseDeleteGoal(input: String): AssistantAction.DeleteGoal? {
        DELETE_GOAL.find(input)?.let { match ->
            val goalName = match.groupValues[1].trim()
            if (goalName.isNotEmpty()) {
                Log.d(TAG, "Parsed delete_goal: $goalName")
                return AssistantAction.DeleteGoal(
                    message = "I'll delete the goal \"$goalName\".",
                    goalTitle = goalName
                )
            }
        }
        return null
    }
    
    /**
     * Parse delete task command
     */
    private fun tryParseDeleteTask(input: String): AssistantAction.DeleteTask? {
        DELETE_TASK.find(input)?.let { match ->
            val taskName = match.groupValues[1].trim()
            if (taskName.isNotEmpty()) {
                Log.d(TAG, "Parsed delete_task: $taskName")
                return AssistantAction.DeleteTask(
                    message = "I'll delete the task \"$taskName\".",
                    taskTitle = taskName
                )
            }
        }
        return null
    }
    
    /**
     * Parse list/progress commands
     */
    private fun tryParseListOrProgress(input: String): AssistantAction? {
        if (SHOW_PROGRESS.containsMatchIn(input)) {
            return AssistantAction.ShowProgress("Here's your progress summary!")
        }
        if (LIST_GOALS.containsMatchIn(input)) {
            return AssistantAction.Reply("Here are your current goals.")
        }
        if (LIST_TASKS.containsMatchIn(input)) {
            return AssistantAction.Reply("Here are your tasks for today.")
        }
        return null
    }
    
    /**
     * Normalize due date string to consistent format
     */
    private fun normalizeDueDate(input: String): String {
        return when (input.lowercase().replace("\\s+".toRegex(), "_")) {
            "today" -> "today"
            "tomorrow" -> "tomorrow"
            "next_week" -> "next_week"
            else -> input
        }
    }
    
    /**
     * Check if input looks like a command (vs. general chat)
     */
    fun looksLikeCommand(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("create") ||
               lower.contains("add") ||
               lower.contains("delete") ||
               lower.contains("remove") ||
               lower.contains("complete") ||
               lower.contains("finish") ||
               lower.contains("done") ||
               lower.contains("list") ||
               lower.contains("show") ||
               lower.contains("progress")
    }
}
