package com.omex.gallery.core.ai.chat

import kotlinx.coroutines.flow.Flow

interface AiChatEngine {
    suspend fun isAvailable(): Boolean
    fun sendMessage(prompt: String): Flow<String>
    fun close()
}
