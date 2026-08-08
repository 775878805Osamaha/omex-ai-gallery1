package com.omex.gallery.core.ai.classifier

import android.content.Context
import android.graphics.Bitmap
import com.omex.gallery.core.ai.model.ExecutionDelegate
import com.omex.gallery.core.ai.orchestrator.AiTask
import com.omex.gallery.core.ai.orchestrator.AiTaskQueue
import com.omex.gallery.core.ai.orchestrator.TaskPriority
import com.omex.gallery.core.ai.pipeline.DefaultImagePreprocessor
import com.omex.gallery.core.ai.pipeline.ImagePreprocessor
import com.omex.gallery.core.ai.pipeline.NormalizationType
import com.omex.gallery.core.ai.pipeline.PreprocessOptions
import com.omex.gallery.core.ai.registry.ModelRegistry
import com.omex.gallery.core.ai.registry.ModelType
import com.omex.gallery.core.ai.runtime.InterpreterPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.PriorityQueue

/**
 * Production implementation of [ImageClassifier] for MobileNetV3.
 */
class DefaultImageClassifier(
    private val context: Context,
    private val interpreterPool: InterpreterPool = InterpreterPool(context),
    private val modelRegistry: ModelRegistry = ModelRegistry(),
    private val preprocessor: ImagePreprocessor = DefaultImagePreprocessor(),
    private val taskQueue: AiTaskQueue? = null
) : ImageClassifier {

    private var labels: List<String> = emptyList()
    @Volatile
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            labels = loadLabels(context)
            isInitialized = true
        }
    }

    override suspend fun classifyImage(
        bitmap: Bitmap,
        topK: Int,
        threshold: Float
    ): Result<List<ClassificationResult>> = withContext(Dispatchers.Default) {
        runCatching {
            check(isInitialized) { "ImageClassifier is not initialized. Call initialize() first." }

            val descriptor = modelRegistry.getDescriptor(ModelType.MOBILENET_V3_LARGE)
                ?: throw IllegalStateException("MobileNetV3 descriptor missing from ModelRegistry")

            val preprocessOptions = PreprocessOptions(
                targetWidth = descriptor.inputShape.width,
                targetHeight = descriptor.inputShape.height,
                normalization = NormalizationType.ZERO_TO_ONE,
                isBgrOrder = false,
                keepAspectRatio = false
            )

            val inputBuffer: ByteBuffer = preprocessor.preprocess(bitmap, preprocessOptions)

            val runner = interpreterPool.acquireRunner(descriptor)
            val outputArray = Array(1) { FloatArray(labels.size.coerceAtLeast(1000)) }

            try {
                val inferenceResult = runner.runInference(inputBuffer, outputArray)
                if (inferenceResult.isFailure) {
                    throw inferenceResult.exceptionOrNull() ?: RuntimeException("Inference failed")
                }
            } finally {
                interpreterPool.releaseRunner(descriptor, runner)
            }

            val probabilities = outputArray[0]
            val results = processProbabilities(probabilities, topK, threshold)

            results
        }
    }

    /**
     * Optional execution via [AiTaskQueue] for prioritized background inference.
     */
    suspend fun classifyImageQueued(
        mediaId: Long,
        bitmap: Bitmap,
        priority: TaskPriority = TaskPriority.HIGH,
        topK: Int = 5,
        threshold: Float = 0.15f
    ): Result<List<ClassificationResult>> {
        val queue = taskQueue ?: return classifyImage(bitmap, topK, threshold)
        
        val task = AiTask(
            mediaId = mediaId,
            modelType = ModelType.MOBILENET_V3_LARGE,
            priority = priority,
            payload = bitmap,
            executeBlock = { payload ->
                classifyImage(payload as Bitmap, topK, threshold)
            }
        )

        queue.enqueue(task)
        val dequeuedTask = queue.dequeue() ?: return Result.failure(IllegalStateException("Queue execution error"))
        
        @Suppress("UNCHECKED_CAST")
        return dequeuedTask.executeBlock(dequeuedTask.payload) as Result<List<ClassificationResult>>
    }

    override fun close() {
        isInitialized = false
    }

    private fun processProbabilities(
        probabilities: FloatArray,
        topK: Int,
        threshold: Float
    ): List<ClassificationResult> {
        val pq = PriorityQueue<Pair<Int, Float>>(topK) { a, b -> a.second.compareTo(b.second) }

        for (i in probabilities.indices) {
            val score = probabilities[i]
            if (score >= threshold) {
                pq.add(Pair(i, score))
                if (pq.size > topK) {
                    pq.poll()
                }
            }
        }

        val topResults = mutableListOf<ClassificationResult>()
        while (pq.isNotEmpty()) {
            val (classId, score) = pq.poll()!!
            val rawLabel = labels.getOrElse(classId) { "Class $classId" }
            val parts = rawLabel.split(",", limit = 2)
            val label = parts.getOrNull(0)?.trim() ?: rawLabel
            val category = parts.getOrNull(1)?.trim() ?: "General"

            topResults.add(
                0,
                ClassificationResult(
                    classId = classId,
                    label = label,
                    category = category,
                    confidence = score
                )
            )
        }

        return topResults
    }

    private fun loadLabels(context: Context): List<String> {
        return try {
            context.assets.open("labels/imagenet_labels.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.toList()
                }
            }
        } catch (_: Exception) {
            // Embedded fallback ImageNet label dictionary subset
            generateDefaultImageNetLabels()
        }
    }

    private fun generateDefaultImageNetLabels(): List<String> {
        val list = MutableList(1000) { index -> "Object $index, General" }
        list[0] = "tench, Tinca tinca, Fish"
        list[1] = "goldfish, Carassius auratus, Fish"
        list[281] = "tabby, tabby cat, Animal"
        list[282] = "tiger cat, Animal"
        list[283] = "persian cat, Animal"
        list[207] = "golden retriever, Animal"
        list[504] = "coffee mug, Object"
        list[751] = "racer, race car, Vehicle"
        list[920] = "traffic light, Object"
        list[980] = "volcano, Nature"
        return list
    }
}
