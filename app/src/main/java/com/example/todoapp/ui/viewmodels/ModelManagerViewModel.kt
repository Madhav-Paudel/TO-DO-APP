package com.example.todoapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.llm.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Model Manager screen
 * Handles model downloading, deletion, and selection
 */
class ModelManagerViewModel(application: Application) : AndroidViewModel(application) {
    
    val modelManager = ModelManager(application)
    
    // Llama inference instance
    private val llamaInference = LlamaInference()
    
    // UI State
    private val _isNativeAvailable = MutableStateFlow(false)
    val isNativeAvailable: StateFlow<Boolean> = _isNativeAvailable.asStateFlow()
    
    private val _nativeVersion = MutableStateFlow("Unknown")
    val nativeVersion: StateFlow<String> = _nativeVersion.asStateFlow()
    
    private val _inferenceState = MutableStateFlow(InferenceState.UNINITIALIZED)
    val inferenceState: StateFlow<InferenceState> = _inferenceState.asStateFlow()
    
    init {
        checkNativeAvailability()
        
        // Observe inference state
        viewModelScope.launch {
            llamaInference.state.collect { state ->
                _inferenceState.value = state
            }
        }
    }
    
    private fun checkNativeAvailability() {
        viewModelScope.launch {
            _isNativeAvailable.value = LlamaInference.isAvailable()
            _nativeVersion.value = LlamaInference.getVersion()
        }
    }
    
    /**
     * Download a model
     */
    fun downloadModel(modelInfo: ModelInfo) {
        viewModelScope.launch {
            modelManager.downloadModel(modelInfo)
        }
    }
    
    /**
     * Delete a model
     */
    fun deleteModel(modelId: String) {
        // Unload if currently loaded
        val selectedPath = modelManager.getSelectedModelPath()
        val modelInfo = modelManager.getModelInfo(modelId)
        if (modelInfo != null && selectedPath?.contains(modelInfo.fileName) == true) {
            llamaInference.unloadModel()
        }
        
        modelManager.deleteModel(modelId)
    }
    
    /**
     * Select and load a model for inference
     */
    fun selectAndLoadModel(modelId: String, threads: Int = 4, contextSize: Int = 2048) {
        viewModelScope.launch {
            modelManager.selectModel(modelId)
            
            val modelPath = modelManager.getSelectedModelPath()
            if (modelPath != null) {
                llamaInference.loadModel(modelPath, threads, contextSize)
            }
        }
    }
    
    /**
     * Generate text using the loaded model
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = LlamaInference.DEFAULT_MAX_TOKENS,
        temperature: Float = LlamaInference.DEFAULT_TEMPERATURE
    ): Result<String> {
        return llamaInference.generate(prompt, maxTokens, temperature)
    }
    
    /**
     * Generate text with streaming
     */
    suspend fun generateStreaming(
        prompt: String,
        maxTokens: Int = LlamaInference.DEFAULT_MAX_TOKENS,
        temperature: Float = LlamaInference.DEFAULT_TEMPERATURE,
        onToken: (String) -> Unit
    ): Result<String> {
        return llamaInference.generateStreaming(prompt, maxTokens, temperature, onToken)
    }
    
    /**
     * Cancel download
     */
    fun cancelDownload() {
        modelManager.cancelDownload()
    }
    
    /**
     * Unload current model
     */
    fun unloadModel() {
        llamaInference.unloadModel()
    }
    
    override fun onCleared() {
        super.onCleared()
        llamaInference.cleanup()
    }
}
