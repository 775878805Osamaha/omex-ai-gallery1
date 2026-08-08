package com.omex.gallery.core.hash

/**
 * Perceptual hashing algorithm variants.
 */
enum class HashAlgorithm {
    AVERAGE_HASH,      // aHash
    DIFFERENCE_HASH,   // dHash
    PERCEPTUAL_HASH    // pHash (DCT based)
}

/**
 * Image perceptual hash computation result.
 */
data class HashResult(
    val hashValue: Long,
    val binaryString: String,
    val algorithm: HashAlgorithm
)
