package com.omex.gallery.core.ai.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Modern, 100% on-device OCR Engine using ML Kit Local Text Recognition.
 * Operates completely offline with zero network connectivity or cloud API dependencies.
 */
class MlKitOcrEngine(
    private val context: Context
) : OcrEngine {

    private var textRecognizer: TextRecognizer? = null

    override fun initialize() {
        if (textRecognizer == null) {
            try {
                textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            } catch (e: Exception) {
                // Managed gracefully if native runtime or test environment differs
            }
        }
    }

    override suspend fun processImage(bitmap: Bitmap): Result<OcrResult> {
        val recognizer = textRecognizer ?: try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { textRecognizer = it }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val fullText = visionText.text
                        val blockTexts = visionText.textBlocks.map { it.text }
                        val detectedLanguage = visionText.textBlocks.firstOrNull()?.recognizedLanguage

                        val result = OcrResult(
                            extractedText = fullText,
                            language = detectedLanguage,
                            blocks = blockTexts
                        )
                        if (continuation.isActive) {
                            continuation.resume(Result.success(result))
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun close() {
        try {
            textRecognizer?.close()
            textRecognizer = null
        } catch (_: Exception) {}
    }
}
