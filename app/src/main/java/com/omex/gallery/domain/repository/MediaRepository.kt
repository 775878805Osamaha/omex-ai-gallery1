package com.omex.gallery.domain.model

import android.content.Context
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for Gallery repository with full AI capabilities.
 */
interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaItem>>
    fun getAllMediaPaged(): Flow<PagingData<MediaItem>>
    fun getPhotos(): Flow<List<MediaItem>>
    fun getPhotosPaged(): Flow<PagingData<MediaItem>>
    fun getVideos(): Flow<List<MediaItem>>
    fun getVideosPaged(): Flow<PagingData<MediaItem>>
    fun getFavorites(): Flow<List<MediaItem>>
    fun getFavoritesPaged(): Flow<PagingData<MediaItem>>
    fun searchMedia(query: String): Flow<List<MediaItem>>
    fun searchMediaAdvanced(
        filterState: SearchFilterState
    ): Flow<List<MediaItem>>
    fun getSearchFilterOptions(): Flow<SearchFilterOptions>
    fun getIndexingProgress(): Flow<IndexingProgress>
    
    suspend fun getMediaById(id: Long): MediaItem?
    suspend fun getAllMediaItems(): List<MediaItem>
    suspend fun insertMediaItems(items: List<MediaItem>)
    suspend fun deleteMediaItem(id: Long)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun scanAndIndexGallery(isFullReindex: Boolean = false): Result<Int>
    suspend fun regenerateThumbnails(): Result<Int>

    // AI Query Methods
    suspend fun getClassificationsForMedia(mediaId: Long): List<AiClassification>
    suspend fun getObjectsForMedia(mediaId: Long): List<AiObject>
    suspend fun getFacesForMedia(mediaId: Long): List<AiFace>
    suspend fun getMetadataForMedia(mediaId: Long): AiMetadata?
    suspend fun getMediaItemWithAi(mediaId: Long): MediaItemWithAi?

    fun getDuplicateGroups(): Flow<List<DuplicateGroupWithMedia>>
    fun getPersonGroups(): Flow<List<PersonGroup>>
    suspend fun getPersonMediaItems(clusterId: String): List<MediaItem>

    // AI Pipeline & Super Resolution
    suspend fun runAiPipelineOnMedia(context: Context, mediaItem: MediaItem): Result<Boolean>
    suspend fun runFullGalleryAiScan(context: Context): Result<Boolean>
    suspend fun superResolveImage(
        context: Context,
        mediaItem: MediaItem,
        scaleFactor: Int = 2,
        onProgress: (Float) -> Unit
    ): Result<String>
}
