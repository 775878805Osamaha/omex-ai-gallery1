package com.omex.gallery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.local.MediaItemEntity
import com.omex.gallery.core.data.repository.MediaRepositoryImpl
import com.omex.gallery.domain.model.MediaItem
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
            context = context
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test insert and query media items`() = runBlocking {
        val sampleItem = MediaItem(
            id = 1001L,
            uriString = "content://media/external/images/media/1001",
            filePath = "/sdcard/Pictures/test.jpg",
            fileName = "test.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1920,
            height = 1080,
            sizeBytes = 2048000L,
            dateTaken = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )

        repository.insertMediaItems(listOf(sampleItem))

        val allMedia = repository.getAllMedia().first()
        assertEquals(1, allMedia.size)
        assertEquals("test.jpg", allMedia[0].fileName)

        val retrieved = repository.getMediaById(1001L)
        assertNotNull(retrieved)
        assertEquals(1001L, retrieved?.id)
    }

    @Test
    fun `test favorite toggle and query filtering`() = runBlocking {
        val item1 = MediaItem(
            id = 2001L,
            uriString = "content://media/external/images/media/2001",
            filePath = "/sdcard/Pictures/photo1.jpg",
            fileName = "photo1.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1000000L,
            dateTaken = 1000L,
            dateModified = 1000L
        )

        repository.insertMediaItems(listOf(item1))
        
        var favorites = repository.getFavorites().first()
        assertTrue(favorites.isEmpty())

        repository.toggleFavorite(2001L, true)
        favorites = repository.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals(2001L, favorites[0].id)
    }

    @Test
    fun `test search functionality`() = runBlocking {
        val item = MediaItem(
            id = 3001L,
            uriString = "content://media/external/images/media/3001",
            filePath = "/sdcard/Pictures/vacation_beach.jpg",
            fileName = "vacation_beach.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            width = 1080,
            height = 1080,
            sizeBytes = 1000000L,
            dateTaken = 2000L,
            dateModified = 2000L
        )

        repository.insertMediaItems(listOf(item))

        val results = repository.searchMedia("beach").first()
        assertEquals(1, results.size)
        assertEquals("vacation_beach.jpg", results[0].fileName)
    }
}
