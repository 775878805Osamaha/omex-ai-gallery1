package com.omex.gallery.ui.feature_detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaItemWithAi
import com.omex.gallery.domain.model.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SuperResolutionState {
    data object Idle : SuperResolutionState()
    data class Processing(val progress: Float) : SuperResolutionState()
    data class Success(val upscaledPath: String) : SuperResolutionState()
    data class Error(val message: String) : SuperResolutionState()
}

/**
 * ViewModel for media detail, AI intelligence visualization (bounding boxes, classifications, faces),
 * EXIF metadata, and Real-ESRGAN super-resolution triggering.
 */
class MediaDetailViewModel(
    private val repository: MediaRepository,
    private val mediaId: Long
) : ViewModel() {

    private val _mediaItem = MutableStateFlow<MediaItem?>(null)
    val mediaItem: StateFlow<MediaItem?> = _mediaItem.asStateFlow()

    private val _mediaItemWithAi = MutableStateFlow<MediaItemWithAi?>(null)
    val mediaItemWithAi: StateFlow<MediaItemWithAi?> = _mediaItemWithAi.asStateFlow()

    private val _showExifSheet = MutableStateFlow(false)
    val showExifSheet: StateFlow<Boolean> = _showExifSheet.asStateFlow()

    private val _superResolutionState = MutableStateFlow<SuperResolutionState>(SuperResolutionState.Idle)
    val superResolutionState: StateFlow<SuperResolutionState> = _superResolutionState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            val item = repository.getMediaById(mediaId)
            _mediaItem.value = item
            val aiDetails = repository.getMediaItemWithAi(mediaId)
            _mediaItemWithAi.value = aiDetails
        }
    }

    fun runAiAnalysis(context: Context) {
        val item = _mediaItem.value ?: return
        viewModelScope.launch {
            repository.runAiPipelineOnMedia(context, item)
            loadMedia()
        }
    }

    fun runSuperResolution(context: Context, scaleFactor: Int) {
        val item = _mediaItem.value ?: return
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
        val current = _mediaItem.value ?: return
        viewModelScope.launch {
            val updated = !current.isFavorite
            repository.toggleFavorite(current.id, updated)
            _mediaItem.value = current.copy(isFavorite = updated)
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
