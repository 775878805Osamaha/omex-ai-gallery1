package com.omex.gallery.core.di

import android.content.Context
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.repository.MediaRepositoryImpl
import com.omex.gallery.domain.model.MediaRepository

/**
 * Single-responsibility Dependency Injection Container providing app-level dependencies.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(
            mediaDao = database.mediaDao(),
            aiDao = database.aiDao(),
            context = context
        )
    }
}
