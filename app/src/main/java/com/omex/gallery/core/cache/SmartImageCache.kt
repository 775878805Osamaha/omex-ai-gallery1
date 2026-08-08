package com.omex.gallery.core.cache

import android.graphics.Bitmap

/**
 * Storage tier policy for bitmap caching.
 */
enum class CacheTier {
    MEMORY_ONLY,
    DISK_ONLY,
    HYBRID
}

/**
 * Configuration options for image caching engine.
 */
data class CacheConfig(
    val maxMemoryCacheMb: Int = 128,
    val maxDiskCacheMb: Int = 512,
    val defaultTier: CacheTier = CacheTier.HYBRID
)

/**
 * Interface contract for multi-tiered in-memory and disk bitmap caching.
 */
interface SmartImageCache {
    /**
     * Stores bitmap in cache with key identifier.
     */
    suspend fun put(key: String, bitmap: Bitmap, tier: CacheTier = CacheTier.HYBRID)

    /**
     * Retrieves cached bitmap by key if present.
     */
    suspend fun get(key: String): Bitmap?

    /**
     * Evicts specific cached item.
     */
    suspend fun remove(key: String)

    /**
     * Flushes in-memory cache allocations.
     */
    fun clearMemoryCache()

    /**
     * Flushes disk cache allocations.
     */
    suspend fun clearDiskCache()
}
