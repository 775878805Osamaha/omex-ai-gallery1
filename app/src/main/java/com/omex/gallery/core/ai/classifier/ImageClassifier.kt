package com.omex.gallery.core.ai.classifier

import android.graphics.Bitmap

/**
 * Interface contract for MobileNetV3 image classification.
 */
interface ImageClassifier {
    /**
     * Initializes classification weights and label mapping dictionary.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Classifies visual content of a bitmap, returning top-K predicted categories.
     */
    suspend fun classifyImage(
        bitmap: Bitmap,
        topK: Int = 5,
        threshold: Float = 0.15f
    ): Result<List<ClassificationResult>>

    /**
     * Releases model interpreter resources.
     */
    fun close()
}
