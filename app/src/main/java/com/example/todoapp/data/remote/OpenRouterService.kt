package com.example.todoapp.data.remote

/**
 * DISABLED: Cloud-based OpenRouter service removed.
 * Using on-device LLM via llama.cpp instead.
 * See: com.example.todoapp.llm.LocalAssistantRepository
 */

/*
import com.example.todoapp.data.model.OpenRouterRequest
import com.example.todoapp.data.model.OpenRouterResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterService {
    @POST("chat/completions")
    suspend fun sendChat(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("HTTP-Referer") referer: String = "https://todoapp.local",
        @Header("X-Title") title: String = "ToDoApp Assistant",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
*/
