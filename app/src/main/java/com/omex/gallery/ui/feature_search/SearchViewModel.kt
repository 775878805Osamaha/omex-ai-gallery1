package com.omex.gallery.ui.feature_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.core.search.SearchHistoryRepository
import com.omex.gallery.core.search.SmartSearchHelper
import com.omex.gallery.domain.model.AiOcrText
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import com.omex.gallery.domain.model.SearchFilterState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchMediaTab {
    ALL, PHOTOS, VIDEOS, FAVORITES
}

data class SearchResultWithOcr(
    val mediaItem: MediaItem,
    val ocrText: AiOcrText? = null,
    val matchedCategories: List<String> = emptyList(),
    val matchSnippet: String? = null
)

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

class SearchViewModel(
    private val mediaRepository: MediaRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMediaTab = MutableStateFlow(SearchMediaTab.ALL)
    val selectedMediaTab: StateFlow<SearchMediaTab> = _selectedMediaTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchFilterState = MutableStateFlow(SearchFilterState())
    val searchFilterState: StateFlow<SearchFilterState> = _searchFilterState.asStateFlow()

    val categories: StateFlow<List<com.omex.gallery.core.data.local.MediaCategoryEntity>> = mediaRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentQueries: StateFlow<List<String>> = searchHistoryRepository.getRecentQueries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<SearchResultWithOcr>> = combine(
        _searchQuery.debounce(150),
        _selectedMediaTab,
        _selectedCategory,
        _searchFilterState
    ) { query, mediaTab, category, advancedFilter ->
        Tuple4(query, mediaTab, category, advancedFilter)
    }.flatMapLatest { (query, mediaTab, category, advancedFilter) ->
        val trimmedQuery = query.trim()
        val isVideoFilter = when (mediaTab) {
            SearchMediaTab.PHOTOS -> false
            SearchMediaTab.VIDEOS -> true
            else -> advancedFilter.isVideo
        }
        val isFavoriteFilter = if (mediaTab == SearchMediaTab.FAVORITES) true else advancedFilter.isFavorite

        val combinedCategories = (advancedFilter.allSelectedCategories + listOfNotNull(category)).toSet()

        val consolidatedFilter = advancedFilter.copy(
            query = if (trimmedQuery.isNotEmpty()) trimmedQuery else advancedFilter.query,
            categoryId = null,
            selectedCategoryIds = combinedCategories,
            isVideo = isVideoFilter,
            isFavorite = isFavoriteFilter
        )

        val hasActiveSearch = consolidatedFilter.query.isNotBlank() ||
                consolidatedFilter.hasActiveFilters ||
                mediaTab != SearchMediaTab.ALL ||
                category != null

        if (!hasActiveSearch) {
            flowOf(emptyList())
        } else {
            mediaRepository.searchMediaAdvanced(consolidatedFilter).flatMapLatest { mediaItems ->
                flow {
                    val resultsWithDetails = mediaItems.map { item ->
                        val ocr = mediaRepository.getOcrTextForMedia(item.id)
                        val cats = mediaRepository.getCategoriesForMedia(item.id)
                        SearchResultWithOcr(
                            mediaItem = item,
                            ocrText = ocr,
                            matchedCategories = cats
                        )
                    }
                    emit(resultsWithDetails)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onMediaTabSelect(tab: SearchMediaTab) {
        _selectedMediaTab.value = tab
    }

    fun onCategorySelect(categoryId: String?) {
        _selectedCategory.value = if (_selectedCategory.value == categoryId) null else categoryId
    }

    fun setFilterState(filter: SearchFilterState) {
        _searchFilterState.value = filter
    }

    fun removeCategoryFilter(categoryId: String) {
        val current = _searchFilterState.value.allSelectedCategories.toMutableSet()
        current.remove(categoryId)
        if (_selectedCategory.value == categoryId) {
            _selectedCategory.value = null
        }
        _searchFilterState.value = _searchFilterState.value.copy(
            categoryId = null,
            selectedCategoryIds = current
        )
    }

    fun removeMediaTypeFilter() {
        _selectedMediaTab.value = SearchMediaTab.ALL
        _searchFilterState.value = _searchFilterState.value.copy(isVideo = null)
    }

    fun removeFavoriteFilter() {
        if (_selectedMediaTab.value == SearchMediaTab.FAVORITES) {
            _selectedMediaTab.value = SearchMediaTab.ALL
        }
        _searchFilterState.value = _searchFilterState.value.copy(isFavorite = null)
    }

    fun removeDateFilter() {
        _searchFilterState.value = _searchFilterState.value.copy(
            dateFilterOption = com.omex.gallery.domain.model.DateFilterOption.ALL,
            startDateMs = null,
            endDateMs = null
        )
    }

    fun removeFileSizeFilter() {
        _searchFilterState.value = _searchFilterState.value.copy(
            fileSizeOption = com.omex.gallery.domain.model.FileSizeFilterOption.ALL
        )
    }

    fun removeExtensionFilter(extension: String) {
        val current = _searchFilterState.value.selectedExtensions.toMutableSet()
        current.remove(extension)
        _searchFilterState.value = _searchFilterState.value.copy(selectedExtensions = current)
    }

    fun removeDimensionFilter() {
        _searchFilterState.value = _searchFilterState.value.copy(
            dimensionOption = com.omex.gallery.domain.model.DimensionFilterOption.ALL
        )
    }

    fun clearFilters() {
        _selectedMediaTab.value = SearchMediaTab.ALL
        _selectedCategory.value = null
        _searchFilterState.value = SearchFilterState()
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isNotBlank()) {
                searchHistoryRepository.addQuery(query.trim())
            }
        }
    }

    fun removeRecentQuery(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val searchHistoryRepository: SearchHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(mediaRepository, searchHistoryRepository) as T
        }
    }
}
