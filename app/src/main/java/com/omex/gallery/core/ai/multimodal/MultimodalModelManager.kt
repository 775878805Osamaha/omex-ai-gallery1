package com.omex.gallery.core.ai.multimodal

import android.content.Context
import com.omex.gallery.core.ai.genai.GenerativeModelManager

class MultimodalModelManager(
    private val context: Context,
    private val modelRepository: MultimodalModelRepository,
    private val textLlmManager: GenerativeModelManager? = null
) {
    private var isEngineInitialized = false

    val isModelAvailable: Boolean
        get() = modelRepository.getModelFile() != null

    @Synchronized
    fun initializeEngine(): Boolean {
        if (isEngineInitialized) return true

        val modelFile = modelRepository.getModelFile() ?: return false
        return try {
            // Memory safety coordination: Close text LLM engine before initializing multimodal LiteRT-LM engine
            textLlmManager?.closeEngine()

            isEngineInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isEngineInitialized = false
            false
        }
    }

    @Synchronized
    fun closeEngine() {
        try {
            isEngineInitialized = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
