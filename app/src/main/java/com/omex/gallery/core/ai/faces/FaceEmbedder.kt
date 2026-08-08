package com.omex.gallery.core.ai.faces

import android.graphics.Bitmap

/**
 * Interface contract for FaceNet / MobileFaceNet 128-d / 512-d feature vector extraction.
 */
interface FaceEmbedder {
    /**
     * Initializes FaceNet TFLite interpreter.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Extracts normalized facial embedding vector from cropped face bitmap.
     */
    suspend fun extractEmbedding(faceCrop: Bitmap): Result<FaceEmbedding>

    /**
     * Computes cosine similarity distance between two facial embedding vectors.
     */
    fun computeSimilarity(embedding1: FaceEmbedding, embedding2: FaceEmbedding): Float

    /**
     * Releases FaceNet interpreter context.
     */
    fun close()
}
