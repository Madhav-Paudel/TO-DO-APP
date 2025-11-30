/**
 * llama_jni.cpp - JNI wrapper for llama.cpp on-device inference
 * 
 * This file provides the native interface between Kotlin/Java and llama.cpp.
 * Currently implements stub functions that return placeholder values until
 * the full llama.cpp library is integrated.
 * 
 * To complete integration:
 * 1. Add llama.cpp as a submodule or place prebuilt libs in jniLibs/
 * 2. Uncomment the llama.cpp includes and linking in CMakeLists.txt
 * 3. Replace stub implementations with actual llama.cpp calls
 */

#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstdlib>
#include <ctime>
#include <unordered_map>
#include <mutex>

// Logging macros for Android logcat
#define LOG_TAG "LlamaInference"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// TODO: Uncomment when llama.cpp is integrated
// #include "llama.h"
// #include "common.h"

// ============================================================================
// Model Context Management
// ============================================================================

// Simulated model context for stub implementation
struct ModelContext {
    std::string modelPath;
    bool isLoaded;
    int contextSize;
    int numThreads;
    
    ModelContext(const std::string& path) 
        : modelPath(path), isLoaded(true), contextSize(2048), numThreads(4) {}
};

// Global context storage with thread-safety
static std::unordered_map<jlong, ModelContext*> g_contexts;
static std::mutex g_contexts_mutex;
static jlong g_next_handle = 1;

// Helper to generate unique handle
static jlong allocateHandle() {
    return g_next_handle++;
}

// ============================================================================
// LlamaNative JNI Functions (Primary Interface)
// ============================================================================

