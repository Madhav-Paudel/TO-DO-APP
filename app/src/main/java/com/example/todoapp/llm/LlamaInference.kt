package com.example.todoapp.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * LlamaInference - Kotlin wrapper for llama.cpp native JNI functions
 * 
 * This class provides a clean Kotlin API for on-device LLM inference
 * using the llama.cpp library via JNI.
 * 
 * Usage:
 * ```kotlin
 * val inference = LlamaInference()
 * inference.initialize()
 * inference.loadModel("/path/to/model.gguf", threads = 4, contextSize = 2048)
 * 
 * val response = inference.generate("Hello, how are you?", maxTokens = 256)
 * 
 * inference.cleanup()
 * ```
 */
class LlamaInference {
    
    companion object {
        private const val TAG = "LlamaInference"
        
        // Default inference parameters
        const val DEFAULT_THREADS = 4
        const val DEFAULT_CONTEXT_SIZE = 2048
        const val DEFAULT_MAX_TOKENS = 256
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_P = 0.9f
        
        /**
         * Check if the native library is available
         */
        @JvmStatic
        fun isAvailable(): Boolean {
            return try {
                System.loadLibrary("llamainference")
                nativeIsAvailable()
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library not available: ${e.message}")
                false
            }
        }
        
        /**
         * Get the native library version
         */
        @JvmStatic
        fun getVersion(): String {
            return try {
                System.loadLibrary("llamainference")
                nativeGetVersion()
            } catch (e: UnsatisfiedLinkError) {
                "Not available"
            }
        }
        
        // Static native methods
        @JvmStatic
        private external fun nativeIsAvailable(): Boolean
        
        @JvmStatic
        private external fun nativeGetVersion(): String
        
        init {
            try {
                System.loadLibrary("llamainference")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }
    
    // State
    private val _state = MutableStateFlow(InferenceState.UNINITIALIZED)
    val state: StateFlow<InferenceState> = _state.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private var modelPath: String? = null
    private var currentThreads: Int = DEFAULT_THREADS
    private var currentContextSize: Int = DEFAULT_CONTEXT_SIZE
    
    /**
     * Initialize the llama.cpp backend
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val success = nativeInit()
            if (success) {
                _state.value = InferenceState.INITIALIZED
                Log.i(TAG, "Initialized successfully")
                Result.success(Unit)
            } else {
                _state.value = InferenceState.ERROR
                Result.failure(LlamaException("Failed to initialize native backend"))
            }
        } catch (e: Exception) {
            _state.value = InferenceState.ERROR
            Log.e(TAG, "Initialize error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Load a GGUF model file
     * 
     * @param modelPath Path to the .gguf model file
     * @param threads Number of threads for inference (default: 4)
     * @param contextSize Maximum context size in tokens (default: 2048)
     */
    suspend fun loadModel(
        modelPath: String,
        threads: Int = DEFAULT_THREADS,
        contextSize: Int = DEFAULT_CONTEXT_SIZE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Ensure initialized
            if (_state.value == InferenceState.UNINITIALIZED) {
                val initResult = initialize()
                if (initResult.isFailure) return@withContext initResult
            }
            
            _state.value = InferenceState.LOADING
            
            val success = nativeLoadModel(modelPath, threads, contextSize)
            if (success) {
                this@LlamaInference.modelPath = modelPath
                this@LlamaInference.currentThreads = threads
                this@LlamaInference.currentContextSize = contextSize
                _state.value = InferenceState.MODEL_LOADED
                Log.i(TAG, "Model loaded: $modelPath")
                Result.success(Unit)
            } else {
                _state.value = InferenceState.ERROR
                Result.failure(LlamaException("Failed to load model: $modelPath"))
            }
        } catch (e: Exception) {
            _state.value = InferenceState.ERROR
            Log.e(TAG, "Load model error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Generate text from a prompt
     * 
     * @param prompt Input prompt
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0 - 2.0)
     * @param topP Top-p sampling parameter
     * @return Generated text
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        temperature: Float = DEFAULT_TEMPERATURE,
        topP: Float = DEFAULT_TOP_P
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (_state.value != InferenceState.MODEL_LOADED) {
                return@withContext Result.failure(
                    LlamaException("Model not loaded. Current state: ${_state.value}")
                )
            }
            
            _isGenerating.value = true
            
            val response = nativeGenerate(prompt, maxTokens, temperature, topP)
            
            _isGenerating.value = false
            
            Log.d(TAG, "Generated ${response.length} chars")
            Result.success(response)
            
        } catch (e: Exception) {
            _isGenerating.value = false
            Log.e(TAG, "Generate error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Generate text with streaming callback
     * 
     * @param prompt Input prompt
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature
     * @param onToken Callback for each generated token
     * @return Final generated text
     */
    suspend fun generateStreaming(
        prompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        temperature: Float = DEFAULT_TEMPERATURE,
        onToken: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (_state.value != InferenceState.MODEL_LOADED) {
                return@withContext Result.failure(
                    LlamaException("Model not loaded. Current state: ${_state.value}")
                )
            }
            
            _isGenerating.value = true
            
            val callback = object : TokenCallback {
                override fun onToken(token: String) {
                    onToken(token)
                }
            }
            
            val response = nativeGenerateWithCallback(prompt, maxTokens, temperature, callback)
            
            _isGenerating.value = false
            Result.success(response)
            
        } catch (e: Exception) {
            _isGenerating.value = false
            Log.e(TAG, "Generate streaming error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Unload the current model
     */
    fun unloadModel() {
        nativeUnloadModel()
        modelPath = null
        _state.value = InferenceState.INITIALIZED
        Log.i(TAG, "Model unloaded")
    }
    
    /**
     * Check if a model is currently loaded
     */
    fun isModelLoaded(): Boolean {
        return nativeIsModelLoaded()
    }
    
    /**
     * Get information about the loaded model
     */
    fun getModelInfo(): String {
        return nativeGetModelInfo()
    }
    
    /**
     * Clean up all resources
     */
    fun cleanup() {
        nativeCleanup()
        modelPath = null
        _state.value = InferenceState.UNINITIALIZED
        Log.i(TAG, "Cleanup complete")
    }
    
    /**
     * Get the path of the currently loaded model
     */
    fun getLoadedModelPath(): String? = modelPath
    
    // Native method declarations
    private external fun nativeInit(): Boolean
    private external fun nativeLoadModel(modelPath: String, nThreads: Int, nCtx: Int): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int, temperature: Float, topP: Float): String
    private external fun nativeGenerateWithCallback(prompt: String, maxTokens: Int, temperature: Float, callback: TokenCallback): String
    private external fun nativeUnloadModel()
    private external fun nativeCleanup()
    private external fun nativeIsModelLoaded(): Boolean
    private external fun nativeGetModelInfo(): String
}

/**
 * Inference state enum
 */
enum class InferenceState {
    UNINITIALIZED,
    INITIALIZED,
    LOADING,
    MODEL_LOADED,
    ERROR
}

/**
 * Callback interface for streaming token generation
 */
interface TokenCallback {
    fun onToken(token: String)
}

/**
 * Exception class for llama.cpp errors
 */
class LlamaException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Data class for generation parameters
 */
data class GenerationParams(
    val maxTokens: Int = LlamaInference.DEFAULT_MAX_TOKENS,
    val temperature: Float = LlamaInference.DEFAULT_TEMPERATURE,
    val topP: Float = LlamaInference.DEFAULT_TOP_P,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f
)

/**
 * Extension function to create a formatted prompt for chat models
 */
fun String.toChatPrompt(
    systemPrompt: String = "You are a helpful assistant.",
    history: List<Pair<String, String>> = emptyList()
): String {
    val sb = StringBuilder()
    
    // System prompt
    sb.append("<|system|>\n$systemPrompt</s>\n")
    
    // History
    history.forEach { (user, assistant) ->
        sb.append("<|user|>\n$user</s>\n")
        sb.append("<|assistant|>\n$assistant</s>\n")
    }
    
    // Current prompt
    sb.append("<|user|>\n$this</s>\n")
    sb.append("<|assistant|>\n")
    
    return sb.toString()
}

/**
 * Llama-style chat prompt format
 */
fun String.toLlamaChatPrompt(
    systemPrompt: String = "You are a helpful assistant.",
    history: List<Pair<String, String>> = emptyList()
): String {
    val sb = StringBuilder()
    
    // System prompt with Llama format
    sb.append("[INST] <<SYS>>\n$systemPrompt\n<</SYS>>\n\n")
    
    // First user message or history
    if (history.isNotEmpty()) {
        val (firstUser, firstAssistant) = history.first()
        sb.append("$firstUser [/INST] $firstAssistant </s>")
        
        // Remaining history
        history.drop(1).forEach { (user, assistant) ->
            sb.append("<s>[INST] $user [/INST] $assistant </s>")
        }
        
        // Current prompt
        sb.append("<s>[INST] $this [/INST]")
    } else {
        // No history, just current prompt
        sb.append("$this [/INST]")
    }
    
    return sb.toString()
}
