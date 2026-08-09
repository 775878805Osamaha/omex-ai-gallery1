package com.omex.gallery.core.indexer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Fast disk-backed thumbnail engine for images and video frame keyframes.
 */
class ThumbnailGenerator(private val context: Context) {

    private val thumbnailDir: File by lazy {
        File(context.cacheDir, "omex_thumbnails").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    /**
     * Returns file path of cached thumbnail, generating it if absent.
     *
     * @param mediaId Unique media ID
     * @param contentUri Content URI of media item
     * @param isVideo True if item is a video
     * @param targetDimension Max target width/height in pixels (default 512px)
     */
    suspend fun getOrCreateThumbnail(
        mediaId: Long,
        contentUri: Uri,
        isVideo: Boolean,
        targetDimension: Int = 512
    ): String = withContext(Dispatchers.IO) {
        val destFile = File(thumbnailDir, "thumb_$mediaId.jpg")
        if (destFile.exists() && destFile.length() > 0) {
            return@withContext destFile.absolutePath
        }

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(
                    contentUri,
                    Size(targetDimension, targetDimension),
                    null
                )
            } catch (e: Exception) {
                fallbackGenerateBitmap(contentUri, isVideo, targetDimension)
            }
        } else {
            fallbackGenerateBitmap(contentUri, isVideo, targetDimension)
        }

        if (bitmap == null) return@withContext ""

        try {
            FileOutputStream(destFile).use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            ""
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun fallbackGenerateBitmap(
        contentUri: Uri,
        isVideo: Boolean,
        targetDimension: Int
    ): Bitmap? {
        return if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                retriever.getFrameAtTime(
                    1_000_000L, // 1 second into video
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } catch (e: Exception) {
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        } else {
            var inputStream: InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(contentUri) ?: return null
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                var sampleSize = 1
                while (options.outWidth / sampleSize > targetDimension || options.outHeight / sampleSize > targetDimension) {
                    sampleSize *= 2
                }

                inputStream = context.contentResolver.openInputStream(contentUri)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } catch (e: Exception) {
                null
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Returns total byte size of cached thumbnails on disk.
     */
    fun getThumbnailCacheSize(): Long {
        return thumbnailDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Clears all cached thumbnail files from disk.
     */
    fun clearCache() {
        thumbnailDir.listFiles()?.forEach { file ->
            try { file.delete() } catch (_: Exception) {}
        }
    }
}
