package com.example.todoapp.llm

import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * JsonResponseParser - Parse and validate JSON responses from the LLM
 * 
 * Handles parsing of structured JSON output and extraction of actions/data.
 * Falls back gracefully when JSON is malformed or missing expected fields.
 */
object JsonResponseParser {
    
    private const val TAG = "JsonResponseParser"
    
    /**
     * Parse LLM response into an AssistantAction
     * 
     * @param response Raw response string from LLM
     * @return Parsed AssistantAction or null if parsing failed
     */
    fun parse(response: String): AssistantAction? {
        // Try to extract JSON if response contains extra text
        val jsonStr = extractJson(response)
        
        return try {
            val json = JSONObject(jsonStr)
            
            val actionType = json.optString("action", "reply")
            val message = json.optString("message", "")
            val data = json.optJSONObject("data") ?: JSONObject()
            
            when (actionType.lowercase()) {
                "create_goal" -> parseCreateGoal(message, data)
                "create_task" -> parseCreateTask(message, data)
                "complete_task" -> parseCompleteTask(message, data)
                "delete_goal" -> parseDeleteGoal(message, data)
                "delete_task" -> parseDeleteTask(message, data)
                "show_progress" -> AssistantAction.ShowProgress(message)
                "reply" -> AssistantAction.Reply(message)
                else -> AssistantAction.Reply(message.ifEmpty { response })
            }
        } catch (e: JSONException) {
            Log.w(TAG, "Failed to parse JSON: ${e.message}")
            null
        }
    }
    
    /**
     * Extract JSON object from a response that may contain extra text
     */
    private fun extractJson(response: String): String {
        val trimmed = response.trim()
        
        // If already starts with {, assume it's JSON
        if (trimmed.startsWith("{")) {
            // Find matching closing brace
            var depth = 0
            var endIndex = -1
            for (i in trimmed.indices) {
                when (trimmed[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            endIndex = i
                            break
                        }
                    }
                }
            }
            return if (endIndex > 0) trimmed.substring(0, endIndex + 1) else trimmed
        }
        
        // Try to find JSON in the response
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        
        return if (jsonStart >= 0 && jsonEnd > jsonStart) {
            trimmed.substring(jsonStart, jsonEnd + 1)
        } else {
            // Return as-is, let JSONObject throw exception
            trimmed
        }
    }
    
    private fun parseCreateGoal(message: String, data: JSONObject): AssistantAction.CreateGoal {
        return AssistantAction.CreateGoal(
            message = message,
            goalTitle = data.optStringAlt("goalTitle", "goal_title", "title") ?: "New Goal",
            durationMonths = data.optIntAlt("durationMonths", "duration_months") ?: 3,
            dailyMinutes = data.optIntAlt("dailyMinutes", "daily_minutes") ?: 30
        )
    }
    
    private fun parseCreateTask(message: String, data: JSONObject): AssistantAction.CreateTask {
        val goalTitleValue = data.optStringAlt("goalTitle", "goal_title")
        return AssistantAction.CreateTask(
            message = message,
            taskTitle = data.optStringAlt("taskTitle", "task_title", "title") ?: "New Task",
            dueDate = data.optStringAlt("dueDate", "due_date") ?: "today",
            minutes = data.optIntAlt("minutes", "duration") ?: 30,
            goalTitle = goalTitleValue
        )
    }
    
    // Helper extension to try multiple field names
    private fun JSONObject.optStringAlt(vararg keys: String): String? {
        for (key in keys) {
            if (has(key) && !isNull(key)) {
                return getString(key)
            }
        }
        return null
    }
    
    private fun JSONObject.optIntAlt(vararg keys: String): Int? {
        for (key in keys) {
            if (has(key) && !isNull(key)) {
                return optInt(key)
            }
        }
        return null
    }
    
    private fun parseCompleteTask(message: String, data: JSONObject): AssistantAction.CompleteTask {
        return AssistantAction.CompleteTask(
            message = message,
            taskTitle = data.optStringAlt("taskTitle", "task_title", "title") ?: ""
        )
    }
    
    private fun parseDeleteGoal(message: String, data: JSONObject): AssistantAction.DeleteGoal {
        return AssistantAction.DeleteGoal(
            message = message,
            goalTitle = data.optStringAlt("goalTitle", "goal_title", "title") ?: ""
        )
    }
    
    private fun parseDeleteTask(message: String, data: JSONObject): AssistantAction.DeleteTask {
        return AssistantAction.DeleteTask(
            message = message,
            taskTitle = data.optStringAlt("taskTitle", "task_title", "title") ?: ""
        )
    }
    
    /**
     * Check if a string appears to be valid JSON
     */
    fun isValidJson(str: String): Boolean {
        return try {
            JSONObject(extractJson(str))
            true
        } catch (e: JSONException) {
            false
        }
    }
    
    /**
     * Parse LLM response and always return a valid action
     * Falls back to Reply if parsing fails
     */
    fun parseResponse(response: String): AssistantAction {
        // Try JSON parsing first
        val jsonAction = parse(response)
        if (jsonAction != null) {
            return jsonAction
        }
        
        // If response is empty or whitespace, return generic message
        if (response.isBlank()) {
            return AssistantAction.Reply(
                "I'm processing your request. Could you try again or rephrase?"
            )
        }
        
        // Check if the raw response looks like a conversational reply
        val cleanResponse = response.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        
        // Return the response as a reply
        return AssistantAction.Reply(cleanResponse)
    }
}

/**
 * Sealed class representing actions the assistant can take
 */
sealed class AssistantAction {
    abstract val message: String
    
    /**
     * Simple text reply with no side effects
     */
    data class Reply(override val message: String) : AssistantAction()
    
    /**
     * Create a new goal
     */
    data class CreateGoal(
        override val message: String,
        val goalTitle: String,
        val durationMonths: Int,
        val dailyMinutes: Int
    ) : AssistantAction()
    
    /**
     * Create a new task
     */
    data class CreateTask(
        override val message: String,
        val taskTitle: String,
        val dueDate: String, // "today", "tomorrow", or "YYYY-MM-DD"
        val minutes: Int,
        val goalTitle: String? = null
    ) : AssistantAction()
    
    /**
     * Mark a task as complete
     */
    data class CompleteTask(
        override val message: String,
        val taskTitle: String
    ) : AssistantAction()
    
    /**
     * Delete a goal
     */
    data class DeleteGoal(
        override val message: String,
        val goalTitle: String
    ) : AssistantAction()
    
    /**
     * Delete a task
     */
    data class DeleteTask(
        override val message: String,
        val taskTitle: String
    ) : AssistantAction()
    
    /**
     * Show progress summary
     */
    data class ShowProgress(override val message: String) : AssistantAction()
}
