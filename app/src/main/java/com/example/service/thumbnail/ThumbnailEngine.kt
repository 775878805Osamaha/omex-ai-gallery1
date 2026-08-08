package com.example.service.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import com.example.core.data.local.MediaItemEntity
import com.omex.gallery.core.log.LogCategory
import com.omex.gallery.core.log.OmexLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Production-grade Thumbnail Engine with LRU Memory Cache, Disk Cache, Eviction Policy,
 * Corruption Recovery, and Structured Logging.
 */
class ThumbnailEngine(private val context: Context) {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory

    private val memoryCache = object : LruCache<Long, Bitmap>(cacheSize) {
        override fun sizeOf(key: Long, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val thumbDir: File by lazy {
        File(context.cacheDir, "omex_thumbnails").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    private val maxDiskCacheSizeBytes: Long = 200 * 1024 * 1024 // 200 MB max

    fun getBitmapFromMemory(mediaId: Long): Bitmap? {
        return memoryCache.get(mediaId)
    }

    suspend fun getOrCreateThumbnail(item: MediaItemEntity): String? = withContext(Dispatchers.IO) {
        val targetFile = File(thumbDir, "thumb_${item.id}.jpg")

        // 1. Check disk cache with corruption validation
        if (targetFile.exists()) {
            if (targetFile.length() > 0) {
                try {
                    // Quick validation check
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(targetFile.absolutePath, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        return@withContext targetFile.absolutePath
                    } else {
                        OmexLogger.w(LogCategory.SCANNER, "ThumbnailEngine", "Corrupted thumbnail found for ${item.id}, deleting...")
                        targetFile.delete()
                    }
                } catch (e: Exception) {
                    OmexLogger.e(LogCategory.ERRORS, "ThumbnailEngine", "Failed thumbnail validation for ${item.id}", e)
                    targetFile.delete()
                }
            } else {
                targetFile.delete()
            }
        }

        // 2. Perform Disk Cache Eviction if needed before writing
        performDiskEvictionIfNeeded()

        // 3. Generate thumbnail
        try {
            val uri = Uri.parse(item.uriString)
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(320, 320), null)
            } else {
                @Suppress("DEPRECATION")
                if (item.isVideo) {
                    val origId = item.id - 1_000_000_000L
                    android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver, origId,
                        android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null
                    )
                } else {
                    android.provider.MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver, item.id,
                        android.provider.MediaStore.Images.Thumbnails.MINI_KIND, null
                    )
                }
            }

            bitmap?.let {
                memoryCache.put(item.id, it)

                FileOutputStream(targetFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                
                OmexLogger.d(LogCategory.SCANNER, "ThumbnailEngine", "Generated thumbnail for ${item.id}")
                return@withContext targetFile.absolutePath
            }
        } catch (e: Exception) {
            OmexLogger.e(LogCategory.ERRORS, "ThumbnailEngine", "Error generating thumbnail for media ID: ${item.id}", e)
        }

        null
    }

    private fun performDiskEvictionIfNeeded() {
        val currentSize = getThumbnailCacheSize()
        if (currentSize > maxDiskCacheSizeBytes) {
            OmexLogger.i(LogCategory.PERFORMANCE, "ThumbnailEngine", "Disk cache size ($currentSize B) exceeds max ($maxDiskCacheSizeBytes B). Evicting old files.")
            val files = thumbDir.listFiles()?.sortedBy { it.lastModified() } ?: return
            var freed = 0L
            val targetFreed = maxDiskCacheSizeBytes / 4 // Free 25% of cache space
            for (file in files) {
                val len = file.length()
                if (file.delete()) {
                    freed += len
                }
                if (freed >= targetFreed) break
            }
            OmexLogger.i(LogCategory.PERFORMANCE, "ThumbnailEngine", "Evicted $freed bytes from disk cache.")
        }
    }

    fun getThumbnailCacheSize(): Long {
        return thumbDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
}
