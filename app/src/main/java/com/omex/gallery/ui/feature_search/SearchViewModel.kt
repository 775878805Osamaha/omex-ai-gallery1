package com.omex.gallery.ui.feature_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.core.search.SearchHistoryRepository
import com.omex.gallery.domain.model.AiOcrText
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResultWithOcr(
    val mediaItem: MediaItem,
    val ocrText: AiOcrText? = null
)

class SearchViewModel(
    private val mediaRepository: MediaRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recentQueries: StateFlow<List<String>> = searchHistoryRepository.getRecentQueries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<SearchResultWithOcr>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                mediaRepository.searchMedia(query.trim()).flatMapLatest { mediaItems ->
                    flow {
                        val resultsWithOcr = mediaItems.map { item ->
                            val ocr = mediaRepository.getOcrTextForMedia(item.id)
                            SearchResultWithOcr(item, ocr)
                        }
                        emit(resultsWithOcr)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isNotBlank()) {
                searchHistoryRepository.addQuery(query)
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
