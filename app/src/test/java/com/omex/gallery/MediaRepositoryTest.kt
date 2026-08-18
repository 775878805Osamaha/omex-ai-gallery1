package com.omex.gallery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.repository.MediaRepositoryImpl
import com.omex.gallery.domain.model.MediaItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MediaRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MediaRepositoryImpl(
            mediaDao = db.mediaDao(),
            aiDao = db.aiDao(),
            categoryDao = db.categoryDao(),
            context = context
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveMediaItem() = runBlocking {
        val item = MediaItem(
            id = 1001L,
            uriString = "content://media/external/images/media/1001",
            filePath = "/storage/emulated/0/DCIM/Camera/test1.jpg",
            fileName = "test1.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1920,
            height = 1080,
            sizeBytes = 2048576L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )

        repository.insertMediaItems(listOf(item))

        val retrieved = repository.getMediaById(1001L)
        assertNotNull(retrieved)
        assertEquals("test1.jpg", retrieved?.fileName)
        assertEquals(1920, retrieved?.width)
    }

    @Test
    fun testFavoriteToggle() = runBlocking {
        val item = MediaItem(
            id = 2001L,
            uriString = "content://media/external/images/media/2001",
            filePath = "/storage/emulated/0/DCIM/Camera/fav_test.jpg",
            fileName = "fav_test.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            isFavorite = false
        )

        repository.insertMediaItems(listOf(item))
        repository.toggleFavorite(2001L, true)

        val updated = repository.getMediaById(2001L)
        assertEquals(true, updated?.isFavorite)
    }

    @Test
    fun testDeleteMediaItem() = runBlocking {
        val item = MediaItem(
            id = 3001L,
            uriString = "content://media/external/images/media/3001",
            filePath = "/storage/emulated/0/DCIM/Camera/delete_test.jpg",
            fileName = "delete_test.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )

        repository.insertMediaItems(listOf(item))
        repository.deleteMediaItem(3001L)

        val retrieved = repository.getMediaById(3001L)
        assertNull(retrieved)
    }

    @Test
    fun testSearchMediaByQuery() = runBlocking {
        val item1 = MediaItem(
            id = 4001L,
            uriString = "content://media/external/images/media/4001",
            filePath = "/storage/emulated/0/Pictures/vacation_beach.jpg",
            fileName = "vacation_beach.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1920,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = 1000L,
            dateModified = 1000L
        )
        val item2 = MediaItem(
            id = 4002L,
            uriString = "content://media/external/images/media/4002",
            filePath = "/storage/emulated/0/Pictures/office_meeting.jpg",
            fileName = "office_meeting.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1920,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = 2000L,
            dateModified = 2000L
        )

        repository.insertMediaItems(listOf(item1, item2))

        val results = repository.searchMedia("beach").first()
        assertEquals(1, results.size)
        assertEquals("vacation_beach.jpg", results[0].fileName)
    }

    @Test
    fun `test product smart folder returns only images classified as PRODUCT`() = runBlocking {
        // Item 1: Real product (watch)
        val productItem = MediaItem(
            id = 5001L,
            uriString = "content://media/external/images/media/5001",
            filePath = "/sdcard/Pictures/luxury_watch.jpg",
            fileName = "luxury_watch.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1500000L,
            dateTaken = 3000L,
            dateModified = 3000L
        )

        // Item 2: Person photo
        val personItem = MediaItem(
            id = 5002L,
            uriString = "content://media/external/images/media/5002",
            filePath = "/sdcard/Pictures/family_photo.jpg",
            fileName = "family_photo.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1500000L,
            dateTaken = 3001L,
            dateModified = 3001L
        )

        // Item 3: Medical receipt document
        val docItem = MediaItem(
            id = 5003L,
            uriString = "content://media/external/images/media/5003",
            filePath = "/sdcard/Pictures/hospital_receipt.jpg",
            fileName = "hospital_receipt.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 800000L,
            dateTaken = 3002L,
            dateModified = 3002L
        )

        repository.insertMediaItems(listOf(productItem, personItem, docItem))

        // Set up classifications
        val categoryDao = db.categoryDao()
        categoryDao.insertCrossRef(com.omex.gallery.core.data.local.MediaItemCategoryCrossRef(mediaId = 5001L, categoryId = "PRODUCT"))
        categoryDao.insertCrossRef(com.omex.gallery.core.data.local.MediaItemCategoryCrossRef(mediaId = 5002L, categoryId = "PERSON"))
        categoryDao.insertCrossRef(com.omex.gallery.core.data.local.MediaItemCategoryCrossRef(mediaId = 5003L, categoryId = "DOCUMENT"))

        // Query Products smart folder
        val productCategoryMedia = repository.getMediaForCategories(listOf("PRODUCT")).first()
        assertEquals(1, productCategoryMedia.size)
        assertEquals(5001L, productCategoryMedia[0].id)
        assertEquals("luxury_watch.jpg", productCategoryMedia[0].fileName)

        // Query Person smart folder
        val personCategoryMedia = repository.getMediaForCategories(listOf("PERSON")).first()
        assertEquals(1, personCategoryMedia.size)
        assertEquals(5002L, personCategoryMedia[0].id)

        // Query Document smart folder
        val docCategoryMedia = repository.getMediaForCategories(listOf("DOCUMENT")).first()
        assertEquals(1, docCategoryMedia.size)
        assertEquals(5003L, docCategoryMedia[0].id)
    }

    @Test
    fun testBatchDeleteMediaItems() = runBlocking {
        val item1 = MediaItem(
            id = 6001L,
            uriString = "content://media/external/images/media/6001",
            filePath = "/storage/emulated/0/DCIM/Camera/batch1.jpg",
            fileName = "batch1.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )
        val item2 = MediaItem(
            id = 6002L,
            uriString = "content://media/external/images/media/6002",
            filePath = "/storage/emulated/0/DCIM/Camera/batch2.jpg",
            fileName = "batch2.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )
        val item3 = MediaItem(
            id = 6003L,
            uriString = "content://media/external/images/media/6003",
            filePath = "/storage/emulated/0/DCIM/Camera/batch3.jpg",
            fileName = "batch3.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1024000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )

        repository.insertMediaItems(listOf(item1, item2, item3))
        assertEquals(3, repository.getAllMediaItems().size)

        // Delete item1 and item2 in a single batch
        val deleteResult = repository.deleteMediaItems(listOf(6001L, 6002L))
        assertEquals(true, deleteResult.isSuccess)
        assertEquals(2, deleteResult.getOrNull())

        val remaining = repository.getAllMediaItems()
        assertEquals(1, remaining.size)
        assertEquals(6003L, remaining[0].id)
    }

    @Test
    fun `test intelligent search by filename and OCR text`() = runBlocking {
        val item1 = MediaItem(
            id = 7001L,
            uriString = "content://media/external/images/media/7001",
            filePath = "/sdcard/Pictures/annual_financial_report.jpg",
            fileName = "annual_financial_report.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 500000L,
            dateTaken = 1000L,
            dateModified = 1000L
        )
        val item2 = MediaItem(
            id = 7002L,
            uriString = "content://media/external/images/media/7002",
            filePath = "/sdcard/Pictures/IMG_0029.jpg",
            fileName = "IMG_0029.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 500000L,
            dateTaken = 2000L,
            dateModified = 2000L
        )

        repository.insertMediaItems(listOf(item1, item2))

        // Insert OCR text for IMG_0029
        val aiDao = db.aiDao()
        aiDao.insertOcrText(
            com.omex.gallery.core.data.local.OcrTextEntity(
                mediaId = 7002L,
                extractedText = "Invoice #98765 Total Due: $150.00 Thank you for shopping with us"
            )
        )

        // 1. Search by filename
        val filenameResults = repository.searchMedia("financial_report").first()
        assertEquals(1, filenameResults.size)
        assertEquals(7001L, filenameResults[0].id)

        // 2. Search by OCR extracted text
        val ocrResults = repository.searchMedia("Invoice #98765").first()
        assertEquals(1, ocrResults.size)
        assertEquals(7002L, ocrResults[0].id)
    }

    @Test
    fun `test intelligent search by AI category alias and Arabic keywords`() = runBlocking {
        val btcItem = MediaItem(
            id = 8001L,
            uriString = "content://media/external/images/media/8001",
            filePath = "/sdcard/Pictures/chart_screenshot.png",
            fileName = "chart_screenshot.png",
            mimeType = "image/png",
            isVideo = false,
            width = 1080,
            height = 1920,
            sizeBytes = 600000L,
            dateTaken = 1000L,
            dateModified = 1000L
        )
        val carItem = MediaItem(
            id = 8002L,
            uriString = "content://media/external/images/media/8002",
            filePath = "/sdcard/Pictures/sports_sedan.jpg",
            fileName = "sports_sedan.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1920,
            height = 1080,
            sizeBytes = 900000L,
            dateTaken = 2000L,
            dateModified = 2000L
        )

        repository.insertMediaItems(listOf(btcItem, carItem))

        val categoryDao = db.categoryDao()
        categoryDao.insertCrossRef(com.omex.gallery.core.data.local.MediaItemCategoryCrossRef(mediaId = 8001L, categoryId = "TRADING"))
        categoryDao.insertCrossRef(com.omex.gallery.core.data.local.MediaItemCategoryCrossRef(mediaId = 8002L, categoryId = "CAR"))

        // Search Arabic "تداول" -> should match TRADING
        val tradingResults = repository.searchMedia("تداول").first()
        assertEquals(1, tradingResults.size)
        assertEquals(8001L, tradingResults[0].id)

        // Search English "BTC" -> should match TRADING category alias
        val btcResults = repository.searchMedia("BTC").first()
        assertEquals(1, btcResults.size)
        assertEquals(8001L, btcResults[0].id)

        // Search Arabic "سيارة" -> should match CAR
        val carResults = repository.searchMedia("سيارة").first()
        assertEquals(1, carResults.size)
        assertEquals(8002L, carResults[0].id)
    }

    @Test
    fun `test advanced multi-criteria search with video and favorite filters`() = runBlocking {
        val favVideo = MediaItem(
            id = 9001L,
            uriString = "content://media/external/video/media/9001",
            filePath = "/sdcard/Movies/trading_tutorial.mp4",
            fileName = "trading_tutorial.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            isFavorite = true,
            width = 1920,
            height = 1080,
            sizeBytes = 25000000L,
            dateTaken = 1000L,
            dateModified = 1000L
        )
        val nonFavPhoto = MediaItem(
            id = 9002L,
            uriString = "content://media/external/images/media/9002",
            filePath = "/sdcard/Pictures/trading_snapshot.jpg",
            fileName = "trading_snapshot.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            isFavorite = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1200000L,
            dateTaken = 2000L,
            dateModified = 2000L
        )

        repository.insertMediaItems(listOf(favVideo, nonFavPhoto))

        // Multi-criteria 1: query="trading" + isVideo=true
        val videoFilter = com.omex.gallery.domain.model.SearchFilterState(
            query = "trading",
            isVideo = true
        )
        val videoResults = repository.searchMediaAdvanced(videoFilter).first()
        assertEquals(1, videoResults.size)
        assertEquals(9001L, videoResults[0].id)

        // Multi-criteria 2: query="trading" + isFavorite=true
        val favFilter = com.omex.gallery.domain.model.SearchFilterState(
            query = "trading",
            isFavorite = true
        )
        val favResults = repository.searchMediaAdvanced(favFilter).first()
        assertEquals(1, favResults.size)
        assertEquals(9001L, favResults[0].id)

        // Multi-criteria 3: query="trading" + isVideo=false
        val photoFilter = com.omex.gallery.domain.model.SearchFilterState(
            query = "trading",
            isVideo = false
        )
        val photoResults = repository.searchMediaAdvanced(photoFilter).first()
        assertEquals(1, photoResults.size)
        assertEquals(9002L, photoResults[0].id)
    }

    @Test
    fun `test search by EXIF camera make and model metadata`() = runBlocking {
        val sonyPhoto = MediaItem(
            id = 9501L,
            uriString = "content://media/external/images/media/9501",
            filePath = "/sdcard/DCIM/DSC001.JPG",
            fileName = "DSC001.JPG",
            mimeType = "image/jpeg",
            cameraMake = "Sony",
            cameraModel = "ILCE-7M4",
            iso = "100",
            isVideo = false,
            width = 4000,
            height = 3000,
            sizeBytes = 8000000L,
            dateTaken = 1000L,
            dateModified = 1000L
        )
        val canonPhoto = MediaItem(
            id = 9502L,
            uriString = "content://media/external/images/media/9502",
            filePath = "/sdcard/DCIM/IMG_999.JPG",
            fileName = "IMG_999.JPG",
            mimeType = "image/jpeg",
            cameraMake = "Canon",
            cameraModel = "EOS R5",
            iso = "400",
            isVideo = false,
            width = 4000,
            height = 3000,
            sizeBytes = 8500000L,
            dateTaken = 2000L,
            dateModified = 2000L
        )

        repository.insertMediaItems(listOf(sonyPhoto, canonPhoto))

        // Search by Camera Make "Sony"
        val sonyResults = repository.searchMedia("Sony").first()
        assertEquals(1, sonyResults.size)
        assertEquals(9501L, sonyResults[0].id)

        // Search by Camera Model "EOS R5"
        val canonResults = repository.searchMedia("EOS R5").first()
        assertEquals(1, canonResults.size)
        assertEquals(9502L, canonResults[0].id)
    }
}
