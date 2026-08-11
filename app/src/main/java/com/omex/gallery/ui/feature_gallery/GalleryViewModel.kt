package com.omex.gallery.ui.feature_gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omex.gallery.core.indexer.IndexScheduler
import com.omex.gallery.domain.model.Album
import com.omex.gallery.domain.model.AlbumType
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.IndexingProgress
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import com.omex.gallery.domain.model.PersonGroup
import com.omex.gallery.domain.model.SearchFilterOptions
import com.omex.gallery.domain.model.SearchFilterState
import com.omex.gallery.domain.model.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class NavTab {
    GALLERY, ALBUMS, SEARCH, AI_STUDIO
}

enum class MediaFilterTab {
    ALL, PHOTOS, VIDEOS, FAVORITES, PEOPLE, DUPLICATES
}

data class DiagnosticResultState(
    val mediaStoreImagesCount: Int = 0,
    val mediaStoreVideosCount: Int = 0,
    val imagesParsed: Int = 0,
    val videosParsed: Int = 0,
    val itemsBeforeInsert: Int = 0,
    val itemsInserted: Int = 0,
    val roomTotalItemsAfterInsert: Int = 0,
    val galleryUiItemCount: Int = 0,
    val isRunning: Boolean = false,
    val hasRun: Boolean = false,
    val lastRunTime: String = ""
)

/**
 * ViewModel managing gallery state, navigation tabs, album grouping, sorting, selection actions, AI indexing, and filters.
 */
class GalleryViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _diagnosticState = MutableStateFlow(DiagnosticResultState())
    val diagnosticState: StateFlow<DiagnosticResultState> = _diagnosticState.asStateFlow()

    private val _selectedNavTab = MutableStateFlow(NavTab.GALLERY)
    val selectedNavTab: StateFlow<NavTab> = _selectedNavTab.asStateFlow()

    private val _selectedTab = MutableStateFlow(MediaFilterTab.ALL)
    val selectedTab: StateFlow<MediaFilterTab> = _selectedTab.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _gridColumnCount = MutableStateFlow(3)
    val gridColumnCount: StateFlow<Int> = _gridColumnCount.asStateFlow()

    private val _searchFilterState = MutableStateFlow(SearchFilterState())
    val searchFilterState: StateFlow<SearchFilterState> = _searchFilterState.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<String>> = _selectedCategoryIds.asStateFlow()

    fun toggleCategoryFilter(categoryId: String) {
        val current = _selectedCategoryIds.value
        _selectedCategoryIds.value = if (current.contains(categoryId)) {
            current - categoryId
        } else {
            current + categoryId
        }
    }

    fun clearCategoryFilters() {
        _selectedCategoryIds.value = emptySet()
    }

    val categories: StateFlow<List<com.omex.gallery.core.data.local.MediaCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun classifyUnclassifiedMedia(context: Context) {
        viewModelScope.launch {
            repository.classifyUnclassifiedMedia(context)
        }
    }

    fun selectNavTab(tab: NavTab) {
        _selectedNavTab.value = tab
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun selectAlbum(album: Album?) {
        _selectedAlbum.value = album
        if (album != null) {
            _selectedNavTab.value = NavTab.GALLERY
        }
    }

    fun toggleGridColumnCount() {
        _gridColumnCount.value = when (_gridColumnCount.value) {
            2 -> 3
            3 -> 4
            else -> 2
        }
    }

    fun toggleSelection(itemId: Long) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(itemId)) {
            current - itemId
        } else {
            current + itemId
        }
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedItemIds.value = items.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedItemIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.deleteMediaItem(it) }
            clearSelection()
        }
    }

    fun favoriteSelected() {
        val ids = _selectedItemIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.toggleFavorite(it, true) }
            clearSelection()
        }
    }

    val filterOptions: StateFlow<SearchFilterOptions> = repository.getSearchFilterOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchFilterOptions())

    val indexingProgress: StateFlow<IndexingProgress> = repository.getIndexingProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IndexingProgress())

    val allMediaRaw: StateFlow<List<MediaItem>> = repository.getAllMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = allMediaRaw.map { mediaList ->
        computeAlbums(mediaList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeAlbums(mediaList: List<MediaItem>): List<Album> {
        if (mediaList.isEmpty()) return emptyList()

        val cameraItems = mediaList.filter { it.filePath.contains("Camera", ignoreCase = true) || it.filePath.contains("DCIM", ignoreCase = true) }
        val screenshotsItems = mediaList.filter { it.filePath.contains("Screenshot", ignoreCase = true) }
        val downloadsItems = mediaList.filter { it.filePath.contains("Download", ignoreCase = true) }
        val videoItems = mediaList.filter { it.isVideo }
        val favoriteItems = mediaList.filter { it.isFavorite }

        val albumList = mutableListOf<Album>()

        if (cameraItems.isNotEmpty()) {
            albumList.add(
                Album(
                    id = "virtual_camera",
                    title = "الكاميرا (Camera)",
                    coverUri = cameraItems.firstOrNull()?.uriString,
                    itemCount = cameraItems.size,
                    albumType = AlbumType.CAMERA
                )
            )
        }

        if (screenshotsItems.isNotEmpty()) {
            albumList.add(
                Album(
                    id = "virtual_screenshots",
                    title = "لقطات الشاشة (Screenshots)",
                    coverUri = screenshotsItems.firstOrNull()?.uriString,
                    itemCount = screenshotsItems.size,
                    albumType = AlbumType.SCREENSHOTS
                )
            )
        }

        if (downloadsItems.isNotEmpty()) {
            albumList.add(
                Album(
                    id = "virtual_downloads",
                    title = "التنزيلات (Downloads)",
                    coverUri = downloadsItems.firstOrNull()?.uriString,
                    itemCount = downloadsItems.size,
                    albumType = AlbumType.DOWNLOADS
                )
            )
        }

        if (videoItems.isNotEmpty()) {
            albumList.add(
                Album(
                    id = "virtual_videos",
                    title = "الفيديوهات (Videos)",
                    coverUri = videoItems.firstOrNull()?.uriString,
                    itemCount = videoItems.size,
                    albumType = AlbumType.VIDEOS
                )
            )
        }

        if (favoriteItems.isNotEmpty()) {
            albumList.add(
                Album(
                    id = "virtual_favorites",
                    title = "المفضلة (Favorites)",
                    coverUri = favoriteItems.firstOrNull()?.uriString,
                    itemCount = favoriteItems.size,
                    albumType = AlbumType.FAVORITES
                )
            )
        }

        // Directory folder buckets
        val folderGroups = mediaList.groupBy { item ->
            if (item.filePath.isNotEmpty()) {
                val parent = File(item.filePath).parentFile
                parent?.name ?: "وسائط أخرى"
            } else {
                "وسائط أخرى"
            }
        }

        folderGroups.forEach { (folderName, items) ->
            if (folderName != "Camera" && folderName != "DCIM" && folderName != "Screenshots" && folderName != "Download") {
                albumList.add(
                    Album(
                        id = "folder_$folderName",
                        title = folderName,
                        coverUri = items.firstOrNull()?.uriString,
                        itemCount = items.size,
                        albumType = AlbumType.FOLDER,
                        folderPath = folderName
                    )
                )
            }
        }

        return albumList
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedMediaItems: Flow<PagingData<MediaItem>> = _selectedTab.flatMapLatest { tab ->
        when (tab) {
            MediaFilterTab.ALL -> repository.getAllMediaPaged()
            MediaFilterTab.PHOTOS -> repository.getPhotosPaged()
            MediaFilterTab.VIDEOS -> repository.getVideosPaged()
            MediaFilterTab.FAVORITES -> repository.getFavoritesPaged()
            else -> repository.getAllMediaPaged()
        }
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItems: StateFlow<List<MediaItem>> = combine(
        _selectedTab,
        _searchFilterState,
        _selectedAlbum,
        _sortOrder,
        _selectedCategoryIds
    ) { tab, filterState, album, sortOrder, categoryIds ->
        Tuple5(tab, filterState, album, sortOrder, categoryIds)
    }.flatMapLatest { (tab, filterState, album, sortOrder, categoryIds) ->
        val baseFlow = if (categoryIds.isNotEmpty()) {
            repository.getMediaForCategories(categoryIds.toList())
        } else if (filterState.hasActiveFilters) {
            repository.searchMediaAdvanced(filterState)
        } else {
            when (tab) {
                MediaFilterTab.ALL -> repository.getAllMedia()
                MediaFilterTab.PHOTOS -> repository.getPhotos()
                MediaFilterTab.VIDEOS -> repository.getVideos()
                MediaFilterTab.FAVORITES -> repository.getFavorites()
                else -> repository.getAllMedia()
            }
        }

        baseFlow.map { list ->
            var filtered = list

            // Apply tab filter if category filter is active along with tabs (e.g. PHOTOS + PRODUCT)
            if (categoryIds.isNotEmpty()) {
                filtered = when (tab) {
                    MediaFilterTab.PHOTOS -> filtered.filter { !it.isVideo }
                    MediaFilterTab.VIDEOS -> filtered.filter { it.isVideo }
                    MediaFilterTab.FAVORITES -> filtered.filter { it.isFavorite }
                    else -> filtered
                }
            }

            // Apply selected album filter if active
            if (album != null) {
                filtered = when (album.albumType) {
                    AlbumType.CAMERA -> filtered.filter { it.filePath.contains("Camera", ignoreCase = true) || it.filePath.contains("DCIM", ignoreCase = true) }
                    AlbumType.SCREENSHOTS -> filtered.filter { it.filePath.contains("Screenshot", ignoreCase = true) }
                    AlbumType.DOWNLOADS -> filtered.filter { it.filePath.contains("Download", ignoreCase = true) }
                    AlbumType.VIDEOS -> filtered.filter { it.isVideo }
                    AlbumType.FAVORITES -> filtered.filter { it.isFavorite }
                    AlbumType.FOLDER -> {
                        val folderName = album.folderPath ?: ""
                        filtered.filter { File(it.filePath).parentFile?.name == folderName }
                    }
                }
            }

            // Apply Sort Order
            when (sortOrder) {
                SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.dateTaken }
                SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.dateTaken }
                SortOrder.LARGEST_FIRST -> filtered.sortedByDescending { it.sizeBytes }
                SortOrder.SMALLEST_FIRST -> filtered.sortedBy { it.sizeBytes }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duplicateGroups: StateFlow<List<DuplicateGroupWithMedia>> = repository.getDuplicateGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personGroups: StateFlow<List<PersonGroup>> = repository.getPersonGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MediaFilterTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchFilterState.value = _searchFilterState.value.copy(query = query)
    }

    fun setCameraModelFilter(model: String?) {
        val current = _searchFilterState.value.cameraModel
        _searchFilterState.value = _searchFilterState.value.copy(
            cameraModel = if (current == model) null else model
        )
    }

    fun setCameraMakeFilter(make: String?) {
        val current = _searchFilterState.value.cameraMake
        _searchFilterState.value = _searchFilterState.value.copy(
            cameraMake = if (current == make) null else make
        )
    }

    fun setMlCategoryFilter(category: String?) {
        val current = _searchFilterState.value.mlCategory
        _searchFilterState.value = _searchFilterState.value.copy(
            mlCategory = if (current == category) null else category
        )
    }

    fun setMlLabelFilter(label: String?) {
        val current = _searchFilterState.value.mlLabel
        _searchFilterState.value = _searchFilterState.value.copy(
            mlLabel = if (current == label) null else label
        )
    }

    fun toggleGpsOnlyFilter() {
        val current = _searchFilterState.value.isGpsOnly
        _searchFilterState.value = _searchFilterState.value.copy(isGpsOnly = !current)
    }

    fun clearAllFilters() {
        _searchFilterState.value = SearchFilterState()
        _selectedAlbum.value = null
    }

    fun triggerGalleryScan(context: Context) {
        IndexScheduler(context.applicationContext).enqueueNormalSync()
        viewModelScope.launch {
            repository.scanAndIndexGallery()
        }
    }

    fun triggerFullReindex(context: Context) {
        IndexScheduler(context.applicationContext).enqueueFullReindex()
        viewModelScope.launch {
            repository.scanAndIndexGallery(isFullReindex = true)
        }
    }

    fun runIndexingDiagnostic(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _diagnosticState.value = _diagnosticState.value.copy(isRunning = true)
            try {
                val scanner = com.omex.gallery.core.indexer.MediaScanner(context.applicationContext)
                val scanData = scanner.scanWithDiagnosticCounts()

                val itemsBeforeInsert = repository.getAllMediaItems().size

                val mediaItemsToInsert = scanData.rawItems.map { raw ->
                    MediaItem(
                        id = raw.id,
                        uriString = raw.contentUri.toString(),
                        filePath = raw.filePath,
                        fileName = raw.displayName,
                        mimeType = raw.mimeType,
                        isVideo = raw.isVideo,
                        width = raw.width,
                        height = raw.height,
                        sizeBytes = raw.sizeBytes,
                        dateTaken = raw.dateTaken,
                        dateModified = raw.dateModified,
                        durationMs = raw.durationMs
                    )
                }

                if (mediaItemsToInsert.isNotEmpty()) {
                    repository.insertMediaItems(mediaItemsToInsert)
                }

                val roomTotalItemsAfterInsert = repository.getAllMediaItems().size
                val currentUiCount = mediaItems.value.size

                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                _diagnosticState.value = DiagnosticResultState(
                    mediaStoreImagesCount = scanData.imagesCursorCount,
                    mediaStoreVideosCount = scanData.videosCursorCount,
                    imagesParsed = scanData.imagesParsedCount,
                    videosParsed = scanData.videosParsedCount,
                    itemsBeforeInsert = itemsBeforeInsert,
                    itemsInserted = mediaItemsToInsert.size,
                    roomTotalItemsAfterInsert = roomTotalItemsAfterInsert,
                    galleryUiItemCount = currentUiCount,
                    isRunning = false,
                    hasRun = true,
                    lastRunTime = timeStr
                )
            } catch (e: Exception) {
                _diagnosticState.value = _diagnosticState.value.copy(isRunning = false)
            }
        }
    }

    fun triggerAiScan(context: Context) {
        viewModelScope.launch {
            repository.runFullGalleryAiScan(context)
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(repository) as T
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val a: A,
    val b: B,
    val c: C,
    val d: D
)

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)

