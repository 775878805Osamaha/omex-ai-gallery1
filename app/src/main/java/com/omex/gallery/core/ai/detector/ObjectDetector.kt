package com.omex.gallery.core.ai.detector

import android.graphics.Bitmap

/**
 * Interface contract for YOLOv8 Nano object detection engine.
 */
interface ObjectDetector {
    /**
     * Initializes YOLOv8 model instance and label map.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Performs object detection and Non-Maximum Suppression (NMS) filtering.
     */
    suspend fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.25f,
        iouThreshold: Float = 0.45f
    ): Result<DetectionResult>

    /**
     * Releases detection engine interpreter.
     */
    fun close()
}
