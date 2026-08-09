package com.omex.gallery.domain.model

import androidx.compose.runtime.Immutable
import com.omex.gallery.core.data.local.MediaItemEntity

/**
 * Domain model representing a gallery media file.
 */
@Immutable
data class MediaItem(
    val id: Long,
    val uriString: String,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val isVideo: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val dateTaken: Long = 0L,
    val dateModified: Long = 0L,
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
    val isIndexed: Boolean = false
)

fun MediaItemEntity.toDomain(): MediaItem = MediaItem(
    id = id,
    uriString = uriString,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    isVideo = isVideo,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    dateTaken = dateTaken,
    dateModified = dateModified,
    durationMs = durationMs,
    isFavorite = isFavorite,
    thumbnailPath = thumbnailPath,
    sha256Hash = sha256Hash,
    dHash = dHash,
    pHash = pHash,
    cameraMake = cameraMake,
    cameraModel = cameraModel,
    iso = iso,
    aperture = aperture,
    exposureTime = exposureTime,
    focalLength = focalLength,
    latitude = latitude,
    longitude = longitude,
    isIndexed = isIndexed
)

fun MediaItem.toEntity(): MediaItemEntity = MediaItemEntity(
    id = id,
    uriString = uriString,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    isVideo = isVideo,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    dateTaken = dateTaken,
    dateModified = dateModified,
    durationMs = durationMs,
    isFavorite = isFavorite,
    thumbnailPath = thumbnailPath,
    sha256Hash = sha256Hash,
    dHash = dHash,
    pHash = pHash,
    cameraMake = cameraMake,
    cameraModel = cameraModel,
    iso = iso,
    aperture = aperture,
    exposureTime = exposureTime,
    focalLength = focalLength,
    latitude = latitude,
    longitude = longitude,
    isIndexed = isIndexed
)
