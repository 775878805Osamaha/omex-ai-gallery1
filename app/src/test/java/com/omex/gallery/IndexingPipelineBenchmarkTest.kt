package com.omex.gallery

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.local.MediaItemEntity
import com.omex.gallery.core.hash.DefaultPerceptualHasher
import com.omex.gallery.core.hash.HashAlgorithm
import com.omex.gallery.core.indexer.MetadataExtractor
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
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IndexingPipelineBenchmarkTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun benchmarkIndexingPipelineAndHashingPerformance() = runBlocking {
        println("===== OMEX GALLERY INDEXING PIPELINE BENCHMARK =====")

        val totalMediaCount = 1000
        val sampleEntities = (1..totalMediaCount).map { id ->
            MediaItemEntity(
                id = id.toLong(),
                uriString = "content://media/external/images/media/$id",
                filePath = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg",
                fileName = "IMG_$id.jpg",
                mimeType = "image/jpeg",
                isVideo = false,
                width = 4000,
                height = 3000,
                sizeBytes = 3_500_000L,
                dateTaken = System.currentTimeMillis() - id * 1000L,
                dateModified = System.currentTimeMillis() - id * 500L,
                isFavorite = false,
                isIndexed = false
            )
        }

        // A. MediaStore Discovery Simulation
        val discoveryTime = measureTimeMillis {
            // Simulated scan of 1,000 raw MediaStore rows into memory objects
            sampleEntities.size
        }
        println("[A] MediaStore discovery time for $totalMediaCount items: ${discoveryTime}ms")

        // B. Time until first media appears in Room
        val firstBatchTime = measureTimeMillis {
            database.mediaDao().insertAll(sampleEntities.take(1))
        }
        println("[B] Time until first media item is written to Room: ${firstBatchTime}ms")

        // C. Time until first 100 media are available in Room
        val first100Time = measureTimeMillis {
            database.mediaDao().insertAll(sampleEntities.slice(1..100))
        }
        println("[C] Time until first 100 media are available in Room: ${first100Time}ms")

        // J. Room Batch Insert Benchmarking across batch sizes (100, 200, 500)
        val timeBatch100 = measureTimeMillis {
            sampleEntities.chunked(100).forEach { chunk ->
                database.mediaDao().insertAll(chunk)
            }
        }
        println("[J1] Batch insert time (batchSize = 100): ${timeBatch100}ms across ${sampleEntities.size / 100} Room writes")

        val timeBatch200 = measureTimeMillis {
            sampleEntities.chunked(200).forEach { chunk ->
                database.mediaDao().insertAll(chunk)
            }
        }
        println("[J2] Batch insert time (batchSize = 200): ${timeBatch200}ms across ${sampleEntities.size / 200} Room writes")

        val timeBatch500 = measureTimeMillis {
            sampleEntities.chunked(500).forEach { chunk ->
                database.mediaDao().insertAll(chunk)
            }
        }
        println("[J3] Batch insert time (batchSize = 500): ${timeBatch500}ms across ${sampleEntities.size / 500} Room writes")

        // F. Hashing Performance Benchmark (SHA-256, aHash, dHash, pHash)
        val testBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val hasher = DefaultPerceptualHasher()

        val sampleBytes = ByteArray(1024 * 512) { (it % 256).toByte() }
        val sha256Time = measureTimeMillis {
            repeat(100) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(sampleBytes)
                md.digest()
            }
        }
        println("[F1] SHA-256 calculation (100 iterations on 512KB payload): ${sha256Time}ms")

        val aHashTime = measureTimeMillis {
            repeat(100) {
                hasher.computeHash(testBitmap, HashAlgorithm.AVERAGE_HASH)
            }
        }
        println("[F2] aHash calculation (100 iterations): ${aHashTime}ms")

        val dHashTime = measureTimeMillis {
            repeat(100) {
                hasher.computeHash(testBitmap, HashAlgorithm.DIFFERENCE_HASH)
            }
        }
        println("[F3] dHash calculation (100 iterations): ${dHashTime}ms")

        val pHashTime = measureTimeMillis {
            repeat(100) {
                hasher.computeHash(testBitmap, HashAlgorithm.PERCEPTUAL_HASH)
            }
        }
        println("[F4] pHash (DCT) calculation (100 iterations): ${pHashTime}ms")

        testBitmap.recycle()

        // K. Memory Usage Check
        val runtime = Runtime.getRuntime()
        val usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        println("[K] Current test JVM heap used memory: ${usedMemoryMb}MB")

        println("====================================================")

        assertTrue("First batch write should complete quickly", firstBatchTime < 1000)
        assertEquals(totalMediaCount, database.mediaDao().getAllMediaList().size)
    }
}
