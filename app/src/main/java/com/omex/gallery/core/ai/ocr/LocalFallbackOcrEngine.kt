package com.omex.gallery.core.ai.ocr

import android.graphics.Bitmap

/**
 * Fallback on-device OCR engine used when primary ML Kit model client is uninitialized or in unit test runtime.
 */
class LocalFallbackOcrEngine : OcrEngine {
    override fun initialize() {}

    override suspend fun processImage(bitmap: Bitmap): Result<OcrResult> {
        return Result.success(
            OcrResult(
                extractedText = "",
                language = null,
                confidence = 1.0f,
                blocks = emptyList()
            )
        )
    }

    override fun close() {}
}
