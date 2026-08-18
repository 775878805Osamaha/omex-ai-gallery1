package com.omex.gallery.ui.feature_storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

enum class StorageGroupingMode {
    BY_FILE_TYPE,
    BY_CLASSIFICATION,
    BY_MEDIA_KIND,
    BY_SIZE
}

enum class StorageMetricMode {
    SIZE,
    COUNT
}

enum class StorageQuickFilter {
    ALL,
    LARGE_VIDEOS_50MB,
    LARGE_VIDEOS_100MB,
    LARGE_PHOTOS_5MB,
    LARGE_FILES_5MB,
    LARGE_FILES_50MB,
    LARGE_FILES_100MB,
    DUPLICATES,
    SCREENSHOTS,
    LARGEST_FILES
}

enum class StorageSortMode {
    LARGEST_FIRST,
    SMALLEST_FIRST,
    NEWEST_FIRST,
    OLDEST_FIRST,
    BY_TYPE
}

data class StorageDistributionItem(
    val key: String,
    val label: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val count: Int,
    val colorHex: String,
    val percentage: Float
)

data class StorageQuickCleanSuggestion(
    val filter: StorageQuickFilter,
    val title: String,
    val reason: String,
    val count: Int,
    val sizeBytes: Long,
    val formattedSize: String,
    val iconName: String
)

