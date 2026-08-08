package com.omex.gallery.core.hash

import android.graphics.Bitmap

/**
 * Interface contract for perceptual visual hashing and duplicate detection.
 */
interface PerceptualHasher {
    /**
     * Computes perceptual hash for a bitmap image using selected algorithm.
     */
    fun computeHash(bitmap: Bitmap, algorithm: HashAlgorithm = HashAlgorithm.DIFFERENCE_HASH): HashResult

    /**
     * Computes Hamming distance bit-difference between two perceptual hashes (0 = identical, >10 = distinct).
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int

    /**
     * Returns true if distance is within duplicate detection threshold (e.g., <= 5).
     */
    fun isDuplicate(hash1: Long, hash2: Long, threshold: Int = 5): Boolean
}
