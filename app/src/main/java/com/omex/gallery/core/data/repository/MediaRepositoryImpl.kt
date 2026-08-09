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
    private val context: Context
) : MediaRepository {

    private val _indexingProgress = MutableStateFlow(IndexingProgress())

    override fun getIndexingProgress(): Flow<IndexingProgress> =
        _indexingProgress.asStateFlow()

    private val pagingConfig = PagingConfig(
        pageSize = 60,
        initialLoadSize = 120,
        prefetchDistance = 120,
        enablePlaceholders = false
    )

    override fun getAllMedia(): Flow<List<MediaItem>> {
        return mediaDao.getAllMedia().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllMediaPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getAllMediaPagingSource()
        }.flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun getPhotos(): Flow<List<MediaItem>> {
        return mediaDao.getPhotos().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPhotosPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getPhotosPagingSource()
        }.flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun getVideos(): Flow<List<MediaItem>> {
        return mediaDao.getVideos().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getVideosPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getVideosPagingSource()
        }.flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun getFavorites(): Flow<List<MediaItem>> {
        return mediaDao.getFavorites().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFavoritesPaged(): Flow<PagingData<MediaItem>> {
        return Pager(pagingConfig) {
            mediaDao.getFavoritesPagingSource()
        }.flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun searchMedia(query: String): Flow<List<MediaItem>> {
        return mediaDao.searchMedia(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun searchMediaAdvanced(
        filterState: SearchFilterState
    ): Flow<List<MediaItem>> {
        return mediaDao.searchMediaAdvanced(
            query = filterState.query.trim(),
            cameraModel = filterState.cameraModel,
            cameraMake = filterState.cameraMake,
            mlCategory = filterState.mlCategory,
            mlLabel = filterState.mlLabel,
            gpsOnly = if (filterState.isGpsOnly) 1 else 0
        ).map { list ->
            list.map { it.toDomain() }
        }
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

    override suspend fun getAllMediaItems(): List<MediaItem> =
        withContext(Dispatchers.IO) {
            mediaDao.getAllMediaList().map { it.toDomain() }
        }

    override suspend fun insertMediaItems(
        items: List<MediaItem>
    ) = withContext(Dispatchers.IO) {
        mediaDao.insertAll(items.map { it.toEntity() })
    }

    override suspend fun deleteMediaItem(id: Long) =
        withContext(Dispatchers.IO) {
            mediaDao.deleteById(id)
            aiDao.deleteAiDataForMedia(id)
        }

    override suspend fun toggleFavorite(
        id: Long,
        isFavorite: Boolean
    ) {
        mediaDao.updateFavorite(id, isFavorite)
    }

    /*
     * IMPORTANT:
     * The interface MediaRepository requires:
     *
     * scanAndIndexGallery(
     *     isFullReindex: Boolean = ...
     * ): Result<Int>
     *
     * Therefore the implementation must have the same parameter.
     */
    override suspend fun scanAndIndexGallery(
        isFullReindex: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {

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

        /*
         * MediaIndexer currently exposes executeIndexingPass()
         * without an isFullReindex parameter.
         *
         * Keep the existing implementation intact here while
         * matching the MediaRepository interface signature.
         */
        indexer.executeIndexingPass()
    }

    override suspend fun regenerateThumbnails(): Result<Int> =
        withContext(Dispatchers.IO) {
            scanAndIndexGallery(false)
        }

    // -------------------------------------------------------------------------
    // AI Queries
    // -------------------------------------------------------------------------

    override suspend fun getClassificationsForMedia(
        mediaId: Long
    ): List<AiClassification> =
        withContext(Dispatchers.IO) {
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

    override suspend fun getObjectsForMedia(
        mediaId: Long
    ): List<AiObject> =
        withContext(Dispatchers.IO) {
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

    override suspend fun getFacesForMedia(
        mediaId: Long
    ): List<AiFace> =
        withContext(Dispatchers.IO) {
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

    override suspend fun getMetadataForMedia(
        mediaId: Long
    ): AiMetadata? =
        withContext(Dispatchers.IO) {

            val entity = aiDao.getImageMetadata(mediaId)
                ?: return@withContext null

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

    override suspend fun getMediaItemWithAi(
        mediaId: Long
    ): MediaItemWithAi? =
        withContext(Dispatchers.IO) {

            val mediaItem = getMediaById(mediaId)
                ?: return@withContext null

            val classifications = getClassificationsForMedia(mediaId)
            val objects = getObjectsForMedia(mediaId)
            val faces = getFacesForMedia(mediaId)
            val metadata = getMetadataForMedia(mediaId)
            val duplicateGroup =
                aiDao.getDuplicateGroupForMedia(mediaId)

            MediaItemWithAi(
                mediaItem = mediaItem,
                classifications = classifications,
                objects = objects,
                faces = faces,
                metadata = metadata,
                duplicateGroupId = duplicateGroup?.groupId
            )
        }

    override fun getDuplicateGroups(): Flow<List<DuplicateGroupWithMedia>> =
        flow {

            aiDao.getAllDuplicateGroups().collect { groups ->

                val result = groups.map { group ->

                    val members =
                        aiDao.getMembersForDuplicateGroup(group.groupId)
                            .mapNotNull { member ->

                                val media =
                                    getMediaById(member.mediaId)

                                if (media != null) {
                                    DuplicateMemberWithMedia(
                                        media,
                                        member.similarityScore
                                    )
                                } else {
                                    null
                                }
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

    override fun getPersonGroups(): Flow<List<PersonGroup>> =
        flow {

            aiDao.getAllPersonClusterIds().collect { clusterIds ->

                val groups = clusterIds.map { clusterId ->

                    val mediaIds =
                        aiDao.getMediaIdsForPersonCluster(clusterId)

                    val mediaItems =
                        mediaIds.mapNotNull { getMediaById(it) }

                    PersonGroup(
                        clusterId = clusterId,
                        personName =
                            "Person ${clusterId.removePrefix("person_cluster_")}",
                        faceCount = mediaItems.size,
                        mediaItems = mediaItems
                    )
                }

                emit(groups)
            }
        }

    override suspend fun getPersonMediaItems(
        clusterId: String
    ): List<MediaItem> =
        withContext(Dispatchers.IO) {

            val ids =
                aiDao.getMediaIdsForPersonCluster(clusterId)

            ids.mapNotNull {
                getMediaById(it)
            }
        }

    override suspend fun runAiPipelineOnMedia(
        context: Context,
        mediaItem: MediaItem
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {

            val executor =
                AiPipelineExecutor(context, aiDao)

            executor.processMediaItem(mediaItem)
        }

    override suspend fun runFullGalleryAiScan(
        context: Context
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {

            try {

                val allMedia =
                    getAllMediaItems()

                val executor =
                    AiPipelineExecutor(context, aiDao)

                val total =
                    allMedia.size

                allMedia.forEachIndexed { index, item ->

                    _indexingProgress.value =
                        IndexingProgress(
                            status = IndexingStatus.INDEXING_EXIF,
                            scannedCount = index + 1,
                            totalCount = total,
                            currentFileName = item.fileName,
                            message =
                                "Running AI analysis (${index + 1}/$total)"
                        )

                    executor.processMediaItem(item)
                }

                val clusterEngine =
                    FaceClusterEngine(context, aiDao)

                clusterEngine.clusterAllFaces()

                val duplicateDetector =
                    DuplicateDetector(aiDao)

                duplicateDetector.detectAndPersistDuplicates()

                _indexingProgress.value =
                    IndexingProgress(
                        status = IndexingStatus.COMPLETED,
                        scannedCount = total,
                        totalCount = total,
                        message = "AI gallery scan complete"
                    )

                Result.success(true)

            } catch (e: Exception) {

                _indexingProgress.value =
                    IndexingProgress(
                        status = IndexingStatus.ERROR,
                        message =
                            "AI scan failed: ${e.localizedMessage}"
                    )

                Result.failure(e)
            }
        }

    override suspend fun superResolveImage(
        context: Context,
        mediaItem: MediaItem,
        scaleFactor: Int,
        onProgress: (Float) -> Unit
    ): Result<String> =
        withContext(Dispatchers.IO) {

            try {

                val uri =
                    Uri.parse(mediaItem.uriString)

                val inputStream =
                    context.contentResolver
                        .openInputStream(uri)
                        ?: return@withContext Result.failure(
                            Exception("Cannot open stream")
                        )

                val srcBitmap =
                    BitmapFactory.decodeStream(inputStream)

                inputStream.close()

                if (srcBitmap == null) {
                    return@withContext Result.failure(
                        Exception("Failed to decode image")
                    )
                }

                val superResolver =
                    DefaultImageSuperResolver(context)

                superResolver.initialize()

                val config =
                    SuperResolutionConfig(
                        scale =
                            if (scaleFactor == 4) {
                                UpscaleScale.X4
                            } else {
                                UpscaleScale.X2
                            }
                    )

                val res =
                    superResolver.enhanceImageWithProgress(
                        srcBitmap,
                        config,
                        onProgress
                    )

                if (res.isFailure) {
                    return@withContext Result.failure(
                        res.exceptionOrNull()
                            ?: Exception("Super resolution failed")
                    )
                }

                val enhancedBitmap =
                    res.getOrThrow().enhancedBitmap

                val outputDir =
                    File(
                        context.cacheDir,
                        "super_resolution"
                    )

                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val outputFile =
                    File(
                        outputDir,
                        "sr_${scaleFactor}x_${mediaItem.id}.jpg"
                    )

                FileOutputStream(outputFile).use { out ->

                    enhancedBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        out
                    )
                }

                Result.success(
                    outputFile.absolutePath
                )

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
}
