package com.example.todoapp.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LocalAssistantRepository - Repository for on-device LLM inference
 * 
 * Manages the lifecycle of the native llama.cpp model and provides
 * a clean API for generating responses. Uses JSON-first prompt templates
 * and falls back to deterministic parsing when needed.
 * 
 * Thread-safe: All model operations are protected by a mutex.
 */
class LocalAssistantRepository(
    private val context: Context,
    private val modelManager: ModelManager
) {
    
    companion object {
        private const val TAG = "LocalAssistantRepository"
        private const val DEFAULT_MAX_TOKENS = 256
    }
    
    // Model state
    private var contextPtr: Long = 0L
    private var currentModelPath: String? = null
    private val mutex = Mutex()
    
    // Status
    val isModelLoaded: Boolean
        get() = contextPtr != 0L
    
    val isNativeAvailable: Boolean
        get() = LlamaNative.isLibraryLoaded
    
    /**
     * Initialize the model from the currently selected model in ModelManager
     */
    suspend fun initializeModel(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // Check if native library is loaded
                if (!LlamaNative.isLibraryLoaded) {
                    return@withContext Result.failure(
                        LocalAssistantException("Native library not available")
                    )
                }
                
                // Get selected model path
                val modelPath = modelManager.getSelectedModelPath()
                    ?: return@withContext Result.failure(
                        LocalAssistantException("No model selected. Please download a model first.")
                    )
                
                // If same model is already loaded, skip
                if (contextPtr != 0L && currentModelPath == modelPath) {
                    Log.i(TAG, "Model already loaded: $modelPath")
                    return@withContext Result.success(Unit)
                }
                
                // Unload existing model if any
                if (contextPtr != 0L) {
                    LlamaNative.freeModel(contextPtr)
                    contextPtr = 0L
                }
                
                // Load new model
                Log.i(TAG, "Loading model: $modelPath")
                val result = LlamaNative.initModelSafe(modelPath)
                
                return@withContext result.fold(
                    onSuccess = { handle ->
                        contextPtr = handle
                        currentModelPath = modelPath
                        Log.i(TAG, "Model loaded successfully, handle: $handle")
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to load model: ${error.message}")
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing model: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate a response from the model
     * 
     * @param userMessage The user's input
     * @param goals Current goals for context
     * @param tasks Current tasks for context
     * @param maxTokens Maximum tokens to generate
     * @return AssistantAction parsed from the model's response
     */
    suspend fun generate(
        userMessage: String,
        goals: List<GoalContext> = emptyList(),
        tasks: List<TaskContext> = emptyList(),
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): Result<AssistantAction> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // Check model status
                if (contextPtr == 0L) {
                    // Try to initialize model first
                    val initResult = withContext(Dispatchers.IO) { 
                        // Temporarily release lock for init
                        mutex.withLock {
                            initializeModelInternal()
                        }
                    }
                    if (initResult.isFailure) {
                        // Fall back to deterministic parser
                        Log.w(TAG, "Model not loaded, using deterministic parser")
                        val deterministicAction = DeterministicParser.parse(userMessage)
                        if (deterministicAction != null) {
                            return@withContext Result.success(deterministicAction)
                        }
                        return@withContext Result.failure(
                            LocalAssistantException("Model not loaded and no command pattern matched")
                        )
                    }
                }
                
                // Build prompt
                val prompt = PromptTemplates.buildSimplePrompt(
                    userMessage = userMessage,
                    goals = goals,
                    tasks = tasks
                )
                
                Log.d(TAG, "Generating response for: $userMessage")
                
                // Generate response
                val generateResult = LlamaNative.generateSafe(contextPtr, prompt, maxTokens)
                
                return@withContext generateResult.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Raw response: $response")
                        
                        // Try to parse JSON response
                        val action = JsonResponseParser.parse(response)
                        if (action != null) {
                            Log.i(TAG, "Parsed action: ${action::class.simpleName}")
                            Result.success(action)
                        } else {
                            // Try deterministic parser as fallback
                            Log.w(TAG, "JSON parsing failed, trying deterministic parser")
                            val deterministicAction = DeterministicParser.parse(userMessage)
                            if (deterministicAction != null) {
                                Result.success(deterministicAction)
                            } else {
                                // Return raw response as reply
                                Result.success(AssistantAction.Reply(response))
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Generation failed: ${error.message}")
                        
                        // Try deterministic parser
                        val deterministicAction = DeterministicParser.parse(userMessage)
                        if (deterministicAction != null) {
                            Result.success(deterministicAction)
                        } else {
                            Result.failure(error)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in generate: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Internal initialize without lock (caller must hold lock)
     */
    private fun initializeModelInternal(): Result<Unit> {
        if (!LlamaNative.isLibraryLoaded) {
            return Result.failure(LocalAssistantException("Native library not available"))
        }
        
        val modelPath = modelManager.getSelectedModelPath()
            ?: return Result.failure(LocalAssistantException("No model selected"))
        
        if (contextPtr != 0L && currentModelPath == modelPath) {
            return Result.success(Unit)
        }
        
        if (contextPtr != 0L) {
            LlamaNative.freeModel(contextPtr)
            contextPtr = 0L
        }
        
        return LlamaNative.initModelSafe(modelPath).fold(
            onSuccess = { handle ->
                contextPtr = handle
                currentModelPath = modelPath
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }
    
    /**
     * Generate a simple response without context (for quick queries)
     */
    suspend fun generateSimple(
        userMessage: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): Result<String> = withContext(Dispatchers.IO) {
        generate(userMessage, maxTokens = maxTokens).map { action ->
            action.message
        }
    }
    
    /**
     * Check if the model is ready for inference
     */
    fun isReady(): Boolean = contextPtr != 0L && LlamaNative.isLibraryLoaded
    
    /**
     * Get info about the loaded model
     */
    fun getModelInfo(): ModelInfo? {
        val selectedId = modelManager.selectedModelId.value ?: return null
        return modelManager.getModelInfo(selectedId)
    }
    
    /**
     * Close the repository and free resources
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (contextPtr != 0L) {
                Log.i(TAG, "Freeing model resources")
                LlamaNative.freeModelSafe(contextPtr)
                contextPtr = 0L
                currentModelPath = null
            }
        }
    }
    
    /**
     * Reload model (useful after downloading a new model)
     */
    suspend fun reloadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Force unload
            if (contextPtr != 0L) {
                LlamaNative.freeModel(contextPtr)
                contextPtr = 0L
                currentModelPath = null
            }
        }
        // Re-initialize
        initializeModel()
    }
}

/**
 * Exception for LocalAssistantRepository errors
 */
class LocalAssistantException(message: String, cause: Throwable? = null) : Exception(message, cause)
