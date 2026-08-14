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
}
