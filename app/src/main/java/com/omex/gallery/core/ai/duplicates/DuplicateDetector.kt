package com.omex.gallery.core.ai.duplicates

import com.omex.gallery.core.data.local.AiDao
import com.omex.gallery.core.data.local.DuplicateGroupEntity
import com.omex.gallery.core.data.local.DuplicateMemberEntity
import com.omex.gallery.core.data.local.ImageMetadataEntity
import com.omex.gallery.core.hash.DefaultPerceptualHasher
import java.util.UUID

enum class DuplicateType {
    EXACT,
    NEAR,
    VISUAL
}

data class DuplicateGroup(
    val groupId: String,
    val type: DuplicateType,
    val members: List<DuplicateMember>
)

data class DuplicateMember(
    val mediaId: Long,
    val similarityScore: Float
)

class DuplicateDetector(
    private val aiDao: AiDao,
    private val hasher: DefaultPerceptualHasher = DefaultPerceptualHasher()
) {

    suspend fun detectAndPersistDuplicates(): List<DuplicateGroup> {
        val metadataList = aiDao.getAllImageMetadata()
        if (metadataList.size < 2) return emptyList()

        aiDao.clearDuplicateMembers()
        aiDao.clearDuplicateGroups()

        val groups = mutableListOf<DuplicateGroup>()
        val processedMediaIds = mutableSetOf<Long>()

        for (i in metadataList.indices) {
            val base = metadataList[i]
            if (processedMediaIds.contains(base.mediaId)) continue

            val exactMatches = mutableListOf<ImageMetadataEntity>()
            val nearMatches = mutableListOf<ImageMetadataEntity>()
            val visualMatches = mutableListOf<ImageMetadataEntity>()

            for (j in i + 1 until metadataList.size) {
                val candidate = metadataList[j]
                if (processedMediaIds.contains(candidate.mediaId)) continue

                // Check exact SHA-256 match
                if (base.sha256Hash.isNotEmpty() && base.sha256Hash == candidate.sha256Hash) {
                    exactMatches.add(candidate)
                    continue
                }

                // Check dHash & pHash Hamming distance
                val dDist = hasher.hammingDistance(base.dHash, candidate.dHash)
                val pDist = hasher.hammingDistance(base.pHash, candidate.pHash)

                if (dDist == 0 && pDist == 0) {
                    exactMatches.add(candidate)
                } else if (dDist <= 5 || pDist <= 5) {
                    nearMatches.add(candidate)
                } else if (dDist <= 12 || pDist <= 12) {
                    visualMatches.add(candidate)
                }
            }

            if (exactMatches.isNotEmpty()) {
                val groupId = "group_exact_${UUID.randomUUID()}"
                val members = mutableListOf(DuplicateMember(base.mediaId, 1.0f))
                exactMatches.forEach {
                    members.add(DuplicateMember(it.mediaId, 1.0f))
                    processedMediaIds.add(it.mediaId)
                }
                processedMediaIds.add(base.mediaId)

                val group = DuplicateGroup(groupId, DuplicateType.EXACT, members)
                groups.add(group)
                persistGroup(group)
            } else if (nearMatches.isNotEmpty()) {
                val groupId = "group_near_${UUID.randomUUID()}"
                val members = mutableListOf(DuplicateMember(base.mediaId, 1.0f))
                nearMatches.forEach {
                    val score = 1.0f - (hasher.hammingDistance(base.pHash, it.pHash) / 64.0f)
                    members.add(DuplicateMember(it.mediaId, score))
                    processedMediaIds.add(it.mediaId)
                }
                processedMediaIds.add(base.mediaId)

                val group = DuplicateGroup(groupId, DuplicateType.NEAR, members)
                groups.add(group)
                persistGroup(group)
            } else if (visualMatches.isNotEmpty()) {
                val groupId = "group_visual_${UUID.randomUUID()}"
                val members = mutableListOf(DuplicateMember(base.mediaId, 1.0f))
                visualMatches.forEach {
                    val score = 1.0f - (hasher.hammingDistance(base.pHash, it.pHash) / 64.0f)
                    members.add(DuplicateMember(it.mediaId, score))
                    processedMediaIds.add(it.mediaId)
                }
                processedMediaIds.add(base.mediaId)

                val group = DuplicateGroup(groupId, DuplicateType.VISUAL, members)
                groups.add(group)
                persistGroup(group)
            }
        }

        return groups
    }

    private suspend fun persistGroup(group: DuplicateGroup) {
        aiDao.insertDuplicateGroup(
            DuplicateGroupEntity(
                groupId = group.groupId,
                groupType = group.type.name,
                createdTimestamp = System.currentTimeMillis()
            )
        )
        aiDao.insertDuplicateMembers(
            group.members.map {
                DuplicateMemberEntity(
                    groupId = group.groupId,
                    mediaId = it.mediaId,
                    similarityScore = it.similarityScore
                )
            }
        )
    }
}
