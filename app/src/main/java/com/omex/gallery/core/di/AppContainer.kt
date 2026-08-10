package com.omex.gallery.core.di

import android.content.Context
import com.omex.gallery.core.ai.chat.AiChatEngine
import com.omex.gallery.core.ai.chat.MediaPipeAiChatEngine
import com.omex.gallery.core.ai.genai.GenerativeModelManager
import com.omex.gallery.core.ai.genai.GenerativeModelRepository
import com.omex.gallery.core.ai.multimodal.AskImageEngine
import com.omex.gallery.core.ai.multimodal.LiteRtAskImageEngine
import com.omex.gallery.core.ai.multimodal.MultimodalModelManager
import com.omex.gallery.core.ai.multimodal.MultimodalModelRepository
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.local.ChatDao
import com.omex.gallery.core.data.repository.MediaRepositoryImpl
import com.omex.gallery.core.search.SearchHistoryRepository
import com.omex.gallery.domain.model.MediaRepository

/**
 * Single-responsibility Dependency Injection Container providing app-level dependencies.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val chatDao: ChatDao by lazy {
        database.chatDao()
    }

    val searchHistoryRepository: SearchHistoryRepository by lazy {
        SearchHistoryRepository(context)
    }

    val generativeModelRepository: GenerativeModelRepository by lazy {
        GenerativeModelRepository(context)
    }

    val generativeModelManager: GenerativeModelManager by lazy {
        GenerativeModelManager(context, generativeModelRepository)
    }

    val aiChatEngine: AiChatEngine by lazy {
        MediaPipeAiChatEngine(generativeModelManager)
    }

    val multimodalModelRepository: MultimodalModelRepository by lazy {
        MultimodalModelRepository(context)
    }

    val multimodalModelManager: MultimodalModelManager by lazy {
        MultimodalModelManager(context, multimodalModelRepository, generativeModelManager)
    }

    val askImageEngine: AskImageEngine by lazy {
        LiteRtAskImageEngine(multimodalModelManager, multimodalModelRepository)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(
            mediaDao = database.mediaDao(),
            aiDao = database.aiDao(),
            context = context
        )
    }
}
