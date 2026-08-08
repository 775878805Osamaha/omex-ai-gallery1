package com.omex.gallery.core.ai.detector

/**
 * Normalized bounding box coordinates for a detected object.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val score: Float,
    val classId: Int,
    val labelName: String
)

/**
 * Container holding object detection outputs for an image.
 */
data class DetectionResult(
    val boxes: List<BoundingBox>,
    val inferenceTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)
