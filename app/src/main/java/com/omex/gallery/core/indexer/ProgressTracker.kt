package com.omex.gallery.core.indexer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Snapshot model representing indexing progress.
 */
data class IndexingProgressState(
    val isIndexing: Boolean = false,
    val isPaused: Boolean = false,
    val currentFileName: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val progressFraction: Float = 0f,
    val estimatedTimeRemainingMs: Long = 0L,
    val statusMessage: String = "Idle"
)

/**
 * Thread-safe reactive state tracker for long-running batch indexing.
 */
class ProgressTracker {

    private val _progressState = MutableStateFlow(IndexingProgressState())
    val progressState: StateFlow<IndexingProgressState> = _progressState.asStateFlow()

    private var startTimeMs: Long = 0L
    private val lastUpdateTimestamp = AtomicLong(0L)

    /**
     * Resets state and initializes tracker for a new indexing pass.
     */
    fun startTracking(totalItems: Int) {
        startTimeMs = System.currentTimeMillis()
        lastUpdateTimestamp.set(startTimeMs)

        _progressState.value = IndexingProgressState(
            isIndexing = true,
            isPaused = false,
            currentFileName = "Preparing indexer...",
            processedCount = 0,
            totalCount = totalItems,
            progressFraction = 0f,
            estimatedTimeRemainingMs = 0L,
            statusMessage = "Starting indexing pass for $totalItems items"
        )
    }

    /**
     * Updates tracker with current item progress.
     */
    fun updateProgress(processed: Int, currentFile: String) {
        val total = _progressState.value.totalCount
        val safeTotal = if (total <= 0) 1 else total
        val fraction = (processed.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)

        val now = System.currentTimeMillis()
        val elapsedMs = now - startTimeMs

        val etaMs = if (processed > 5 && elapsedMs > 500) {
            val msPerItem = elapsedMs.toDouble() / processed.toDouble()
            val remainingItems = safeTotal - processed
            (remainingItems * msPerItem).toLong()
        } else {
            0L
        }

        _progressState.value = _progressState.value.copy(
            isIndexing = true,
            currentFileName = currentFile,
            processedCount = processed,
            progressFraction = fraction,
            estimatedTimeRemainingMs = etaMs,
            statusMessage = "Indexing $processed / $safeTotal: $currentFile"
        )
    }

    /**
     * Pauses status state.
     */
    fun pause() {
        _progressState.value = _progressState.value.copy(
            isPaused = true,
            statusMessage = "Indexing paused"
        )
    }

    /**
     * Resumes status state.
     */
    fun resume() {
        _progressState.value = _progressState.value.copy(
            isPaused = false,
            statusMessage = "Indexing resumed"
        )
    }

    /**
     * Marks indexing pass as complete.
     */
    fun complete(summary: String = "Indexing completed successfully") {
        _progressState.value = IndexingProgressState(
            isIndexing = false,
            isPaused = false,
            currentFileName = "",
            processedCount = _progressState.value.totalCount,
            totalCount = _progressState.value.totalCount,
            progressFraction = 1f,
            estimatedTimeRemainingMs = 0L,
            statusMessage = summary
        )
    }

    /**
     * Marks indexing pass as failed with error.
     */
    fun error(errorMessage: String) {
        _progressState.value = _progressState.value.copy(
            isIndexing = false,
            statusMessage = "Error: $errorMessage"
        )
    }
}
