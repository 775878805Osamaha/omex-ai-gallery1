package com.omex.gallery.core.ai.faces

import android.graphics.Bitmap

/**
 * Interface for detecting faces in image files using ML Kit / TFLite Face Detector.
 */
interface FaceDetector {
    /**
     * Initializes face detector engine.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Detects face regions and landmarks in a bitmap.
     */
    suspend fun detectFaces(bitmap: Bitmap): Result<List<FaceDetectionResult>>

    /**
     * Releases detector resources.
     */
    fun close()
}
