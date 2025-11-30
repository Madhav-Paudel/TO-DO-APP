package com.example.todoapp.llm

import android.util.Log

/**
 * LlamaNative - Kotlin bridge to native llama.cpp JNI functions
 * 
 * This object provides the primary interface for on-device LLM inference.
 * It uses a handle-based API where initModel returns a context pointer
 * that must be passed to generate and freed with freeModel.
 * 
 * Usage:
 * ```kotlin
 * val ctxPtr = LlamaNative.initModel("/path/to/model.gguf")
 * if (ctxPtr != 0L) {
 *     val response = LlamaNative.generate(ctxPtr, prompt, maxTokens = 256)
 *     // ... use response ...
 *     LlamaNative.freeModel(ctxPtr)
 * }
 * ```
 */
object LlamaNative {
    
    private const val TAG = "LlamaNative"
    
    /**
     * Flag indicating if the native library was loaded successfully
     */
    var isLibraryLoaded: Boolean = false
        private set
    
    init {
        try {
            System.loadLibrary("llamainference")
            isLibraryLoaded = true
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            Log.e(TAG, "Failed to load native library: ${e.message}")
        }
    }
    
    /**
     * Initialize a model and return a context handle
     * 
     * @param modelPath Absolute path to the .gguf model file
     * @return Context handle (Long), 0 if initialization failed
     */
    external fun initModel(modelPath: String): Long
    
    /**
     * Generate text from a prompt
     * 
     * @param ctxPtr Context handle from initModel
     * @param prompt Input prompt text (should follow JSON-output template)
     * @param maxTokens Maximum tokens to generate
     * @return Generated text (expected to be JSON)
     */
    external fun generate(ctxPtr: Long, prompt: String, maxTokens: Int): String
    
    /**
     * Free model resources
     * 
     * @param ctxPtr Context handle to free
     */
    external fun freeModel(ctxPtr: Long)
    
    /**
     * Safe wrapper for initModel that catches native errors
     */
    fun initModelSafe(modelPath: String): Result<Long> {
        return try {
            if (!isLibraryLoaded) {
                return Result.failure(NativeLibraryException("Native library not loaded"))
            }
            val handle = initModel(modelPath)
            if (handle == 0L) {
                Result.failure(ModelLoadException("Failed to load model: $modelPath"))
            } else {
                Result.success(handle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initModel: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Safe wrapper for generate that catches native errors
     */
    fun generateSafe(ctxPtr: Long, prompt: String, maxTokens: Int = 256): Result<String> {
        return try {
            if (!isLibraryLoaded) {
                return Result.failure(NativeLibraryException("Native library not loaded"))
            }
            if (ctxPtr == 0L) {
                return Result.failure(InvalidContextException("Invalid context handle"))
            }
            val response = generate(ctxPtr, prompt, maxTokens)
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error in generate: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Safe wrapper for freeModel that catches native errors
     */
    fun freeModelSafe(ctxPtr: Long): Result<Unit> {
        return try {
            if (!isLibraryLoaded) {
                return Result.failure(NativeLibraryException("Native library not loaded"))
            }
            if (ctxPtr != 0L) {
                freeModel(ctxPtr)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in freeModel: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Exception thrown when native library is not loaded
 */
class NativeLibraryException(message: String) : Exception(message)

/**
 * Exception thrown when model fails to load
 */
class ModelLoadException(message: String) : Exception(message)

/**
 * Exception thrown when using an invalid context handle
 */
class InvalidContextException(message: String) : Exception(message)
