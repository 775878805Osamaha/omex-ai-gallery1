package com.omex.gallery.core.search

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omex.gallery.core.data.local.AiDao
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.local.MediaItemEntity
import com.omex.gallery.core.data.local.OcrTextEntity
import com.omex.gallery.core.data.repository.MediaRepositoryImpl
import com.omex.gallery.core.ai.ocr.LocalFallbackOcrEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OcrAndSearchTest {

    private lateinit var db: AppDatabase
    private lateinit var aiDao: AiDao
    private lateinit var repository: MediaRepositoryImpl
    private lateinit var searchHistoryRepository: SearchHistoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        aiDao = db.aiDao()
        repository = MediaRepositoryImpl(
            mediaDao = db.mediaDao(),
            aiDao = db.aiDao(),
            context = context
        )
        searchHistoryRepository = SearchHistoryRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test OCR text entity storage and retrieval`() = runBlocking {
        val mediaId = 5001L
        val ocrText = OcrTextEntity(
            mediaId = mediaId,
            extractedText = "إيصال شراء بقيمة 150 ريال - فاتورة رقم 98765",
            language = "ar",
            processingStatus = "COMPLETED"
        )

        aiDao.insertOcrText(ocrText)

        val retrieved = aiDao.getOcrForMedia(mediaId)
        assertNotNull(retrieved)
        assertEquals("ar", retrieved?.language)
        assertEquals("COMPLETED", retrieved?.processingStatus)
        assertTrue(retrieved?.extractedText?.contains("فاتورة") == true)
    }

    @Test
    fun `test local text search with OCR content`() = runBlocking {
        val item = MediaItemEntity(
            id = 5002L,
            uriString = "content://media/external/images/media/5002",
            filePath = "/sdcard/Pictures/invoice_scan.jpg",
            fileName = "invoice_scan.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1920,
            sizeBytes = 500000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )
        db.mediaDao().insertAll(listOf(item))

        val ocrEntity = OcrTextEntity(
            mediaId = 5002L,
            extractedText = "Total Amount: $250.00 Invoice #12345 Paid in Full",
            language = "en",
            processingStatus = "COMPLETED"
        )
        aiDao.insertOcrText(ocrEntity)

        // Search by OCR text keyword
        val searchResults = db.mediaDao().searchMedia("12345").first()
        assertEquals(1, searchResults.size)
        assertEquals("invoice_scan.jpg", searchResults[0].fileName)
    }

    @Test
    fun `test empty OCR result handling`() = runBlocking {
        val mediaId = 5003L
        val emptyOcr = OcrTextEntity(
            mediaId = mediaId,
            extractedText = "",
            language = null,
            processingStatus = "EMPTY"
        )

        aiDao.insertOcrText(emptyOcr)

        val retrieved = aiDao.getOcrForMedia(mediaId)
        assertNotNull(retrieved)
        assertEquals("", retrieved?.extractedText)
        assertEquals("EMPTY", retrieved?.processingStatus)

        val searchResults = aiDao.searchOcrText("nonexistent")
        assertTrue(searchResults.isEmpty())
    }

    @Test
    fun `test failed OCR processing handling`() = runBlocking {
        val mediaId = 5004L
        val failedOcr = OcrTextEntity(
            mediaId = mediaId,
            extractedText = "",
            processingStatus = "FAILED"
        )

        aiDao.insertOcrText(failedOcr)

        val retrieved = aiDao.getOcrForMedia(mediaId)
        assertNotNull(retrieved)
        assertEquals("FAILED", retrieved?.processingStatus)
    }

    @Test
    fun `test LocalFallbackOcrEngine execution`() = runBlocking {
        val fallbackEngine = LocalFallbackOcrEngine()
        fallbackEngine.initialize()

        val mockBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val result = fallbackEngine.processImage(mockBitmap)

        assertTrue(result.isSuccess)
        val ocrResult = result.getOrThrow()
        assertEquals("", ocrResult.extractedText)

        fallbackEngine.close()
    }

    @Test
    fun `test SearchHistoryRepository add, query, remove, clear`() = runBlocking {
        searchHistoryRepository.clearHistory()

        searchHistoryRepository.addQuery("فاتورة")
        searchHistoryRepository.addQuery("invoice")

        val history = searchHistoryRepository.getRecentQueries().first()
        assertTrue(history.contains("invoice"))
        assertTrue(history.contains("فاتورة"))

        searchHistoryRepository.removeQuery("invoice")
        val updatedHistory = searchHistoryRepository.getRecentQueries().first()
        assertTrue(!updatedHistory.contains("invoice"))

        searchHistoryRepository.clearHistory()
        val clearedHistory = searchHistoryRepository.getRecentQueries().first()
        assertTrue(clearedHistory.isEmpty())
    }
}
