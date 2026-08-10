package com.omex.gallery.ui.feature_ask_image

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.core.ai.multimodal.AskImageEngine
import com.omex.gallery.core.ai.multimodal.AskImageMessage
import com.omex.gallery.core.ai.multimodal.MultimodalModelInfo
import com.omex.gallery.core.ai.multimodal.MultimodalModelRepository
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskImageUiState(
    val mediaItem: MediaItem? = null,
    val messages: List<AskImageMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isImportingModel: Boolean = false,
    val errorMessage: String? = null
)

class AskImageViewModel(
    private val mediaRepository: MediaRepository,
    private val askImageEngine: AskImageEngine,
    private val modelRepository: MultimodalModelRepository,
    private val mediaId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(AskImageUiState())
    val uiState: StateFlow<AskImageUiState> = _uiState.asStateFlow()

    val modelInfo: StateFlow<MultimodalModelInfo> = modelRepository.modelInfo

    private var currentJob: Job? = null

    init {
        loadMediaItem()
    }

    private fun loadMediaItem() {
        viewModelScope.launch {
            val item = mediaRepository.getMediaById(mediaId)
            _uiState.update { it.copy(mediaItem = item) }
        }
    }

    fun sendMessage(context: Context, promptText: String) {
        if (promptText.isBlank()) return

        val userMsg = AskImageMessage(role = "user", content = promptText)
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isGenerating = true,
                errorMessage = null
            )
        }

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val history = _uiState.value.messages
            askImageEngine.askImage(
                context = context,
                mediaId = mediaId,
                mediaRepository = mediaRepository,
                prompt = promptText,
                history = history
            ).collect { responseMsg ->
                _uiState.update {
                    it.copy(
                        messages = it.messages + responseMsg,
                        isGenerating = false
                    )
                }
            }
        }
    }

    fun cancelGeneration() {
        currentJob?.cancel()
        currentJob = null
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun importModel(context: Context, uri: Uri) {
        _uiState.update { it.copy(isImportingModel = true, errorMessage = null) }
        viewModelScope.launch {
            val result = modelRepository.importModelFromUri(context, uri)
            _uiState.update {
                it.copy(
                    isImportingModel = false,
                    errorMessage = if (result.isFailure) result.exceptionOrNull()?.localizedMessage else null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        askImageEngine.close()
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val askImageEngine: AskImageEngine,
        private val modelRepository: MultimodalModelRepository,
        private val mediaId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AskImageViewModel(mediaRepository, askImageEngine, modelRepository, mediaId) as T
        }
    }
}
