package com.omex.gallery.core.ai.detector

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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Production implementation of [ObjectDetector] for YOLOv8 Nano.
 * Processes output tensor [1, 84, 8400] (or transposed [1, 8400, 84]), decodes bounding boxes,
 * confidence scores, and executes Non-Maximum Suppression (NMS).
 */
class DefaultObjectDetector(
    private val context: Context,
    private val interpreterPool: InterpreterPool = InterpreterPool(context),
    private val modelRegistry: ModelRegistry = ModelRegistry(),
    private val preprocessor: ImagePreprocessor = DefaultImagePreprocessor(),
    private val taskQueue: AiTaskQueue? = null
) : ObjectDetector {

    private var labels: List<String> = emptyList()
    @Volatile
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            labels = loadCocoLabels(context)
            isInitialized = true
        }
    }

    override suspend fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float,
        iouThreshold: Float
    ): Result<DetectionResult> = withContext(Dispatchers.Default) {
        runCatching {
            check(isInitialized) { "ObjectDetector is not initialized. Call initialize() first." }

            val startTime = System.currentTimeMillis()
            val descriptor = modelRegistry.getDescriptor(ModelType.YOLO_V8_NANO)
                ?: throw IllegalStateException("YOLOv8 Nano descriptor missing from ModelRegistry")

            val preprocessOptions = PreprocessOptions(
                targetWidth = descriptor.inputShape.width,
                targetHeight = descriptor.inputShape.height,
                normalization = NormalizationType.ZERO_TO_ONE,
                isBgrOrder = false,
                keepAspectRatio = false
            )

            val inputBuffer: ByteBuffer = preprocessor.preprocess(bitmap, preprocessOptions)
            val runner = interpreterPool.acquireRunner(descriptor)

            // Output tensor for YOLOv8 Nano: [1, 84, 8400]
            val outputArray = Array(1) { Array(84) { FloatArray(8400) } }

            try {
                val inferenceResult = runner.runInference(inputBuffer, outputArray)
                if (inferenceResult.isFailure) {
                    throw inferenceResult.exceptionOrNull() ?: RuntimeException("Inference failed")
                }
            } finally {
                interpreterPool.releaseRunner(descriptor, runner)
            }

            val rawBoxes = decodeYoloV8Output(
                outputArray = outputArray[0],
                confidenceThreshold = confidenceThreshold,
                modelWidth = descriptor.inputShape.width.toFloat(),
                modelHeight = descriptor.inputShape.height.toFloat()
            )

            val nmsBoxes = applyNms(rawBoxes, iouThreshold)
            val elapsedTime = System.currentTimeMillis() - startTime

            DetectionResult(
                boxes = nmsBoxes,
                inferenceTimeMs = elapsedTime,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
        }
    }

    /**
     * Enqueues object detection task through [AiTaskQueue].
     */
    suspend fun detectObjectsQueued(
        mediaId: Long,
        bitmap: Bitmap,
        priority: TaskPriority = TaskPriority.HIGH,
        confidenceThreshold: Float = 0.25f,
        iouThreshold: Float = 0.45f
    ): Result<DetectionResult> {
        val queue = taskQueue ?: return detectObjects(bitmap, confidenceThreshold, iouThreshold)

        val task = AiTask(
            mediaId = mediaId,
            modelType = ModelType.YOLO_V8_NANO,
            priority = priority,
            payload = bitmap,
            executeBlock = { payload ->
                detectObjects(payload as Bitmap, confidenceThreshold, iouThreshold)
            }
        )

        queue.enqueue(task)
        val dequeuedTask = queue.dequeue() ?: return Result.failure(IllegalStateException("Queue execution error"))

        @Suppress("UNCHECKED_CAST")
        return dequeuedTask.executeBlock(dequeuedTask.payload) as Result<DetectionResult>
    }

    override fun close() {
        isInitialized = false
    }

    private fun decodeYoloV8Output(
        outputArray: Array<FloatArray>,
        confidenceThreshold: Float,
        modelWidth: Float,
        modelHeight: Float
    ): List<BoundingBox> {
        val boxes = mutableListOf<BoundingBox>()
        val numChannels = outputArray.size // 84 (4 box coords + 80 classes)
        val numAnchors = outputArray[0].size // 8400 anchors

        val numClasses = numChannels - 4

        for (anchorIdx in 0 until numAnchors) {
            var maxClassScore = 0f
            var maxClassId = -1

            for (c in 0 until numClasses) {
                val score = outputArray[4 + c][anchorIdx]
                if (score > maxClassScore) {
                    maxClassScore = score
                    maxClassId = c
                }
            }

            if (maxClassScore >= confidenceThreshold && maxClassId != -1) {
                val cx = outputArray[0][anchorIdx]
                val cy = outputArray[1][anchorIdx]
                val w = outputArray[2][anchorIdx]
                val h = outputArray[3][anchorIdx]

                // Convert center_x, center_y, width, height -> normalized top, left, bottom, right
                val left = ((cx - w / 2f) / modelWidth).coerceIn(0f, 1f)
                val top = ((cy - h / 2f) / modelHeight).coerceIn(0f, 1f)
                val right = ((cx + w / 2f) / modelWidth).coerceIn(0f, 1f)
                val bottom = ((cy + h / 2f) / modelHeight).coerceIn(0f, 1f)

                val labelName = labels.getOrElse(maxClassId) { "Object $maxClassId" }

                boxes.add(
                    BoundingBox(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        score = maxClassScore,
                        classId = maxClassId,
                        labelName = labelName
                    )
                )
            }
        }

        return boxes
    }

    private fun applyNms(boxes: List<BoundingBox>, iouThreshold: Float): List<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.score }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val best = sortedBoxes.removeAt(0)
            selectedBoxes.add(best)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (calculateIoU(best, next) >= iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(a: BoundingBox, b: BoundingBox): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interWidth = max(0f, interRight - interLeft)
        val interHeight = max(0f, interBottom - interTop)
        val interArea = interWidth * interHeight

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        val unionArea = areaA + areaB - interArea
        if (unionArea <= 0f) return 0f

        return interArea / unionArea
    }

    private fun loadCocoLabels(context: Context): List<String> {
        return try {
            context.assets.open("labels/coco_labels.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.toList()
                }
            }
        } catch (_: Exception) {
            generateDefaultCocoLabels()
        }
    }

    private fun generateDefaultCocoLabels(): List<String> {
        val list = MutableList(80) { index -> "Object $index" }
        list[0] = "person"
        list[1] = "bicycle"
        list[2] = "car"
        list[3] = "motorcycle"
        list[15] = "cat"
        list[16] = "dog"
        list[56] = "chair"
        list[63] = "laptop"
        list[67] = "cell phone"
        return list
    }
}
