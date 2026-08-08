package com.example.core.di

import android.content.Context
import com.example.core.data.local.AppDatabase
import com.example.core.data.repository.MediaRepositoryImpl
import com.example.domain.model.MediaRepository
import com.example.service.indexer.MetadataIndexer
import com.example.service.scanner.GalleryScanner
import com.example.service.thumbnail.ThumbnailEngine

/**
 * Single-responsibility Dependency Injection Container providing app-level dependencies.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val galleryScanner: GalleryScanner by lazy {
        GalleryScanner(context)
    }

    val thumbnailEngine: ThumbnailEngine by lazy {
        ThumbnailEngine(context)
    }

    val metadataIndexer: MetadataIndexer by lazy {
        MetadataIndexer(context)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(
            mediaDao = database.mediaDao(),
            aiDao = database.aiDao(),
            galleryScanner = galleryScanner,
            thumbnailEngine = thumbnailEngine,
            metadataIndexer = metadataIndexer
        )
    }
}
