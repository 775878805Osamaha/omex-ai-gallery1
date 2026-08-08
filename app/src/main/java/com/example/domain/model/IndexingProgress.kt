package com.example.domain.model

/**
 * State representing scanning, thumbnail generation, and metadata indexing progress.
 */
data class IndexingProgress(
    val status: IndexingStatus = IndexingStatus.IDLE,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val currentFileName: String = "",
    val message: String = ""
)

enum class IndexingStatus {
    IDLE,
    SCANNING,
    GENERATING_THUMBNAILS,
    INDEXING_EXIF,
    COMPLETED,
    ERROR
}
