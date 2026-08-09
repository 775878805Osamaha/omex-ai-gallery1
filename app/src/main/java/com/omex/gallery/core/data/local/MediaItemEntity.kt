package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing local gallery media indexed in OMEX AI Gallery.
 */
@Entity(
    tableName = "media_items",
    indices = [
        Index("dateTaken"),
        Index("dateModified"),
        Index("isFavorite"),
        Index("isVideo"),
        Index("isIndexed"),
        Index("isAiProcessed"),
        Index("fileName")
    ]
)
data class MediaItemEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null,
    val sha256Hash: String? = null,
    val dHash: Long = 0L,
    val pHash: Long = 0L,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val iso: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isIndexed: Boolean = false,
    val isAiProcessed: Boolean = false,
    val indexedTimestamp: Long = System.currentTimeMillis()
)
