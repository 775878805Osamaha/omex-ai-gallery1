package com.omex.gallery.ui.feature_detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaItemWithAi
import com.omex.gallery.domain.model.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SuperResolutionState {
    data object Idle : SuperResolutionState()
    data class Processing(val progress: Float) : SuperResolutionState()
    data class Success(val upscaledPath: String) : SuperResolutionState()
    data class Error(val message: String) : SuperResolutionState()
}

/**
 * ViewModel for media detail pager, zoom, video playback, AI intelligence visualization, EXIF metadata, and Real-ESRGAN upscaling.
 */
class MediaDetailViewModel(
    private val repository: MediaRepository,
    private val initialMediaId: Long
) : ViewModel() {

    private val _mediaItemList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItemList: StateFlow<List<MediaItem>> = _mediaItemList.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _mediaItemWithAi = MutableStateFlow<MediaItemWithAi?>(null)
    val mediaItemWithAi: StateFlow<MediaItemWithAi?> = _mediaItemWithAi.asStateFlow()

    private val _showExifSheet = MutableStateFlow(false)
    val showExifSheet: StateFlow<Boolean> = _showExifSheet.asStateFlow()

    private val _superResolutionState = MutableStateFlow<SuperResolutionState>(SuperResolutionState.Idle)
    val superResolutionState: StateFlow<SuperResolutionState> = _superResolutionState.asStateFlow()

    val currentMediaItem: StateFlow<MediaItem?> = combine(_mediaItemList, _currentIndex) { list, index ->
        if (list.isNotEmpty() && index in list.indices) list[index] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadMediaList()
    }

    private fun loadMediaList() {
        viewModelScope.launch {
            val allMedia = repository.getAllMediaItems()
            if (allMedia.isNotEmpty()) {
                _mediaItemList.value = allMedia
                val initialIdx = allMedia.indexOfFirst { it.id == initialMediaId }
                if (initialIdx >= 0) {
                    _currentIndex.value = initialIdx
                }
            } else {
                // Fallback single item query if database empty
                val single = repository.getMediaById(initialMediaId)
                if (single != null) {
                    _mediaItemList.value = listOf(single)
                    _currentIndex.value = 0
                }
            }
            loadAiForCurrent()
        }
    }

    fun setCurrentIndex(index: Int) {
        if (index in _mediaItemList.value.indices) {
            _currentIndex.value = index
            loadAiForCurrent()
        }
    }

    private fun loadAiForCurrent() {
        val list = _mediaItemList.value
        val idx = _currentIndex.value
        if (list.isNotEmpty() && idx in list.indices) {
            val item = list[idx]
            viewModelScope.launch {
                val aiDetails = repository.getMediaItemWithAi(item.id)
                _mediaItemWithAi.value = aiDetails
            }
        }
    }

    fun runAiAnalysis(context: Context) {
        val item = currentMediaItem.value ?: return
        viewModelScope.launch {
            repository.runAiPipelineOnMedia(context, item)
            loadAiForCurrent()
        }
    }

    fun runSuperResolution(context: Context, scaleFactor: Int) {
        val item = currentMediaItem.value ?: return
        viewModelScope.launch {
            _superResolutionState.value = SuperResolutionState.Processing(0f)
            val result = repository.superResolveImage(
                context = context,
                mediaItem = item,
                scaleFactor = scaleFactor,
                onProgress = { progress ->
                    _superResolutionState.value = SuperResolutionState.Processing(progress)
                }
            )
            if (result.isSuccess) {
                _superResolutionState.value = SuperResolutionState.Success(result.getOrThrow())
            } else {
                _superResolutionState.value = SuperResolutionState.Error(result.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }

    fun resetSuperResolutionState() {
        _superResolutionState.value = SuperResolutionState.Idle
    }

    fun toggleFavorite() {
        val current = currentMediaItem.value ?: return
        viewModelScope.launch {
            val updated = !current.isFavorite
            repository.toggleFavorite(current.id, updated)
            val list = _mediaItemList.value.toMutableList()
            val idx = _currentIndex.value
            if (idx in list.indices) {
                list[idx] = list[idx].copy(isFavorite = updated)
                _mediaItemList.value = list
            }
        }
    }

    fun deleteCurrentMedia(onDeletedAll: () -> Unit) {
        val item = currentMediaItem.value ?: return
        viewModelScope.launch {
            repository.deleteMediaItem(item.id)
            val updatedList = repository.getAllMediaItems()
            if (updatedList.isEmpty()) {
                onDeletedAll()
            } else {
                _mediaItemList.value = updatedList
                val newIndex = _currentIndex.value.coerceAtMost(updatedList.size - 1)
                _currentIndex.value = newIndex
                loadAiForCurrent()
            }
        }
    }

    fun toggleExifSheet() {
        _showExifSheet.value = !_showExifSheet.value
    }

    class Factory(
        private val repository: MediaRepository,
        private val mediaId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MediaDetailViewModel(repository, mediaId) as T
        }
    }
}

