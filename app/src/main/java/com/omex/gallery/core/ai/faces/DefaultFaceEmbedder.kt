package com.omex.gallery.core.ai.faces

import android.content.Context
import android.graphics.Bitmap
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
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * Cluster of face embeddings representing a unique identity.
 */
data class FaceCluster(
    val clusterId: String,
    val centroid: FaceEmbedding,
    val faceCount: Int
)

/**
 * Production implementation of [FaceEmbedder] producing 512-dimensional normalized face embeddings.
 * Supports cosine similarity metric and face clustering.
 */
class DefaultFaceEmbedder(
    private val context: Context,
    private val interpreterPool: InterpreterPool = InterpreterPool(context),
    private val modelRegistry: ModelRegistry = ModelRegistry(),
    private val preprocessor: ImagePreprocessor = DefaultImagePreprocessor(),
    private val taskQueue: AiTaskQueue? = null
) : FaceEmbedder {

    @Volatile
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            isInitialized = true
        }
    }

    override suspend fun extractEmbedding(faceCrop: Bitmap): Result<FaceEmbedding> = withContext(Dispatchers.Default) {
        runCatching {
            check(isInitialized) { "FaceEmbedder is not initialized. Call initialize() first." }

            val descriptor = modelRegistry.getDescriptor(ModelType.FACE_NET)
                ?: throw IllegalStateException("FaceNet descriptor missing from ModelRegistry")

            val preprocessOptions = PreprocessOptions(
                targetWidth = descriptor.inputShape.width,
                targetHeight = descriptor.inputShape.height,
                normalization = NormalizationType.MINUS_ONE_TO_ONE,
                isBgrOrder = false,
                keepAspectRatio = false
            )

            val inputBuffer: ByteBuffer = preprocessor.preprocess(faceCrop, preprocessOptions)
            val runner = interpreterPool.acquireRunner(descriptor)

            val targetDim = 512
            val outputArray = Array(1) { FloatArray(targetDim) }

            try {
                val inferenceResult = runner.runInference(inputBuffer, outputArray)
                if (inferenceResult.isFailure) {
                    throw inferenceResult.exceptionOrNull() ?: RuntimeException("Inference failed")
                }
            } finally {
                interpreterPool.releaseRunner(descriptor, runner)
            }

            val rawVector = outputArray[0]
            val normalizedVector = l2Normalize(rawVector)

            FaceEmbedding(
                vector = normalizedVector,
                dimension = targetDim
            )
        }
    }

    /**
     * Enqueues face embedding extraction through [AiTaskQueue].
     */
    suspend fun extractEmbeddingQueued(
        mediaId: Long,
        faceCrop: Bitmap,
        priority: TaskPriority = TaskPriority.HIGH
    ): Result<FaceEmbedding> {
        val queue = taskQueue ?: return extractEmbedding(faceCrop)

        val task = AiTask(
            mediaId = mediaId,
            modelType = ModelType.FACE_NET,
            priority = priority,
            payload = faceCrop,
            executeBlock = { payload ->
                extractEmbedding(payload as Bitmap)
            }
        )

        queue.enqueue(task)
        val dequeuedTask = queue.dequeue() ?: return Result.failure(IllegalStateException("Queue execution error"))

        @Suppress("UNCHECKED_CAST")
        return dequeuedTask.executeBlock(dequeuedTask.payload) as Result<FaceEmbedding>
    }

    override fun computeSimilarity(embedding1: FaceEmbedding, embedding2: FaceEmbedding): Float {
        val v1 = embedding1.vector
        val v2 = embedding2.vector

        val minDim = minOf(v1.size, v2.size)
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in 0 until minDim) {
            val val1 = v1[i]
            val val2 = v2[i]
            dotProduct += val1 * val2
            norm1 += val1 * val1
            norm2 += val2 * val2
        }

        if (norm1 <= 0f || norm2 <= 0f) return 0f

        return (dotProduct / (sqrt(norm1) * sqrt(norm2))).coerceIn(-1.0f, 1.0f)
    }

    /**
     * Clusters a set of face embeddings using cosine similarity distance thresholding.
     */
    fun clusterFaces(embeddings: List<FaceEmbedding>, threshold: Float = 0.65f): List<FaceCluster> {
        val clusters = mutableListOf<MutableList<FaceEmbedding>>()

        for (emb in embeddings) {
            var bestMatchIdx = -1
            var maxSim = -1.0f

            for ((idx, cluster) in clusters.withIndex()) {
                val sim = computeSimilarity(emb, computeCentroid(cluster))
                if (sim > maxSim) {
                    maxSim = sim
                    bestMatchIdx = idx
                }
            }

            if (bestMatchIdx != -1 && maxSim >= threshold) {
                clusters[bestMatchIdx].add(emb)
            } else {
                clusters.add(mutableListOf(emb))
            }
        }

        return clusters.mapIndexed { index, list ->
            FaceCluster(
                clusterId = "person_cluster_${index + 1}",
                centroid = computeCentroid(list),
                faceCount = list.size
            )
        }
    }

    override fun close() {
        isInitialized = false
    }

    private fun computeCentroid(list: List<FaceEmbedding>): FaceEmbedding {
        if (list.isEmpty()) return FaceEmbedding(FloatArray(512), 512)
        val dim = list[0].vector.size
        val sum = FloatArray(dim)

        for (emb in list) {
            for (i in 0 until dim) {
                sum[i] += emb.vector[i]
            }
        }

        for (i in 0 until dim) {
            sum[i] /= list.size.toFloat()
        }

        return FaceEmbedding(l2Normalize(sum), dim)
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquare = 0f
        for (v in vector) {
            sumSquare += v * v
        }
        val norm = sqrt(sumSquare)
        if (norm <= 0f) return vector

        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }
}
