package com.example.service.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.core.data.local.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for scanning local device MediaStore for images and videos.
 */
class GalleryScanner(private val context: Context) {

    suspend fun scanMediaStore(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItemEntity>()

        // 1. Scan Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA
        )

        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            imageUri,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateModColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "IMG_$id.jpg"
                val mime = cursor.getString(mimeColumn) ?: "image/jpeg"
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                var dateTaken = cursor.getLong(dateTakenColumn)
                val dateMod = cursor.getLong(dateModColumn) * 1000
                val dataPath = cursor.getString(dataColumn) ?: ""

                if (dateTaken <= 0) {
                    dateTaken = if (dateMod > 0) dateMod else System.currentTimeMillis()
                }

                val contentUri = ContentUris.withAppendedId(imageUri, id)

                mediaList.add(
                    MediaItemEntity(
                        id = id,
                        uriString = contentUri.toString(),
                        filePath = dataPath,
                        fileName = name,
                        mimeType = mime,
                        isVideo = false,
                        width = width,
                        height = height,
                        sizeBytes = size,
                        dateTaken = dateTaken,
                        dateModified = dateMod,
                        durationMs = 0L,
                        isFavorite = false,
                        thumbnailPath = null,
                        isIndexed = false
                    )
                )
            }
        }

        // 2. Scan Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            videoUri,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val dateModColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn) + 1_000_000_000L // Offset video IDs to avoid collision with image IDs
                val name = cursor.getString(nameColumn) ?: "VID_$id.mp4"
                val mime = cursor.getString(mimeColumn) ?: "video/mp4"
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                var dateTaken = cursor.getLong(dateTakenColumn)
                val dateMod = cursor.getLong(dateModColumn) * 1000
                val duration = cursor.getLong(durationColumn)
                val dataPath = cursor.getString(dataColumn) ?: ""

                if (dateTaken <= 0) {
                    dateTaken = if (dateMod > 0) dateMod else System.currentTimeMillis()
                }

                val originalVideoId = id - 1_000_000_000L
                val contentUri = ContentUris.withAppendedId(videoUri, originalVideoId)

                mediaList.add(
                    MediaItemEntity(
                        id = id,
                        uriString = contentUri.toString(),
                        filePath = dataPath,
                        fileName = name,
                        mimeType = mime,
                        isVideo = true,
                        width = width,
                        height = height,
                        sizeBytes = size,
                        dateTaken = dateTaken,
                        dateModified = dateMod,
                        durationMs = duration,
                        isFavorite = false,
                        thumbnailPath = null,
                        isIndexed = false
                    )
                )
            }
        }

        mediaList.sortedByDescending { it.dateTaken }
    }
}
