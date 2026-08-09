package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "image_classifications",
    indices = [
        Index("mediaId"),
        Index("category")
    ]
)
data class ImageClassificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val classId: Int,
    val label: String,
    val category: String,
    val confidence: Float
)
