package com.omex.gallery.core.ai.classifier

/**
 * Result representing a single visual category classification prediction.
 */
data class ClassificationResult(
    val classId: Int,
    val label: String,
    val category: String,
    val confidence: Float
)
