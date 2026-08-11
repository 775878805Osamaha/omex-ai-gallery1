package com.omex.gallery.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.omex.gallery.core.data.local.AiDao
import com.omex.gallery.core.data.local.MediaDao
import com.omex.gallery.domain.model.AiClassification
import com.omex.gallery.domain.model.AiFace
import com.omex.gallery.domain.model.AiMetadata
import com.omex.gallery.domain.model.AiObject
import com.omex.gallery.domain.model.AiOcrText
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.DuplicateMemberWithMedia
import com.omex.gallery.domain.model.IndexingProgress
import com.omex.gallery.domain.model.IndexingStatus
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaItemWithAi
import com.omex.gallery.domain.model.MediaRepository
import com.omex.gallery.domain.model.PersonGroup
import com.omex.gallery.domain.model.SearchFilterOptions
import com.omex.gallery.domain.model.SearchFilterState
import com.omex.gallery.domain.model.toDomain
import com.omex.gallery.domain.model.toEntity
import com.omex.gallery.core.indexer.HashGenerator
import com.omex.gallery.core.indexer.MediaIndexer
import com.omex.gallery.core.indexer.MediaScanner
import com.omex.gallery.core.indexer.MetadataExtractor
import com.omex.gallery.core.indexer.ProgressTracker
import com.omex.gallery.core.indexer.ThumbnailGenerator
import com.omex.gallery.core.ai.duplicates.DuplicateDetector
import com.omex.gallery.core.ai.faces.FaceClusterEngine
import com.omex.gallery.core.ai.pipeline.AiPipelineExecutor
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import com.omex.gallery.core.ai.superresolution.DefaultImageSuperResolver
import com.omex.gallery.core.ai.superresolution.SuperResolutionConfig
import com.omex.gallery.core.ai.superresolution.UpscaleScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaRepositoryImpl(
    private val mediaDao: MediaDao,
    private val aiDao: AiDao,
    private val categoryDao: com.omex.gallery.core.data.local.CategoryDao,
    private val context: Context
) : MediaRepository {

    companion object {
        @Volatile
        var lastIndexingError: String = "None"
    }

    private val _indexingProgress = MutableStateFlow(IndexingProgress())
    override fun getIndexingProgress(): Flow<IndexingProgress> = _indexingProgress.asStateFlow()

    override suspend fun getRoomMediaCount(): Int = withContext(Dispatchers.IO) {
        mediaDao.getMediaCount()
    }

    override suspend fun getRoomPhotosCount(): Int = withContext(Dispatchers.IO) {
        mediaDao.getPhotosCount()
    }

    override suspend fun getRoomVideosCount(): Int = withContext(Dispatchers.IO) {
        mediaDao.getVideosCount()
    }

    override suspend fun getLastIndexingError(): String = withContext(Dispatchers.IO) {
        if (_indexingProgress.value.status == IndexingStatus.ERROR && _indexingProgress.value.message.isNotEmpty()) {
            _indexingProgress.value.message
        } else {
            lastIndexingError
        }
    }

    private val pagingConfig = PagingConfig(
        pageSize = 60,
        initialLoadSize = 120,
        prefetchDistance = 120,
        enablePlaceholders = false
    )

    override fun getAllMedia(): Flow<List<MediaItem>> {
        return mediaDao.getAllMedia().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllMediaPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getAllMediaPagingSource()
        }.flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override fun getPhotos(): Flow<List<MediaItem>> {
        return mediaDao.getPhotos().map { list -> list.map { it.toDomain() } }
    }

    override fun getPhotosPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getPhotosPagingSource()
        }.flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override fun getVideos(): Flow<List<MediaItem>> {
        return mediaDao.getVideos().map { list -> list.map { it.toDomain() } }
    }

    override fun getVideosPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getVideosPagingSource()
        }.flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override fun getFavorites(): Flow<List<MediaItem>> {
        return mediaDao.getFavorites().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoritesPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getFavoritesPagingSource()
        }.flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override fun searchMedia(query: String): Flow<List<MediaItem>> {
        return mediaDao.searchMedia(query).map { list -> list.map { it.toDomain() } }
    }

    override fun searchMediaAdvanced(filterState: SearchFilterState): Flow<List<MediaItem>> {
        return mediaDao.searchMediaAdvanced(
            query = filterState.query.trim(),
            cameraModel = filterState.cameraModel,
            cameraMake = filterState.cameraMake,
            mlCategory = filterState.mlCategory,
            mlLabel = filterState.mlLabel,
            gpsOnly = if (filterState.isGpsOnly) 1 else 0
        ).map { list -> list.map { it.toDomain() } }
    }

    override fun getSearchFilterOptions(): Flow<SearchFilterOptions> {
        return combine(
            mediaDao.getDistinctCameraModels(),
            mediaDao.getDistinctCameraMakes(),
            aiDao.getDistinctMlCategories(),
            aiDao.getDistinctMlLabels()
        ) { models, makes, categories, labels ->
            SearchFilterOptions(
                cameraModels = models,
                cameraMakes = makes,
                mlCategories = categories,
                mlLabels = labels
            )
        }
    }

    override suspend fun getMediaById(id: Long): MediaItem? {
        return mediaDao.getMediaById(id)?.toDomain()
    }

    override suspend fun getAllMediaItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaDao.getAllMediaList().map { it.toDomain() }
    }

    override suspend fun insertMediaItems(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val countBefore = mediaDao.getAllMediaList().size
        android.util.Log.i("MediaRepository", "items before insert = $countBefore")
        com.omex.gallery.core.log.OmexLogger.i(com.omex.gallery.core.log.LogCategory.DATABASE, "MediaRepository", "items before insert = $countBefore")

        mediaDao.insertAll(items.map { it.toEntity() })

        val insertedCount = items.size
        android.util.Log.i("MediaRepository", "items inserted = $insertedCount")
        com.omex.gallery.core.log.OmexLogger.i(com.omex.gallery.core.log.LogCategory.DATABASE, "MediaRepository", "items inserted = $insertedCount")
    }

    override suspend fun deleteMediaItem(id: Long) = withContext(Dispatchers.IO) {
        val item = mediaDao.getMediaById(id)
        if (item != null) {
            try {
                if (item.uriString.isNotEmpty()) {
                    context.contentResolver.delete(Uri.parse(item.uriString), null, null)
                }
            } catch (e: Exception) {
                // Ignore security or system exceptions on scoped storage
            }
            if (item.filePath.isNotEmpty()) {
                try {
                    val f = File(item.filePath)
                    if (f.exists()) {
                        f.delete()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        mediaDao.deleteById(id)
        aiDao.deleteAiDataForMedia(id)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        mediaDao.updateFavorite(id, isFavorite)
    }

    override suspend fun scanAndIndexGallery(isFullReindex: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val scanner = MediaScanner(context)
            val metadataExtractor = MetadataExtractor(context)
            val hashGenerator = HashGenerator(context)
            val thumbnailGenerator = ThumbnailGenerator(context)
            val progressTracker = ProgressTracker()

            val indexer = MediaIndexer(
                scanner = scanner,
                metadataExtractor = metadataExtractor,
                hashGenerator = hashGenerator,
                thumbnailGenerator = thumbnailGenerator,
                repository = this@MediaRepositoryImpl,
                progressTracker = progressTracker
            )

            val res = indexer.executeIndexingPass()
            if (res.isFailure) {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Indexing pass failed"
                lastIndexingError = err
                _indexingProgress.value = IndexingProgress(
                    status = IndexingStatus.ERROR,
                    message = err
                )
            }
            res
        } catch (e: Exception) {
            val err = e.localizedMessage ?: e.message ?: "Indexing pass exception"
            lastIndexingError = err
            _indexingProgress.value = IndexingProgress(
                status = IndexingStatus.ERROR,
                message = err
            )
            Result.failure(e)
        }
    }

    override suspend fun regenerateThumbnails(): Result<Int> = withContext(Dispatchers.IO) {
        scanAndIndexGallery()
    }

    // AI Queries
    override suspend fun getClassificationsForMedia(mediaId: Long): List<AiClassification> = withContext(Dispatchers.IO) {
        aiDao.getClassificationsForMedia(mediaId).map {
            AiClassification(
                id = it.id,
                mediaId = it.mediaId,
                classId = it.classId,
                label = it.label,
                category = it.category,
                confidence = it.confidence
            )
        }
    }

    override suspend fun getObjectsForMedia(mediaId: Long): List<AiObject> = withContext(Dispatchers.IO) {
        aiDao.getObjectsForMedia(mediaId).map {
            AiObject(
                id = it.id,
                mediaId = it.mediaId,
                classId = it.classId,
                labelName = it.labelName,
                score = it.score,
                left = it.left,
                top = it.top,
                right = it.right,
                bottom = it.bottom
            )
        }
    }

    override suspend fun getFacesForMedia(mediaId: Long): List<AiFace> = withContext(Dispatchers.IO) {
        aiDao.getFacesForMedia(mediaId).map {
            AiFace(
                id = it.id,
                mediaId = it.mediaId,
                left = it.left,
                top = it.top,
                right = it.right,
                bottom = it.bottom,
                confidence = it.confidence,
                clusterId = it.clusterId
            )
        }
    }

    override suspend fun getMetadataForMedia(mediaId: Long): AiMetadata? = withContext(Dispatchers.IO) {
        val entity = aiDao.getImageMetadata(mediaId) ?: return@withContext null
        AiMetadata(
            mediaId = entity.mediaId,
            sha256Hash = entity.sha256Hash,
            aHash = entity.aHash,
            dHash = entity.dHash,
            pHash = entity.pHash,
            cameraMake = entity.cameraMake,
            cameraModel = entity.cameraModel,
            iso = entity.iso,
            aperture = entity.aperture,
            exposureTime = entity.exposureTime,
            focalLength = entity.focalLength,
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }

    override suspend fun getOcrTextForMedia(mediaId: Long): AiOcrText? = withContext(Dispatchers.IO) {
        val entity = aiDao.getOcrForMedia(mediaId) ?: return@withContext null
        AiOcrText(
            id = entity.id,
            mediaId = entity.mediaId,
            extractedText = entity.extractedText,
            language = entity.language,
            processingStatus = entity.processingStatus,
            modelVersion = entity.modelVersion
        )
    }

    override suspend fun getMediaItemWithAi(mediaId: Long): MediaItemWithAi? = withContext(Dispatchers.IO) {
        val mediaItem = getMediaById(mediaId) ?: return@withContext null
        val classifications = getClassificationsForMedia(mediaId)
        val objects = getObjectsForMedia(mediaId)
        val faces = getFacesForMedia(mediaId)
        val metadata = getMetadataForMedia(mediaId)
        val ocr = getOcrTextForMedia(mediaId)
        val duplicateGroup = aiDao.getDuplicateGroupForMedia(mediaId)

        MediaItemWithAi(
            mediaItem = mediaItem,
            classifications = classifications,
            objects = objects,
            faces = faces,
            metadata = metadata,
            ocrText = ocr,
            duplicateGroupId = duplicateGroup?.groupId
        )
    }

    override fun getDuplicateGroups(): Flow<List<DuplicateGroupWithMedia>> = flow {
        aiDao.getAllDuplicateGroups().collect { groups ->
            val result = groups.map { group ->
                val members = aiDao.getMembersForDuplicateGroup(group.groupId).mapNotNull { member ->
                    val media = getMediaById(member.mediaId)
                    if (media != null) DuplicateMemberWithMedia(media, member.similarityScore) else null
                }
                DuplicateGroupWithMedia(
                    groupId = group.groupId,
                    groupType = group.groupType,
                    members = members
                )
            }
            emit(result)
        }
    }

    override fun getPersonGroups(): Flow<List<PersonGroup>> = flow {
        aiDao.getAllPersonClusterIds().collect { clusterIds ->
            val groups = clusterIds.map { clusterId ->
                val mediaIds = aiDao.getMediaIdsForPersonCluster(clusterId)
                val mediaItems = mediaIds.mapNotNull { getMediaById(it) }
                PersonGroup(
                    clusterId = clusterId,
                    personName = "Person ${clusterId.removePrefix("person_cluster_")}",
                    faceCount = mediaItems.size,
                    mediaItems = mediaItems
                )
            }
            emit(groups)
        }
    }

    override suspend fun getPersonMediaItems(clusterId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val ids = aiDao.getMediaIdsForPersonCluster(clusterId)
        ids.mapNotNull { getMediaById(it) }
    }

    override suspend fun runAiPipelineOnMedia(context: Context, mediaItem: MediaItem): Result<Boolean> = withContext(Dispatchers.IO) {
        val executor = AiPipelineExecutor(context, aiDao, categoryDao)
        executor.processMediaItem(mediaItem)
    }

    override suspend fun runFullGalleryAiScan(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val allMedia = getAllMediaItems()
            val executor = AiPipelineExecutor(context, aiDao, categoryDao)
            val total = allMedia.size

            allMedia.forEachIndexed { index, item ->
                _indexingProgress.value = IndexingProgress(
                    status = IndexingStatus.INDEXING_EXIF,
                    scannedCount = index + 1,
                    totalCount = total,
                    currentFileName = item.fileName,
                    message = "Running AI analysis (${index + 1}/$total)"
                )
                executor.processMediaItem(item)
            }

            // Cluster faces & detect duplicates
            val clusterEngine = FaceClusterEngine(context, aiDao)
            clusterEngine.clusterAllFaces()

            val duplicateDetector = DuplicateDetector(aiDao)
            duplicateDetector.detectAndPersistDuplicates()

            _indexingProgress.value = IndexingProgress(
                status = IndexingStatus.COMPLETED,
                scannedCount = total,
                totalCount = total,
                message = "AI gallery scan complete"
            )

            Result.success(true)
        } catch (e: Exception) {
            _indexingProgress.value = IndexingProgress(
                status = IndexingStatus.ERROR,
                message = "AI scan failed: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    override suspend fun superResolveImage(
        context: Context,
        mediaItem: MediaItem,
        scaleFactor: Int,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var srcBitmap: Bitmap? = null
        var enhancedBitmap: Bitmap? = null
        try {
            val uri = Uri.parse(mediaItem.uriString)
            var inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open stream"))

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream.close()

            var sampleSize = 1
            val maxDimension = 1024
            while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open stream"))
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            srcBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()

            if (srcBitmap == null) return@withContext Result.failure(Exception("Failed to decode image"))

            val superResolver = DefaultImageSuperResolver(context)
            superResolver.initialize()

            val config = SuperResolutionConfig(
                scale = if (scaleFactor == 4) UpscaleScale.X4 else UpscaleScale.X2
            )
            val res = superResolver.enhanceImageWithProgress(srcBitmap, config, onProgress)

            if (res.isFailure) {
                return@withContext Result.failure(res.exceptionOrNull() ?: Exception("Super resolution failed"))
            }

            enhancedBitmap = res.getOrThrow().enhancedBitmap

            val outputDir = File(context.cacheDir, "super_resolution")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "sr_${scaleFactor}x_${mediaItem.id}.jpg")

            FileOutputStream(outputFile).use { out ->
                enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (srcBitmap != null && !srcBitmap.isRecycled) {
                srcBitmap.recycle()
            }
            if (enhancedBitmap != null && !enhancedBitmap.isRecycled) {
                enhancedBitmap.recycle()
            }
        }
    }

    // Virtual Categories Implementation
    override fun getAllCategories(): Flow<List<com.omex.gallery.core.data.local.MediaCategoryEntity>> {
        return flow {
            ensureDefaultCategories()
            categoryDao.getAllCategories().collect { emit(it) }
        }
    }

    private suspend fun ensureDefaultCategories() {
        val defaultCategories = listOf(
            com.omex.gallery.core.data.local.MediaCategoryEntity("PERSON", "الأشخاص", "person"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("PRODUCT", "المنتجات", "shopping_bag"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("TRADING", "التداول", "show_chart"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("SCREENSHOT", "لقطات الشاشة", "crop_free"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("DOCUMENT", "المستندات", "description"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("CAR", "السيارات", "directions_car"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("FOOD", "الطعام", "restaurant"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("NATURE", "الطبيعة", "park"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("TRAVEL", "السفر", "flight"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("WORK", "صور العمل", "work"),
            com.omex.gallery.core.data.local.MediaCategoryEntity("OTHER", "أخرى", "category")
        )
        categoryDao.insertCategories(defaultCategories)
    }

    override suspend fun getCategoriesForMedia(mediaId: Long): List<String> = withContext(Dispatchers.IO) {
        categoryDao.getCategoriesForMedia(mediaId)
    }

    override fun getCategoriesForMediaFlow(mediaId: Long): Flow<List<String>> {
        return categoryDao.getCategoriesForMediaFlow(mediaId)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getMediaForCategories(categoryIds: List<String>): Flow<List<MediaItem>> = flow {
        if (categoryIds.isEmpty()) {
            mediaDao.getAllMedia().map { list -> list.map { it.toDomain() } }.collect { emit(it) }
        } else {
            val matchingIds = categoryDao.getMediaIdsMatchingAllCategories(categoryIds, categoryIds.size)
            val items = mediaDao.getAllMediaList()
                .filter { matchingIds.contains(it.id) }
                .map { it.toDomain() }
            emit(items)
        }
    }

    override fun getCategoryMediaCount(categoryId: String): Flow<Int> {
        return categoryDao.getCategoryMediaCountFlow(categoryId)
    }

    override suspend fun getLatestMediaForCategory(categoryId: String): MediaItem? = withContext(Dispatchers.IO) {
        categoryDao.getLatestMediaForCategory(categoryId)?.toDomain()
    }

    override suspend fun classifyUnclassifiedMedia(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allMedia = mediaDao.getAllMediaList()
            var classifiedCount = 0

            allMedia.forEach { itemEntity ->
                val existingCategories = categoryDao.getCategoriesForMedia(itemEntity.id)
                if (existingCategories.isEmpty()) {
                    val classifications = aiDao.getClassificationsForMedia(itemEntity.id)
                    val objects = aiDao.getObjectsForMedia(itemEntity.id)
                    val faces = aiDao.getFacesForMedia(itemEntity.id)
                    val ocr = aiDao.getOcrForMedia(itemEntity.id)

                    val categories = com.omex.gallery.core.ai.classifier.CategoryClassifier.classifyMediaItem(
                        mediaItem = itemEntity,
                        classifications = classifications,
                        objects = objects,
                        faces = faces,
                        ocrText = ocr
                    )

                    val crossRefs = categories.map { catId ->
                        com.omex.gallery.core.data.local.MediaItemCategoryCrossRef(
                            mediaId = itemEntity.id,
                            categoryId = catId
                        )
                    }
                    categoryDao.insertCrossRefs(crossRefs)
                    classifiedCount++
                }
            }

            Result.success(classifiedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
