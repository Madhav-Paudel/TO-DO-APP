package com.example.todoapp.llm

/**
 * PromptTemplates - JSON-only prompt engineering templates for on-device LLM
 * 
 * These templates are designed to produce structured JSON output that can be
 * reliably parsed and converted into app actions (create goal, add task, etc.)
 * 
 * The prompts use a strict format:
 * - System instruction demanding JSON-only output
 * - Context about current goals/tasks (compact format)
 * - User message
 * 
 * Expected output format:
 * {
 *   "action": "reply" | "create_goal" | "create_task" | "complete_task" | "delete_goal" | "delete_task" | "show_progress",
 *   "message": "Human readable response",
 *   "data": { ... action-specific fields ... }
 * }
 */
object PromptTemplates {
    
    /**
     * System instruction that enforces JSON-only output
     */
    const val SYSTEM_INSTRUCTION = """You are an assistant for a productivity/to-do app. You MUST reply in valid JSON only, with NO extra text before or after.

The JSON must be a single object with these keys:
- "action": one of "reply", "create_goal", "create_task", "complete_task", "delete_goal", "delete_task", "show_progress"
- "message": a short, friendly response to show the user
- "data": an object with fields relevant to the action

For "create_goal" data: {"goalTitle": string, "durationMonths": number, "dailyMinutes": number}
For "create_task" data: {"taskTitle": string, "dueDate": "today"|"tomorrow"|"YYYY-MM-DD", "minutes": number, "goalTitle": string (optional)}
For "complete_task" data: {"taskTitle": string}
For "delete_goal" data: {"goalTitle": string}
For "delete_task" data: {"taskTitle": string}
For "reply" and "show_progress": data can be empty {}

IMPORTANT: Output ONLY the JSON object. No markdown, no explanations, no code blocks."""

    /**
     * Compact system instruction for smaller context windows
     */
    const val SYSTEM_INSTRUCTION_COMPACT = """Reply in JSON only. Format: {"action":"<type>","message":"<text>","data":{...}}
Actions: reply, create_goal, create_task, complete_task, show_progress
create_goal data: goalTitle, durationMonths, dailyMinutes
create_task data: taskTitle, dueDate, minutes
Output JSON only, no other text."""

    /**
     * Build a complete prompt for the LLM
     * 
     * @param userMessage The user's input message
     * @param goals List of current goals (title, dailyMinutes, endDate)
     * @param tasks List of today's tasks (title, isCompleted, minutes)
     * @param useCompact Use compact system instruction for smaller models
     * @return Complete formatted prompt
     */
    fun buildPrompt(
        userMessage: String,
        goals: List<GoalContext> = emptyList(),
        tasks: List<TaskContext> = emptyList(),
        useCompact: Boolean = false
    ): String {
        val systemPrompt = if (useCompact) SYSTEM_INSTRUCTION_COMPACT else SYSTEM_INSTRUCTION
        val context = buildContext(goals, tasks)
        
        return """<|system|>
$systemPrompt
$context
</s>
<|user|>
$userMessage
</s>
<|assistant|>
"""
    }
    
    /**
     * Build Llama-style prompt format
     */
    fun buildLlamaPrompt(
        userMessage: String,
        goals: List<GoalContext> = emptyList(),
        tasks: List<TaskContext> = emptyList()
    ): String {
        val context = buildContext(goals, tasks)
        
        return """[INST] <<SYS>>
$SYSTEM_INSTRUCTION_COMPACT
$context
<</SYS>>

$userMessage [/INST]"""
    }
    
    /**
     * Build ChatML-style prompt format
     */
    fun buildChatMLPrompt(
        userMessage: String,
        goals: List<GoalContext> = emptyList(),
        tasks: List<TaskContext> = emptyList()
    ): String {
        val context = buildContext(goals, tasks)
        
        return """<|im_start|>system
$SYSTEM_INSTRUCTION_COMPACT
$context
<|im_end|>
<|im_start|>user
$userMessage
<|im_end|>
<|im_start|>assistant
"""
    }
    
    /**
     * Build simple prompt format (for models without special tokens)
     */
    fun buildSimplePrompt(
        userMessage: String,
        goals: List<GoalContext> = emptyList(),
        tasks: List<TaskContext> = emptyList()
    ): String {
        val context = buildContext(goals, tasks)
        
        return """### Instruction:
$SYSTEM_INSTRUCTION_COMPACT
$context

### Input:
$userMessage

### Response (JSON only):
"""
    }
    
    /**
     * Build context section from goals and tasks
     */
    private fun buildContext(goals: List<GoalContext>, tasks: List<TaskContext>): String {
        val sb = StringBuilder()
        
        if (goals.isNotEmpty()) {
            sb.append("\nContext - Goals: ")
            goals.take(5).forEachIndexed { index, goal ->
                if (index > 0) sb.append("; ")
                sb.append("${goal.title}|${goal.dailyMinutes}min|ends:${goal.endDate}")
            }
        }
        
        if (tasks.isNotEmpty()) {
            sb.append("\nContext - Today's Tasks: ")
            tasks.take(5).forEachIndexed { index, task ->
                if (index > 0) sb.append("; ")
                val status = if (task.isCompleted) "✓" else "○"
                sb.append("$status${task.title}")
                if (task.minutes > 0) sb.append("|${task.minutes}min")
            }
        }
        
        if (goals.isEmpty() && tasks.isEmpty()) {
            sb.append("\nContext: No active goals or tasks yet.")
        }
        
        return sb.toString()
    }
    
    // ========================================================================
    // Example Prompts for Specific Actions
    // ========================================================================
    
    /**
     * Example prompt for creating a goal
     */
    fun exampleCreateGoal(): String = buildSimplePrompt(
        userMessage = "Create a goal \"Learn Python\" in 6 months at 60 minutes per day",
        goals = listOf(GoalContext("Learn Spanish", 30, "2025-06-01"))
    )
    
    /**
     * Example prompt for adding a task
     */
    fun exampleAddTask(): String = buildSimplePrompt(
        userMessage = "Add task \"Watch OOP video\" tomorrow for 30 minutes",
        tasks = listOf(TaskContext("Review notes", false, 20))
    )
    
    /**
     * Example prompt for completing a task
     */
    fun exampleCompleteTask(): String = buildSimplePrompt(
        userMessage = "I finished the \"Review notes\" task",
        tasks = listOf(
            TaskContext("Review notes", false, 20),
            TaskContext("Practice coding", false, 45)
        )
    )
    
    /**
     * Example prompt for general query
     */
    fun exampleGeneralQuery(): String = buildSimplePrompt(
        userMessage = "How am I doing today?",
        goals = listOf(GoalContext("Learn Python", 60, "2025-12-01")),
        tasks = listOf(
            TaskContext("Morning review", true, 15),
            TaskContext("Coding practice", false, 30)
        )
    )
}

/**
 * Compact goal context for prompts
 */
data class GoalContext(
    val title: String,
    val dailyMinutes: Int,
    val endDate: String // YYYY-MM-DD format
)

/**
 * Compact task context for prompts
 */
data class TaskContext(
    val title: String,
    val isCompleted: Boolean,
    val minutes: Int = 0
)
