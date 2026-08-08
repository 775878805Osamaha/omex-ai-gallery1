package com.omex.gallery.core.ai.model

import android.content.Context
import java.nio.MappedByteBuffer

/**
 * Interface for loading neural network model file buffers from APK assets or internal disk storage.
 */
interface ModelLoader {
    /**
     * Loads a memory-mapped byte buffer for a given model asset path.
     */
    suspend fun loadModelFromAssets(context: Context, assetPath: String): Result<MappedByteBuffer>

    /**
     * Loads a memory-mapped byte buffer from a absolute file system path.
     */
    suspend fun loadModelFromFile(filePath: String): Result<MappedByteBuffer>
}
