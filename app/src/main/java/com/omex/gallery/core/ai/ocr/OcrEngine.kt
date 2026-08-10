package com.omex.gallery.core.ai.ocr

import android.graphics.Bitmap

/**
 * Interface for modular on-device OCR engine implementations.
 */
interface OcrEngine {
    fun initialize()
    suspend fun processImage(bitmap: Bitmap): Result<OcrResult>
    fun close()
}
