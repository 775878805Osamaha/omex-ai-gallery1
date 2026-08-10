package com.omex.gallery.ui.feature_chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omex.gallery.core.ai.chat.AiChatEngine
import com.omex.gallery.core.ai.genai.GenerativeModelInfo
import com.omex.gallery.core.ai.genai.GenerativeModelRepository
import com.omex.gallery.core.data.local.ChatDao
import com.omex.gallery.core.data.local.ChatMessageEntity
import com.omex.gallery.core.data.local.ChatSessionEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiChatUiState(
    val currentSessionId: Long? = null,
    val isGenerating: Boolean = false,
    val streamingResponse: String = "",
    val errorMessage: String? = null
)

class AiChatViewModel(
    private val chatDao: ChatDao,
    private val chatEngine: AiChatEngine,
    private val modelRepository: GenerativeModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    val modelInfo: StateFlow<GenerativeModelInfo> = modelRepository.modelInfo

    val sessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _uiState
        .flatMapLatest { state ->
            val sessionId = state.currentSessionId
            if (sessionId != null) {
                chatDao.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            val existingSessions = chatDao.getAllSessions().firstOrNull()
            if (!existingSessions.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(currentSessionId = existingSessions.first().id)
            } else {
                createNewSession("محادثة جديدة")
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _uiState.value = _uiState.value.copy(currentSessionId = sessionId, errorMessage = null)
    }

    fun createNewSession(title: String = "محادثة جديدة") {
        viewModelScope.launch {
            val session = ChatSessionEntity(title = title)
            val newId = chatDao.insertSession(session)
            _uiState.value = _uiState.value.copy(currentSessionId = newId, errorMessage = null)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                val remaining = chatDao.getAllSessions().firstOrNull()
                if (!remaining.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(currentSessionId = remaining.first().id)
                } else {
                    createNewSession("محادثة جديدة")
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            var sessionId = _uiState.value.currentSessionId
            if (sessionId == null) {
                val newSession = ChatSessionEntity(title = trimmed.take(25))
                sessionId = chatDao.insertSession(newSession)
                _uiState.value = _uiState.value.copy(currentSessionId = sessionId)
            } else {
                val existing = chatDao.getSessionById(sessionId)
                if (existing != null && existing.title == "محادثة جديدة") {
                    chatDao.updateSession(existing.copy(title = trimmed.take(25), updatedAt = System.currentTimeMillis()))
                }
            }

            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = trimmed
            )
            chatDao.insertMessage(userMsg)

            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                streamingResponse = "",
                errorMessage = null
            )

            generationJob = viewModelScope.launch {
                try {
                    val isAvailable = chatEngine.isAvailable()
                    if (!isAvailable) {
                        val fallbackNotice = "نموذج الذكاء الاصطناعي المحلي غير مثبت في الجهاز. يرجى تنزيل ملف Gemma 2B ووضعه في المجلد المخصص لتفعيل المحادثة بدون إنترنت."
                        val assistantMsg = ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = fallbackNotice
                        )
                        chatDao.insertMessage(assistantMsg)
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            errorMessage = "النموذج غير مثبت"
                        )
                        return@launch
                    }

                    var accumulatedResponse = ""
                    chatEngine.sendMessage(trimmed).collect { chunk ->
                        accumulatedResponse = chunk
                        _uiState.value = _uiState.value.copy(streamingResponse = accumulatedResponse)
                    }

                    if (accumulatedResponse.isNotBlank()) {
                        val assistantMsg = ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = accumulatedResponse
                        )
                        chatDao.insertMessage(assistantMsg)
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "خطأ أثناء التوليد المحلي: ${e.message}"
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        streamingResponse = ""
                    )
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.value = _uiState.value.copy(isGenerating = false, streamingResponse = "")
    }

    fun clearCurrentConversation() {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            chatDao.deleteMessagesForSession(sessionId)
        }
    }

    fun refreshModelStatus() {
        modelRepository.refreshModelStatus()
    }

    fun importModel(context: android.content.Context, uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = modelRepository.importModelFromUri(context, uri)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (result.isSuccess) {
                    onResult(true, result.getOrDefault("تم استيراد النموذج بنجاح"))
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "فشل استيراد النموذج")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelGeneration()
        chatEngine.close()
    }

    class Factory(
        private val chatDao: ChatDao,
        private val chatEngine: AiChatEngine,
        private val modelRepository: GenerativeModelRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiChatViewModel(chatDao, chatEngine, modelRepository) as T
        }
    }
}
