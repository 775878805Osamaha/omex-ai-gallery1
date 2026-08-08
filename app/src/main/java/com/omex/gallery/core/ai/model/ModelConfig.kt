package com.omex.gallery.core.ai.model

import java.nio.ByteBuffer

/**
 * Execution delegate hardware backend for TensorFlow Lite runtime.
 */
enum class ExecutionDelegate {
    CPU,
    GPU,
    NNAPI
}

/**
 * Tensor precision format used by model weights.
 */
enum class ModelPrecision {
    FLOAT32,
    FLOAT16,
    INT8
}

/**
 * Shape dimension descriptor for input and output model tensors.
 */
data class TensorShape(
    val batchSize: Int,
    val width: Int,
    val height: Int,
    val channels: Int
) {
    val totalElements: Int get() = batchSize * width * height * channels
}

/**
 * Configuration contract for loading TFLite model instances.
 */
data class ModelConfig(
    val modelPath: String,
    val numThreads: Int = 4,
    val delegate: ExecutionDelegate = ExecutionDelegate.CPU,
    val precision: ModelPrecision = ModelPrecision.FLOAT32,
    val inputShape: TensorShape
)
