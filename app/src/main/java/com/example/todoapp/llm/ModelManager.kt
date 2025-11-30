package com.example.todoapp.llm

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * ModelManager handles downloading, storing, and managing GGUF model files
 * for on-device LLM inference using llama.cpp.
 * 
 * Models are stored in the app's external files directory:
 * /Android/data/com.example.todoapp/files/models/
 * 
 * This location doesn't require special permissions on Android 10+ (scoped storage)
 */
class ModelManager(private val context: Context) {
    
    companion object {
        // Model directory name
        private const val MODELS_DIR = "models"
        
        // Minimum free space required (in bytes) - 500MB buffer
        private const val MIN_FREE_SPACE_BYTES = 500L * 1024 * 1024
        
        // Download timeout settings
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        
        // Available models for download
        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "tinyllama-1.1b-q4",
                name = "TinyLlama 1.1B (Q4_K_M)",
                description = "Smallest model, fastest inference. Good for basic tasks.",
                sizeBytes = 668 * 1024 * 1024L, // ~668 MB
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                parameters = "1.1B"
            ),
            ModelInfo(
                id = "phi-2-q4",
                name = "Phi-2 2.7B (Q4_K_M)",
                description = "Microsoft's efficient model. Great balance of size and capability.",
                sizeBytes = 1_600 * 1024 * 1024L, // ~1.6 GB
                downloadUrl = "https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf",
                fileName = "phi-2.Q4_K_M.gguf",
                parameters = "2.7B"
            ),
            ModelInfo(
                id = "llama-3.2-1b-q4",
                name = "Llama 3.2 1B (Q4_K_M)",
                description = "Meta's latest small model. Optimized for mobile.",
                sizeBytes = 750 * 1024 * 1024L, // ~750 MB
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                parameters = "1B"
            ),
            ModelInfo(
                id = "llama-3.2-3b-q4",
                name = "Llama 3.2 3B (Q4_K_M)",
                description = "Meta's 3B model. Best quality for on-device inference.",
                sizeBytes = 2_000 * 1024 * 1024L, // ~2 GB
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                parameters = "3B"
            )
        )
    }
    
    // State flows for UI
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    private val _installedModels = MutableStateFlow<List<InstalledModel>>(emptyList())
    val installedModels: StateFlow<List<InstalledModel>> = _installedModels.asStateFlow()
    
    private val _selectedModelId = MutableStateFlow<String?>(null)
    val selectedModelId: StateFlow<String?> = _selectedModelId.asStateFlow()
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    init {
        refreshInstalledModels()
    }
    
    /**
     * Get the models directory, creating it if necessary
     */
    fun getModelsDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), MODELS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Check available free space on storage
     */
    fun getAvailableSpaceBytes(): Long {
        val dir = getModelsDirectory()
        return try {
            val stat = StatFs(dir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if there's enough space for a model
     */
    fun hasEnoughSpace(modelSizeBytes: Long): Boolean {
        return getAvailableSpaceBytes() >= (modelSizeBytes + MIN_FREE_SPACE_BYTES)
    }
    
    /**
     * Format bytes to human-readable string
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * Refresh the list of installed models
     */
    fun refreshInstalledModels() {
        val dir = getModelsDirectory()
        val installed = mutableListOf<InstalledModel>()
        
        AVAILABLE_MODELS.forEach { modelInfo ->
            val file = File(dir, modelInfo.fileName)
            if (file.exists()) {
                installed.add(
                    InstalledModel(
                        info = modelInfo,
                        file = file,
                        sizeOnDisk = file.length()
                    )
                )
            }
        }
        
        _installedModels.value = installed
        
        // Auto-select first installed model if none selected
        if (_selectedModelId.value == null && installed.isNotEmpty()) {
            _selectedModelId.value = installed.first().info.id
        }
    }
    
    /**
     * Download a model
     */
    suspend fun downloadModel(modelInfo: ModelInfo): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Check space
            if (!hasEnoughSpace(modelInfo.sizeBytes)) {
                val needed = formatSize(modelInfo.sizeBytes + MIN_FREE_SPACE_BYTES)
                val available = formatSize(getAvailableSpaceBytes())
                return@withContext Result.failure(
                    InsufficientSpaceException("Need $needed but only $available available")
                )
            }
            
            _downloadState.value = DownloadState.Downloading(
                modelId = modelInfo.id,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = modelInfo.sizeBytes
            )
            
            val request = Request.Builder()
                .url(modelInfo.downloadUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Error("Download failed: ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            
            val body = response.body ?: run {
                _downloadState.value = DownloadState.Error("Empty response")
                return@withContext Result.failure(Exception("Empty response body"))
            }
            
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: modelInfo.sizeBytes
            val file = File(getModelsDirectory(), modelInfo.fileName)
            val tempFile = File(getModelsDirectory(), "${modelInfo.fileName}.tmp")
            
            var downloadedBytes = 0L
            
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        _downloadState.value = DownloadState.Downloading(
                            modelId = modelInfo.id,
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    }
                }
            }
            
            // Rename temp file to final name
            tempFile.renameTo(file)
            
            _downloadState.value = DownloadState.Completed(modelInfo.id)
            refreshInstalledModels()
            
            // Auto-select newly downloaded model
            _selectedModelId.value = modelInfo.id
            
            Result.success(file)
            
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }
    
    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        // The actual cancellation would need to be implemented with a Job reference
        _downloadState.value = DownloadState.Idle
    }
    
    /**
     * Delete a downloaded model
     */
    fun deleteModel(modelId: String): Boolean {
        val modelInfo = AVAILABLE_MODELS.find { it.id == modelId } ?: return false
        val file = File(getModelsDirectory(), modelInfo.fileName)
        
        val deleted = if (file.exists()) {
            file.delete()
        } else {
            false
        }
        
        refreshInstalledModels()
        
        // Clear selection if deleted model was selected
        if (_selectedModelId.value == modelId) {
            _selectedModelId.value = _installedModels.value.firstOrNull()?.info?.id
        }
        
        return deleted
    }
    
    /**
     * Select a model for inference
     */
    fun selectModel(modelId: String) {
        if (_installedModels.value.any { it.info.id == modelId }) {
            _selectedModelId.value = modelId
        }
    }
    
    /**
     * Get the file path of the selected model
     */
    fun getSelectedModelPath(): String? {
        val selectedId = _selectedModelId.value ?: return null
        val installed = _installedModels.value.find { it.info.id == selectedId }
        return installed?.file?.absolutePath
    }
    
    /**
     * Get model info by ID
     */
    fun getModelInfo(modelId: String): ModelInfo? {
        return AVAILABLE_MODELS.find { it.id == modelId }
    }
    
    /**
     * Reset download state to idle
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}

/**
 * Information about an available model
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val fileName: String,
    val parameters: String
)

/**
 * Information about an installed model
 */
data class InstalledModel(
    val info: ModelInfo,
    val file: File,
    val sizeOnDisk: Long
)

/**
 * Download state sealed class for UI
 */
sealed class DownloadState {
    object Idle : DownloadState()
    
    data class Downloading(
        val modelId: String,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    
    data class Completed(val modelId: String) : DownloadState()
    
    data class Error(val message: String) : DownloadState()
}

/**
 * Exception for insufficient storage space
 */
class InsufficientSpaceException(message: String) : Exception(message)
