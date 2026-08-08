package com.omex.gallery.core.ai.pipeline

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * Normalization type for image pixel RGB channels.
 */
enum class NormalizationType {
    ZERO_TO_ONE,        // [0.0, 1.0]
    MINUS_ONE_TO_ONE,   // [-1.0, 1.0]
    IMAGENET_MEAN_STD,  // (x - mean) / std using ImageNet stats
    NONE                // Raw [0, 255] byte or float
}

/**
 * Settings describing target dimensions and pixel transforms for model input tensors.
 */
data class PreprocessOptions(
    val targetWidth: Int,
    val targetHeight: Int,
    val normalization: NormalizationType = NormalizationType.ZERO_TO_ONE,
    val isBgrOrder: Boolean = false,
    val keepAspectRatio: Boolean = true
)

/**
 * Interface for converting Android Bitmaps into preprocessed ByteBuffers formatted for model input.
 */
interface ImagePreprocessor {
    /**
     * Resizes, crops, converts color channels, and normalizes input bitmap into TFLite tensor buffer.
     */
    fun preprocess(bitmap: Bitmap, options: PreprocessOptions): ByteBuffer

    /**
     * Preprocesses a cropped sub-region of a source bitmap.
     */
    fun preprocessCrop(
        bitmap: Bitmap,
        cropLeft: Int,
        cropTop: Int,
        cropRight: Int,
        cropBottom: Int,
        options: PreprocessOptions
    ): ByteBuffer
}