extern "C" {

/**
 * Initialize a model and return a context handle
 * 
 * @param env JNI environment
 * @param clazz Java class reference
 * @param modelPath Path to the .gguf model file
 * @return Context handle (jlong), 0 if failed
 */
JNIEXPORT jlong JNICALL
Java_com_example_todoapp_llm_LlamaNative_initModel(
        JNIEnv* env,
        jclass clazz,
        jstring modelPath) {
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("LlamaNative.initModel called with path: %s", path);
    
    std::string pathStr(path);
    env->ReleaseStringUTFChars(modelPath, path);
    
    // TODO: Replace with actual llama.cpp model loading
    /*
    llama_model_params model_params = llama_model_default_params();
    llama_model* model = llama_load_model_from_file(pathStr.c_str(), model_params);
    if (model == nullptr) {
        LOGE("Failed to load model from: %s", pathStr.c_str());
        return 0;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = 4;
    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    */
    
    // Stub implementation: create a simulated context
    ModelContext* ctx = new ModelContext(pathStr);
    
    std::lock_guard<std::mutex> lock(g_contexts_mutex);
    jlong handle = allocateHandle();
    g_contexts[handle] = ctx;
    
    LOGI("Model initialized with handle: %lld", (long long)handle);
    return handle;
}

/**
 * Generate text from a prompt
 * 
 * @param env JNI environment
 * @param clazz Java class reference
 * @param ctxPtr Context handle from initModel
 * @param prompt Input prompt text
 * @param maxTokens Maximum tokens to generate
 * @return Generated text (JSON in production, stub JSON for now)
 */
JNIEXPORT jstring JNICALL
Java_com_example_todoapp_llm_LlamaNative_generate(
        JNIEnv* env,
        jclass clazz,
        jlong ctxPtr,
        jstring prompt,
        jint maxTokens) {
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("LlamaNative.generate called - handle: %lld, maxTokens: %d", (long long)ctxPtr, maxTokens);
    LOGD("Prompt: %.100s...", promptStr);
    
    std::string promptText(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    // Check if context exists
    {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        if (g_contexts.find(ctxPtr) == g_contexts.end()) {
            LOGE("Invalid context handle: %lld", (long long)ctxPtr);
            return env->NewStringUTF("{\"action\":\"reply\",\"message\":\"Error: Model not loaded\",\"data\":{}}");
        }
    }
    
    // TODO: Replace with actual llama.cpp generation
    /*
    std::vector<llama_token> tokens = llama_tokenize(ctx, promptText, true);
    std::string result;
    // ... actual generation loop ...
    return env->NewStringUTF(result.c_str());
    */
    
    // Stub implementation: analyze prompt and return appropriate JSON response
    std::string response;
    std::string lowerPrompt = promptText;
    for (auto& c : lowerPrompt) c = tolower(c);
    
    // Detect intent from prompt and return structured JSON
    if (lowerPrompt.find("create") != std::string::npos && 
        lowerPrompt.find("goal") != std::string::npos) {
        // Extract goal name if present (simple heuristic)
        std::string goalName = "New Goal";
        size_t quoteStart = promptText.find('"');
        size_t quoteEnd = promptText.find('"', quoteStart + 1);
        if (quoteStart != std::string::npos && quoteEnd != std::string::npos) {
            goalName = promptText.substr(quoteStart + 1, quoteEnd - quoteStart - 1);
        }
        
        response = "{\"action\":\"create_goal\",\"message\":\"I'll create a goal for " + goalName + 
                   "\",\"data\":{\"goalTitle\":\"" + goalName + 
                   "\",\"durationMonths\":3,\"dailyMinutes\":30}}";
    }
    else if (lowerPrompt.find("add") != std::string::npos && 
             lowerPrompt.find("task") != std::string::npos) {
        std::string taskName = "New Task";
        size_t quoteStart = promptText.find('"');
        size_t quoteEnd = promptText.find('"', quoteStart + 1);
        if (quoteStart != std::string::npos && quoteEnd != std::string::npos) {
            taskName = promptText.substr(quoteStart + 1, quoteEnd - quoteStart - 1);
        }
        
        response = "{\"action\":\"create_task\",\"message\":\"I'll add the task: " + taskName + 
                   "\",\"data\":{\"taskTitle\":\"" + taskName + 
                   "\",\"dueDate\":\"today\",\"minutes\":30}}";
    }
    else if (lowerPrompt.find("list") != std::string::npos || 
             lowerPrompt.find("show") != std::string::npos) {
        response = "{\"action\":\"reply\",\"message\":\"Here are your current items. You can ask me to create goals or add tasks!\",\"data\":{}}";
    }
    else if (lowerPrompt.find("help") != std::string::npos) {
        response = "{\"action\":\"reply\",\"message\":\"I can help you manage goals and tasks! Try saying: 'Create a goal to learn Python' or 'Add task review notes tomorrow'\",\"data\":{}}";
    }
    else if (lowerPrompt.find("complete") != std::string::npos || 
             lowerPrompt.find("done") != std::string::npos ||
             lowerPrompt.find("finish") != std::string::npos) {
        std::string taskName = "task";
        size_t quoteStart = promptText.find('"');
        size_t quoteEnd = promptText.find('"', quoteStart + 1);
        if (quoteStart != std::string::npos && quoteEnd != std::string::npos) {
            taskName = promptText.substr(quoteStart + 1, quoteEnd - quoteStart - 1);
        }
        
        response = "{\"action\":\"complete_task\",\"message\":\"Great job! I'll mark that as complete.\",\"data\":{\"taskTitle\":\"" + taskName + "\"}}";
    }
    else if (lowerPrompt.find("delete") != std::string::npos || 
             lowerPrompt.find("remove") != std::string::npos) {
        response = "{\"action\":\"reply\",\"message\":\"To delete an item, please specify exactly which goal or task you want to remove.\",\"data\":{}}";
    }
    else if (lowerPrompt.find("progress") != std::string::npos || 
             lowerPrompt.find("how am i") != std::string::npos ||
             lowerPrompt.find("status") != std::string::npos) {
        response = "{\"action\":\"show_progress\",\"message\":\"Let me show you your progress summary!\",\"data\":{}}";
    }
    else {
        // Default conversational reply
        response = "{\"action\":\"reply\",\"message\":\"I'm your local AI assistant running on-device! I can help you create goals, add tasks, and track your progress. What would you like to do?\",\"data\":{}}";
    }
    
    LOGI("Generated response: %s", response.c_str());
    return env->NewStringUTF(response.c_str());
}

/**
 * Free model resources
 * 
 * @param env JNI environment
 * @param clazz Java class reference
 * @param ctxPtr Context handle to free
 */
JNIEXPORT void JNICALL
Java_com_example_todoapp_llm_LlamaNative_freeModel(
        JNIEnv* env,
        jclass clazz,
        jlong ctxPtr) {
    
    LOGI("LlamaNative.freeModel called - handle: %lld", (long long)ctxPtr);
    
    std::lock_guard<std::mutex> lock(g_contexts_mutex);
    auto it = g_contexts.find(ctxPtr);
    if (it != g_contexts.end()) {
        delete it->second;
        g_contexts.erase(it);
        LOGI("Model context freed successfully");
    } else {
        LOGE("Invalid context handle: %lld", (long long)ctxPtr);
    }
    
    // TODO: Replace with actual llama.cpp cleanup
    /*
    llama_free(ctx);
    llama_free_model(model);
    */
}

// ============================================================================
// LlamaInference JNI Functions (Extended Interface - backward compatibility)
// ============================================================================

/**
 * Initialize the llama.cpp library
 */
JNIEXPORT jboolean JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeInit(
        JNIEnv* env,
        jobject thiz) {
    LOGI("LlamaInference.nativeInit called");
    // TODO: llama_backend_init(false);
    return JNI_TRUE;
}

/**
 * Load a GGUF model file (instance method version)
 */
JNIEXPORT jboolean JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeLoadModel(
        JNIEnv* env,
        jobject thiz,
        jstring modelPath,
        jint nThreads,
        jint nCtx) {
    
    jlong handle = Java_com_example_todoapp_llm_LlamaNative_initModel(env, nullptr, modelPath);
    return handle != 0 ? JNI_TRUE : JNI_FALSE;
}

/**
 * Generate text (instance method version)
 */
JNIEXPORT jstring JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeGenerate(
        JNIEnv* env,
        jobject thiz,
        jstring prompt,
        jint maxTokens,
        jfloat temperature,
        jfloat topP) {
    
    // Use default context (first one if exists)
    jlong handle = 0;
    {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        if (!g_contexts.empty()) {
            handle = g_contexts.begin()->first;
        }
    }
    
    if (handle == 0) {
        return env->NewStringUTF("{\"action\":\"reply\",\"message\":\"No model loaded. Please download a model first.\",\"data\":{}}");
    }
    
    return Java_com_example_todoapp_llm_LlamaNative_generate(env, nullptr, handle, prompt, maxTokens);
}

/**
 * Generate with streaming callback
 */
JNIEXPORT jstring JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeGenerateWithCallback(
        JNIEnv* env,
        jobject thiz,
        jstring prompt,
        jint maxTokens,
        jfloat temperature,
        jobject callback) {
    
    // For stub, just call regular generate
    return Java_com_example_todoapp_llm_LlamaInference_nativeGenerate(
            env, thiz, prompt, maxTokens, temperature, 0.9f);
}

/**
 * Unload model
 */
JNIEXPORT void JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeUnloadModel(
        JNIEnv* env,
        jobject thiz) {
    LOGI("LlamaInference.nativeUnloadModel called");
    
    // Free all contexts
    std::lock_guard<std::mutex> lock(g_contexts_mutex);
    for (auto& pair : g_contexts) {
        delete pair.second;
    }
    g_contexts.clear();
}

/**
 * Cleanup all resources
 */
JNIEXPORT void JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeCleanup(
        JNIEnv* env,
        jobject thiz) {
    LOGI("LlamaInference.nativeCleanup called");
    Java_com_example_todoapp_llm_LlamaInference_nativeUnloadModel(env, thiz);
    // TODO: llama_backend_free();
}

/**
 * Check if model is loaded
 */
JNIEXPORT jboolean JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeIsModelLoaded(
        JNIEnv* env,
        jobject thiz) {
    std::lock_guard<std::mutex> lock(g_contexts_mutex);
    return !g_contexts.empty() ? JNI_TRUE : JNI_FALSE;
}

/**
 * Get model info as JSON
 */
JNIEXPORT jstring JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeGetModelInfo(
        JNIEnv* env,
        jobject thiz) {
    
    std::lock_guard<std::mutex> lock(g_contexts_mutex);
    if (g_contexts.empty()) {
        return env->NewStringUTF("");
    }
    
    auto* ctx = g_contexts.begin()->second;
    std::string info = "{\"status\":\"loaded\",\"path\":\"" + ctx->modelPath + 
                       "\",\"contextSize\":" + std::to_string(ctx->contextSize) +
                       ",\"threads\":" + std::to_string(ctx->numThreads) + "}";
    return env->NewStringUTF(info.c_str());
}

/**
 * Get native library version
 */
JNIEXPORT jstring JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeGetVersion(
        JNIEnv* env,
        jclass clazz) {
    return env->NewStringUTF("llama.cpp JNI v1.0.0 (stub with JSON responses)");
}

/**
 * Check if native library is available
 */
JNIEXPORT jboolean JNICALL
Java_com_example_todoapp_llm_LlamaInference_nativeIsAvailable(
        JNIEnv* env,
        jclass clazz) {
    return JNI_TRUE;
}

} // extern "C"
