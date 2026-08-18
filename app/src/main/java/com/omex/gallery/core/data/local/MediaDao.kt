package com.omex.gallery.core.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local media query, insertion, metadata indexing, and status updates.
 */
@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY dateTaken DESC")
    fun getAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY dateTaken DESC")
    fun getAllMediaPagingSource(): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items ORDER BY dateTaken DESC")
    suspend fun getAllMediaList(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 ORDER BY dateTaken DESC")
    fun getPhotos(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 ORDER BY dateTaken DESC")
    fun getPhotosPagingSource(): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 ORDER BY dateTaken DESC")
    fun getVideos(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 ORDER BY dateTaken DESC")
    fun getVideosPagingSource(): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateTaken DESC")
    fun getFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateTaken DESC")
    fun getFavoritesPagingSource(): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaItemEntity?

    @Query("""
        SELECT * FROM media_items m
        WHERE 
            (:query = '' OR (
                m.fileName LIKE '%' || :query || '%' OR
                m.filePath LIKE '%' || :query || '%' OR
                m.cameraMake LIKE '%' || :query || '%' OR
                m.cameraModel LIKE '%' || :query || '%' OR
                m.id IN (SELECT mediaId FROM ocr_text_results WHERE extractedText LIKE '%' || :query || '%') OR
                m.id IN (SELECT mediaId FROM image_classifications WHERE label LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') OR
                m.id IN (SELECT mediaId FROM detected_objects WHERE labelName LIKE '%' || :query || '%') OR
                m.id IN (SELECT mediaId FROM media_item_category_cross_ref WHERE categoryId LIKE '%' || :query || '%' OR (:categoryAlias != '' AND categoryId = :categoryAlias))
            ))
        ORDER BY m.dateTaken DESC
    """)
    fun searchMedia(query: String, categoryAlias: String = ""): Flow<List<MediaItemEntity>>

    @Query("""
        SELECT * FROM media_items m
        WHERE 
            (:query = '' OR (
                m.fileName LIKE '%' || :query || '%' OR
                m.filePath LIKE '%' || :query || '%' OR
                m.cameraMake LIKE '%' || :query || '%' OR
                m.cameraModel LIKE '%' || :query || '%' OR
                m.id IN (SELECT mediaId FROM ocr_text_results WHERE extractedText LIKE '%' || :query || '%') OR
                m.id IN (SELECT mediaId FROM image_classifications WHERE label LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') OR
                m.id IN (SELECT mediaId FROM detected_objects WHERE labelName LIKE '%' || :query || '%') OR
                m.id IN (SELECT mediaId FROM media_item_category_cross_ref WHERE categoryId LIKE '%' || :query || '%' OR (:categoryAlias != '' AND categoryId = :categoryAlias))
            ))
            AND (:cameraModel IS NULL OR :cameraModel = '' OR m.cameraModel = :cameraModel)
            AND (:cameraMake IS NULL OR :cameraMake = '' OR m.cameraMake = :cameraMake)
            AND (:mlCategory IS NULL OR :mlCategory = '' OR m.id IN (SELECT mediaId FROM image_classifications WHERE category LIKE '%' || :mlCategory || '%'))
            AND (:mlLabel IS NULL OR :mlLabel = '' OR m.id IN (SELECT mediaId FROM image_classifications WHERE label LIKE '%' || :mlLabel || '%') OR m.id IN (SELECT mediaId FROM detected_objects WHERE labelName LIKE '%' || :mlLabel || '%'))
            AND (:categoryId IS NULL OR :categoryId = '' OR m.id IN (SELECT mediaId FROM media_item_category_cross_ref WHERE categoryId = :categoryId))
            AND (:categoryIdsCount = 0 OR m.id IN (SELECT mediaId FROM media_item_category_cross_ref WHERE categoryId IN (:categoryIds)))
            AND (:isVideo IS NULL OR m.isVideo = :isVideo)
            AND (:isFavorite IS NULL OR m.isFavorite = :isFavorite)
            AND (:minDateMs IS NULL OR (CASE WHEN m.dateTaken > 0 THEN m.dateTaken ELSE m.dateModified END) >= :minDateMs)
            AND (:maxDateMs IS NULL OR (CASE WHEN m.dateTaken > 0 THEN m.dateTaken ELSE m.dateModified END) <= :maxDateMs)
            AND (:minSizeBytes IS NULL OR m.sizeBytes >= :minSizeBytes)
            AND (:maxSizeBytes IS NULL OR m.sizeBytes <= :maxSizeBytes)
            AND (:minPixels IS NULL OR (m.width * m.height) >= :minPixels)
            AND (:maxPixels IS NULL OR (m.width * m.height) <= :maxPixels)
            AND (:extensionsCount = 0 OR UPPER(SUBSTR(m.fileName, -3)) IN (:extensions) OR UPPER(SUBSTR(m.fileName, -4)) IN (:extensions))
            AND (:gpsOnly = 0 OR (m.latitude IS NOT NULL AND m.longitude IS NOT NULL))
        ORDER BY m.dateTaken DESC
    """)
    fun searchMediaAdvanced(
        query: String = "",
        categoryAlias: String = "",
        cameraModel: String? = null,
        cameraMake: String? = null,
        mlCategory: String? = null,
        mlLabel: String? = null,
        categoryId: String? = null,
        categoryIds: List<String> = emptyList(),
        categoryIdsCount: Int = 0,
        isVideo: Boolean? = null,
        isFavorite: Boolean? = null,
        minDateMs: Long? = null,
        maxDateMs: Long? = null,
        minSizeBytes: Long? = null,
        maxSizeBytes: Long? = null,
        minPixels: Long? = null,
        maxPixels: Long? = null,
        extensions: List<String> = emptyList(),
        extensionsCount: Int = 0,
        gpsOnly: Int = 0
    ): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT cameraModel FROM media_items WHERE cameraModel IS NOT NULL AND cameraModel != '' UNION SELECT DISTINCT cameraModel FROM image_metadata WHERE cameraModel IS NOT NULL AND cameraModel != ''")
    fun getDistinctCameraModels(): Flow<List<String>>

    @Query("SELECT DISTINCT cameraMake FROM media_items WHERE cameraMake IS NOT NULL AND cameraMake != '' UNION SELECT DISTINCT cameraMake FROM image_metadata WHERE cameraMake IS NOT NULL AND cameraMake != ''")
    fun getDistinctCameraMakes(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM media_items")
    fun getMediaCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE isVideo = 0")
    suspend fun getPhotosCount(): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE isVideo = 1")
    suspend fun getVideosCount(): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE isIndexed = 1")
    fun getIndexedCountFlow(): Flow<Int>

    @Query("SELECT * FROM media_items WHERE isIndexed = 0 ORDER BY dateTaken DESC")
    suspend fun getUnindexedMediaList(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isAiProcessed = 0 AND isVideo = 0 ORDER BY dateTaken DESC")
    suspend fun getPendingAiMediaList(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity)

    @Update
    suspend fun update(item: MediaItemEntity)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET thumbnailPath = :thumbnailPath WHERE id = :id")
    suspend fun updateThumbnail(id: Long, thumbnailPath: String)

    @Query("UPDATE media_items SET isAiProcessed = :isAiProcessed WHERE id = :id")
    suspend fun updateAiProcessedStatus(id: Long, isAiProcessed: Boolean)

    @Query("DELETE FROM media_items WHERE id NOT IN (:validIds)")
    suspend fun deleteRemovedMedia(validIds: List<Long>)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaByIds(ids: List<Long>): List<MediaItemEntity>

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
