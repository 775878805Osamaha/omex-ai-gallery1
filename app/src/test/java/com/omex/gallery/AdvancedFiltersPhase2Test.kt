package com.omex.gallery

import com.omex.gallery.core.data.local.MediaCategoryEntity
import com.omex.gallery.core.data.local.MediaItemEntity
import com.omex.gallery.domain.model.DateFilterOption
import com.omex.gallery.domain.model.DimensionFilterOption
import com.omex.gallery.domain.model.FileSizeFilterOption
import com.omex.gallery.domain.model.SearchFilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedFiltersPhase2Test {

    private val nowMs = System.currentTimeMillis()

    private val sampleItems = listOf(
        MediaItemEntity(
            id = 1L,
            uriString = "content://media/1",
            filePath = "/storage/emulated/0/DCIM/Camera/photo_person.jpg",
            fileName = "photo_person.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            sizeBytes = 800_000L, // 800 KB (< 1MB)
            dateTaken = nowMs - 1_000L, // Today
            dateModified = nowMs - 1_000L,
            durationMs = 0L,
            width = 1080,
            height = 1920, // 2.07 MP -> MEDIUM
            isFavorite = true,
            isAiProcessed = true,
            isIndexed = true
        ),
        MediaItemEntity(
            id = 2L,
            uriString = "content://media/2",
            filePath = "/storage/emulated/0/DCIM/Screenshots/trading_chart.png",
            fileName = "trading_chart.png",
            mimeType = "image/png",
            isVideo = false,
            sizeBytes = 2_500_000L, // 2.5 MB (1–5 MB)
            dateTaken = nowMs - (24 * 3600 * 1000L), // Yesterday (Last 7 Days)
            dateModified = nowMs - (24 * 3600 * 1000L),
            durationMs = 0L,
            width = 3840,
            height = 2160, // 8.29 MP -> HIGH_RES
            isFavorite = false,
            isAiProcessed = true,
            isIndexed = true
        ),
        MediaItemEntity(
            id = 3L,
            uriString = "content://media/3",
            filePath = "/storage/emulated/0/DCIM/Camera/vacation_video.mp4",
            fileName = "vacation_video.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 60_000_000L, // 60 MB (> 50 MB)
            dateTaken = nowMs - (3 * 24 * 3600 * 1000L), // 3 days ago (Last 7 Days)
            dateModified = nowMs - (3 * 24 * 3600 * 1000L),
            durationMs = 45_000L,
            width = 1920,
            height = 1080, // 2.07 MP -> MEDIUM
            isFavorite = true,
            isAiProcessed = true,
            isIndexed = true
        ),
        MediaItemEntity(
            id = 4L,
            uriString = "content://media/4",
            filePath = "/storage/emulated/0/Download/invoice_document.pdf",
            fileName = "invoice_document.pdf",
            mimeType = "application/pdf",
            isVideo = false,
            sizeBytes = 7_000_000L, // 7 MB (5–50 MB)
            dateTaken = nowMs - (15 * 24 * 3600 * 1000L), // 15 days ago (Last 30 Days)
            dateModified = nowMs - (15 * 24 * 3600 * 1000L),
            durationMs = 0L,
            width = 800,
            height = 600, // 0.48 MP -> SMALL
            isFavorite = false,
            isAiProcessed = true,
            isIndexed = true
        ),
        MediaItemEntity(
            id = 5L,
            uriString = "content://media/5",
            filePath = "/storage/emulated/0/DCIM/Camera/nature_sunset.webp",
            fileName = "nature_sunset.webp",
            mimeType = "image/webp",
            isVideo = false,
            sizeBytes = 1_200_000L, // 1.2 MB (1–5 MB)
            dateTaken = nowMs - (60 * 24 * 3600 * 1000L), // 60 days ago (This Year)
            dateModified = nowMs - (60 * 24 * 3600 * 1000L),
            durationMs = 0L,
            width = 1280,
            height = 720, // 0.92 MP -> SMALL
            isFavorite = true,
            isAiProcessed = true,
            isIndexed = true
        )
    )

    private val sampleCategoryMappings = mapOf(
        1L to listOf("PERSON", "WORK"),
        2L to listOf("TRADING", "SCREENSHOT"),
        3L to listOf("TRAVEL", "NATURE"),
        4L to listOf("DOCUMENT", "WORK"),
        5L to listOf("NATURE")
    )

    // Helper filter simulator
    private fun filterMedia(filter: SearchFilterState): List<MediaItemEntity> {
        return sampleItems.filter { item ->
            // MediaType
            if (filter.isVideo != null) {
                val isVid = item.mimeType.startsWith("video")
                if (isVid != filter.isVideo) return@filter false
            }

            // Favorite
            if (filter.isFavorite != null) {
                if (item.isFavorite != filter.isFavorite) return@filter false
            }

            // Categories
            val allCats = filter.allSelectedCategories
            if (allCats.isNotEmpty()) {
                val itemCats = sampleCategoryMappings[item.id] ?: emptyList()
                if (itemCats.none { it in allCats }) return@filter false
            }

            // Extensions
            if (filter.selectedExtensions.isNotEmpty()) {
                val ext = item.fileName.substringAfterLast(".", "").lowercase()
                val match = filter.selectedExtensions.any { it.equals(ext, ignoreCase = true) }
                if (!match) return@filter false
            }

            // File Size
            when (filter.fileSizeOption) {
                FileSizeFilterOption.LESS_THAN_1MB -> if (item.sizeBytes >= 1_024_000L) return@filter false
                FileSizeFilterOption.BETWEEN_1_5MB -> if (item.sizeBytes !in 1_000_000L..5_000_000L) return@filter false
                FileSizeFilterOption.BETWEEN_5_50MB -> if (item.sizeBytes !in 5_000_000L..50_000_000L) return@filter false
                FileSizeFilterOption.GREATER_THAN_50MB -> if (item.sizeBytes <= 50_000_000L) return@filter false
                FileSizeFilterOption.ALL -> Unit
            }

            // Dimensions (Resolution MP)
            val pixels = item.width.toLong() * item.height.toLong()
            when (filter.dimensionOption) {
                DimensionFilterOption.SMALL -> if (pixels >= 1_000_000L) return@filter false
                DimensionFilterOption.MEDIUM -> if (pixels !in 1_000_000L..4_000_000L) return@filter false
                DimensionFilterOption.HIGH_RES -> if (pixels <= 4_000_000L) return@filter false
                DimensionFilterOption.ALL -> Unit
            }

            true
        }
    }

    // --- TEST 1: Media Type Filter (Photos only) ---
    @Test
    fun test01_filterMediaPhotosOnly() {
        val filter = SearchFilterState(isVideo = false)
        val result = filterMedia(filter)
        assertTrue(result.all { !it.mimeType.startsWith("video") })
        assertFalse(result.any { it.id == 3L })
    }

    // --- TEST 2: Media Type Filter (Videos only) ---
    @Test
    fun test02_filterMediaVideosOnly() {
        val filter = SearchFilterState(isVideo = true)
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    // --- TEST 3: Favorite State Filter (Favorites only) ---
    @Test
    fun test03_filterFavoritesOnly() {
        val filter = SearchFilterState(isFavorite = true)
        val result = filterMedia(filter)
        assertEquals(3, result.size)
        assertTrue(result.all { it.isFavorite })
    }

    // --- TEST 4: Non-Favorites Filter ---
    @Test
    fun test04_filterNonFavoritesOnly() {
        val filter = SearchFilterState(isFavorite = false)
        val result = filterMedia(filter)
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isFavorite })
    }

    // --- TEST 5: Single Smart Category (PERSON) ---
    @Test
    fun test05_filterCategoryPerson() {
        val filter = SearchFilterState(selectedCategoryIds = setOf("PERSON"))
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    // --- TEST 6: Single Smart Category (TRADING) ---
    @Test
    fun test06_filterCategoryTrading() {
        val filter = SearchFilterState(selectedCategoryIds = setOf("TRADING"))
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    // --- TEST 7: Multi-Category Selection (WORK or NATURE) ---
    @Test
    fun test07_filterMultiCategoryWorkOrNature() {
        val filter = SearchFilterState(selectedCategoryIds = setOf("WORK", "NATURE"))
        val result = filterMedia(filter)
        // IDs 1 (WORK), 3 (NATURE), 4 (WORK), 5 (NATURE)
        assertEquals(4, result.size)
        assertTrue(result.map { it.id }.containsAll(listOf(1L, 3L, 4L, 5L)))
    }

    // --- TEST 8: File Size Filter (< 1MB) ---
    @Test
    fun test08_filterFileSizeLessThan1MB() {
        val filter = SearchFilterState(fileSizeOption = FileSizeFilterOption.LESS_THAN_1MB)
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    // --- TEST 9: File Size Filter (1MB - 5MB) ---
    @Test
    fun test09_filterFileSizeBetween1MBAnd5MB() {
        val filter = SearchFilterState(fileSizeOption = FileSizeFilterOption.BETWEEN_1_5MB)
        val result = filterMedia(filter)
        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.containsAll(listOf(2L, 5L)))
    }

    // --- TEST 10: File Size Filter (> 50MB) ---
    @Test
    fun test10_filterFileSizeGreaterThan50MB() {
        val filter = SearchFilterState(fileSizeOption = FileSizeFilterOption.GREATER_THAN_50MB)
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    // --- TEST 11: File Extension Filter (PNG only) ---
    @Test
    fun test11_filterExtensionPngOnly() {
        val filter = SearchFilterState(selectedExtensions = setOf("png"))
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    // --- TEST 12: File Extension Multi-Select (JPG, WEBP) ---
    @Test
    fun test12_filterExtensionJpgAndWebp() {
        val filter = SearchFilterState(selectedExtensions = setOf("jpg", "webp"))
        val result = filterMedia(filter)
        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.containsAll(listOf(1L, 5L)))
    }

    // --- TEST 13: Dimension Filter (Small < 1 MP) ---
    @Test
    fun test13_filterDimensionSmall() {
        val filter = SearchFilterState(dimensionOption = DimensionFilterOption.SMALL)
        val result = filterMedia(filter)
        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.containsAll(listOf(4L, 5L)))
    }

    // --- TEST 14: Dimension Filter (Medium 1–4 MP) ---
    @Test
    fun test14_filterDimensionMedium() {
        val filter = SearchFilterState(dimensionOption = DimensionFilterOption.MEDIUM)
        val result = filterMedia(filter)
        assertEquals(2, result.size)
        assertTrue(result.map { it.id }.containsAll(listOf(1L, 3L)))
    }

    // --- TEST 15: Dimension Filter (High-Res > 4 MP) ---
    @Test
    fun test15_filterDimensionHighRes() {
        val filter = SearchFilterState(dimensionOption = DimensionFilterOption.HIGH_RES)
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }

    // --- TEST 16: Complex Combined Filter (Videos + Favorite + Size > 50MB) ---
    @Test
    fun test16_complexCombinedFilterVideosFavoriteLarge() {
        val filter = SearchFilterState(
            isVideo = true,
            isFavorite = true,
            fileSizeOption = FileSizeFilterOption.GREATER_THAN_50MB
        )
        val result = filterMedia(filter)
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    // --- TEST 17: Filter Active Count and hasActiveFilters verification ---
    @Test
    fun test17_activeFilterCountAndFlags() {
        val emptyFilter = SearchFilterState()
        assertFalse(emptyFilter.hasActiveFilters)
        assertEquals(0, emptyFilter.activeFilterCount)

        val fullFilter = SearchFilterState(
            isVideo = false,
            isFavorite = true,
            selectedCategoryIds = setOf("PERSON", "DOCUMENT"),
            dateFilterOption = DateFilterOption.LAST_30_DAYS,
            fileSizeOption = FileSizeFilterOption.BETWEEN_1_5MB,
            dimensionOption = DimensionFilterOption.HIGH_RES,
            selectedExtensions = setOf("jpg", "png")
        )

        assertTrue(fullFilter.hasActiveFilters)
        // isVideo (1) + isFavorite (1) + categories (2) + date (1) + size (1) + dimension (1) + extensions (2) = 9
        assertEquals(9, fullFilter.activeFilterCount)
    }

    // --- TEST 18: Reset and Clearing Filter Helpers ---
    @Test
    fun test18_filterResetAndModification() {
        var state = SearchFilterState(
            isVideo = true,
            isFavorite = true,
            selectedCategoryIds = setOf("PERSON", "WORK"),
            fileSizeOption = FileSizeFilterOption.LESS_THAN_1MB
        )
        assertEquals(5, state.activeFilterCount)

        // Remove one category
        val updatedCategories = state.selectedCategoryIds.toMutableSet().apply { remove("PERSON") }
        state = state.copy(selectedCategoryIds = updatedCategories)
        assertEquals(4, state.activeFilterCount)
        assertEquals(setOf("WORK"), state.selectedCategoryIds)

        // Remove media type
        state = state.copy(isVideo = null)
        assertEquals(3, state.activeFilterCount)
        assertNull(state.isVideo)

        // Reset all
        state = SearchFilterState()
        assertFalse(state.hasActiveFilters)
        assertEquals(0, state.activeFilterCount)
    }
}
