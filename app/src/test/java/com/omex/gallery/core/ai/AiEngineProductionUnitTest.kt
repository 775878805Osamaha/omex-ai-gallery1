package com.omex.gallery.core.ai

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.omex.gallery.core.ai.classifier.DefaultImageClassifier
import com.omex.gallery.core.ai.detector.DefaultObjectDetector
import com.omex.gallery.core.ai.faces.DefaultFaceDetector
import com.omex.gallery.core.ai.faces.DefaultFaceEmbedder
import com.omex.gallery.core.ai.orchestrator.AiTask
import com.omex.gallery.core.ai.orchestrator.AiTaskQueue
import com.omex.gallery.core.ai.orchestrator.TaskPriority
import com.omex.gallery.core.ai.pipeline.DefaultImagePreprocessor
import com.omex.gallery.core.ai.pipeline.NormalizationType
import com.omex.gallery.core.ai.pipeline.PreprocessOptions
import com.omex.gallery.core.ai.registry.ModelRegistry
import com.omex.gallery.core.ai.registry.ModelType
import com.omex.gallery.core.ai.superresolution.DefaultImageSuperResolver
import com.omex.gallery.core.ai.superresolution.SuperResolutionConfig
import com.omex.gallery.core.ai.superresolution.UpscaleScale
import com.omex.gallery.core.hash.DefaultPerceptualHasher
import com.omex.gallery.core.hash.HashAlgorithm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiEngineProductionUnitTest {

    private lateinit var context: Context
    private lateinit var testBitmap: Bitmap

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun testDefaultImagePreprocessor() {
        val preprocessor = DefaultImagePreprocessor()
        val options = PreprocessOptions(
            targetWidth = 224,
            targetHeight = 224,
            normalization = NormalizationType.ZERO_TO_ONE
        )
        val buffer = preprocessor.preprocess(testBitmap, options)
        assertNotNull(buffer)
        assertEquals(224 * 224 * 3 * 4, buffer.capacity())
    }

    @Test
    fun testDefaultImageClassifier() = runBlocking {
        val classifier = DefaultImageClassifier(context)
        val initResult = classifier.initialize()
        assertTrue(initResult.isSuccess)

        val classifyResult = classifier.classifyImage(testBitmap, topK = 5, threshold = 0.0f)
        assertTrue(classifyResult.isSuccess)
        val predictions = classifyResult.getOrThrow()
        assertTrue(predictions.isNotEmpty())
        assertTrue(predictions.size <= 5)
        classifier.close()
    }

    @Test
    fun testDefaultObjectDetectorNms() = runBlocking {
        val detector = DefaultObjectDetector(context)
        val initResult = detector.initialize()
        assertTrue(initResult.isSuccess)

        val detectResult = detector.detectObjects(testBitmap, confidenceThreshold = 0.0f, iouThreshold = 0.45f)
        assertTrue(detectResult.isSuccess)
        val result = detectResult.getOrThrow()
        assertNotNull(result.boxes)
        detector.close()
    }

    @Test
    fun testFaceDetectorAndEmbedder() = runBlocking {
        val faceDetector = DefaultFaceDetector(context)
        assertTrue(faceDetector.initialize().isSuccess)

        val facesResult = faceDetector.detectFaces(testBitmap)
        assertTrue(facesResult.isSuccess)
        val faces = facesResult.getOrThrow()
        assertTrue(faces.isNotEmpty())

        val embedder = DefaultFaceEmbedder(context)
        assertTrue(embedder.initialize().isSuccess)

        val faceCrop = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val embeddingResult = embedder.extractEmbedding(faceCrop)
        assertTrue(embeddingResult.isSuccess)

        val emb = embeddingResult.getOrThrow()
        assertEquals(512, emb.dimension)
        assertEquals(512, emb.vector.size)

        val sim = embedder.computeSimilarity(emb, emb)
        assertTrue(sim > 0.95f)

        val clusters = embedder.clusterFaces(listOf(emb, emb))
        assertEquals(1, clusters.size)

        faceDetector.close()
        embedder.close()
    }

    @Test
    fun testSuperResolutionTiled() = runBlocking {
        val superResolver = DefaultImageSuperResolver(context)
        assertTrue(superResolver.initialize().isSuccess)

        val smallBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val config = SuperResolutionConfig(scale = UpscaleScale.X2, enableTileProcessing = true, tileSize = 64)

        var lastProgress = 0f
        val enhanceResult = superResolver.enhanceImageWithProgress(smallBitmap, config) { progress ->
            lastProgress = progress
        }

        assertTrue(enhanceResult.isSuccess)
        val result = enhanceResult.getOrThrow()
        assertEquals(256, result.enhancedWidth)
        assertEquals(256, result.enhancedHeight)
        assertTrue(lastProgress > 0f)
        superResolver.close()
    }

    @Test
    fun testPerceptualHasher() {
        val hasher = DefaultPerceptualHasher()
        
        val aHash = hasher.computeHash(testBitmap, HashAlgorithm.AVERAGE_HASH)
        val dHash = hasher.computeHash(testBitmap, HashAlgorithm.DIFFERENCE_HASH)
        val pHash = hasher.computeHash(testBitmap, HashAlgorithm.PERCEPTUAL_HASH)

        assertNotNull(aHash.hashValue)
        assertNotNull(dHash.hashValue)
        assertNotNull(pHash.hashValue)

        val dist = hasher.hammingDistance(aHash.hashValue, aHash.hashValue)
        assertEquals(0, dist)
        assertTrue(hasher.isDuplicate(aHash.hashValue, aHash.hashValue, threshold = 5))
    }

    @Test
    fun testAiTaskQueueIntegration() = runBlocking {
        val queue = AiTaskQueue()
        assertTrue(queue.isEmpty())

        val task1 = AiTask(
            mediaId = 101L,
            modelType = ModelType.MOBILENET_V3_LARGE,
            priority = TaskPriority.LOW,
            payload = "low_priority_task",
            executeBlock = { payload -> Result.success("Done: $payload") }
        )

        val task2 = AiTask(
            mediaId = 102L,
            modelType = ModelType.MOBILENET_V3_LARGE,
            priority = TaskPriority.HIGH,
            payload = "high_priority_task",
            executeBlock = { payload -> Result.success("Done: $payload") }
        )

        queue.enqueue(task1)
        queue.enqueue(task2)

        assertEquals(2, queue.queueSize.value)

        val dequeuedFirst = queue.dequeue()
        assertNotNull(dequeuedFirst)
        assertEquals(TaskPriority.HIGH, dequeuedFirst!!.priority)
        assertEquals("high_priority_task", dequeuedFirst.payload)

        val dequeuedSecond = queue.dequeue()
        assertNotNull(dequeuedSecond)
        assertEquals(TaskPriority.LOW, dequeuedSecond!!.priority)
        assertEquals("low_priority_task", dequeuedSecond.payload)

        assertTrue(queue.isEmpty())
    }
}
