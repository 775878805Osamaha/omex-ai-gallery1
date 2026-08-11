package com.omex.gallery.core.ai.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.omex.gallery.core.data.local.AiDao
import com.omex.gallery.core.data.local.CategoryDao
import com.omex.gallery.core.data.local.DetectedFaceEntity
import com.omex.gallery.core.data.local.DetectedObjectEntity
import com.omex.gallery.core.data.local.FaceEmbeddingEntity
import com.omex.gallery.core.data.local.ImageClassificationEntity
import com.omex.gallery.core.data.local.ImageMetadataEntity
import com.omex.gallery.core.data.local.MediaItemCategoryCrossRef
import com.omex.gallery.core.data.local.OcrTextEntity
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.toEntity
import com.omex.gallery.core.ai.classifier.CategoryClassifier
import com.omex.gallery.core.ai.classifier.DefaultImageClassifier
import com.omex.gallery.core.ai.detector.DefaultObjectDetector
import com.omex.gallery.core.ai.faces.DefaultFaceDetector
import com.omex.gallery.core.ai.faces.DefaultFaceEmbedder
import com.omex.gallery.core.ai.ocr.MlKitOcrEngine
import com.omex.gallery.core.ai.ocr.OcrEngine
import com.omex.gallery.core.hash.DefaultPerceptualHasher
import com.omex.gallery.core.hash.HashAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream
import java.security.MessageDigest

