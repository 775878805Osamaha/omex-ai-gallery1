package com.example.domain.model

data class AiClassification(
    val id: Long = 0,
    val mediaId: Long,
    val classId: Int,
    val label: String,
    val category: String,
    val confidence: Float
)

data class AiObject(
    val id: Long = 0,
    val mediaId: Long,
    val classId: Int,
    val labelName: String,
    val score: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class AiFace(
    val id: Long = 0,
    val mediaId: Long,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val clusterId: String? = null
)

data class AiMetadata(
    val mediaId: Long,
    val sha256Hash: String,
    val aHash: Long,
    val dHash: Long,
    val pHash: Long,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val iso: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class DuplicateGroupWithMedia(
    val groupId: String,
    val groupType: String,
    val members: List<DuplicateMemberWithMedia>
)

data class DuplicateMemberWithMedia(
    val mediaItem: MediaItem,
    val similarityScore: Float
)

data class PersonGroup(
    val clusterId: String,
    val personName: String,
    val faceCount: Int,
    val mediaItems: List<MediaItem>
)

data class MediaItemWithAi(
    val mediaItem: MediaItem,
    val classifications: List<AiClassification>,
    val objects: List<AiObject>,
    val faces: List<AiFace>,
    val metadata: AiMetadata? = null,
    val duplicateGroupId: String? = null
)

data class SearchFilterState(
    val query: String = "",
    val cameraModel: String? = null,
    val cameraMake: String? = null,
    val mlCategory: String? = null,
    val mlLabel: String? = null,
    val isGpsOnly: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (query.isNotBlank()) count++
            if (!cameraModel.isNullOrBlank()) count++
            if (!cameraMake.isNullOrBlank()) count++
            if (!mlCategory.isNullOrBlank()) count++
            if (!mlLabel.isNullOrBlank()) count++
            if (isGpsOnly) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0
}

data class SearchFilterOptions(
    val cameraModels: List<String> = emptyList(),
    val cameraMakes: List<String> = emptyList(),
    val mlCategories: List<String> = emptyList(),
    val mlLabels: List<String> = emptyList()
)
