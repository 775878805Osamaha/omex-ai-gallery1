package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_embeddings",
    indices = [
        Index("mediaId")
    ]
)
data class FaceEmbeddingEntity(
    @PrimaryKey val faceId: Long,
    val mediaId: Long,
    val vectorJson: String,
    val dimension: Int = 512
)
