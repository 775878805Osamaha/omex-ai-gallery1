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

    @Query("SELECT * FROM media_items WHERE fileName LIKE '%' || :query || '%' OR cameraModel LIKE '%' || :query || '%' ORDER BY dateTaken DESC")
    fun searchMedia(query: String): Flow<List<MediaItemEntity>>

    @Query("""
        SELECT DISTINCT m.* FROM media_items m
        LEFT JOIN image_metadata meta ON m.id = meta.mediaId
        LEFT JOIN image_classifications c ON m.id = c.mediaId
        LEFT JOIN detected_objects o ON m.id = o.mediaId
        WHERE 
            (:query = '' OR (
                m.fileName LIKE '%' || :query || '%' OR
                m.cameraModel LIKE '%' || :query || '%' OR
                m.cameraMake LIKE '%' || :query || '%' OR
                m.iso LIKE '%' || :query || '%' OR
                m.aperture LIKE '%' || :query || '%' OR
                m.focalLength LIKE '%' || :query || '%' OR
                meta.cameraMake LIKE '%' || :query || '%' OR
                meta.cameraModel LIKE '%' || :query || '%' OR
                meta.iso LIKE '%' || :query || '%' OR
                meta.aperture LIKE '%' || :query || '%' OR
                meta.focalLength LIKE '%' || :query || '%' OR
                c.label LIKE '%' || :query || '%' OR
                c.category LIKE '%' || :query || '%' OR
                o.labelName LIKE '%' || :query || '%'
            ))
            AND (:cameraModel IS NULL OR :cameraModel = '' OR m.cameraModel = :cameraModel OR meta.cameraModel = :cameraModel)
            AND (:cameraMake IS NULL OR :cameraMake = '' OR m.cameraMake = :cameraMake OR meta.cameraMake = :cameraMake)
            AND (:mlCategory IS NULL OR :mlCategory = '' OR c.category LIKE '%' || :mlCategory || '%')
            AND (:mlLabel IS NULL OR :mlLabel = '' OR c.label LIKE '%' || :mlLabel || '%' OR o.labelName LIKE '%' || :mlLabel || '%')
            AND (:gpsOnly = 0 OR (m.latitude IS NOT NULL AND m.longitude IS NOT NULL) OR (meta.latitude IS NOT NULL AND meta.longitude IS NOT NULL))
        ORDER BY m.dateTaken DESC
    """)
    fun searchMediaAdvanced(
        query: String = "",
        cameraModel: String? = null,
        cameraMake: String? = null,
        mlCategory: String? = null,
        mlLabel: String? = null,
        gpsOnly: Int = 0
    ): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT cameraModel FROM media_items WHERE cameraModel IS NOT NULL AND cameraModel != '' UNION SELECT DISTINCT cameraModel FROM image_metadata WHERE cameraModel IS NOT NULL AND cameraModel != ''")
    fun getDistinctCameraModels(): Flow<List<String>>

    @Query("SELECT DISTINCT cameraMake FROM media_items WHERE cameraMake IS NOT NULL AND cameraMake != '' UNION SELECT DISTINCT cameraMake FROM image_metadata WHERE cameraMake IS NOT NULL AND cameraMake != ''")
    fun getDistinctCameraMakes(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM media_items")
    fun getMediaCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE isIndexed = 1")
    fun getIndexedCountFlow(): Flow<Int>

    @Query("SELECT * FROM media_items WHERE isIndexed = 0 ORDER BY dateTaken DESC")
    suspend fun getUnindexedMediaList(): List<MediaItemEntity>

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

    @Query("DELETE FROM media_items WHERE id NOT IN (:validIds)")
    suspend fun deleteRemovedMedia(validIds: List<Long>)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
