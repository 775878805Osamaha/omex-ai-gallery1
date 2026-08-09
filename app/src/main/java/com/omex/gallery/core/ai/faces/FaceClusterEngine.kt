package com.omex.gallery.core.ai.faces

import android.content.Context
import com.omex.gallery.core.data.local.AiDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class FaceClusterEngine(
    private val context: Context,
    private val aiDao: AiDao,
    private val faceEmbedder: DefaultFaceEmbedder = DefaultFaceEmbedder(context)
) {

    suspend fun clusterAllFaces(threshold: Float = 0.65f) = withContext(Dispatchers.IO) {
        faceEmbedder.initialize()
        val embeddingEntities = aiDao.getAllEmbeddings()
        if (embeddingEntities.isEmpty()) return@withContext

        val faceIdToEntity = embeddingEntities.associateBy { it.faceId }
        val faceEmbeddings = embeddingEntities.map { entity ->
            FaceEmbedding(
                vector = jsonToFloatArray(entity.vectorJson),
                dimension = entity.dimension
            )
        }

        val clusters = faceEmbedder.clusterFaces(faceEmbeddings, threshold)

        for (cluster in clusters) {
            for (emb in faceEmbeddings) {
                if (faceEmbedder.computeSimilarity(emb, cluster.centroid) >= threshold) {
                    val matchingEntity = faceIdToEntity.values.firstOrNull {
                        jsonToFloatArray(it.vectorJson).contentEquals(emb.vector)
                    }
                    if (matchingEntity != null) {
                        aiDao.updateFaceClusterId(matchingEntity.faceId, cluster.clusterId)
                    }
                }
            }
        }
    }

    private fun jsonToFloatArray(jsonString: String): FloatArray {
        return try {
            val jsonArray = JSONArray(jsonString)
            FloatArray(jsonArray.length()) { i ->
                jsonArray.getDouble(i).toFloat()
            }
        } catch (e: Exception) {
            FloatArray(512)
        }
    }
}