class AiPipelineExecutor(
    private val context: Context,
    private val aiDao: AiDao,
    private val categoryDao: CategoryDao? = null,
    private val classifier: DefaultImageClassifier = DefaultImageClassifier(context),
    private val detector: DefaultObjectDetector = DefaultObjectDetector(context),
    private val faceDetector: DefaultFaceDetector = DefaultFaceDetector(context),
    private val faceEmbedder: DefaultFaceEmbedder = DefaultFaceEmbedder(context),
    private val hasher: DefaultPerceptualHasher = DefaultPerceptualHasher(),
    private val ocrEngine: OcrEngine = MlKitOcrEngine(context)
) {

    suspend fun processMediaItem(mediaItem: MediaItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (mediaItem.isVideo) return@withContext Result.success(true)

            val uri = Uri.parse(mediaItem.uriString)
            val bitmap = loadBitmap(context, uri) ?: return@withContext Result.failure(Exception("Failed to decode bitmap"))

            // Initialize AI engines
            classifier.initialize()
            detector.initialize()
            faceDetector.initialize()
            faceEmbedder.initialize()
            ocrEngine.initialize()

            try {
                // Task 0: On-Device OCR Text Extraction
                var ocrEntity: OcrTextEntity? = null
                val ocrResult = ocrEngine.processImage(bitmap)
                if (ocrResult.isSuccess) {
                    val result = ocrResult.getOrThrow()
                    ocrEntity = OcrTextEntity(
                        mediaId = mediaItem.id,
                        extractedText = result.extractedText,
                        language = result.language,
                        processingStatus = if (result.extractedText.isNotBlank()) "COMPLETED" else "EMPTY",
                        modelVersion = "1.0.0",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    aiDao.insertOcrText(ocrEntity)
                } else {
                    ocrEntity = OcrTextEntity(
                        mediaId = mediaItem.id,
                        extractedText = "",
                        processingStatus = "FAILED",
                        modelVersion = "1.0.0",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    aiDao.insertOcrText(ocrEntity)
                }

                // Task 1: Hashes & Metadata
                val sha256 = calculateSha256(context, uri)
                val aHash = hasher.computeHash(bitmap, HashAlgorithm.AVERAGE_HASH).hashValue
                val dHash = hasher.computeHash(bitmap, HashAlgorithm.DIFFERENCE_HASH).hashValue
                val pHash = hasher.computeHash(bitmap, HashAlgorithm.PERCEPTUAL_HASH).hashValue

                val metadataEntity = ImageMetadataEntity(
                    mediaId = mediaItem.id,
                    sha256Hash = sha256,
                    aHash = aHash,
                    dHash = dHash,
                    pHash = pHash,
                    cameraMake = mediaItem.cameraMake,
                    cameraModel = mediaItem.cameraModel,
                    iso = mediaItem.iso,
                    aperture = mediaItem.aperture,
                    exposureTime = mediaItem.exposureTime,
                    focalLength = mediaItem.focalLength,
                    latitude = mediaItem.latitude,
                    longitude = mediaItem.longitude
                )
                aiDao.insertImageMetadata(metadataEntity)

                // Task 2: MobileNetV3 Classification
                var classEntities: List<ImageClassificationEntity> = emptyList()
                val classRes = classifier.classifyImage(bitmap, topK = 5)
                if (classRes.isSuccess) {
                    val classifications = classRes.getOrThrow()
                    classEntities = classifications.map {
                        ImageClassificationEntity(
                            mediaId = mediaItem.id,
                            classId = it.classId,
                            label = it.label,
                            category = it.category,
                            confidence = it.confidence
                        )
                    }
                    aiDao.insertClassifications(classEntities)
                }

                // Task 3: YOLOv8 Object Detection
                var objEntities: List<DetectedObjectEntity> = emptyList()
                val detRes = detector.detectObjects(bitmap)
                if (detRes.isSuccess) {
                    val detections = detRes.getOrThrow().boxes
                    objEntities = detections.map {
                        DetectedObjectEntity(
                            mediaId = mediaItem.id,
                            classId = it.classId,
                            labelName = it.labelName,
                            score = it.score,
                            left = it.left,
                            top = it.top,
                            right = it.right,
                            bottom = it.bottom
                        )
                    }
                    aiDao.insertObjects(objEntities)
                }

                // Task 4: Face Detection & FaceNet Embeddings
                var faceEntities: List<DetectedFaceEntity> = emptyList()
                val faceRes = faceDetector.detectFaces(bitmap)
                if (faceRes.isSuccess) {
                    val faces = faceRes.getOrThrow()
                    if (faces.isNotEmpty()) {
                        faceEntities = faces.map {
                            DetectedFaceEntity(
                                mediaId = mediaItem.id,
                                left = it.left,
                                top = it.top,
                                right = it.right,
                                bottom = it.bottom,
                                confidence = it.confidence
                            )
                        }
                        val faceIds = aiDao.insertFaces(faceEntities)

                        for (i in faces.indices) {
                            val face = faces[i]
                            val faceId = faceIds.getOrElse(i) { 0L }
                            val faceBox = android.graphics.RectF(face.left, face.top, face.right, face.bottom)
                            val faceCrop = cropFaceBitmap(bitmap, faceBox)
                            if (faceCrop != null) {
                                val embRes = faceEmbedder.extractEmbedding(faceCrop)
                                if (embRes.isSuccess) {
                                    val embedding = embRes.getOrThrow()
                                    val json = JSONArray(embedding.vector.toList()).toString()
                                    aiDao.insertEmbedding(
                                        FaceEmbeddingEntity(
                                            faceId = faceId,
                                            mediaId = mediaItem.id,
                                            vectorJson = json,
                                            dimension = embedding.dimension
                                        )
                                    )
                                }
                                if (!faceCrop.isRecycled && faceCrop != bitmap) faceCrop.recycle()
                            }
                        }
                    }
                }

                // Task 5: Virtual Category Classification & Persistence
                if (categoryDao != null) {
                    val categoryIds = CategoryClassifier.classifyMediaItem(
                        mediaItem = mediaItem.toEntity(),
                        classifications = classEntities,
                        objects = objEntities,
                        faces = faceEntities,
                        ocrText = ocrEntity
                    )
                    categoryDao.clearCategoriesForMedia(mediaItem.id)
                    val crossRefs = categoryIds.map { catId ->
                        MediaItemCategoryCrossRef(mediaId = mediaItem.id, categoryId = catId)
                    }
                    categoryDao.insertCrossRefs(crossRefs)
                }

                Result.success(true)
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            var inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream.close()

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val secondStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(secondStream, null, decodeOptions).also {
                secondStream?.close()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSha256(context: Context, uri: Uri): String {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun cropFaceBitmap(bitmap: Bitmap, box: android.graphics.RectF): Bitmap? {
        val left = (box.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (box.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (box.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (box.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
