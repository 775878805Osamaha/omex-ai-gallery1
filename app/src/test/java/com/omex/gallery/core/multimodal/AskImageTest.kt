package com.omex.gallery.core.multimodal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.omex.gallery.core.ai.multimodal.AskImageEngine
import com.omex.gallery.core.ai.multimodal.AskImageMessage
import com.omex.gallery.core.ai.multimodal.MultimodalModelManager
import com.omex.gallery.core.ai.multimodal.MultimodalModelRepository
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaRepository
import com.omex.gallery.ui.feature_ask_image.AskImageViewModel
import com.omex.gallery.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AskImageTest {

    private lateinit var context: Context
    private lateinit var modelRepository: MultimodalModelRepository
    private lateinit var modelManager: MultimodalModelManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        modelRepository = MultimodalModelRepository(context)
        modelManager = MultimodalModelManager(context, modelRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMultimodalModelRepository_UninstalledByDefault() {
        val info = modelRepository.checkModelStatus()
        assertFalse(info.isInstalled)
        assertFalse(info.isValidated)
        assertEquals("gemma-3n-e2b-it.litertlm", info.modelFileName)
        assertTrue(info.statusMessage.contains(".litertlm"))
    }

    @Test
    fun testMultimodalModelRepository_ValidatesLitertlmExtensionOnly() {
        val modelsDir = File(context.filesDir, "litert_models")
        modelsDir.mkdirs()

        // Create dummy invalid file extension (.bin)
        val binFile = File(modelsDir, "test.bin")
        binFile.writeBytes(ByteArray(1024 * 1024))

        val infoBin = modelRepository.checkModelStatus()
        assertFalse("Raw .bin files should not be recognized as valid .litertlm models", infoBin.isInstalled)

        // Create dummy valid extension (.litertlm) with minimum size
        val litertFile = File(modelsDir, "gemma-3n-e2b-it.litertlm")
        val dummyData = ByteArray(12 * 1024 * 1024) { 1 } // 12MB
        litertFile.writeBytes(dummyData)

        val infoLitert = modelRepository.checkModelStatus()
        assertTrue(infoLitert.isInstalled)
        assertTrue(infoLitert.isValidated)
        assertEquals("gemma-3n-e2b-it.litertlm", infoLitert.modelFileName)

        // Cleanup
        litertFile.delete()
        binFile.delete()
    }

    @Test
    fun testAskImageRoute_CreatedCorrectly() {
        val route = Screen.AskImage.createRoute(42L)
        assertEquals("ask_image/42", route)
    }

    @Test
    fun testAskImageViewModel_SendMessageAndClear() = runTest {
        val fakeRepo = FakeMediaRepository()
        val fakeEngine = FakeAskImageEngine()
        val viewModel = AskImageViewModel(fakeRepo, fakeEngine, modelRepository, 1L)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendMessage(context, "What is in this picture?")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("What is in this picture?", state.messages[0].content)
        assertEquals("Fake AI Response for Image 1", state.messages[1].content)

        viewModel.clearMessages()
        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    private class FakeAskImageEngine : AskImageEngine {
        override suspend fun isAvailable(): Boolean = true

        override fun askImage(
            context: Context,
            mediaId: Long,
            mediaRepository: MediaRepository,
            prompt: String,
            history: List<AskImageMessage>
        ): Flow<AskImageMessage> = flow {
            emit(
                AskImageMessage(
                    role = "assistant",
                    content = "Fake AI Response for Image $mediaId",
                    isMultimodalReal = false
                )
            )
        }

        override fun close() {}
    }

    private class FakeMediaRepository : MediaRepository {
        override suspend fun getRoomMediaCount(): Int = 0
        override suspend fun getRoomPhotosCount(): Int = 0
        override suspend fun getRoomVideosCount(): Int = 0
        override suspend fun getLastIndexingError(): String = "None"

        override suspend fun getMediaById(id: Long): MediaItem {
            return MediaItem(
                id = id,
                fileName = "sample.jpg",
                filePath = "/path/sample.jpg",
                uriString = "content://media/sample.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 1024,
                dateTaken = System.currentTimeMillis(),
                width = 1920,
                height = 1080,
                durationMs = 0,
                isVideo = false
            )
        }

        override suspend fun getMediaItemWithAi(mediaId: Long): com.omex.gallery.domain.model.MediaItemWithAi? = null
        override fun getAllMedia(): Flow<List<MediaItem>> = flow { emit(emptyList()) }
        override fun getAllMediaPaged(): Flow<androidx.paging.PagingData<MediaItem>> = flow { emit(androidx.paging.PagingData.empty()) }
        override fun getPhotos(): Flow<List<MediaItem>> = flow { emit(emptyList()) }
        override fun getPhotosPaged(): Flow<androidx.paging.PagingData<MediaItem>> = flow { emit(androidx.paging.PagingData.empty()) }
        override fun getVideos(): Flow<List<MediaItem>> = flow { emit(emptyList()) }
        override fun getVideosPaged(): Flow<androidx.paging.PagingData<MediaItem>> = flow { emit(androidx.paging.PagingData.empty()) }
        override fun getFavorites(): Flow<List<MediaItem>> = flow { emit(emptyList()) }
        override fun getFavoritesPaged(): Flow<androidx.paging.PagingData<MediaItem>> = flow { emit(androidx.paging.PagingData.empty()) }
        override fun searchMedia(query: String): Flow<List<MediaItem>> = flow { emit(emptyList()) }
        override fun searchMediaAdvanced(filterState: com.omex.gallery.domain.model.SearchFilterState): Flow<List<MediaItem>> = flow { emit(emptyList()) }
        override fun getSearchFilterOptions(): Flow<com.omex.gallery.domain.model.SearchFilterOptions> = flow { emit(com.omex.gallery.domain.model.SearchFilterOptions()) }
        override fun getIndexingProgress(): Flow<com.omex.gallery.domain.model.IndexingProgress> = flow { emit(com.omex.gallery.domain.model.IndexingProgress()) }
        override suspend fun getAllMediaItems(): List<MediaItem> = emptyList()
        override suspend fun insertMediaItems(items: List<MediaItem>) {}
        override suspend fun deleteMediaItem(id: Long) {}
        override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {}
        override suspend fun scanAndIndexGallery(isFullReindex: Boolean): Result<Int> = Result.success(0)
        override suspend fun regenerateThumbnails(): Result<Int> = Result.success(0)
        override suspend fun getClassificationsForMedia(mediaId: Long): List<com.omex.gallery.domain.model.AiClassification> = emptyList()
        override suspend fun getObjectsForMedia(mediaId: Long): List<com.omex.gallery.domain.model.AiObject> = emptyList()
        override suspend fun getFacesForMedia(mediaId: Long): List<com.omex.gallery.domain.model.AiFace> = emptyList()
        override suspend fun getMetadataForMedia(mediaId: Long): com.omex.gallery.domain.model.AiMetadata? = null
        override suspend fun getOcrTextForMedia(mediaId: Long): com.omex.gallery.domain.model.AiOcrText? = null
        override fun getDuplicateGroups(): Flow<List<com.omex.gallery.domain.model.DuplicateGroupWithMedia>> = flow { emit(emptyList()) }
        override fun getPersonGroups(): Flow<List<com.omex.gallery.domain.model.PersonGroup>> = flow { emit(emptyList()) }
        override suspend fun getPersonMediaItems(clusterId: String): List<MediaItem> = emptyList()
        override suspend fun runAiPipelineOnMedia(context: Context, mediaItem: MediaItem): Result<Boolean> = Result.success(true)
        override suspend fun runFullGalleryAiScan(context: Context): Result<Boolean> = Result.success(true)
        override suspend fun superResolveImage(context: Context, mediaItem: MediaItem, scaleFactor: Int, onProgress: (Float) -> Unit): Result<String> = Result.success("")
    }
}
