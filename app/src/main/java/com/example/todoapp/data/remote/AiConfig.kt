package com.example.todoapp.data.remote

import com.example.todoapp.BuildConfig

object AiConfig {
    // Google AI (Gemini) Configuration
    const val BASE_URL = "https://generativelanguage.googleapis.com/"
    val API_KEY = BuildConfig.GOOGLE_AI_API_KEY
    const val MODEL = "gemini-1.5-flash"
    
    // OpenRouter Configuration (for Chat Assistant)
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"
    val OPENROUTER_API_KEY = BuildConfig.OPENROUTER_API_KEY
    
    // Using a reliable free model from OpenRouter
    // Options: "meta-llama/llama-3.2-3b-instruct:free", "google/gemma-2-9b-it:free", "qwen/qwen-2-7b-instruct:free"
    const val OPENROUTER_MODEL = "meta-llama/llama-3.2-3b-instruct:free"
}
