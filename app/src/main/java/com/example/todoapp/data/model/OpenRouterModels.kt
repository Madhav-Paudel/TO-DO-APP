package com.example.todoapp.data.model

/**
 * DISABLED: Cloud-based AI models removed.
 * Using on-device LLM via llama.cpp instead.
 * 
 * This file is kept for reference but all content is commented out.
 */

/*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Gemini API Models
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val message: String?,
    val status: String?
)

// OpenRouter API Models (OpenAI-compatible format)
@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>? = null,
    val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val message: OpenRouterMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    val message: String?,
    val code: String? = null
)
*/
