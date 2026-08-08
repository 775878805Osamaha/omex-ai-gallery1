package com.example.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDao {

    // Classifications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassifications(classifications: List<ImageClassificationEntity>)

    @Query("SELECT * FROM image_classifications WHERE mediaId = :mediaId ORDER BY confidence DESC")
    suspend fun getClassificationsForMedia(mediaId: Long): List<ImageClassificationEntity>

    // Detected Objects
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjects(objects: List<DetectedObjectEntity>)

    @Query("SELECT * FROM detected_objects WHERE mediaId = :mediaId ORDER BY score DESC")
    suspend fun getObjectsForMedia(mediaId: Long): List<DetectedObjectEntity>

    // ML Search Filter Metadata
    @Query("SELECT DISTINCT category FROM image_classifications WHERE category IS NOT NULL AND category != ''")
    fun getDistinctMlCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT label FROM image_classifications WHERE label IS NOT NULL AND label != '' UNION SELECT DISTINCT labelName FROM detected_objects WHERE labelName IS NOT NULL AND labelName != ''")
    fun getDistinctMlLabels(): Flow<List<String>>

    // Faces & Embeddings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<DetectedFaceEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: FaceEmbeddingEntity)

    @Query("SELECT * FROM detected_faces WHERE mediaId = :mediaId")
    suspend fun getFacesForMedia(mediaId: Long): List<DetectedFaceEntity>

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllEmbeddings(): List<FaceEmbeddingEntity>

    @Query("UPDATE detected_faces SET clusterId = :clusterId WHERE id = :faceId")
    suspend fun updateFaceClusterId(faceId: Long, clusterId: String)

    @Query("SELECT DISTINCT clusterId FROM detected_faces WHERE clusterId IS NOT NULL")
    fun getAllPersonClusterIds(): Flow<List<String>>

    @Query("SELECT DISTINCT mediaId FROM detected_faces WHERE clusterId = :clusterId")
    suspend fun getMediaIdsForPersonCluster(clusterId: String): List<Long>

    // Metadata & Hashes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageMetadata(metadata: ImageMetadataEntity)

    @Query("SELECT * FROM image_metadata WHERE mediaId = :mediaId")
    suspend fun getImageMetadata(mediaId: Long): ImageMetadataEntity?

    @Query("SELECT * FROM image_metadata")
    suspend fun getAllImageMetadata(): List<ImageMetadataEntity>

    // Duplicates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuplicateGroup(group: DuplicateGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuplicateMembers(members: List<DuplicateMemberEntity>)

    @Query("DELETE FROM duplicate_groups")
    suspend fun clearDuplicateGroups()

    @Query("DELETE FROM duplicate_members")
    suspend fun clearDuplicateMembers()

    @Query("SELECT * FROM duplicate_groups")
    fun getAllDuplicateGroups(): Flow<List<DuplicateGroupEntity>>

    @Query("SELECT * FROM duplicate_members WHERE groupId = :groupId")
    suspend fun getMembersForDuplicateGroup(groupId: String): List<DuplicateMemberEntity>

    @Query("SELECT * FROM duplicate_groups WHERE groupId = (SELECT groupId FROM duplicate_members WHERE mediaId = :mediaId LIMIT 1)")
    suspend fun getDuplicateGroupForMedia(mediaId: Long): DuplicateGroupEntity?

    // Cleanup
    @Query("DELETE FROM image_classifications WHERE mediaId = :mediaId")
    suspend fun deleteClassifications(mediaId: Long)

    @Query("DELETE FROM detected_objects WHERE mediaId = :mediaId")
    suspend fun deleteObjects(mediaId: Long)

    @Query("DELETE FROM detected_faces WHERE mediaId = :mediaId")
    suspend fun deleteFaces(mediaId: Long)

    @Query("DELETE FROM face_embeddings WHERE mediaId = :mediaId")
    suspend fun deleteEmbeddings(mediaId: Long)

    @Query("DELETE FROM image_metadata WHERE mediaId = :mediaId")
    suspend fun deleteMetadata(mediaId: Long)

    suspend fun deleteAiDataForMedia(mediaId: Long) {
        deleteClassifications(mediaId)
        deleteObjects(mediaId)
        deleteFaces(mediaId)
        deleteEmbeddings(mediaId)
        deleteMetadata(mediaId)
    }
}
