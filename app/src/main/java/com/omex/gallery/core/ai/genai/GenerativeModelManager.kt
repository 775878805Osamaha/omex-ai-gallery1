package com.omex.gallery.core.ai.genai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

class GenerativeModelManager(
    private val context: Context,
    private val modelRepository: GenerativeModelRepository
) {

    private var llmInference: LlmInference? = null

    val isModelAvailable: Boolean
        get() = modelRepository.getModelFile() != null

    @Synchronized
    fun initializeEngine(): Boolean {
        if (llmInference != null) return true

        val modelFile = modelRepository.getModelFile() ?: return false
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done -> }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            llmInference = null
            false
        }
    }

    fun generateResponseStream(prompt: String): Flow<String> = callbackFlow {
        if (llmInference == null) {
            val initialized = initializeEngine()
            if (!initialized || llmInference == null) {
                trySend("خطأ: تعذر تحميل نموذج الذكاء الاصطناعي المحلي.")
                close()
                return@callbackFlow
            }
        }

        var accumulated = ""
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelRepository.getModelFile()?.absolutePath ?: "")
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    accumulated += partialResult
                    trySend(accumulated)
                    if (done) {
                        close()
                    }
                }
                .build()

            val tempInference = LlmInference.createFromOptions(context, options)
            tempInference.generateResponseAsync(prompt)
        } catch (e: Exception) {
            trySend("خطأ أثناء معالجة المحادثة: ${e.localizedMessage}")
            close()
        }

        awaitClose { }
    }

    @Synchronized
    fun closeEngine() {
        try {
            llmInference = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
