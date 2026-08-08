package com.omex.gallery.core.search

/**
 * Vector search query payload.
 */
data class VectorQuery(
    val embedding: FloatArray,
    val topK: Int = 20,
    val minSimilarity: Float = 0.5f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VectorQuery
        return embedding.contentEquals(other.embedding) && topK == other.topK && minSimilarity == other.minSimilarity
    }

    override fun hashCode(): Int {
        var result = embedding.contentHashCode()
        result = 31 * result + topK
        result = 31 * result + minSimilarity.hashCode()
        return result
    }
}

/**
 * Match result item returned by local vector similarity search engine.
 */
data class SearchResult(
    val mediaId: Long,
    val similarityScore: Float,
    val distance: Float
)
