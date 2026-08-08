package com.omex.gallery.core.search

/**
 * Interface contract for offline high-dimensional vector search and nearest-neighbor index matching.
 */
interface VectorSearchEngine {
    /**
     * Inserts or updates media vector embedding index.
     */
    suspend fun insertVector(mediaId: Long, vector: FloatArray)

    /**
     * Removes media vector entry from similarity index.
     */
    suspend fun removeVector(mediaId: Long)

    /**
     * Queries index for top-K nearest vector matches using cosine similarity.
     */
    suspend fun search(query: VectorQuery): List<SearchResult>

    /**
     * Clears all cached in-memory vector indices.
     */
    suspend fun clearIndex()
}
