package com.omex.gallery.core.ai.registry

import com.omex.gallery.core.ai.model.ExecutionDelegate
import com.omex.gallery.core.ai.model.ModelConfig
import com.omex.gallery.core.ai.model.ModelPrecision
import com.omex.gallery.core.ai.model.TensorShape
import java.util.concurrent.ConcurrentHashMap

/**
 * Known model identifiers supported in OMEX AI Gallery.
 */
enum class ModelType(val id: String, val displayName: String) {
    MOBILENET_V3_LARGE("mobilenet_v3_large", "MobileNetV3 Image Classifier"),
    YOLO_V8_NANO("yolov8_nano", "YOLOv8 Nano Object Detector"),
    FACE_NET("facenet_512", "FaceNet 512-d Face Embedder"),
    REAL_ESRGAN_X2("real_esrgan_x2", "Real-ESRGAN x2 Super Resolver"),
    OCR_TEXT_RECOGNIZER("ocr_text_recognizer", "ML Kit / TFLite OCR Recognizer")
}

/**
 * Full metadata specification for a registered neural model.
 */
data class ModelDescriptor(
    val type: ModelType,
    val version: String,
    val assetFileName: String,
    val sha256Checksum: String,
    val preferredDelegate: ExecutionDelegate = ExecutionDelegate.GPU,
    val fallbackDelegate: ExecutionDelegate = ExecutionDelegate.CPU,
    val precision: ModelPrecision = ModelPrecision.FLOAT32,
    val inputShape: TensorShape,
    val requiredMemoryMb: Int,
    val isQuantized: Boolean = false
) {
    fun toModelConfig(): ModelConfig {
        return ModelConfig(
            modelPath = assetFileName,
            numThreads = 4,
            delegate = preferredDelegate,
            precision = precision,
            inputShape = inputShape
        )
    }
}

/**
 * Thread-safe registry maintaining descriptors, versions, and configuration mappings
 * for all AI models utilized across the app.
 */
class ModelRegistry {

    private val registeredModels = ConcurrentHashMap<ModelType, ModelDescriptor>()

    init {
        registerDefaultModels()
    }

    private fun registerDefaultModels() {
        // MobileNetV3 Large Classifier
        registerModel(
            ModelDescriptor(
                type = ModelType.MOBILENET_V3_LARGE,
                version = "1.0.0",
                assetFileName = "models/mobilenet_v3_large_float32.tflite",
                sha256Checksum = "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef",
                preferredDelegate = ExecutionDelegate.GPU,
                fallbackDelegate = ExecutionDelegate.CPU,
                precision = ModelPrecision.FLOAT32,
                inputShape = TensorShape(batchSize = 1, width = 224, height = 224, channels = 3),
                requiredMemoryMb = 24,
                isQuantized = false
            )
        )

        // YOLOv8 Nano Object Detector
        registerModel(
            ModelDescriptor(
                type = ModelType.YOLO_V8_NANO,
                version = "8.0.0",
                assetFileName = "models/yolov8n_float32.tflite",
                sha256Checksum = "b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef1a",
                preferredDelegate = ExecutionDelegate.GPU,
                fallbackDelegate = ExecutionDelegate.CPU,
                precision = ModelPrecision.FLOAT32,
                inputShape = TensorShape(batchSize = 1, width = 640, height = 640, channels = 3),
                requiredMemoryMb = 32,
                isQuantized = false
            )
        )

        // FaceNet 512-d Face Embedder
        registerModel(
            ModelDescriptor(
                type = ModelType.FACE_NET,
                version = "2.1.0",
                assetFileName = "models/facenet_512_int8.tflite",
                sha256Checksum = "c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef12b",
                preferredDelegate = ExecutionDelegate.NNAPI,
                fallbackDelegate = ExecutionDelegate.CPU,
                precision = ModelPrecision.INT8,
                inputShape = TensorShape(batchSize = 1, width = 160, height = 160, channels = 3),
                requiredMemoryMb = 18,
                isQuantized = true
            )
        )

        // Real-ESRGAN x2 Super Resolver
        registerModel(
            ModelDescriptor(
                type = ModelType.REAL_ESRGAN_X2,
                version = "1.0.1",
                assetFileName = "models/real_esrgan_x2_float16.tflite",
                sha256Checksum = "d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef123c",
                preferredDelegate = ExecutionDelegate.GPU,
                fallbackDelegate = ExecutionDelegate.CPU,
                precision = ModelPrecision.FLOAT16,
                inputShape = TensorShape(batchSize = 1, width = 256, height = 256, channels = 3),
                requiredMemoryMb = 64,
                isQuantized = false
            )
        )
    }

    fun registerModel(descriptor: ModelDescriptor) {
        registeredModels[descriptor.type] = descriptor
    }

    fun getDescriptor(modelType: ModelType): ModelDescriptor? {
        return registeredModels[modelType]
    }

    fun getAllDescriptors(): List<ModelDescriptor> {
        return registeredModels.values.toList()
    }

    fun isRegistered(modelType: ModelType): Boolean {
        return registeredModels.containsKey(modelType)
    }
}
