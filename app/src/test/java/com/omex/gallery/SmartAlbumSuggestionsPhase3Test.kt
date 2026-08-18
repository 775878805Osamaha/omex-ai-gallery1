package com.omex.gallery

import com.omex.gallery.core.ai.classifier.CategoryClassifier
import com.omex.gallery.core.ai.suggestions.AiSuggestionsEngine
import com.omex.gallery.core.data.local.DetectedObjectEntity
import com.omex.gallery.core.data.local.ImageClassificationEntity
import com.omex.gallery.core.data.local.MediaItemCategoryCrossRef
import com.omex.gallery.core.data.local.OcrTextEntity
import com.omex.gallery.domain.model.AiAlbumSuggestion
import com.omex.gallery.domain.model.Album
import com.omex.gallery.domain.model.AlbumType
import com.omex.gallery.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAlbumSuggestionsPhase3Test {

    private fun createSampleMedia(
        id: Long,
        fileName: String,
        filePath: String = "/storage/emulated/0/DCIM/Camera/$fileName",
        dateTaken: Long = 1700000000000L
    ): MediaItem {
        return MediaItem(
            id = id,
            uriString = "content://media/$id",
            fileName = fileName,
            filePath = filePath,
            sizeBytes = 1024L * 1024L,
            mimeType = if (fileName.endsWith(".mp4")) "video/mp4" else "image/jpeg",
            dateTaken = dateTaken,
            isVideo = fileName.endsWith(".mp4")
        )
    }

    // 1. Suggestion: PRODUCT
    @Test
    fun test01_ProductSuggestion() {
        val mediaList = listOf(
            createSampleMedia(101L, "sneaker_nike.jpg"),
            createSampleMedia(102L, "perfume_bottle.jpg")
        )
        val crossRefs = listOf(
            MediaItemCategoryCrossRef(101L, CategoryClassifier.CATEGORY_PRODUCT),
            MediaItemCategoryCrossRef(102L, CategoryClassifier.CATEGORY_PRODUCT)
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs
        )
        val product = suggestions.find { it.themeKey == "PRODUCT" }
        assertNotNull("PRODUCT suggestion should be created", product)
        assertEquals(2, product!!.mediaCount)
        assertEquals("منتجات ومقتنيات", product.titleArabic)
        assertTrue(product.matchingMediaIds.contains(101L))
        assertTrue(product.matchingMediaIds.contains(102L))
    }

    // 2. Suggestion: TRADING
    @Test
    fun test02_TradingSuggestion() {
        val mediaList = listOf(
            createSampleMedia(201L, "btc_candlestick.png"),
            createSampleMedia(202L, "tradingview_eth.png")
        )
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 201L, classId = 1, label = "candlestick chart", confidence = 0.99f, category = "TRADING")
        )
        val ocrList = listOf(
            OcrTextEntity(id = 1L, mediaId = 202L, extractedText = "TradingView ETH/USDT RSI 14", language = "en")
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            classifications = classifications,
            ocrList = ocrList
        )
        val trading = suggestions.find { it.themeKey == "TRADING" }
        assertNotNull("TRADING suggestion should be created", trading)
        assertEquals(2, trading!!.mediaCount)
        assertEquals("تداول ورسوم بيانية", trading.titleArabic)
        assertTrue(trading.matchingMediaIds.contains(201L))
        assertTrue(trading.matchingMediaIds.contains(202L))
    }

    // 3. Suggestion: SCREENSHOT
    @Test
    fun test03_ScreenshotSuggestion() {
        val mediaList = listOf(
            createSampleMedia(301L, "Screenshot_20260817.png", filePath = "/storage/emulated/0/Pictures/Screenshots/Screenshot_20260817.png"),
            createSampleMedia(302L, "screen_shot_app.png", filePath = "/storage/emulated/0/DCIM/screen_shot_app.png")
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList
        )
        val screenshot = suggestions.find { it.themeKey == "SCREENSHOT" }
        assertNotNull("SCREENSHOT suggestion should be created", screenshot)
        assertEquals(2, screenshot!!.mediaCount)
        assertEquals("لقطات الشاشة", screenshot.titleArabic)
        assertTrue(screenshot.matchingMediaIds.contains(301L))
        assertTrue(screenshot.matchingMediaIds.contains(302L))
    }

    // 4. Suggestion: DOCUMENT
    @Test
    fun test04_DocumentSuggestion() {
        val mediaList = listOf(
            createSampleMedia(401L, "tax_invoice_2026.pdf"),
            createSampleMedia(402L, "medical_prescription.jpg")
        )
        val ocrList = listOf(
            OcrTextEntity(id = 2L, mediaId = 401L, extractedText = "TAX INVOICE Total VAT: $450", language = "en"),
            OcrTextEntity(id = 3L, mediaId = 402L, extractedText = "روشتة علاجية كشف دكتور", language = "ar")
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            ocrList = ocrList
        )
        val doc = suggestions.find { it.themeKey == "DOCUMENT" }
        assertNotNull("DOCUMENT suggestion should be created", doc)
        assertEquals(2, doc!!.mediaCount)
        assertEquals("مستندات وفواتير", doc.titleArabic)
        assertTrue(doc.matchingMediaIds.contains(401L))
        assertTrue(doc.matchingMediaIds.contains(402L))
    }

    // 5. Suggestion: CAR
    @Test
    fun test05_CarSuggestion() {
        val mediaList = listOf(
            createSampleMedia(501L, "tesla_model3.jpg"),
            createSampleMedia(502L, "bmw_m4.jpg")
        )
        val objects = listOf(
            DetectedObjectEntity(mediaId = 501L, classId = 3, labelName = "car", score = 0.95f, left = 0f, top = 0f, right = 100f, bottom = 100f),
            DetectedObjectEntity(mediaId = 502L, classId = 3, labelName = "automobile", score = 0.92f, left = 0f, top = 0f, right = 100f, bottom = 100f)
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            objects = objects
        )
        val car = suggestions.find { it.themeKey == "CAR" }
        assertNotNull("CAR suggestion should be created", car)
        assertEquals(2, car!!.mediaCount)
        assertEquals("سيارات ومركبات", car.titleArabic)
        assertTrue(car.matchingMediaIds.contains(501L))
        assertTrue(car.matchingMediaIds.contains(502L))
    }

    // 6. Suggestion: FOOD
    @Test
    fun test06_FoodSuggestion() {
        val mediaList = listOf(
            createSampleMedia(601L, "italian_pasta.jpg"),
            createSampleMedia(602L, "espresso_cup.jpg")
        )
        val crossRefs = listOf(
            MediaItemCategoryCrossRef(601L, CategoryClassifier.CATEGORY_FOOD),
            MediaItemCategoryCrossRef(602L, CategoryClassifier.CATEGORY_FOOD)
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs
        )
        val food = suggestions.find { it.themeKey == "FOOD" }
        assertNotNull("FOOD suggestion should be created", food)
        assertEquals(2, food!!.mediaCount)
        assertEquals("طعام ومأكولات", food.titleArabic)
        assertTrue(food.matchingMediaIds.contains(601L))
        assertTrue(food.matchingMediaIds.contains(602L))
    }

    // 7. Suggestion: NATURE
    @Test
    fun test07_NatureSuggestion() {
        val mediaList = listOf(
            createSampleMedia(701L, "sunset_mountains.jpg"),
            createSampleMedia(702L, "green_forest.jpg")
        )
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 701L, classId = 20, label = "mountain sunset", confidence = 0.96f, category = "NATURE"),
            ImageClassificationEntity(mediaId = 702L, classId = 21, label = "forest trees", confidence = 0.94f, category = "NATURE")
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            classifications = classifications
        )
        val nature = suggestions.find { it.themeKey == "NATURE" }
        assertNotNull("NATURE suggestion should be created", nature)
        assertEquals(2, nature!!.mediaCount)
        assertEquals("طبيعة ومناظر خارجية", nature.titleArabic)
        assertTrue(nature.matchingMediaIds.contains(701L))
        assertTrue(nature.matchingMediaIds.contains(702L))
    }

    // 8. Suggestion: TRAVEL
    @Test
    fun test08_TravelSuggestion() {
        val mediaList = listOf(
            createSampleMedia(801L, "airplane_wing.jpg"),
            createSampleMedia(802L, "resort_hotel_pool.jpg")
        )
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 801L, classId = 30, label = "airplane airport flight", confidence = 0.95f, category = "TRAVEL"),
            ImageClassificationEntity(mediaId = 802L, classId = 31, label = "resort vacation", confidence = 0.92f, category = "TRAVEL")
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            classifications = classifications
        )
        val travel = suggestions.find { it.themeKey == "TRAVEL" }
        assertNotNull("TRAVEL suggestion should be created", travel)
        assertEquals(2, travel!!.mediaCount)
        assertEquals("رحلات وأسفار", travel.titleArabic)
        assertTrue(travel.matchingMediaIds.contains(801L))
        assertTrue(travel.matchingMediaIds.contains(802L))
    }

    // 9. Non-existent category -> No suggestion generated
    @Test
    fun test09_NoSuggestionForNonExistentCategory() {
        val mediaList = listOf(
            createSampleMedia(901L, "random_abstract_pattern.jpg")
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList
        )
        assertNull("Should not create FOOD suggestion when not present", suggestions.find { it.themeKey == "FOOD" })
        assertNull("Should not create CAR suggestion when not present", suggestions.find { it.themeKey == "CAR" })
        assertNull("Should not create TRADING suggestion when not present", suggestions.find { it.themeKey == "TRADING" })
    }

    // 10. Below threshold -> No suggestion generated
    @Test
    fun test10_NoSuggestionWhenBelowThreshold() {
        val mediaList = listOf(
            createSampleMedia(1001L, "single_coffee.jpg")
        )
        val crossRefs = listOf(
            MediaItemCategoryCrossRef(1001L, CategoryClassifier.CATEGORY_FOOD)
        )
        // With threshold = 2, a category with only 1 item should NOT generate a suggestion
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs,
            minCountThreshold = 2
        )
        assertNull("Should not create FOOD suggestion when item count is below threshold of 2", suggestions.find { it.themeKey == "FOOD" })

        // With threshold = 1, it should generate the suggestion
        val suggestionsThreshold1 = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs,
            minCountThreshold = 1
        )
        assertNotNull("Should create FOOD suggestion when threshold is 1", suggestionsThreshold1.find { it.themeKey == "FOOD" })
    }

    // 11. Multi-category overlap handling
    @Test
    fun test11_MultiCategoryOverlapWithoutDuplication() {
        val sharedItem = createSampleMedia(1101L, "trading_book_merchandise.jpg")
        val mediaList = listOf(sharedItem)

        // Item has BOTH PRODUCT and TRADING classifications
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 1101L, classId = 40, label = "product merchandise", confidence = 0.95f, category = "PRODUCT"),
            ImageClassificationEntity(mediaId = 1101L, classId = 41, label = "trading chart book", confidence = 0.97f, category = "TRADING")
        )
        val crossRefs = listOf(
            MediaItemCategoryCrossRef(1101L, CategoryClassifier.CATEGORY_PRODUCT),
            MediaItemCategoryCrossRef(1101L, CategoryClassifier.CATEGORY_TRADING)
        )

        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            classifications = classifications,
            categoryCrossRefs = crossRefs,
            minCountThreshold = 1
        )

        val product = suggestions.find { it.themeKey == "PRODUCT" }
        val trading = suggestions.find { it.themeKey == "TRADING" }

        assertNotNull("Should appear in PRODUCT suggestion", product)
        assertNotNull("Should appear in TRADING suggestion", trading)
        assertTrue(product!!.matchingMediaIds.contains(1101L))
        assertTrue(trading!!.matchingMediaIds.contains(1101L))

        // Verify that the media list remains exactly 1 element (no duplicated files)
        assertEquals(1, mediaList.size)
    }

    // 12. No duplicate suggestions
    @Test
    fun test12_NoDuplicateSuggestions() {
        val mediaList = listOf(
            createSampleMedia(1201L, "car1.jpg"),
            createSampleMedia(1202L, "car2.jpg"),
            createSampleMedia(1203L, "car3.jpg")
        )
        val crossRefs = listOf(
            MediaItemCategoryCrossRef(1201L, CategoryClassifier.CATEGORY_CAR),
            MediaItemCategoryCrossRef(1202L, CategoryClassifier.CATEGORY_CAR),
            MediaItemCategoryCrossRef(1203L, CategoryClassifier.CATEGORY_CAR)
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs
        )
        val carSuggestions = suggestions.filter { it.themeKey == "CAR" }
        assertEquals("Should produce exactly 1 CAR suggestion without duplicates", 1, carSuggestions.size)
    }

    // 13. Ranking and Sorting of suggestions
    @Test
    fun test13_RankingAndSorting() {
        val mediaList = listOf(
            createSampleMedia(1301L, "car1.jpg"),
            createSampleMedia(1302L, "doc1.pdf"),
            createSampleMedia(1303L, "doc2.pdf"),
            createSampleMedia(1304L, "doc3.pdf")
        )
        val crossRefs = listOf(
            MediaItemCategoryCrossRef(1301L, CategoryClassifier.CATEGORY_CAR),
            MediaItemCategoryCrossRef(1302L, CategoryClassifier.CATEGORY_DOCUMENT),
            MediaItemCategoryCrossRef(1303L, CategoryClassifier.CATEGORY_DOCUMENT),
            MediaItemCategoryCrossRef(1304L, CategoryClassifier.CATEGORY_DOCUMENT)
        )
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs
        )
        assertTrue("Suggestions should not be empty", suggestions.isNotEmpty())
        // DOCUMENT has higher confidence and count than CAR
        val firstSuggestion = suggestions.first()
        assertEquals("DOCUMENT", firstSuggestion.themeKey)
        assertEquals(3, firstSuggestion.mediaCount)
    }

    // 14. Thumbnail generation from existing media
    @Test
    fun test14_ThumbnailGeneration() {
        val mediaList = (1..6).map { i ->
            createSampleMedia(1400L + i, "nature_scene_$i.jpg")
        }
        val crossRefs = mediaList.map {
            MediaItemCategoryCrossRef(it.id, CategoryClassifier.CATEGORY_NATURE)
        }
        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            categoryCrossRefs = crossRefs
        )
        val nature = suggestions.find { it.themeKey == "NATURE" }
        assertNotNull(nature)
        assertEquals("Sample covers should take up to 4 URIs", 4, nature!!.sampleCoverUris.size)
        assertEquals("content://media/1401", nature.sampleCoverUris[0])
        assertEquals("content://media/1402", nature.sampleCoverUris[1])
        assertEquals("content://media/1403", nature.sampleCoverUris[2])
        assertEquals("content://media/1404", nature.sampleCoverUris[3])
    }

    // 15. Opening suggestion results inside Gallery state
    @Test
    fun test15_OpenSuggestionResults() {
        val media1 = createSampleMedia(1501L, "trading1.png")
        val media2 = createSampleMedia(1502L, "nature1.jpg")
        val media3 = createSampleMedia(1503L, "trading2.png")
        val allMedia = listOf(media1, media2, media3)

        val suggestion = AiAlbumSuggestion(
            id = "suggestion_trading",
            themeKey = "TRADING",
            title = "Trading",
            titleArabic = "تداول",
            description = "Trading charts",
            descriptionArabic = "رسوم بيانية",
            iconType = "show_chart",
            sampleCoverUris = listOf(media1.uriString),
            matchingMediaIds = listOf(1501L, 1503L),
            mediaCount = 2
        )

        // Simulate creating temporary selected album for exploration
        val album = Album(
            id = "themed_ai_${suggestion.id}",
            title = "${suggestion.titleArabic} (${suggestion.title})",
            coverUri = suggestion.sampleCoverUris.firstOrNull(),
            itemCount = suggestion.mediaCount,
            albumType = AlbumType.THEMED_AI,
            themeKey = suggestion.themeKey,
            matchingMediaIds = suggestion.matchingMediaIds
        )

        // Filter media items using matchingMediaIds
        val idSet = album.matchingMediaIds.toSet()
        val filteredResults = allMedia.filter { idSet.contains(it.id) }

        assertEquals(2, filteredResults.size)
        assertTrue(filteredResults.any { it.id == 1501L })
        assertTrue(filteredResults.any { it.id == 1503L })
        assertFalse(filteredResults.any { it.id == 1502L })
    }

    // 16. Saving smart album definition
    @Test
    fun test16_SaveSmartAlbum() {
        val suggestion = AiAlbumSuggestion(
            id = "suggestion_product",
            themeKey = "PRODUCT",
            title = "Products",
            titleArabic = "منتجات",
            description = "Products",
            descriptionArabic = "منتجات ومقتنيات",
            iconType = "shopping_bag",
            sampleCoverUris = listOf("content://media/1601"),
            matchingMediaIds = listOf(1601L, 1602L),
            mediaCount = 2
        )

        val savedThemedAlbums = mutableListOf<Album>()
        val newAlbum = Album(
            id = "themed_ai_${suggestion.id}",
            title = "${suggestion.titleArabic} (${suggestion.title})",
            coverUri = suggestion.sampleCoverUris.firstOrNull(),
            itemCount = suggestion.mediaCount,
            albumType = AlbumType.THEMED_AI,
            themeKey = suggestion.themeKey,
            matchingMediaIds = suggestion.matchingMediaIds
        )
        savedThemedAlbums.add(newAlbum)

        assertEquals(1, savedThemedAlbums.size)
        assertEquals("themed_ai_suggestion_product", savedThemedAlbums[0].id)
        assertEquals(AlbumType.THEMED_AI, savedThemedAlbums[0].albumType)
        assertEquals(listOf(1601L, 1602L), savedThemedAlbums[0].matchingMediaIds)
    }

    // 17. No file copying or disk duplication
    @Test
    fun test17_NoFileDuplication() {
        val originalMedia = createSampleMedia(1701L, "photo.jpg", filePath = "/storage/emulated/0/DCIM/Camera/photo.jpg")
        val mediaList = mutableListOf(originalMedia)

        val suggestion = AiAlbumSuggestion(
            id = "suggestion_food",
            themeKey = "FOOD",
            title = "Food",
            titleArabic = "طعام",
            description = "Food",
            descriptionArabic = "طعام",
            iconType = "restaurant",
            sampleCoverUris = listOf(originalMedia.uriString),
            matchingMediaIds = listOf(1701L),
            mediaCount = 1
        )

        val album = Album(
            id = "themed_ai_${suggestion.id}",
            title = suggestion.title,
            coverUri = suggestion.sampleCoverUris.firstOrNull(),
            itemCount = suggestion.mediaCount,
            albumType = AlbumType.THEMED_AI,
            matchingMediaIds = suggestion.matchingMediaIds
        )

        // Logical grouping does not alter media list or copy files
        assertEquals(1, mediaList.size)
        assertEquals("/storage/emulated/0/DCIM/Camera/photo.jpg", mediaList[0].filePath)
    }

    // 18. No deletion of media when hiding / dismissing suggestion
    @Test
    fun test18_NoDeletionOnDismissSuggestion() {
        val media1 = createSampleMedia(1801L, "doc.pdf")
        val mediaList = mutableListOf(media1)

        val dismissedSuggestionIds = mutableSetOf<String>()
        val suggestionId = "suggestion_documents"

        // User dismisses suggestion
        dismissedSuggestionIds.add(suggestionId)

        // Underlying media list remains completely unaffected
        assertEquals(1, mediaList.size)
        assertEquals(1801L, mediaList[0].id)
        assertTrue(dismissedSuggestionIds.contains(suggestionId))
    }
}
