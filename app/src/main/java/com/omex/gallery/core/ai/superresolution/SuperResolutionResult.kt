package com.omex.gallery.core.ai.superresolution

import android.graphics.Bitmap

/**
 * Super resolution upscale scale factor.
 */
enum class UpscaleScale {
    X2,
    X4
}

/**
 * Super resolution engine runtime parameters.
 */
data class SuperResolutionConfig(
    val scale: UpscaleScale = UpscaleScale.X2,
    val enableTileProcessing: Boolean = true,
    val tileSize: Int = 256
)

/**
 * Result of Real-ESRGAN image enhancement execution.
 */
data class SuperResolutionResult(
    val enhancedBitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int,
    val enhancedWidth: Int,
    val enhancedHeight: Int,
    val durationMs: Long
)
