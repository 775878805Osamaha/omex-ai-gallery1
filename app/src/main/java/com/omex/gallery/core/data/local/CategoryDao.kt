package com.omex.gallery.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<MediaCategoryEntity>)

    @Query("SELECT * FROM media_categories")
    fun getAllCategories(): Flow<List<MediaCategoryEntity>>

    @Query("SELECT * FROM media_categories")
    suspend fun getAllCategoriesList(): List<MediaCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<MediaItemCategoryCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: MediaItemCategoryCrossRef)

    @Query("SELECT * FROM media_item_category_cross_ref")
    fun getAllCrossRefsFlow(): Flow<List<MediaItemCategoryCrossRef>>

    @Query("SELECT * FROM media_item_category_cross_ref")
    suspend fun getAllCrossRefs(): List<MediaItemCategoryCrossRef>

    @Query("DELETE FROM media_item_category_cross_ref WHERE mediaId = :mediaId")
    suspend fun clearCategoriesForMedia(mediaId: Long)

    @Query("DELETE FROM media_item_category_cross_ref WHERE mediaId IN (:mediaIds)")
    suspend fun clearCategoriesForMediaList(mediaIds: List<Long>)

    @Query("SELECT categoryId FROM media_item_category_cross_ref WHERE mediaId = :mediaId")
    suspend fun getCategoriesForMedia(mediaId: Long): List<String>

    @Query("SELECT categoryId FROM media_item_category_cross_ref WHERE mediaId = :mediaId")
    fun getCategoriesForMediaFlow(mediaId: Long): Flow<List<String>>

    @Query("""
        SELECT mediaId FROM media_item_category_cross_ref 
        WHERE categoryId IN (:categoryIds) 
        GROUP BY mediaId 
        HAVING COUNT(DISTINCT categoryId) = :categoryCount
    """)
    suspend fun getMediaIdsMatchingAllCategories(categoryIds: List<String>, categoryCount: Int): List<Long>

    @Query("""
        SELECT mediaId FROM media_item_category_cross_ref 
        WHERE categoryId IN (:categoryIds) 
        GROUP BY mediaId 
        HAVING COUNT(DISTINCT categoryId) = :categoryCount
    """)
    fun getMediaIdsMatchingAllCategoriesFlow(categoryIds: List<String>, categoryCount: Int): Flow<List<Long>>

    @Query("""
        SELECT COUNT(DISTINCT crossRef.mediaId) 
        FROM media_item_category_cross_ref crossRef
        INNER JOIN media_items m ON crossRef.mediaId = m.id
        WHERE crossRef.categoryId = :categoryId
    """)
    fun getCategoryMediaCountFlow(categoryId: String): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT crossRef.mediaId) 
        FROM media_item_category_cross_ref crossRef
        INNER JOIN media_items m ON crossRef.mediaId = m.id
        WHERE crossRef.categoryId = :categoryId
    """)
    suspend fun getCategoryMediaCount(categoryId: String): Int

    @Query("""
        SELECT m.* FROM media_items m
        INNER JOIN media_item_category_cross_ref crossRef ON m.id = crossRef.mediaId
        WHERE crossRef.categoryId = :categoryId
        ORDER BY m.dateTaken DESC LIMIT 1
    """)
    suspend fun getLatestMediaForCategory(categoryId: String): MediaItemEntity?

    @Query("""
        SELECT m.* FROM media_items m
        INNER JOIN media_item_category_cross_ref crossRef ON m.id = crossRef.mediaId
        WHERE crossRef.categoryId = :categoryId
        ORDER BY m.dateTaken DESC LIMIT 1
    """)
    fun getLatestMediaForCategoryFlow(categoryId: String): Flow<MediaItemEntity?>
}
