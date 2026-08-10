package com.omex.gallery.core.ai.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalFallbackAiChatEngine : AiChatEngine {

    override suspend fun isAvailable(): Boolean = false

    override fun sendMessage(prompt: String): Flow<String> = flow {
        emit("محادثة الذكاء الاصطناعي المحلية غير متوفرة لعدم وجود ملف نموذج Gemma 2B.")
    }

    override fun close() { }
}