data class StorageAnalysisState(
    val totalSizeBytes: Long = 0L,
    val formattedTotalSize: String = "0 B",
    val totalCount: Int = 0,
    val photosSizeBytes: Long = 0L,
    val photosCount: Int = 0,
    val videosSizeBytes: Long = 0L,
    val videosCount: Int = 0,
    val otherSizeBytes: Long = 0L,
    val otherCount: Int = 0,
    val duplicatesSizeBytes: Long = 0L,
    val duplicatesCount: Int = 0,
    val reclaimableSizeBytes: Long = 0L,
    val formattedReclaimableSize: String = "0 B",
    val screenshotsSizeBytes: Long = 0L,
    val screenshotsCount: Int = 0,
    val distributionItems: List<StorageDistributionItem> = emptyList(),
    val largestMediaItems: List<MediaItem> = emptyList(),
    val filteredMediaItems: List<MediaItem> = emptyList(),
    val quickCleanSuggestions: List<StorageQuickCleanSuggestion> = emptyList(),
    val selectedGroupingMode: StorageGroupingMode = StorageGroupingMode.BY_FILE_TYPE,
    val selectedMetricMode: StorageMetricMode = StorageMetricMode.SIZE,
    val selectedSliceKey: String? = null,
    val selectedSliceLabel: String? = null,
    val quickFilter: StorageQuickFilter = StorageQuickFilter.ALL,
    val selectedSortMode: StorageSortMode = StorageSortMode.LARGEST_FIRST,
    val selectedItemIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class StorageAnalyzerViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _selectedGroupingMode = MutableStateFlow(StorageGroupingMode.BY_FILE_TYPE)
    val selectedGroupingMode: StateFlow<StorageGroupingMode> = _selectedGroupingMode.asStateFlow()

    private val _selectedMetricMode = MutableStateFlow(StorageMetricMode.SIZE)
    val selectedMetricMode: StateFlow<StorageMetricMode> = _selectedMetricMode.asStateFlow()

    private val _selectedSliceKey = MutableStateFlow<String?>(null)
    val selectedSliceKey: StateFlow<String?> = _selectedSliceKey.asStateFlow()

    private val _selectedSliceLabel = MutableStateFlow<String?>(null)
    val selectedSliceLabel: StateFlow<String?> = _selectedSliceLabel.asStateFlow()

    private val _quickFilter = MutableStateFlow(StorageQuickFilter.ALL)
    val quickFilter: StateFlow<StorageQuickFilter> = _quickFilter.asStateFlow()

    private val _selectedSortMode = MutableStateFlow(StorageSortMode.LARGEST_FIRST)
    val selectedSortMode: StateFlow<StorageSortMode> = _selectedSortMode.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val allMediaFlow = repository.getAllMedia()
    val duplicateGroups: Flow<List<DuplicateGroupWithMedia>> = repository.getDuplicateGroups()

    val uiState: StateFlow<StorageAnalysisState> = combine(
        allMediaFlow,
        duplicateGroups,
        _selectedGroupingMode,
        _selectedMetricMode,
        _selectedSliceKey,
        _quickFilter,
        _selectedSortMode,
        _selectedItemIds,
        _userMessage,
        _isLoading
    ) { rawArgs ->
        @Suppress("UNCHECKED_CAST")
        val mediaList = rawArgs[0] as List<MediaItem>
        @Suppress("UNCHECKED_CAST")
        val duplicates = rawArgs[1] as List<DuplicateGroupWithMedia>
        val groupingMode = rawArgs[2] as StorageGroupingMode
        val metricMode = rawArgs[3] as StorageMetricMode
        val sliceKey = rawArgs[4] as String?
        val qFilter = rawArgs[5] as StorageQuickFilter
        val sortMode = rawArgs[6] as StorageSortMode
        @Suppress("UNCHECKED_CAST")
        val selectedIds = rawArgs[7] as Set<Long>
        val message = rawArgs[8] as String?
        val loading = rawArgs[9] as Boolean

        computeAnalysisState(
            mediaList = mediaList,
            duplicateGroups = duplicates,
            groupingMode = groupingMode,
            metricMode = metricMode,
            sliceKey = sliceKey,
            sliceLabel = _selectedSliceLabel.value,
            quickFilter = qFilter,
            sortMode = sortMode,
            selectedItemIds = selectedIds,
            userMessage = message,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StorageAnalysisState(isLoading = true)
    )

    fun setGroupingMode(mode: StorageGroupingMode) {
        _selectedGroupingMode.value = mode
        _selectedSliceKey.value = null
        _selectedSliceLabel.value = null
    }

    fun setMetricMode(metric: StorageMetricMode) {
        _selectedMetricMode.value = metric
    }

    fun setSortMode(sortMode: StorageSortMode) {
        _selectedSortMode.value = sortMode
    }

    fun selectSlice(key: String?, label: String?) {
        if (key.isNullOrBlank()) {
            _selectedSliceKey.value = null
            _selectedSliceLabel.value = null
        } else {
            _selectedSliceKey.value = key
            _selectedSliceLabel.value = label
        }
    }

    fun clearSliceSelection() {
        _selectedSliceKey.value = null
        _selectedSliceLabel.value = null
    }

    fun setQuickFilter(filter: StorageQuickFilter) {
        _quickFilter.value = filter
        _selectedSliceKey.value = null
        _selectedSliceLabel.value = null
    }

    fun toggleItemSelection(id: Long) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedItemIds.value = items.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun deleteSelectedItems(itemsToDelete: List<MediaItem>) {
        if (itemsToDelete.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val ids = itemsToDelete.map { it.id }
            val totalFreedBytes = itemsToDelete.sumOf { it.sizeBytes }
            val res = repository.deleteMediaItems(ids)
            _isLoading.value = false
            clearSelection()
            if (res.isSuccess) {
                _userMessage.value = "تم تحرير ${formatBytes(totalFreedBytes)} وحذف ${ids.size} ملف بنجاح"
            } else {
                _userMessage.value = "حدث خطأ أثناء حذف الملفات: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteSingleItem(item: MediaItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteMediaItems(listOf(item.id))
            _isLoading.value = false
            if (res.isSuccess) {
                _userMessage.value = "تم تحرير ${formatBytes(item.sizeBytes)} بنجاح"
            }
        }
    }

    /**
     * Cleans duplicate copies while preserving at least one original copy per group.
     */
    fun cleanDuplicateCopies(duplicates: List<DuplicateGroupWithMedia>) {
        viewModelScope.launch {
            val redundantIds = mutableListOf<Long>()
            var reclaimableBytes = 0L
            duplicates.forEach { group ->
                if (group.members.size > 1) {
                    val memberItems = group.members.map { it.mediaItem }
                    val original = selectOriginalMember(memberItems)
                    val redundant = memberItems.filter { it.id != original.id }
                    redundant.forEach { member ->
                        redundantIds.add(member.id)
                        reclaimableBytes += member.sizeBytes
                    }
                }
            }
            if (redundantIds.isNotEmpty()) {
                _isLoading.value = true
                val res = repository.deleteMediaItems(redundantIds)
                _isLoading.value = false
                if (res.isSuccess) {
                    _userMessage.value = "تم تنظيف ${redundantIds.size} ملف مكرر وتحرير ${formatBytes(reclaimableBytes)}"
                }
            }
        }
    }

    private fun computeAnalysisState(
        mediaList: List<MediaItem>,
        duplicateGroups: List<DuplicateGroupWithMedia>,
        groupingMode: StorageGroupingMode,
        metricMode: StorageMetricMode,
        sliceKey: String?,
        sliceLabel: String?,
        quickFilter: StorageQuickFilter,
        sortMode: StorageSortMode,
        selectedItemIds: Set<Long>,
        userMessage: String?,
        isLoading: Boolean
    ): StorageAnalysisState {
        val totalSize = mediaList.sumOf { it.sizeBytes }
        val totalCount = mediaList.size

        val photos = mediaList.filter { !it.isVideo && !isOtherDocumentOrArchive(it) }
        val videos = mediaList.filter { it.isVideo }
        val otherFiles = mediaList.filter { !it.isVideo && isOtherDocumentOrArchive(it) }
        val screenshots = mediaList.filter { inferCategory(it) == "SCREENSHOT" }

        val photosSize = photos.sumOf { it.sizeBytes }
        val videosSize = videos.sumOf { it.sizeBytes }
        val otherSize = otherFiles.sumOf { it.sizeBytes }
        val screenshotsSize = screenshots.sumOf { it.sizeBytes }

        // Duplicate savings calculation: sum of all non-primary duplicate members
        var duplicateTotalSize = 0L
        var duplicateCount = 0
        var reclaimableDuplicateBytes = 0L
        val duplicateMediaIds = mutableSetOf<Long>()

        duplicateGroups.forEach { group ->
            if (group.members.size > 1) {
                val memberItems = group.members.map { it.mediaItem }
                memberItems.forEach { duplicateMediaIds.add(it.id) }
                duplicateTotalSize += memberItems.sumOf { it.sizeBytes }
                val original = selectOriginalMember(memberItems)
                val redundant = memberItems.filter { it.id != original.id }
                reclaimableDuplicateBytes += redundant.sumOf { it.sizeBytes }
                duplicateCount += redundant.size
            }
        }

        // Color palette for charts
        val palette = listOf(
            "#00E5FF", // Cyan
            "#FFB300", // Amber
            "#BB86FC", // Purple
            "#00E676", // Green
            "#FF4081", // Pink
            "#2979FF", // Blue
            "#FF9100", // Orange
            "#1DE9B6", // Teal
            "#FF5252", // Coral Red
            "#7C4DFF", // Deep Purple
            "#E040FB", // Magenta
            "#64B5F6", // Light Blue
            "#81C784", // Light Green
            "#90A4AE"  // Grey Blue
        )

        val distributionItems = when (groupingMode) {
            StorageGroupingMode.BY_FILE_TYPE -> {
                val map = mediaList.groupBy { item ->
                    extractExtensionOrMime(item)
                }
                map.entries.sortedByDescending { if (metricMode == StorageMetricMode.SIZE) it.value.sumOf { m -> m.sizeBytes } else it.value.size.toLong() }
                    .mapIndexed { index, entry ->
                        val ext = entry.key
                        val list = entry.value
                        val size = list.sumOf { it.sizeBytes }
                        val count = list.size
                        val totalVal = if (metricMode == StorageMetricMode.SIZE) totalSize else totalCount.toLong()
                        val currentVal = if (metricMode == StorageMetricMode.SIZE) size else count.toLong()
                        val pct = if (totalVal > 0) currentVal.toFloat() / totalVal.toFloat() else 0f
                        StorageDistributionItem(
                            key = ext,
                            label = formatFileExtensionLabel(ext),
                            sizeBytes = size,
                            formattedSize = formatBytes(size),
                            count = count,
                            colorHex = palette[index % palette.size],
                            percentage = pct
                        )
                    }
            }

            StorageGroupingMode.BY_CLASSIFICATION -> {
                val map = mediaList.groupBy { item ->
                    inferCategory(item)
                }
                map.entries.sortedByDescending { if (metricMode == StorageMetricMode.SIZE) it.value.sumOf { m -> m.sizeBytes } else it.value.size.toLong() }
                    .mapIndexed { index, entry ->
                        val cat = entry.key
                        val list = entry.value
                        val size = list.sumOf { it.sizeBytes }
                        val count = list.size
                        val totalVal = if (metricMode == StorageMetricMode.SIZE) totalSize else totalCount.toLong()
                        val currentVal = if (metricMode == StorageMetricMode.SIZE) size else count.toLong()
                        val pct = if (totalVal > 0) currentVal.toFloat() / totalVal.toFloat() else 0f
                        StorageDistributionItem(
                            key = cat,
                            label = formatCategoryLabel(cat),
                            sizeBytes = size,
                            formattedSize = formatBytes(size),
                            count = count,
                            colorHex = palette[index % palette.size],
                            percentage = pct
                        )
                    }
            }

            StorageGroupingMode.BY_MEDIA_KIND -> {
                val items = mutableListOf<StorageDistributionItem>()
                val totalVal = if (metricMode == StorageMetricMode.SIZE) totalSize else totalCount.toLong()

                // Videos
                val vidVal = if (metricMode == StorageMetricMode.SIZE) videosSize else videos.size.toLong()
                val vidPct = if (totalVal > 0) vidVal.toFloat() / totalVal.toFloat() else 0f
                items.add(
                    StorageDistributionItem(
                        key = "VIDEOS",
                        label = "فيديوهات (Videos)",
                        sizeBytes = videosSize,
                        formattedSize = formatBytes(videosSize),
                        count = videos.size,
                        colorHex = "#00E5FF",
                        percentage = vidPct
                    )
                )

                // Photos
                val phoVal = if (metricMode == StorageMetricMode.SIZE) photosSize else photos.size.toLong()
                val phoPct = if (totalVal > 0) phoVal.toFloat() / totalVal.toFloat() else 0f
                items.add(
                    StorageDistributionItem(
                        key = "PHOTOS",
                        label = "صور (Photos)",
                        sizeBytes = photosSize,
                        formattedSize = formatBytes(photosSize),
                        count = photos.size,
                        colorHex = "#FFB300",
                        percentage = phoPct
                    )
                )

                // Duplicates
                if (duplicateCount > 0) {
                    val dupVal = if (metricMode == StorageMetricMode.SIZE) reclaimableDuplicateBytes else duplicateCount.toLong()
                    val dupPct = if (totalVal > 0) dupVal.toFloat() / totalVal.toFloat() else 0f
                    items.add(
                        StorageDistributionItem(
                            key = "DUPLICATES",
                            label = "مكررات (Duplicates)",
                            sizeBytes = reclaimableDuplicateBytes,
                            formattedSize = formatBytes(reclaimableDuplicateBytes),
                            count = duplicateCount,
                            colorHex = "#FF5252",
                            percentage = dupPct
                        )
                    )
                }

                // Other
                if (otherSize > 0 || otherFiles.isNotEmpty()) {
                    val othVal = if (metricMode == StorageMetricMode.SIZE) otherSize else otherFiles.size.toLong()
                    val othPct = if (totalVal > 0) othVal.toFloat() / totalVal.toFloat() else 0f
                    items.add(
                        StorageDistributionItem(
                            key = "OTHER",
                            label = "أخرى (Other Files)",
                            sizeBytes = otherSize,
                            formattedSize = formatBytes(otherSize),
                            count = otherFiles.size,
                            colorHex = "#90A4AE",
                            percentage = othPct
                        )
                    )
                }

                items
            }

            StorageGroupingMode.BY_SIZE -> {
                val sizeRanges = listOf(
                    "> 100 MB" to mediaList.filter { it.sizeBytes > 100 * 1024 * 1024L },
                    "50 – 100 MB" to mediaList.filter { it.sizeBytes in (50 * 1024 * 1024L)..(100 * 1024 * 1024L) },
                    "10 – 50 MB" to mediaList.filter { it.sizeBytes in (10 * 1024 * 1024L)..(50 * 1024 * 1024L) },
                    "5 – 10 MB" to mediaList.filter { it.sizeBytes in (5 * 1024 * 1024L)..(10 * 1024 * 1024L) },
                    "< 5 MB" to mediaList.filter { it.sizeBytes < 5 * 1024 * 1024L }
                )
                val totalVal = if (metricMode == StorageMetricMode.SIZE) totalSize else totalCount.toLong()
                sizeRanges.filter { it.second.isNotEmpty() }.mapIndexed { index, (label, list) ->
                    val size = list.sumOf { it.sizeBytes }
                    val count = list.size
                    val currentVal = if (metricMode == StorageMetricMode.SIZE) size else count.toLong()
                    val pct = if (totalVal > 0) currentVal.toFloat() / totalVal.toFloat() else 0f
                    StorageDistributionItem(
                        key = label,
                        label = label,
                        sizeBytes = size,
                        formattedSize = formatBytes(size),
                        count = count,
                        colorHex = palette[index % palette.size],
                        percentage = pct
                    )
                }
            }
        }

        // Base largest files
        val largest = mediaList.sortedByDescending { it.sizeBytes }

        // Filtered media items based on quick filter or active slice
        val baseFiltered = when {
            sliceKey != null -> {
                when (groupingMode) {
                    StorageGroupingMode.BY_FILE_TYPE -> largest.filter { extractExtensionOrMime(it) == sliceKey }
                    StorageGroupingMode.BY_CLASSIFICATION -> largest.filter { inferCategory(it) == sliceKey }
                    StorageGroupingMode.BY_MEDIA_KIND -> when (sliceKey) {
                        "VIDEOS" -> largest.filter { it.isVideo }
                        "PHOTOS" -> largest.filter { !it.isVideo && !isOtherDocumentOrArchive(it) }
                        "DUPLICATES" -> largest.filter { duplicateMediaIds.contains(it.id) }
                        "OTHER" -> largest.filter { isOtherDocumentOrArchive(it) }
                        else -> largest
                    }
                    StorageGroupingMode.BY_SIZE -> when (sliceKey) {
                        "> 100 MB" -> largest.filter { it.sizeBytes > 100 * 1024 * 1024L }
                        "50 – 100 MB" -> largest.filter { it.sizeBytes in (50 * 1024 * 1024L)..(100 * 1024 * 1024L) }
                        "10 – 50 MB" -> largest.filter { it.sizeBytes in (10 * 1024 * 1024L)..(50 * 1024 * 1024L) }
                        "5 – 10 MB" -> largest.filter { it.sizeBytes in (5 * 1024 * 1024L)..(10 * 1024 * 1024L) }
                        "< 5 MB" -> largest.filter { it.sizeBytes < 5 * 1024 * 1024L }
                        else -> largest
                    }
                }
            }
            quickFilter == StorageQuickFilter.LARGE_VIDEOS_50MB -> largest.filter { it.isVideo && it.sizeBytes > 50 * 1024 * 1024L }
            quickFilter == StorageQuickFilter.LARGE_VIDEOS_100MB -> largest.filter { it.isVideo && it.sizeBytes > 100 * 1024 * 1024L }
            quickFilter == StorageQuickFilter.LARGE_PHOTOS_5MB -> largest.filter { !it.isVideo && it.sizeBytes > 5 * 1024 * 1024L }
            quickFilter == StorageQuickFilter.LARGE_FILES_5MB -> largest.filter { it.sizeBytes > 5 * 1024 * 1024L }
            quickFilter == StorageQuickFilter.LARGE_FILES_50MB -> largest.filter { it.sizeBytes > 50 * 1024 * 1024L }
            quickFilter == StorageQuickFilter.LARGE_FILES_100MB -> largest.filter { it.sizeBytes > 100 * 1024 * 1024L }
            quickFilter == StorageQuickFilter.DUPLICATES -> largest.filter { duplicateMediaIds.contains(it.id) }
            quickFilter == StorageQuickFilter.SCREENSHOTS -> largest.filter { inferCategory(it) == "SCREENSHOT" }
            quickFilter == StorageQuickFilter.LARGEST_FILES -> largest
            else -> largest
        }

        // Apply Sorting
        val sortedFiltered = when (sortMode) {
            StorageSortMode.LARGEST_FIRST -> baseFiltered.sortedByDescending { it.sizeBytes }
            StorageSortMode.SMALLEST_FIRST -> baseFiltered.sortedBy { it.sizeBytes }
            StorageSortMode.NEWEST_FIRST -> baseFiltered.sortedByDescending { if (it.dateTaken > 0L) it.dateTaken else it.dateModified }
            StorageSortMode.OLDEST_FIRST -> baseFiltered.sortedBy { if (it.dateTaken > 0L) it.dateTaken else it.dateModified }
            StorageSortMode.BY_TYPE -> baseFiltered.sortedWith(compareBy({ if (it.isVideo) 1 else 0 }, { extractExtensionOrMime(it) }))
        }

        // Quick Cleanup Suggestions
        val suggestions = mutableListOf<StorageQuickCleanSuggestion>()
        val vid100 = mediaList.filter { it.isVideo && it.sizeBytes > 100 * 1024 * 1024L }
        if (vid100.isNotEmpty()) {
            suggestions.add(
                StorageQuickCleanSuggestion(
                    filter = StorageQuickFilter.LARGE_VIDEOS_100MB,
                    title = "فيديوهات > 100 MB",
                    reason = "فيديوهات ضخمة جدًا تستهلك مساحة كبيرة",
                    count = vid100.size,
                    sizeBytes = vid100.sumOf { it.sizeBytes },
                    formattedSize = formatBytes(vid100.sumOf { it.sizeBytes }),
                    iconName = "videocam"
                )
            )
        }

        val vid50 = mediaList.filter { it.isVideo && it.sizeBytes > 50 * 1024 * 1024L }
        if (vid50.isNotEmpty() && vid50.size != vid100.size) {
            suggestions.add(
                StorageQuickCleanSuggestion(
                    filter = StorageQuickFilter.LARGE_VIDEOS_50MB,
                    title = "فيديوهات > 50 MB",
                    reason = "مقاطع فيديو عالية الاستهلاك للمساحة",
                    count = vid50.size,
                    sizeBytes = vid50.sumOf { it.sizeBytes },
                    formattedSize = formatBytes(vid50.sumOf { it.sizeBytes }),
                    iconName = "videocam"
                )
            )
        }

        if (duplicateCount > 0 && reclaimableDuplicateBytes > 0) {
            suggestions.add(
                StorageQuickCleanSuggestion(
                    filter = StorageQuickFilter.DUPLICATES,
                    title = "الملفات المكررة",
                    reason = "نسخ إضافية متطابقة يمكن تنظيفها مع إبقاء الأصل",
                    count = duplicateCount,
                    sizeBytes = reclaimableDuplicateBytes,
                    formattedSize = formatBytes(reclaimableDuplicateBytes),
                    iconName = "duplicates"
                )
            )
        }

        val pho5 = mediaList.filter { !it.isVideo && it.sizeBytes > 5 * 1024 * 1024L }
        if (pho5.isNotEmpty()) {
            suggestions.add(
                StorageQuickCleanSuggestion(
                    filter = StorageQuickFilter.LARGE_PHOTOS_5MB,
                    title = "صور > 5 MB",
                    reason = "صور فائقة الدقة أو صور خام كبيرة الحجم",
                    count = pho5.size,
                    sizeBytes = pho5.sumOf { it.sizeBytes },
                    formattedSize = formatBytes(pho5.sumOf { it.sizeBytes }),
                    iconName = "photo"
                )
            )
        }

        if (screenshots.isNotEmpty()) {
            suggestions.add(
                StorageQuickCleanSuggestion(
                    filter = StorageQuickFilter.SCREENSHOTS,
                    title = "لقطات الشاشة",
                    reason = "لقطات شاشة مؤقتة ومعلومات شاشة قد لا تحتاجها",
                    count = screenshots.size,
                    sizeBytes = screenshotsSize,
                    formattedSize = formatBytes(screenshotsSize),
                    iconName = "screenshot"
                )
            )
        }

        return StorageAnalysisState(
            totalSizeBytes = totalSize,
            formattedTotalSize = formatBytes(totalSize),
            totalCount = totalCount,
            photosSizeBytes = photosSize,
            photosCount = photos.size,
            videosSizeBytes = videosSize,
            videosCount = videos.size,
            otherSizeBytes = otherSize,
            otherCount = otherFiles.size,
            duplicatesSizeBytes = duplicateTotalSize,
            duplicatesCount = duplicateCount,
            reclaimableSizeBytes = reclaimableDuplicateBytes,
            formattedReclaimableSize = formatBytes(reclaimableDuplicateBytes),
            screenshotsSizeBytes = screenshotsSize,
            screenshotsCount = screenshots.size,
            distributionItems = distributionItems,
            largestMediaItems = largest,
            filteredMediaItems = sortedFiltered,
            quickCleanSuggestions = suggestions,
            selectedGroupingMode = groupingMode,
            selectedMetricMode = metricMode,
            selectedSliceKey = sliceKey,
            selectedSliceLabel = sliceLabel,
            quickFilter = quickFilter,
            selectedSortMode = sortMode,
            selectedItemIds = selectedItemIds,
            isLoading = isLoading,
            userMessage = userMessage
        )
    }

    fun extractExtensionOrMime(item: MediaItem): String {
        val path = item.filePath.ifEmpty { item.fileName }
        val ext = path.substringAfterLast('.', "").uppercase(Locale.ROOT)
        if (ext.isNotEmpty() && ext.length in 2..5) {
            return ext
        }
        val mime = item.mimeType.substringAfter('/', "").uppercase(Locale.ROOT)
        return if (mime.isNotEmpty()) mime else if (item.isVideo) "VIDEO" else "IMAGE"
    }

    fun inferCategory(item: MediaItem): String {
        val nameLower = (item.fileName + " " + item.filePath).lowercase(Locale.ROOT)
        return when {
            nameLower.contains("screenshot") || nameLower.contains("capture") || nameLower.contains("لقطة") -> "SCREENSHOT"
            nameLower.contains("chart") || nameLower.contains("trade") || nameLower.contains("crypto") || nameLower.contains("stock") || nameLower.contains("تداول") -> "TRADING"
            nameLower.contains("product") || nameLower.contains("منتج") || nameLower.contains("سلعة") || nameLower.contains("بضاعة") -> "PRODUCT"
            nameLower.contains("doc") || nameLower.contains("pdf") || nameLower.contains("scan") || nameLower.contains("مستند") || nameLower.contains("فاتورة") || nameLower.contains("receipt") -> "DOCUMENT"
            nameLower.contains("car") || nameLower.contains("auto") || nameLower.contains("vehicle") || nameLower.contains("سيارة") || nameLower.contains("مركبة") -> "CAR"
            nameLower.contains("food") || nameLower.contains("restaurant") || nameLower.contains("مطعم") || nameLower.contains("طعام") || nameLower.contains("وجبة") -> "FOOD"
            nameLower.contains("person") || nameLower.contains("selfie") || nameLower.contains("portrait") || nameLower.contains("شخص") -> "PERSON"
            nameLower.contains("nature") || nameLower.contains("beach") || nameLower.contains("mountain") || nameLower.contains("طبيعة") || nameLower.contains("بحر") -> "NATURE"
            nameLower.contains("travel") || nameLower.contains("trip") || nameLower.contains("سفر") || nameLower.contains("رحلة") -> "TRAVEL"
            nameLower.contains("work") || nameLower.contains("job") || nameLower.contains("office") || nameLower.contains("عمل") -> "WORK"
            nameLower.contains("camera") || nameLower.contains("dcim") -> if (item.isVideo) "VIDEO" else "CAMERA_PHOTO"
            item.isVideo -> "VIDEO"
            else -> "OTHER"
        }
    }

    private fun isOtherDocumentOrArchive(item: MediaItem): Boolean {
        val ext = extractExtensionOrMime(item)
        val imageVideoExts = setOf("JPG", "JPEG", "PNG", "WEBP", "HEIC", "HEIF", "GIF", "BMP", "MP4", "MKV", "MOV", "3GP", "AVI", "WEBM")
        return !imageVideoExts.contains(ext)
    }

    private fun formatFileExtensionLabel(ext: String): String {
        return when (ext) {
            "JPG", "JPEG" -> "JPEG ($ext)"
            "PNG" -> "PNG Images"
            "MP4" -> "MP4 Videos"
            "MKV" -> "MKV Videos"
            "MOV" -> "MOV Videos"
            "WEBP" -> "WEBP Photos"
            "GIF" -> "GIF Animations"
            "HEIC", "HEIF" -> "HEIC Photos"
            "3GP" -> "3GP Clips"
            else -> ext
        }
    }

    private fun formatCategoryLabel(cat: String): String {
        return when (cat) {
            "SCREENSHOT" -> "لقطات الشاشة (Screenshots)"
            "DOCUMENT" -> "المستندات والفواتير (Documents)"
            "TRADING" -> "التداول والشارتات (Trading)"
            "PRODUCT" -> "المنتجات (Products)"
            "PERSON" -> "الأشخاص والصور الشخصية (People)"
            "CAR" -> "السيارات والمركبات (Vehicles)"
            "FOOD" -> "الأطعمة والمشروبات (Food)"
            "NATURE" -> "الطبيعة والمناظر (Nature)"
            "TRAVEL" -> "السفر والرحلات (Travel)"
            "WORK" -> "العمل والمهام (Work)"
            "CAMERA_PHOTO" -> "صور الكاميرا (Camera)"
            "VIDEO" -> "مقاطع الفيديو (Videos)"
            "OTHER" -> "أخرى وغير مصنفة (Other)"
            else -> cat
        }
    }

    fun buildD3JsonString(state: StorageAnalysisState): String {
        val root = JSONObject()
        root.put("formattedTotal", if (state.selectedMetricMode == StorageMetricMode.SIZE) state.formattedTotalSize else "${state.totalCount} Items")
        val itemsArray = JSONArray()
        state.distributionItems.forEach { item ->
            val obj = JSONObject()
            obj.put("key", item.key)
            obj.put("label", item.label)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("count", item.count)
            obj.put("colorHex", item.colorHex)
            obj.put("percentage", item.percentage.toDouble())
            itemsArray.put(obj)
        }
        root.put("items", itemsArray)
        return root.toString()
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val k = 1024.0
            val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
            val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt().coerceIn(0, sizes.size - 1)
            val v = bytes / Math.pow(k, i.toDouble())
            return String.format(Locale.US, "%.1f %s", v, sizes[i])
        }

        /**
         * Determines the original/primary member to preserve from a group of duplicates.
         * Priority:
         * 1. DCIM/Camera path (original capture folder).
         * 2. Oldest dateTaken (> 0) with a valid Content URI.
         * 3. Lowest database ID.
         */
        fun selectOriginalMember(items: List<MediaItem>): MediaItem {
            if (items.isEmpty()) throw IllegalArgumentException("Items cannot be empty")
            if (items.size == 1) return items.first()

            // 1. Camera / DCIM original location
            val cameraOriginal = items.firstOrNull { it.filePath.contains("DCIM/Camera", ignoreCase = true) }
            if (cameraOriginal != null) return cameraOriginal

            // 2. Oldest dateTaken with valid URI
            val validDated = items.filter { it.dateTaken > 0L && it.uriString.isNotBlank() }
            if (validDated.isNotEmpty()) {
                return validDated.minByOrNull { it.dateTaken }!!
            }

            // 3. Lowest media ID
            return items.minByOrNull { it.id } ?: items.first()
        }

        fun calculateReclaimableDuplicateBytes(duplicates: List<DuplicateGroupWithMedia>): Long {
            var reclaimable = 0L
            duplicates.forEach { group ->
                if (group.members.size > 1) {
                    val memberItems = group.members.map { it.mediaItem }
                    val original = selectOriginalMember(memberItems)
                    val redundant = memberItems.filter { it.id != original.id }
                    reclaimable += redundant.sumOf { it.sizeBytes }
                }
            }
            return reclaimable
        }
    }

    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StorageAnalyzerViewModel(repository) as T
        }
    }
}
