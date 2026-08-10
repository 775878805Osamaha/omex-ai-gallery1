package com.omex.gallery.core.ai.chat

import com.omex.gallery.core.ai.genai.GenerativeModelManager
import kotlinx.coroutines.flow.Flow

class MediaPipeAiChatEngine(
    private val generativeModelManager: GenerativeModelManager
) : AiChatEngine {

    override suspend fun isAvailable(): Boolean {
        return generativeModelManager.isModelAvailable
    }

    override fun sendMessage(prompt: String): Flow<String> {
        return generativeModelManager.generateResponseStream(prompt)
    }

    override fun close() {
        generativeModelManager.closeEngine()
    }
}
