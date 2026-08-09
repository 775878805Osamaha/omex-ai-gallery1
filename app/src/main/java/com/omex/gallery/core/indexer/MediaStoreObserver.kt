package com.omex.gallery.core.indexer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.omex.gallery.core.log.LogCategory
import com.omex.gallery.core.log.OmexLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ContentObserver observing MediaStore changes for Images and Videos.
 * Debounces events and enqueues background synchronization via WorkManager without blocking the UI thread.
 */
class MediaStoreObserver(
    private val context: Context,
    private val indexScheduler: IndexScheduler = IndexScheduler(context)
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var debounceJob: Job? = null

    fun register() {
        try {
            val contentResolver = context.contentResolver
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            OmexLogger.i(LogCategory.SCANNER, "MediaStoreObserver", "Registered MediaStore ContentObserver")
        } catch (e: Exception) {
            OmexLogger.e(LogCategory.ERRORS, "MediaStoreObserver", "Failed to register ContentObserver", e)
        }
    }

    fun unregister() {
        try {
            context.contentResolver.unregisterContentObserver(this)
            debounceJob?.cancel()
            OmexLogger.i(LogCategory.SCANNER, "MediaStoreObserver", "Unregistered MediaStore ContentObserver")
        } catch (e: Exception) {
            OmexLogger.e(LogCategory.ERRORS, "MediaStoreObserver", "Failed to unregister ContentObserver", e)
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        OmexLogger.d(LogCategory.SCANNER, "MediaStoreObserver", "MediaStore change detected: $uri")

        // Debounce frequent MediaStore changes (e.g. 2000ms window)
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(2000L)
            OmexLogger.i(LogCategory.SCANNER, "MediaStoreObserver", "Debounce delay completed, enqueuing background sync")
            indexScheduler.enqueueNormalSync()
        }
    }
}
