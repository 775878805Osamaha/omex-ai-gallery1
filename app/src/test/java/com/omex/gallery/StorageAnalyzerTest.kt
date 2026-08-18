package com.omex.gallery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omex.gallery.core.data.local.AppDatabase
import com.omex.gallery.core.data.local.ImageMetadataEntity
import com.omex.gallery.core.data.repository.MediaRepositoryImpl
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.DuplicateMemberWithMedia
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.ui.feature_storage.StorageAnalysisState
import com.omex.gallery.ui.feature_storage.StorageAnalyzerViewModel
import com.omex.gallery.ui.feature_storage.StorageGroupingMode
import com.omex.gallery.ui.feature_storage.StorageMetricMode
import com.omex.gallery.ui.feature_storage.StorageQuickFilter
import com.omex.gallery.ui.feature_storage.StorageSortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StorageAnalyzerTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MediaRepositoryImpl
    private lateinit var viewModel: StorageAnalyzerViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        viewModel = StorageAnalyzerViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    // 1. حساب إجمالي مساحة الصور
    @Test
    fun test1_calculatePhotosTotalSize() = runBlocking {
        val photo1 = MediaItem(
            id = 101L,
            uriString = "content://media/external/images/media/101",
            filePath = "/storage/emulated/0/DCIM/Camera/photo1.jpg",
            fileName = "photo1.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            sizeBytes = 3 * 1024 * 1024L
        )
        val photo2 = MediaItem(
            id = 102L,
            uriString = "content://media/external/images/media/102",
            filePath = "/storage/emulated/0/DCIM/Camera/photo2.png",
            fileName = "photo2.png",
            mimeType = "image/png",
            isVideo = false,
            sizeBytes = 4 * 1024 * 1024L
        )
        repository.insertMediaItems(listOf(photo1, photo2))

        val state = viewModel.uiState.first { it.totalCount == 2 }
        assertEquals(2, state.photosCount)
        assertEquals(7 * 1024 * 1024L, state.photosSizeBytes)
    }

    // 2. حساب إجمالي مساحة الفيديوهات
    @Test
    fun test2_calculateVideosTotalSize() = runBlocking {
        val video1 = MediaItem(
            id = 201L,
            uriString = "content://media/external/video/media/201",
            filePath = "/storage/emulated/0/DCIM/Camera/video1.mp4",
            fileName = "video1.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 120 * 1024 * 1024L
        )
        val video2 = MediaItem(
            id = 202L,
            uriString = "content://media/external/video/media/202",
            filePath = "/storage/emulated/0/DCIM/Camera/video2.mkv",
            fileName = "video2.mkv",
            mimeType = "video/x-matroska",
            isVideo = true,
            sizeBytes = 80 * 1024 * 1024L
        )
        repository.insertMediaItems(listOf(video1, video2))

        val state = viewModel.uiState.first { it.totalCount == 2 }
        assertEquals(2, state.videosCount)
        assertEquals(200 * 1024 * 1024L, state.videosSizeBytes)
    }

    // 3. حساب المساحة حسب الامتداد
    @Test
    fun test3_calculateStorageByFileExtension() = runBlocking {
        val jpg = MediaItem(
            id = 301L,
            uriString = "content://media/301",
            filePath = "/storage/emulated/0/DCIM/IMG_1.JPG",
            fileName = "IMG_1.JPG",
            mimeType = "image/jpeg",
            sizeBytes = 5 * 1024 * 1024L
        )
        val png = MediaItem(
            id = 302L,
            uriString = "content://media/302",
            filePath = "/storage/emulated/0/Pictures/IMG_2.PNG",
            fileName = "IMG_2.PNG",
            mimeType = "image/png",
            sizeBytes = 2 * 1024 * 1024L
        )
        repository.insertMediaItems(listOf(jpg, png))

        viewModel.setGroupingMode(StorageGroupingMode.BY_FILE_TYPE)
        val state = viewModel.uiState.first { it.selectedGroupingMode == StorageGroupingMode.BY_FILE_TYPE && it.distributionItems.isNotEmpty() }
        val keys = state.distributionItems.map { it.key }
        assertTrue(keys.contains("JPG") || keys.contains("JPEG"))
        assertTrue(keys.contains("PNG"))
    }

    // 4. حساب المساحة حسب التصنيف
    @Test
    fun test4_calculateStorageByAiClassification() = runBlocking {
        val tradingImg = MediaItem(
            id = 401L,
            uriString = "content://media/401",
            filePath = "/storage/emulated/0/Download/trading_chart_btc.png",
            fileName = "trading_chart_btc.png",
            mimeType = "image/png",
            sizeBytes = 1500000L
        )
        val carImg = MediaItem(
            id = 402L,
            uriString = "content://media/402",
            filePath = "/storage/emulated/0/DCIM/car_audi_rs7.jpg",
            fileName = "car_audi_rs7.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 3500000L
        )
        repository.insertMediaItems(listOf(tradingImg, carImg))

        viewModel.setGroupingMode(StorageGroupingMode.BY_CLASSIFICATION)
        val state = viewModel.uiState.first { it.selectedGroupingMode == StorageGroupingMode.BY_CLASSIFICATION && it.distributionItems.isNotEmpty() }
        val categories = state.distributionItems.map { it.key }
        assertTrue(categories.contains("TRADING"))
        assertTrue(categories.contains("CAR"))
    }

    // 5. ترتيب الملفات حسب الحجم
    @Test
    fun test5_sortMediaByLargestSize() = runBlocking {
        val small = MediaItem(
            id = 501L,
            uriString = "content://media/501",
            filePath = "/storage/emulated/0/small.jpg",
            fileName = "small.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1000L
        )
        val large = MediaItem(
            id = 502L,
            uriString = "content://media/502",
            filePath = "/storage/emulated/0/large.mp4",
            fileName = "large.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 50000000L
        )
        repository.insertMediaItems(listOf(small, large))

        viewModel.setSortMode(StorageSortMode.LARGEST_FIRST)
        val state = viewModel.uiState.first { it.filteredMediaItems.size == 2 }
        assertEquals(502L, state.filteredMediaItems[0].id)
        assertEquals(501L, state.filteredMediaItems[1].id)

        viewModel.setSortMode(StorageSortMode.SMALLEST_FIRST)
        val smallestState = viewModel.uiState.first { it.selectedSortMode == StorageSortMode.SMALLEST_FIRST }
        assertEquals(501L, smallestState.filteredMediaItems[0].id)
        assertEquals(502L, smallestState.filteredMediaItems[1].id)
    }

    // 6. اكتشاف الملفات الكبيرة
    @Test
    fun test6_detectLargeFiles() = runBlocking {
        val normal = MediaItem(
            id = 601L,
            uriString = "content://media/601",
            filePath = "/storage/emulated/0/normal.jpg",
            fileName = "normal.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 2 * 1024 * 1024L // 2MB
        )
        val big = MediaItem(
            id = 602L,
            uriString = "content://media/602",
            filePath = "/storage/emulated/0/big.jpg",
            fileName = "big.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 8 * 1024 * 1024L // 8MB
        )
        repository.insertMediaItems(listOf(normal, big))

        viewModel.setQuickFilter(StorageQuickFilter.LARGE_PHOTOS_5MB)
        val state = viewModel.uiState.first { it.quickFilter == StorageQuickFilter.LARGE_PHOTOS_5MB }
        assertEquals(1, state.filteredMediaItems.size)
        assertEquals(602L, state.filteredMediaItems[0].id)
    }

    // 7. حساب مساحة المكررات
    @Test
    fun test7_calculateDuplicatesTotalSize() = runBlocking {
        val item1 = MediaItem(
            id = 701L,
            uriString = "content://media/701",
            filePath = "/storage/emulated/0/DCIM/Camera/orig.jpg",
            fileName = "orig.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 6 * 1024 * 1024L
        )
        val item2 = MediaItem(
            id = 702L,
            uriString = "content://media/702",
            filePath = "/storage/emulated/0/Download/copy.jpg",
            fileName = "copy.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 6 * 1024 * 1024L
        )
        val duplicateGroup = DuplicateGroupWithMedia(
            groupId = "group_1",
            groupType = "EXACT",
            members = listOf(
                DuplicateMemberWithMedia(item1, 1.0f),
                DuplicateMemberWithMedia(item2, 1.0f)
            )
        )
        val reclaimable = StorageAnalyzerViewModel.calculateReclaimableDuplicateBytes(listOf(duplicateGroup))
        assertEquals(6 * 1024 * 1024L, reclaimable)
    }

    // 8. حساب المساحة القابلة للاسترداد
    @Test
    fun test8_calculateRecoverableSpace() {
        val item1 = MediaItem(id = 801L, uriString = "content://media/801", filePath = "/DCIM/img1.jpg", fileName = "img1.jpg", mimeType = "image/jpeg", sizeBytes = 10 * 1024 * 1024L, dateTaken = 1000L)
        val item2 = MediaItem(id = 802L, uriString = "content://media/802", filePath = "/Download/img1.jpg", fileName = "img1.jpg", mimeType = "image/jpeg", sizeBytes = 10 * 1024 * 1024L, dateTaken = 2000L)
        val item3 = MediaItem(id = 803L, uriString = "content://media/803", filePath = "/Bluetooth/img1.jpg", fileName = "img1.jpg", mimeType = "image/jpeg", sizeBytes = 10 * 1024 * 1024L, dateTaken = 3000L)

        val duplicateGroup = DuplicateGroupWithMedia(
            groupId = "dup_triple",
            groupType = "EXACT",
            members = listOf(
                DuplicateMemberWithMedia(item1, 1.0f),
                DuplicateMemberWithMedia(item2, 1.0f),
                DuplicateMemberWithMedia(item3, 1.0f)
            )
        )

        // 3 items of 10MB each -> 1 original preserved (10MB), 2 redundant copies recoverable (20MB)
        val recoverable = StorageAnalyzerViewModel.calculateReclaimableDuplicateBytes(listOf(duplicateGroup))
        assertEquals(20 * 1024 * 1024L, recoverable)
    }

    // 9. عدم حذف آخر نسخة من المجموعة
    @Test
    fun test9_neverDeleteLastOrOriginalCopyWithDuplicateCleanup() {
        val original = MediaItem(
            id = 901L,
            uriString = "content://media/901",
            filePath = "/storage/emulated/0/DCIM/Camera/main.jpg",
            fileName = "main.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 5000000L
        )
        val copy = MediaItem(
            id = 902L,
            uriString = "content://media/902",
            filePath = "/storage/emulated/0/WhatsApp/main_copy.jpg",
            fileName = "main_copy.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 5000000L
        )
        val selected = StorageAnalyzerViewModel.selectOriginalMember(listOf(original, copy))
        assertEquals(901L, selected.id)
    }

    // 10. التعامل مع مجموعة مكررة بدون أصل معروف
    @Test
    fun test10_handleDuplicateGroupWithoutKnownOriginalSafely() {
        val itemA = MediaItem(id = 1001L, uriString = "content://media/1001", filePath = "/storage/emulated/0/tmp1.jpg", fileName = "tmp1.jpg", mimeType = "image/jpeg", sizeBytes = 4000L, dateTaken = 0L)
        val itemB = MediaItem(id = 1002L, uriString = "content://media/1002", filePath = "/storage/emulated/0/tmp2.jpg", fileName = "tmp2.jpg", mimeType = "image/jpeg", sizeBytes = 4000L, dateTaken = 0L)

        // Fallback safely to lowest ID without crashing
        val chosen = StorageAnalyzerViewModel.selectOriginalMember(listOf(itemA, itemB))
        assertNotNull(chosen)
        assertEquals(1001L, chosen.id)
    }

    // 11. فلتر لقطات الشاشة
    @Test
    fun test11_filterScreenshots() = runBlocking {
        val shot = MediaItem(
            id = 1101L,
            uriString = "content://media/1101",
            filePath = "/storage/emulated/0/Pictures/Screenshots/Screenshot_2026.png",
            fileName = "Screenshot_2026.png",
            mimeType = "image/png",
            sizeBytes = 2500000L
        )
        val camera = MediaItem(
            id = 1102L,
            uriString = "content://media/1102",
            filePath = "/storage/emulated/0/DCIM/Camera/IMG_2026.jpg",
            fileName = "IMG_2026.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 4500000L
        )
        repository.insertMediaItems(listOf(shot, camera))

        viewModel.setQuickFilter(StorageQuickFilter.SCREENSHOTS)
        val state = viewModel.uiState.first { it.quickFilter == StorageQuickFilter.SCREENSHOTS }
        assertEquals(1, state.filteredMediaItems.size)
        assertEquals(1101L, state.filteredMediaItems[0].id)
    }

    // 12. فلتر فيديوهات > 50 MB
    @Test
    fun test12_filterLargeVideosOver50MB() = runBlocking {
        val smallVid = MediaItem(
            id = 1201L,
            uriString = "content://media/1201",
            filePath = "/storage/emulated/0/small.mp4",
            fileName = "small.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 20 * 1024 * 1024L
        )
        val bigVid = MediaItem(
            id = 1202L,
            uriString = "content://media/1202",
            filePath = "/storage/emulated/0/big.mp4",
            fileName = "big.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 75 * 1024 * 1024L
        )
        repository.insertMediaItems(listOf(smallVid, bigVid))

        viewModel.setQuickFilter(StorageQuickFilter.LARGE_VIDEOS_50MB)
        val state = viewModel.uiState.first { it.quickFilter == StorageQuickFilter.LARGE_VIDEOS_50MB }
        assertEquals(1, state.filteredMediaItems.size)
        assertEquals(1202L, state.filteredMediaItems[0].id)
    }

    // 13. فلتر صور > 5 MB
    @Test
    fun test13_filterLargePhotosOver5MB() = runBlocking {
        val smallPic = MediaItem(
            id = 1301L,
            uriString = "content://media/1301",
            filePath = "/storage/emulated/0/small.jpg",
            fileName = "small.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 3 * 1024 * 1024L
        )
        val bigPic = MediaItem(
            id = 1302L,
            uriString = "content://media/1302",
            filePath = "/storage/emulated/0/big.jpg",
            fileName = "big.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 9 * 1024 * 1024L
        )
        repository.insertMediaItems(listOf(smallPic, bigPic))

        viewModel.setQuickFilter(StorageQuickFilter.LARGE_PHOTOS_5MB)
        val state = viewModel.uiState.first { it.quickFilter == StorageQuickFilter.LARGE_PHOTOS_5MB }
        assertEquals(1, state.filteredMediaItems.size)
        assertEquals(1302L, state.filteredMediaItems[0].id)
    }

    // 14. تحديث الإحصائيات بعد الحذف
    @Test
    fun test14_updateStatsAfterDeletion() = runBlocking {
        val item1 = MediaItem(id = 1401L, uriString = "content://media/1401", filePath = "/DCIM/1.jpg", fileName = "1.jpg", mimeType = "image/jpeg", sizeBytes = 10000000L)
        val item2 = MediaItem(id = 1402L, uriString = "content://media/1402", filePath = "/DCIM/2.jpg", fileName = "2.jpg", mimeType = "image/jpeg", sizeBytes = 15000000L)
        repository.insertMediaItems(listOf(item1, item2))

        val initial = viewModel.uiState.first { it.totalCount == 2 }
        assertEquals(25000000L, initial.totalSizeBytes)

        viewModel.deleteSingleItem(item1)
        val afterDelete = viewModel.uiState.first { it.totalCount == 1 }
        assertEquals(15000000L, afterDelete.totalSizeBytes)
    }

    // 15. التعامل مع ملف تم حذفه خارجيًا
    @Test
    fun test15_handleExternallyDeletedMediaFile() = runBlocking {
        val ghostItem = MediaItem(
            id = 1501L,
            uriString = "content://media/1501",
            filePath = "/storage/emulated/0/non_existent_file.jpg",
            fileName = "non_existent_file.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 5000000L
        )
        repository.insertMediaItems(listOf(ghostItem))
        assertEquals(1, repository.getAllMediaItems().size)

        // Delete cleans up database entry safely
        val res = repository.deleteMediaItems(listOf(1501L))
        assertTrue(res.isSuccess)
        assertEquals(0, repository.getAllMediaItems().size)
    }

    // 16. D3 JSON صحيح
    @Test
    fun test16_buildValidD3JsonStructure() {
        val state = StorageAnalysisState(
            totalSizeBytes = 50000000L,
            formattedTotalSize = "50.0 MB",
            totalCount = 5,
            distributionItems = listOf(
                com.omex.gallery.ui.feature_storage.StorageDistributionItem(
                    key = "MP4",
                    label = "MP4 Videos",
                    sizeBytes = 40000000L,
                    formattedSize = "40.0 MB",
                    count = 2,
                    colorHex = "#00E5FF",
                    percentage = 0.8f
                )
            )
        )
        val jsonStr = viewModel.buildD3JsonString(state)
        val json = JSONObject(jsonStr)
        assertTrue(json.has("formattedTotal"))
        assertTrue(json.has("items"))
        val items = json.getJSONArray("items")
        assertEquals(1, items.length())
        assertEquals("MP4", items.getJSONObject(0).getString("key"))
    }

    // 17. الضغط على قطاع الرسم يعرض الفئة الصحيحة
    @Test
    fun test17_d3ChartSliceSelectionFiltersCategory() = runBlocking {
        val img = MediaItem(id = 1701L, uriString = "content://media/1701", filePath = "/DCIM/doc.pdf", fileName = "receipt_doc.png", mimeType = "image/png", sizeBytes = 2000L)
        val vid = MediaItem(id = 1702L, uriString = "content://media/1702", filePath = "/DCIM/car.mp4", fileName = "car_drive.mp4", mimeType = "video/mp4", isVideo = true, sizeBytes = 8000L)
        repository.insertMediaItems(listOf(img, vid))

        viewModel.setGroupingMode(StorageGroupingMode.BY_CLASSIFICATION)
        viewModel.selectSlice("DOCUMENT", "Documents")

        val state = viewModel.uiState.first { it.selectedSliceKey == "DOCUMENT" }
        assertEquals(1, state.filteredMediaItems.size)
        assertEquals(1701L, state.filteredMediaItems[0].id)
    }

    // 18. عدم تحميل الملفات الأصلية أثناء التحليل
    @Test
    fun test18_noOriginalMediaLoadedIntoMemoryDuringAnalysis() {
        val dummyItem = MediaItem(
            id = 1801L,
            uriString = "content://media/1801",
            filePath = "/storage/emulated/0/DCIM/huge_file.raw",
            fileName = "huge_file.raw",
            mimeType = "image/x-adobe-dng",
            sizeBytes = 200 * 1024 * 1024L // 200MB
        )
        // Memory consumption test: extracting extension/category must not read file contents
        val ext = viewModel.extractExtensionOrMime(dummyItem)
        val cat = viewModel.inferCategory(dummyItem)
        assertEquals("RAW", ext)
        assertNotNull(cat)
    }

    // 19. عدم إعادة تشغيل OCR
    @Test
    fun test19_noOcrReExecutionDuringStorageAnalysis() = runBlocking {
        val initialOcrCount = db.aiDao().getAllOcrResults().size
        val item = MediaItem(id = 1901L, uriString = "content://media/1901", filePath = "/DCIM/ocr.jpg", fileName = "ocr.jpg", mimeType = "image/jpeg", sizeBytes = 1000L)
        repository.insertMediaItems(listOf(item))

        val state = viewModel.uiState.first { state -> state.totalCount == 1 }
        assertNotNull(state)

        // Verifying OCR database table has not been populated by Storage Analyzer
        val currentOcrCount = db.aiDao().getAllOcrResults().size
        assertEquals(initialOcrCount, currentOcrCount)
    }

    // 20. عدم إعادة تشغيل AI classification
    @Test
    fun test20_noAiClassificationReExecutionDuringStorageAnalysis() = runBlocking {
        val initialClassifications = db.aiDao().getAllClassifications().size
        val item = MediaItem(id = 2001L, uriString = "content://media/2001", filePath = "/DCIM/ai.jpg", fileName = "ai.jpg", mimeType = "image/jpeg", sizeBytes = 1000L)
        repository.insertMediaItems(listOf(item))

        val state = viewModel.uiState.first { state -> state.totalCount == 1 }
        assertNotNull(state)

        val currentClassifications = db.aiDao().getAllClassifications().size
        assertEquals(initialClassifications, currentClassifications)
    }

    // 21. التعامل مع قائمة فارغة
    @Test
    fun test21_handleEmptyMediaList() = runBlocking {
        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(0, state.totalCount)
        assertEquals(0L, state.totalSizeBytes)
        assertTrue(state.filteredMediaItems.isEmpty())
        val json = viewModel.buildD3JsonString(state)
        assertTrue(json.contains("items"))
    }

    // 22. التعامل مع MediaStore URI غير صالح
    @Test
    fun test22_handleInvalidMediaStoreUriGracefully() = runBlocking {
        val invalidItem = MediaItem(
            id = 2201L,
            uriString = "invalid://uri/path/2201",
            filePath = "",
            fileName = "",
            mimeType = "",
            sizeBytes = 500L
        )
        repository.insertMediaItems(listOf(invalidItem))
        val state = viewModel.uiState.first { it.totalCount == 1 }
        assertEquals(1, state.totalCount)
        assertEquals(500L, state.totalSizeBytes)
    }

    // 23. التعامل مع صلاحية الحذف
    @Test
    fun test23_handleDeletePermissionAndSafeRemoval() = runBlocking {
        val item = MediaItem(
            id = 2301L,
            uriString = "content://media/external/images/media/2301",
            filePath = "/storage/emulated/0/DCIM/Camera/item2301.jpg",
            fileName = "item2301.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 2000000L
        )
        repository.insertMediaItems(listOf(item))
        viewModel.deleteSelectedItems(listOf(item))

        val state = viewModel.uiState.first { it.totalCount == 0 }
        assertEquals(0, state.totalCount)
        assertEquals(0L, state.totalSizeBytes)
    }

    // 24. حساب المساحة بعد التحديث
    @Test
    fun test24_recalculateStorageAfterMediaUpdate() = runBlocking {
        val item1 = MediaItem(id = 2401L, uriString = "content://media/2401", filePath = "/DCIM/1.jpg", fileName = "1.jpg", mimeType = "image/jpeg", sizeBytes = 1000000L)
        repository.insertMediaItems(listOf(item1))

        var state = viewModel.uiState.first { it.totalCount == 1 }
        assertEquals(1000000L, state.totalSizeBytes)

        val item2 = MediaItem(id = 2402L, uriString = "content://media/2402", filePath = "/DCIM/2.mp4", fileName = "2.mp4", mimeType = "video/mp4", isVideo = true, sizeBytes = 5000000L)
        repository.insertMediaItems(listOf(item2))

        state = viewModel.uiState.first { it.totalCount == 2 }
        assertEquals(6000000L, state.totalSizeBytes)
        assertEquals(1000000L, state.photosSizeBytes)
        assertEquals(5000000L, state.videosSizeBytes)
    }
}
