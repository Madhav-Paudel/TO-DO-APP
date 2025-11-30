package com.example.todoapp.data.model

enum class ChatSender {
    USER, ASSISTANT, SYSTEM
}

enum class ActionType {
    GOAL_CREATED, TASK_CREATED, GOAL_DELETED, TASK_DELETED, TASK_COMPLETED, LIST_SHOWN, NONE
}

data class ActionTaken(
    val type: ActionType,
    val itemName: String,
    val details: String = ""
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: ActionTaken? = null
)
