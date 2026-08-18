package com.omex.gallery
 
import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import kotlinx.coroutines.Dispatchers

/**
 * Custom Application class setting up optimized global Coil ImageLoader
 * with memory caching, disk caching, and background decoding pipelines.
 */
class OmexApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("omex_coil_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128 MB disk cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .dispatcher(Dispatchers.IO)
            .allowHardware(true)
            .build()
    }
}
