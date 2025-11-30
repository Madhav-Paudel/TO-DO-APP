package com.example.todoapp.data.remote

/**
 * DISABLED: Cloud-based AI repository removed.
 * Using on-device LLM via llama.cpp instead.
 * See: com.example.todoapp.llm.LocalAssistantRepository
 */

/*
import com.example.todoapp.data.model.ChatMessage
import com.example.todoapp.data.model.ChatSender
import com.example.todoapp.data.model.GeminiContent
import com.example.todoapp.data.model.GeminiPart
import com.example.todoapp.data.model.GeminiRequest
import com.example.todoapp.data.model.OpenRouterMessage
import com.example.todoapp.data.model.OpenRouterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository(
    private val aiService: AiService,
    private val openRouterService: OpenRouterService
) {

    suspend fun sendChat(
        systemPrompt: String,
        contextMessage: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // Use OpenRouter with Nvidia model for chat
            val messages = mutableListOf<OpenRouterMessage>()
            
            // Add system prompt
            messages.add(OpenRouterMessage(role = "system", content = systemPrompt))
            
            // Add context as a system message
            if (contextMessage.isNotBlank()) {
                messages.add(OpenRouterMessage(role = "system", content = contextMessage))
            }
            
            // Add conversation history (last 10 messages)
            val recentHistory = history.takeLast(10)
            recentHistory.forEach { chatMessage ->
                val role = when (chatMessage.sender) {
                    ChatSender.USER -> "user"
                    ChatSender.ASSISTANT -> "assistant"
                    ChatSender.SYSTEM -> "system"
                }
                messages.add(OpenRouterMessage(role = role, content = chatMessage.text))
            }
            
            // Add current user message
            messages.add(OpenRouterMessage(role = "user", content = userMessage))

            val request = OpenRouterRequest(
                model = AiConfig.OPENROUTER_MODEL,
                messages = messages
            )
            
            val authHeader = "Bearer ${AiConfig.OPENROUTER_API_KEY}"
            val response = openRouterService.sendChat(authHeader, request = request)
            
            if (response.error != null) {
                return@withContext "Error: ${response.error.message ?: "Unknown error"}"
            }
            
            response.choices?.firstOrNull()?.message?.content
                ?: "Sorry, I received an empty response from the AI."
                
        } catch (e: Exception) {
            e.printStackTrace()
            "Sorry, I couldn't reach the AI service right now. Please try again later. (Error: ${e.message})"
        }
    }
    
    // Keep this method for potential future Gemini usage
    suspend fun sendChatGemini(
        systemPrompt: String,
        contextMessage: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val contents = mutableListOf<GeminiContent>()
            
            // Add conversation history (last 10 messages)
            val recentHistory = history.takeLast(10)
            recentHistory.forEach { chatMessage ->
                val role = when (chatMessage.sender) {
                    ChatSender.USER -> "user"
                    ChatSender.ASSISTANT -> "model"
                    ChatSender.SYSTEM -> "user" // Gemini doesn't have system role in contents
                }
                contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(chatMessage.text))))
            }
            
            // Add current user message with context
            val fullUserMessage = """
                $contextMessage
                
                User: $userMessage
            """.trimIndent()
            contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(fullUserMessage))))

            // System instruction
            val systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(systemPrompt))
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = systemInstruction
            )
            
            val response = aiService.sendChat(AiConfig.API_KEY, request)
            
            if (response.error != null) {
                return@withContext "Error: ${response.error.message ?: "Unknown error"}"
            }
            
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Sorry, I received an empty response from the AI."
                
        } catch (e: Exception) {
            e.printStackTrace()
            "Sorry, I couldn't reach the AI service right now. Please try again later. (Error: ${e.message})"
        }
    }
}
*/
