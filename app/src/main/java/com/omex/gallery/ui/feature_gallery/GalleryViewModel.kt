package com.omex.gallery.ui.feature_gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omex.gallery.core.indexer.IndexScheduler
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.IndexingProgress
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import com.omex.gallery.domain.model.PersonGroup
import com.omex.gallery.domain.model.SearchFilterOptions
import com.omex.gallery.domain.model.SearchFilterState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MediaFilterTab {
    ALL, PHOTOS, VIDEOS, FAVORITES, PEOPLE, DUPLICATES
}

/**
 * ViewModel managing gallery state, AI indexing, EXIF & ML tag search, duplicate detection, and face clustering.
 */
class GalleryViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MediaFilterTab.ALL)
    val selectedTab: StateFlow<MediaFilterTab> = _selectedTab.asStateFlow()

    private val _searchFilterState = MutableStateFlow(SearchFilterState())
    val searchFilterState: StateFlow<SearchFilterState> = _searchFilterState.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    fun toggleSelection(itemId: Long) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(itemId)) {
            current - itemId
        } else {
            current + itemId
        }
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    val filterOptions: StateFlow<SearchFilterOptions> = repository.getSearchFilterOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchFilterOptions())

    val indexingProgress: StateFlow<IndexingProgress> = repository.getIndexingProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IndexingProgress())

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
    val mediaItems: StateFlow<List<MediaItem>> = combine(_selectedTab, _searchFilterState) { tab, filterState ->
        Pair(tab, filterState)
    }.flatMapLatest { (tab, filterState) ->
        if (filterState.hasActiveFilters) {
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
    }

    fun triggerGalleryScan(context: Context) {
        IndexScheduler(context.applicationContext).enqueueNormalSync()
    }

    fun triggerFullReindex(context: Context) {
        IndexScheduler(context.applicationContext).enqueueFullReindex()
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
