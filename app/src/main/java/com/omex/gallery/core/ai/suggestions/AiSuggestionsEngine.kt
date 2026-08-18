package com.omex.gallery.core.ai.suggestions

import com.omex.gallery.core.ai.classifier.CategoryClassifier
import com.omex.gallery.core.data.local.DetectedObjectEntity
import com.omex.gallery.core.data.local.ImageClassificationEntity
import com.omex.gallery.core.data.local.MediaItemCategoryCrossRef
import com.omex.gallery.core.data.local.OcrTextEntity
import com.omex.gallery.domain.model.AiAlbumSuggestion
import com.omex.gallery.domain.model.MediaItem
import java.util.Locale

object AiSuggestionsEngine {

    const val DEFAULT_MIN_COUNT_THRESHOLD = 1

    fun generateSuggestions(
        mediaList: List<MediaItem>,
        classifications: List<ImageClassificationEntity> = emptyList(),
        objects: List<DetectedObjectEntity> = emptyList(),
        categoryCrossRefs: List<MediaItemCategoryCrossRef> = emptyList(),
        ocrList: List<OcrTextEntity> = emptyList(),
        minCountThreshold: Int = DEFAULT_MIN_COUNT_THRESHOLD,
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<AiAlbumSuggestion> {
        if (mediaList.isEmpty()) return emptyList()

        val mediaById = mediaList.associateBy { it.id }
        val classificationsByMedia = classifications.groupBy { it.mediaId }
        val objectsByMedia = objects.groupBy { it.mediaId }
        val categoriesByMedia = categoryCrossRefs.groupBy({ it.mediaId }, { it.categoryId })
        val ocrByMedia = ocrList.associateBy { it.mediaId }

        val suggestions = mutableListOf<AiAlbumSuggestion>()

        // 1. PRODUCT (منتجات)
        val productMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_PRODUCT,
            targetKeywords = listOf("product", "shoe", "sneaker", "bottle", "perfume", "smartphone", "phone", "watch", "smartwatch", "headphones", "gadget", "merchandise", "cosmetics", "box", "package", "منتج", "سلعة", "بضاعة", "مقتنيات", "عطر", "حذاء"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (productMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = productMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_product",
                    themeKey = "PRODUCT",
                    title = "Products & Goods",
                    titleArabic = "منتجات ومقتنيات",
                    description = "Found ${productMatch.matchedIds.size} photos categorized as products and merchandise",
                    descriptionArabic = "تم العثور على ${productMatch.matchedIds.size} صورة مصنفة كمنتجات ومقتنيات",
                    iconType = "shopping_bag",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = productMatch.matchedIds,
                    mediaCount = productMatch.matchedIds.size,
                    confidenceScore = 0.95f,
                    matchedTags = productMatch.matchedKeywords.take(4)
                )
            )
        }

        // 2. TRADING (تداول)
        val tradingOcrKeywords = listOf("btc", "eth", "usdt", "crypto", "tradingview", "binance", "candlestick", "forex", "stock", "rsi", "macd", "chart", "تداول", "عملات", "فوركس", "شموع", "بيتكوين", "مؤشر", "شارت")
        val tradingMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_TRADING,
            targetKeywords = listOf("chart", "graph", "candlestick", "stock", "trading", "crypto", "forex", "tradingview", "market", "currency", "binance", "تداول", "فوركس", "مؤشر", "شموع", "بيتكوين", "شارت"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia,
            ocrKeywords = tradingOcrKeywords
        )
        if (tradingMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = tradingMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_trading",
                    themeKey = "TRADING",
                    title = "Trading & Charts",
                    titleArabic = "تداول ورسوم بيانية",
                    description = "You have ${tradingMatch.matchedIds.size} photos classified as financial & trading charts",
                    descriptionArabic = "لديك ${tradingMatch.matchedIds.size} صورة مصنفة كتداول ورسوم بيانية",
                    iconType = "show_chart",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = tradingMatch.matchedIds,
                    mediaCount = tradingMatch.matchedIds.size,
                    confidenceScore = 0.98f,
                    matchedTags = tradingMatch.matchedKeywords.take(4)
                )
            )
        }

        // 3. SCREENSHOT (لقطات الشاشة)
        val screenshotMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_SCREENSHOT,
            targetKeywords = listOf("screenshot", "screen_shot", "screen", "capture", "لقطة", "شاشة", "لقطة شاشة"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (screenshotMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = screenshotMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_screenshot",
                    themeKey = "SCREENSHOT",
                    title = "Screenshots",
                    titleArabic = "لقطات الشاشة",
                    description = "${screenshotMatch.matchedIds.size} screenshots identified on device",
                    descriptionArabic = "تم العثور على ${screenshotMatch.matchedIds.size} لقطة شاشة محفوظة",
                    iconType = "crop_free",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = screenshotMatch.matchedIds,
                    mediaCount = screenshotMatch.matchedIds.size,
                    confidenceScore = 0.99f,
                    matchedTags = screenshotMatch.matchedKeywords.take(4)
                )
            )
        }

        // 4. DOCUMENT (مستندات وفواتير)
        val docOcrKeywords = listOf("invoice", "receipt", "total", "subtotal", "tax", "vat", "bill", "contract", "signature", "prescription", "فاتورة", "إيصال", "مستند", "تقرير", "عقد", "شهادة", "روشتة", "كشف حساب")
        val docMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_DOCUMENT,
            targetKeywords = listOf("document", "doc", "paper", "receipt", "invoice", "sheet", "page", "prescription", "bill", "contract", "notes", "handwritten", "certificate", "letter", "statement", "text", "فاتورة", "مستند", "تقرير", "عقد", "شهادة", "روشتة", "ايصال", "إيصال", "ملاحظات"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia,
            ocrKeywords = docOcrKeywords
        )
        if (docMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = docMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_documents",
                    themeKey = "DOCUMENT",
                    title = "Documents & Invoices",
                    titleArabic = "مستندات وفواتير",
                    description = "${docMatch.matchedIds.size} documents, invoices, text notes & receipts recognized",
                    descriptionArabic = "لديك ${docMatch.matchedIds.size} مستندات وفواتير وأوراق نصية مصنفة بالذكاء الاصطناعي",
                    iconType = "description",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = docMatch.matchedIds,
                    mediaCount = docMatch.matchedIds.size,
                    confidenceScore = 0.97f,
                    matchedTags = docMatch.matchedKeywords.take(4)
                )
            )
        }

        // 5. CAR / Vehicles (سيارات ومركبات)
        val carMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_CAR,
            targetKeywords = listOf("car", "vehicle", "automobile", "truck", "motorcycle", "bus", "jeep", "sedan", "suv", "van", "wheel", "سيارة", "مركبة", "شاحنة", "دراجة", "عربة"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (carMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = carMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_car",
                    themeKey = "CAR",
                    title = "Vehicles & Transport",
                    titleArabic = "سيارات ومركبات",
                    description = "${carMatch.matchedIds.size} automobile & transport photos detected",
                    descriptionArabic = "تم العثور على ${carMatch.matchedIds.size} صورة سيارات ومركبات",
                    iconType = "directions_car",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = carMatch.matchedIds,
                    mediaCount = carMatch.matchedIds.size,
                    confidenceScore = 0.94f,
                    matchedTags = carMatch.matchedKeywords.take(4)
                )
            )
        }

        // 6. FOOD (طعام ومأكولات)
        val foodMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_FOOD,
            targetKeywords = listOf("food", "meal", "dish", "cuisine", "pizza", "sandwich", "burger", "fruit", "cake", "bread", "coffee", "tea", "drink", "restaurant", "apple", "banana", "orange", "dessert", "snack", "breakfast", "lunch", "dinner", "طعام", "وجبة", "أكل", "مطعم", "مشروب", "قهوة", "حلوى"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (foodMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = foodMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_food",
                    themeKey = "FOOD",
                    title = "Food & Dining",
                    titleArabic = "طعام ومأكولات",
                    description = "${foodMatch.matchedIds.size} photos detected with meals, dishes, drinks, or restaurants",
                    descriptionArabic = "لديك ${foodMatch.matchedIds.size} صورة مصنفة كأطباق ومأكولات ومطاعم",
                    iconType = "restaurant",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = foodMatch.matchedIds,
                    mediaCount = foodMatch.matchedIds.size,
                    confidenceScore = 0.96f,
                    matchedTags = foodMatch.matchedKeywords.take(4)
                )
            )
        }

        // 7. NATURE (طبيعة ومناظر طبيعية)
        val natureMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_NATURE,
            targetKeywords = listOf("nature", "mountain", "lake", "river", "ocean", "beach", "forest", "tree", "flower", "sunset", "sunrise", "sky", "park", "garden", "landscape", "plant", "طبيعة", "جبل", "بحر", "غابة", "غروب", "شروق", "حديقة", "زهور", "نبات", "سماء"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (natureMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = natureMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_nature",
                    themeKey = "NATURE",
                    title = "Nature & Outdoors",
                    titleArabic = "طبيعة ومناظر خارجية",
                    description = "${natureMatch.matchedIds.size} nature, mountain, sunset & outdoor scenic shots",
                    descriptionArabic = "تم العثور على ${natureMatch.matchedIds.size} صورة طبيعة ومناظر ومساحات خضراء",
                    iconType = "park",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = natureMatch.matchedIds,
                    mediaCount = natureMatch.matchedIds.size,
                    confidenceScore = 0.95f,
                    matchedTags = natureMatch.matchedKeywords.take(4)
                )
            )
        }

        // 8. TRAVEL (سفر ورحلات)
        val travelMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_TRAVEL,
            targetKeywords = listOf("travel", "trip", "tour", "airplane", "plane", "airport", "hotel", "beach", "landmark", "monument", "luggage", "suitcase", "passport", "tourist", "vacation", "resort", "سفر", "رحلة", "فندق", "مطار", "طيران", "شاطئ", "سياحة", "جواز سفر"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (travelMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = travelMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_travel",
                    themeKey = "TRAVEL",
                    title = "Travel & Adventures",
                    titleArabic = "رحلات وأسفار",
                    description = "${travelMatch.matchedIds.size} travel, vacation & scenic destination photos",
                    descriptionArabic = "لديك ${travelMatch.matchedIds.size} صورة رحلات ومعالم سياحية ومطارات",
                    iconType = "flight",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = travelMatch.matchedIds,
                    mediaCount = travelMatch.matchedIds.size,
                    confidenceScore = 0.94f,
                    matchedTags = travelMatch.matchedKeywords.take(4)
                )
            )
        }

        // 9. PERSON (أشخاص وصور شخصية)
        val personMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_PERSON,
            targetKeywords = listOf("person", "portrait", "selfie", "human", "face", "man", "woman", "child", "boy", "girl", "people", "شخص", "بورتريه", "سيلفي", "وجه", "أشخاص"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia
        )
        if (personMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = personMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_person",
                    themeKey = "PERSON",
                    title = "Portraits & People",
                    titleArabic = "أشخاص وصور شخصية",
                    description = "${personMatch.matchedIds.size} portrait & personal photos detected",
                    descriptionArabic = "تم العثور على ${personMatch.matchedIds.size} صورة شخصية وبورتريه تحتوي أشخاصًا",
                    iconType = "person",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = personMatch.matchedIds,
                    mediaCount = personMatch.matchedIds.size,
                    confidenceScore = 0.95f,
                    matchedTags = personMatch.matchedKeywords.take(4)
                )
            )
        }

        // 10. WORK (صور ومشاريع العمل)
        val workOcrKeywords = listOf("project", "meeting", "code", "architecture", "diagram", "slide", "presentation", "agenda", "مشروع", "اجتماع", "كود", "عرض تقديمي")
        val workMatch = findMatchingMedia(
            mediaList = mediaList,
            categoryId = CategoryClassifier.CATEGORY_WORK,
            targetKeywords = listOf("work", "office", "desk", "laptop", "computer", "keyboard", "meeting", "presentation", "whiteboard", "code", "project", "عمل", "مكتب", "حاسوب", "اجتماع", "مشروع"),
            classificationsByMedia = classificationsByMedia,
            objectsByMedia = objectsByMedia,
            categoriesByMedia = categoriesByMedia,
            ocrByMedia = ocrByMedia,
            ocrKeywords = workOcrKeywords
        )
        if (workMatch.matchedIds.size >= minCountThreshold) {
            val sampleUris = workMatch.matchedIds.take(4).mapNotNull { mediaById[it]?.uriString }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_work",
                    themeKey = "WORK",
                    title = "Work & Projects",
                    titleArabic = "عمل ومشاريع",
                    description = "${workMatch.matchedIds.size} work-related and project photos detected",
                    descriptionArabic = "تم العثور على ${workMatch.matchedIds.size} صورة متعلقة بالعمل والمشاريع",
                    iconType = "work",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = workMatch.matchedIds,
                    mediaCount = workMatch.matchedIds.size,
                    confidenceScore = 0.93f,
                    matchedTags = workMatch.matchedKeywords.take(4)
                )
            )
        }

        // 11. Temporal Suggestion: This Week
        val oneWeekAgo = currentTimeMs - (7L * 24 * 60 * 60 * 1000)
        val thisWeekItems = mediaList.filter { it.dateTaken in (oneWeekAgo + 1)..currentTimeMs }
        if (thisWeekItems.size >= minCountThreshold && thisWeekItems.size >= 3) {
            val sampleUris = thisWeekItems.take(4).map { it.uriString }
            val ids = thisWeekItems.map { it.id }
            suggestions.add(
                AiAlbumSuggestion(
                    id = "suggestion_temporal_week",
                    themeKey = "RECENT_WEEK",
                    title = "This Week's Highlights",
                    titleArabic = "أبرز لقطات الأسبوع",
                    description = "${thisWeekItems.size} moments captured in the past 7 days",
                    descriptionArabic = "${thisWeekItems.size} لقطة تم التقاطها خلال آخر 7 أيام",
                    iconType = "auto_awesome",
                    sampleCoverUris = sampleUris,
                    matchingMediaIds = ids,
                    mediaCount = ids.size,
                    confidenceScore = 0.90f,
                    matchedTags = listOf("this_week", "recent", "أحدث_الصور")
                )
            )
        }

        // Sort suggestions by relevance: confidenceScore * mediaCount descending, ensuring deterministic ordering
        return suggestions
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<AiAlbumSuggestion> { it.confidenceScore }
                    .thenByDescending { it.mediaCount }
            )
    }

    private data class MatchResult(
        val matchedIds: List<Long>,
        val matchedKeywords: List<String>
    )

    private fun findMatchingMedia(
        mediaList: List<MediaItem>,
        categoryId: String,
        targetKeywords: List<String>,
        classificationsByMedia: Map<Long, List<ImageClassificationEntity>>,
        objectsByMedia: Map<Long, List<DetectedObjectEntity>>,
        categoriesByMedia: Map<Long, List<String>>,
        ocrByMedia: Map<Long, OcrTextEntity>,
        ocrKeywords: List<String> = emptyList()
    ): MatchResult {
        val matchedIds = mutableSetOf<Long>()
        val foundKeywords = mutableSetOf<String>()

        for (item in mediaList) {
            var isMatched = false

            // 1. Direct Category Cross-Ref
            val assignedCats = categoriesByMedia[item.id] ?: emptyList()
            if (assignedCats.contains(categoryId)) {
                isMatched = true
                foundKeywords.add(categoryId.lowercase(Locale.ROOT))
            }

            // 2. Local ML Classification Labels
            val itemClassifications = classificationsByMedia[item.id] ?: emptyList()
            for (cls in itemClassifications) {
                val labelLower = cls.label.lowercase(Locale.ROOT)
                val catLower = cls.category.lowercase(Locale.ROOT)
                for (kw in targetKeywords) {
                    if (labelLower.contains(kw) || catLower.contains(kw)) {
                        isMatched = true
                        foundKeywords.add(kw)
                        break
                    }
                }
            }

            // 3. Local Detected Objects
            val itemObjects = objectsByMedia[item.id] ?: emptyList()
            for (obj in itemObjects) {
                val objLabelLower = obj.labelName.lowercase(Locale.ROOT)
                for (kw in targetKeywords) {
                    if (objLabelLower.contains(kw)) {
                        isMatched = true
                        foundKeywords.add(kw)
                        break
                    }
                }
            }

            // 4. File Path / File Name Context Heuristic
            val pathLower = item.filePath.lowercase(Locale.ROOT)
            val nameLower = item.fileName.lowercase(Locale.ROOT)
            for (kw in targetKeywords) {
                if (pathLower.contains(kw) || nameLower.contains(kw)) {
                    isMatched = true
                    foundKeywords.add(kw)
                    break
                }
            }

            // 5. OCR Text matching
            val ocrText = ocrByMedia[item.id]?.extractedText?.lowercase(Locale.ROOT)
            if (!ocrText.isNullOrBlank()) {
                val combinedOcrKws = if (ocrKeywords.isNotEmpty()) ocrKeywords else targetKeywords
                for (kw in combinedOcrKws) {
                    if (ocrText.contains(kw.lowercase(Locale.ROOT))) {
                        isMatched = true
                        foundKeywords.add(kw)
                        break
                    }
                }
            }

            if (isMatched) {
                matchedIds.add(item.id)
            }
        }

        return MatchResult(
            matchedIds = matchedIds.toList(),
            matchedKeywords = foundKeywords.toList()
        )
    }
}
