package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detected_faces",
    indices = [
        Index("mediaId"),
        Index("clusterId")
    ]
)
data class DetectedFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val clusterId: String? = null
)
