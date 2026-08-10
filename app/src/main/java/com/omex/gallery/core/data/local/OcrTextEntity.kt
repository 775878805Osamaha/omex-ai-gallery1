package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing extracted on-device OCR text for gallery media.
 */
@Entity(
    tableName = "ocr_text_results",
    indices = [
        Index("mediaId"),
        Index("processingStatus")
    ]
)
data class OcrTextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaId: Long,
    val extractedText: String,
    val language: String? = null,
    val processingStatus: String = "COMPLETED", // "PENDING", "COMPLETED", "FAILED", "EMPTY"
    val modelVersion: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
