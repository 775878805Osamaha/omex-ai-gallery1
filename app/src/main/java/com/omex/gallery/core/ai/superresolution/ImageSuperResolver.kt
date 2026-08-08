package com.omex.gallery.core.ai.superresolution

import android.graphics.Bitmap

/**
 * Interface contract for Real-ESRGAN super resolution image enhancement.
 */
interface ImageSuperResolver {
    /**
     * Initializes Real-ESRGAN super resolution TFLite engine.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Upscales input low-resolution bitmap to high-resolution detail.
     */
    suspend fun enhanceImage(
        bitmap: Bitmap,
        config: SuperResolutionConfig
    ): Result<SuperResolutionResult>

    /**
     * Releases model interpreter context.
     */
    fun close()
}
