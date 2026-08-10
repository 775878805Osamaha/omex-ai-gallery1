package com.omex.gallery.core.ai.ocr

/**
 * Result data holder for extracted text from on-device OCR inference.
 */
data class OcrResult(
    val extractedText: String,
    val language: String? = null,
    val confidence: Float = 1.0f,
    val blocks: List<String> = emptyList()
)
