package com.omex.gallery

import com.omex.gallery.core.ai.classifier.CategoryClassifier
import com.omex.gallery.core.ai.suggestions.AiSuggestionsEngine
import com.omex.gallery.core.data.local.DetectedObjectEntity
import com.omex.gallery.core.data.local.ImageClassificationEntity
import com.omex.gallery.core.data.local.MediaItemCategoryCrossRef
import com.omex.gallery.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSuggestionsTest {

    @Test
    fun testAiSuggestionsGroupingFoodDocumentsAndTravel() {
        val mediaList = listOf(
            MediaItem(
                id = 1L,
                uriString = "content://media/1",
                fileName = "italian_pizza.jpg",
                filePath = "/storage/emulated/0/DCIM/Camera/italian_pizza.jpg",
                sizeBytes = 2048000L,
                mimeType = "image/jpeg",
                dateTaken = 1700000000000L
            ),
            MediaItem(
                id = 2L,
                uriString = "content://media/2",
                fileName = "coffee_breakfast.jpg",
                filePath = "/storage/emulated/0/DCIM/Camera/coffee_breakfast.jpg",
                sizeBytes = 1848000L,
                mimeType = "image/jpeg",
                dateTaken = 1700001000000L
            ),
            MediaItem(
                id = 3L,
                uriString = "content://media/3",
                fileName = "invoice_tax_2026.png",
                filePath = "/storage/emulated/0/Download/invoice_tax_2026.png",
                sizeBytes = 512000L,
                mimeType = "image/png",
                dateTaken = 1700002000000L
            ),
            MediaItem(
                id = 4L,
                uriString = "content://media/4",
                fileName = "airport_boarding_pass.jpg",
                filePath = "/storage/emulated/0/DCIM/Camera/airport_boarding_pass.jpg",
                sizeBytes = 3200000L,
                mimeType = "image/jpeg",
                dateTaken = 1700003000000L
            ),
            MediaItem(
                id = 5L,
                uriString = "content://media/5",
                fileName = "sunset_beach_resort.jpg",
                filePath = "/storage/emulated/0/DCIM/Camera/sunset_beach_resort.jpg",
                sizeBytes = 4100000L,
                mimeType = "image/jpeg",
                dateTaken = 1700004000000L
            )
        )

        val classifications = listOf(
            ImageClassificationEntity(
                mediaId = 1L,
                classId = 10,
                label = "pizza",
                confidence = 0.98f,
                category = "FOOD"
            ),
            ImageClassificationEntity(
                mediaId = 2L,
                classId = 11,
                label = "espresso coffee",
                confidence = 0.94f,
                category = "FOOD"
            ),
            ImageClassificationEntity(
                mediaId = 3L,
                classId = 12,
                label = "financial receipt",
                confidence = 0.99f,
                category = "DOCUMENT"
            ),
            ImageClassificationEntity(
                mediaId = 4L,
                classId = 13,
                label = "airplane flight passport",
                confidence = 0.92f,
                category = "TRAVEL"
            ),
            ImageClassificationEntity(
                mediaId = 5L,
                classId = 14,
                label = "tropical beach vacation",
                confidence = 0.96f,
                category = "TRAVEL"
            )
        )

        val objects = listOf(
            DetectedObjectEntity(
                mediaId = 1L,
                classId = 1,
                labelName = "pizza",
                score = 0.95f,
                left = 0f,
                top = 0f,
                right = 100f,
                bottom = 100f
            ),
            DetectedObjectEntity(
                mediaId = 2L,
                classId = 2,
                labelName = "cup",
                score = 0.90f,
                left = 0f,
                top = 0f,
                right = 50f,
                bottom = 50f
            )
        )

        val crossRefs = listOf(
            MediaItemCategoryCrossRef(mediaId = 1L, categoryId = CategoryClassifier.CATEGORY_FOOD),
            MediaItemCategoryCrossRef(mediaId = 2L, categoryId = CategoryClassifier.CATEGORY_FOOD),
            MediaItemCategoryCrossRef(mediaId = 3L, categoryId = CategoryClassifier.CATEGORY_DOCUMENT),
            MediaItemCategoryCrossRef(mediaId = 4L, categoryId = CategoryClassifier.CATEGORY_TRAVEL),
            MediaItemCategoryCrossRef(mediaId = 5L, categoryId = CategoryClassifier.CATEGORY_TRAVEL)
        )

        val suggestions = AiSuggestionsEngine.generateSuggestions(
            mediaList = mediaList,
            classifications = classifications,
            objects = objects,
            categoryCrossRefs = crossRefs
        )

        assertTrue("Should produce multiple themed suggestions", suggestions.size >= 3)

        val foodSuggestion = suggestions.find { it.themeKey == "FOOD" }
        assertNotNull("Food suggestion must exist", foodSuggestion)
        assertEquals(2, foodSuggestion!!.mediaCount)
        assertTrue(foodSuggestion.matchingMediaIds.contains(1L))
        assertTrue(foodSuggestion.matchingMediaIds.contains(2L))
        assertEquals("طعام ومأكولات", foodSuggestion.titleArabic)

        val docSuggestion = suggestions.find { it.themeKey == "DOCUMENT" }
        assertNotNull("Document suggestion must exist", docSuggestion)
        assertEquals(1, docSuggestion!!.mediaCount)
        assertTrue(docSuggestion.matchingMediaIds.contains(3L))
        assertEquals("مستندات وفواتير", docSuggestion.titleArabic)

        val travelSuggestion = suggestions.find { it.themeKey == "TRAVEL" }
        assertNotNull("Travel suggestion must exist", travelSuggestion)
        assertEquals(2, travelSuggestion!!.mediaCount)
        assertTrue(travelSuggestion.matchingMediaIds.contains(4L))
        assertTrue(travelSuggestion.matchingMediaIds.contains(5L))
        assertEquals("رحلات وأسفار", travelSuggestion.titleArabic)
    }
}
