package com.omex.gallery.core.ai.faces

import android.graphics.Bitmap

/**
 * Facial landmark key points.
 */
data class FaceLandmarks(
    val leftEye: Pair<Float, Float>?,
    val rightEye: Pair<Float, Float>?,
    val noseTip: Pair<Float, Float>?,
    val mouthLeft: Pair<Float, Float>?,
    val mouthRight: Pair<Float, Float>?
)

/**
 * Detected face bounding box with confidence score.
 */
data class FaceDetectionResult(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val landmarks: FaceLandmarks?
)

/**
 * High-dimensional vector embedding extracted from face crop via MobileFaceNet / FaceNet.
 */
data class FaceEmbedding(
    val vector: FloatArray,
    val dimension: Int = 128
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceEmbedding
        return vector.contentEquals(other.vector) && dimension == other.dimension
    }

    override fun hashCode(): Int {
        var result = vector.contentHashCode()
        result = 31 * result + dimension
        return result
    }
}
