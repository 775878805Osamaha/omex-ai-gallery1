package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_metadata")
data class ImageMetadataEntity(
    @PrimaryKey val mediaId: Long,
    val sha256Hash: String,
    val aHash: Long,
    val dHash: Long,
    val pHash: Long,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val iso: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
