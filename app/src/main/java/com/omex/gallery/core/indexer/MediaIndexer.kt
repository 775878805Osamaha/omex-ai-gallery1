package com.omex.gallery.core.indexer

import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Orchestrator engine executing end-to-end scanning, EXIF extraction, hashing,
 * thumbnail generation, and batch Room database persistence.
 */
class MediaIndexer(
    private val scanner: MediaScanner,
    private val metadataExtractor: MetadataExtractor,
    private val hashGenerator: HashGenerator,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val repository: MediaRepository,
    private val progressTracker: ProgressTracker = ProgressTracker()
) {

    val progressState: StateFlow<IndexingProgressState> = progressTracker.progressState

    /**
     * Executes complete incremental indexing pass across all images and videos.
     *
     * @param chunkSize Batch chunk size for Room database transactions (default 100 items)
     */
    suspend fun executeIndexingPass(chunkSize: Int = 100): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch existing media records from database for differential comparison
            val existingList = repository.getAllMediaItems()
            val existingMap = existingList.associate { it.id to it.dateModified }

            // 2. Perform incremental MediaStore scan
            val scanResult = scanner.scanIncrementally(existingMap)

            // 3. Purge deleted records from database
            if (scanResult.deletedMediaIds.isNotEmpty()) {
                scanResult.deletedMediaIds.forEach { id ->
                    repository.deleteMediaItem(id)
                }
            }

            val itemsToProcess = scanResult.newOrUpdatedMedia
            if (itemsToProcess.isEmpty()) {
                progressTracker.complete("Index up to date. ${scanResult.totalScannedCount} items scanned.")
                return@withContext Result.success(0)
            }

            // Immediately persist lightweight records to Room so UI receives them
            val lightweightItems = itemsToProcess.map { raw ->
                MediaItem(
                    id = raw.id,
                    uriString = raw.contentUri.toString(),
                    filePath = raw.filePath,
                    fileName = raw.displayName,
                    mimeType = raw.mimeType,
                    isVideo = raw.isVideo,
                    width = raw.width,
                    height = raw.height,
                    sizeBytes = raw.sizeBytes,
                    dateTaken = raw.dateTaken,
                    dateModified = raw.dateModified,
                    durationMs = raw.durationMs,
                    isFavorite = false,
                    thumbnailPath = raw.contentUri.toString(),
                    isIndexed = false
                )
            }
            repository.insertMediaItems(lightweightItems)

            progressTracker.startTracking(itemsToProcess.size)
            var processedCount = 0

            // 4. Background enrichment in memory-safe batch chunks
            itemsToProcess.chunked(chunkSize).forEach { chunk ->
                val processedChunk = mutableListOf<MediaItem>()

                for (raw in chunk) {
                    coroutineContext.ensureActive() // Check for cancellation

                    progressTracker.updateProgress(processedCount, raw.displayName)

                    // Extract EXIF / video metadata
                    val exif = if (!raw.isVideo) {
                        metadataExtractor.extractImageExif(raw.contentUri)
                    } else {
                        ExifMetadata()
                    }

                    // Compute SHA-256 and perceptual visual hashes
                    val hashes = if (!raw.isVideo) {
                        hashGenerator.generateHashes(raw.contentUri)
                    } else {
                        MediaHashes(sha256 = "", dHash = 0L, pHash = 0L)
                    }

                    // Generate disk thumbnail
                    val thumbPath = thumbnailGenerator.getOrCreateThumbnail(
                        mediaId = raw.id,
                        contentUri = raw.contentUri,
                        isVideo = raw.isVideo
                    )

                    val mediaItem = MediaItem(
                        id = raw.id,
                        uriString = raw.contentUri.toString(),
                        filePath = raw.filePath,
                        fileName = raw.displayName,
                        mimeType = raw.mimeType,
                        isVideo = raw.isVideo,
                        width = raw.width,
                        height = raw.height,
                        sizeBytes = raw.sizeBytes,
                        dateTaken = raw.dateTaken,
                        dateModified = raw.dateModified,
                        durationMs = raw.durationMs,
                        isFavorite = false,
                        thumbnailPath = thumbPath.ifEmpty { raw.contentUri.toString() },
                        sha256Hash = hashes.sha256,
                        dHash = hashes.dHash,
                        pHash = hashes.pHash,
                        cameraMake = exif.cameraMake,
                        cameraModel = exif.cameraModel,
                        iso = exif.iso,
                        aperture = exif.aperture,
                        exposureTime = exif.exposureTime,
                        focalLength = exif.focalLength,
                        latitude = exif.latitude,
                        longitude = exif.longitude,
                        isIndexed = true
                    )

                    processedChunk.add(mediaItem)
                    processedCount++
                }

                // Batch insert into Room database
                repository.insertMediaItems(processedChunk)
            }

            progressTracker.complete("Successfully indexed $processedCount media items.")
            Result.success(processedCount)
        } catch (e: Exception) {
            progressTracker.error(e.message ?: "Indexing error occurred")
            Result.failure(e)
        }
    }
}
