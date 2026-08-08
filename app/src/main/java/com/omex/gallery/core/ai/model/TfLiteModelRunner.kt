package com.omex.gallery.core.ai.model

import java.nio.ByteBuffer

/**
 * Interface managing lifecycle, resource allocation, and memory safety for TFLite inference runners.
 */
interface TfLiteModelRunner {
    /**
     * Initializes the TFLite interpreter with the provided configuration.
     */
    suspend fun initialize(config: ModelConfig, modelBuffer: ByteBuffer): Result<Unit>

    /**
     * Executes tensor inference against input byte buffer into output container.
     */
    fun runInference(inputBuffer: ByteBuffer, outputContainer: Any): Result<Long>

    /**
     * Releases hardware delegates, memory mappings, and interpreter context.
     */
    fun close()

    /**
     * Returns true if interpreter is initialized and ready for inference.
     */
    fun isInitialized(): Boolean
}
