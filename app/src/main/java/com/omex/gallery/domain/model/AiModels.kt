package com.omex.gallery.domain.model

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

data class AiOcrText(
    val id: Long = 0,
    val mediaId: Long,
    val extractedText: String,
    val language: String? = null,
    val processingStatus: String = "COMPLETED",
    val modelVersion: String = "1.0.0"
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
    val ocrText: AiOcrText? = null,
    val duplicateGroupId: String? = null
)

enum class DateFilterOption {
    ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS, THIS_YEAR, CUSTOM
}

enum class FileSizeFilterOption {
    ALL, LESS_THAN_1MB, BETWEEN_1_5MB, BETWEEN_5_50MB, GREATER_THAN_50MB
}

enum class DimensionFilterOption {
    ALL, SMALL, MEDIUM, HIGH_RES
}

data class SearchFilterState(
    val query: String = "",
    val cameraModel: String? = null,
    val cameraMake: String? = null,
    val mlCategory: String? = null,
    val mlLabel: String? = null,
    val categoryId: String? = null,
    val selectedCategoryIds: Set<String> = if (!categoryId.isNullOrBlank()) setOf(categoryId) else emptySet(),
    val isVideo: Boolean? = null,
    val isFavorite: Boolean? = null,
    val dateFilterOption: DateFilterOption = DateFilterOption.ALL,
    val startDateMs: Long? = null,
    val endDateMs: Long? = null,
    val fileSizeOption: FileSizeFilterOption = FileSizeFilterOption.ALL,
    val selectedExtensions: Set<String> = emptySet(),
    val dimensionOption: DimensionFilterOption = DimensionFilterOption.ALL,
    val isGpsOnly: Boolean = false
) {
    val allSelectedCategories: Set<String>
        get() {
            val result = selectedCategoryIds.toMutableSet()
            if (!categoryId.isNullOrBlank()) {
                result.add(categoryId)
            }
            return result
        }

    val activeFilterCount: Int
        get() {
            var count = 0
            if (query.isNotBlank()) count++
            if (!cameraModel.isNullOrBlank()) count++
            if (!cameraMake.isNullOrBlank()) count++
            if (!mlCategory.isNullOrBlank()) count++
            if (!mlLabel.isNullOrBlank()) count++
            if (allSelectedCategories.isNotEmpty()) count += allSelectedCategories.size
            if (isVideo != null) count++
            if (isFavorite != null) count++
            if (dateFilterOption != DateFilterOption.ALL) count++
            if (fileSizeOption != FileSizeFilterOption.ALL) count++
            if (selectedExtensions.isNotEmpty()) count += selectedExtensions.size
            if (dimensionOption != DimensionFilterOption.ALL) count++
            if (isGpsOnly) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0

    fun clearAll(): SearchFilterState = SearchFilterState()
}

data class SearchFilterOptions(
    val cameraModels: List<String> = emptyList(),
    val cameraMakes: List<String> = emptyList(),
    val mlCategories: List<String> = emptyList(),
    val mlLabels: List<String> = emptyList()
)
