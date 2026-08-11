package com.omex.gallery.core.ai.classifier

import com.omex.gallery.core.data.local.DetectedFaceEntity
import com.omex.gallery.core.data.local.DetectedObjectEntity
import com.omex.gallery.core.data.local.ImageClassificationEntity
import com.omex.gallery.core.data.local.MediaItemEntity
import com.omex.gallery.core.data.local.OcrTextEntity

object CategoryClassifier {

    const val CATEGORY_PERSON = "PERSON"
    const val CATEGORY_PRODUCT = "PRODUCT"
    const val CATEGORY_SCREENSHOT = "SCREENSHOT"
    const val CATEGORY_DOCUMENT = "DOCUMENT"
    const val CATEGORY_CAR = "CAR"
    const val CATEGORY_FOOD = "FOOD"
    const val CATEGORY_NATURE = "NATURE"
    const val CATEGORY_TRAVEL = "TRAVEL"
    const val CATEGORY_WORK = "WORK"
    const val CATEGORY_TRADING = "TRADING"
    const val CATEGORY_OTHER = "OTHER"

    fun classifyMediaItem(
        mediaItem: MediaItemEntity,
        classifications: List<ImageClassificationEntity> = emptyList(),
        objects: List<DetectedObjectEntity> = emptyList(),
        faces: List<DetectedFaceEntity> = emptyList(),
        ocrText: OcrTextEntity? = null
    ): Set<String> {
        val categories = mutableSetOf<String>()

        val pathLower = mediaItem.filePath.lowercase()
        val nameLower = mediaItem.fileName.lowercase()
        val textLower = (ocrText?.extractedText ?: "").lowercase()

        // 1. SCREENSHOT Detection
        if (pathLower.contains("screenshot") || nameLower.contains("screenshot") ||
            pathLower.contains("screen_shot") || nameLower.contains("screen_shot") ||
            pathLower.contains("لقطة") || nameLower.contains("لقطة")
        ) {
            categories.add(CATEGORY_SCREENSHOT)
        }

        // 2. DOCUMENT Detection
        val docKeywords = listOf(
            "document", "doc", "pdf", "invoice", "receipt", "bill", "contract", "report", "paper", "sheet",
            "فاتورة", "تقرير", "مستند", "عقد", "ورقة", "ايصال", "إيصال", "شهادة"
        )
        val textWordCount = textLower.split("\\s+".toRegex()).count { it.isNotBlank() }
        if (docKeywords.any { pathLower.contains(it) || nameLower.contains(it) } ||
            textWordCount > 25 ||
            docKeywords.any { textLower.contains(it) }
        ) {
            categories.add(CATEGORY_DOCUMENT)
        }

        // 3. PERSON Detection
        if (faces.isNotEmpty()) {
            categories.add(CATEGORY_PERSON)
        }
        if (objects.any { it.labelName.lowercase().contains("person") || it.labelName.lowercase().contains("human") }) {
            categories.add(CATEGORY_PERSON)
        }

        // 4. CAR / Vehicle Detection
        val carLabels = listOf("car", "vehicle", "automobile", "truck", "bus", "jeep", "sedan", "suv", "van", "سيارة", "مركبة", "شاحنة")
        if (carLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            objects.any { obj -> carLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> carLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_CAR)
        }

        // 5. FOOD Detection
        val foodLabels = listOf(
            "food", "meal", "dish", "cuisine", "pizza", "sandwich", "burger", "fruit", "cake", "bread",
            "coffee", "tea", "drink", "restaurant", "apple", "banana", "orange", "طعام", "وجبة", "أكل", "مطعم"
        )
        if (foodLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            objects.any { obj -> foodLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> foodLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_FOOD)
        }

        // 6. NATURE Detection
        val natureLabels = listOf(
            "nature", "mountain", "river", "lake", "ocean", "beach", "forest", "tree", "flower", "plant",
            "sky", "sunset", "park", "garden", "landscape", "طبيعة", "جبل", "بحر", "نهر", "شجرة", "زهرة"
        )
        if (natureLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            objects.any { obj -> natureLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> natureLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_NATURE)
        }

        // 7. TRAVEL Detection
        val travelLabels = listOf(
            "travel", "trip", "tour", "tourist", "airplane", "plane", "airport", "hotel", "resort", "luggage",
            "suitcase", "passport", "landmark", "monument", "سفر", "رحلة", "فندق", "مطار", "طائرة", "حقيبة"
        )
        if (travelLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            objects.any { obj -> travelLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> travelLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_TRAVEL)
        }

        // 8. WORK Detection
        val workLabels = listOf(
            "work", "office", "desk", "laptop", "computer", "keyboard", "presentation", "meeting", "code",
            "عمل", "مكتب", "لاب توب", "كمبيوتر", "اجتماع"
        )
        if (workLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            objects.any { obj -> workLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> workLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_WORK)
        }

        // 9. PRODUCT Detection
        val productLabels = listOf(
            "product", "item", "package", "box", "bottle", "shoe", "sneaker", "boot", "watch", "phone",
            "electronics", "gadget", "clothing", "dress", "shirt", "pants", "bag", "handbag", "tool",
            "container", "retail", "merchandise", "perfume", "cosmetics", "toy", "furniture",
            "منتج", "سلعة", "بضاعة", "سعر", "خصم", "شراء", "للبيع"
        )
        val productPricePatterns = listOf("egp", "sar", "aed", "$", "جنيه", "ريال", "درهم", "سعر", "price", "off")
        if (productLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            productPricePatterns.any { textLower.contains(it) } ||
            objects.any { obj -> productLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> productLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_PRODUCT)
        }

        // 10. TRADING Detection (Visual + OCR + Contextual Rules)
        val strongTradingTerms = listOf(
            "tradingview", "metatrader", "mt4", "mt5", "binance", "bybit", "okx",
            "forex", "crypto", "cryptocurrency", "bitcoin", "ethereum",
            "candlestick", "candles", "stop loss", "take profit", "price action",
            "technical analysis", "p&l", "leverage", "margin",
            "تداول", "تداولات", "التداول", "فوركس", "بيتكوين", "إيثريوم", "العملات الرقمية",
            "تحليل فني", "تحليل تقني", "شموع يابانية", "وقف الخسارة", "جني الأرباح",
            "حساب التداول", "السوق المالي"
        )

        val tickerPatterns = listOf("btc/usd", "eth/usd", "eur/usd", "gbp/usd", "xau/usd", "usdt", "btc", "eth")

        val secondaryTradingTerms = listOf(
            "trading", "trader", "stock", "stocks", "market", "financial market",
            "support", "resistance", "trend", "chart", "position", "order", "orders",
            "سوق", "الأسواق", "دعم", "مقاومة", "ترند", "مؤشر", "مؤشرات", "صفقة", "صفقات", "محفظة"
        )

        val buySellTerms = listOf("buy", "sell", "long", "short", "entry", "exit", "شراء", "بيع", "ربح", "خسارة")

        val visualChartLabels = listOf("chart", "graph", "diagram", "candlestick", "stock", "trading", "financial")

        val hasVisualChart = classifications.any { cls -> visualChartLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } } ||
                objects.any { obj -> visualChartLabels.any { obj.labelName.lowercase().contains(it) } } ||
                pathLower.contains("chart") || nameLower.contains("chart") || pathLower.contains("trading") || nameLower.contains("trading")

        val hasStrongTerm = strongTradingTerms.any { textLower.contains(it) || pathLower.contains(it) || nameLower.contains(it) }
        val hasTicker = tickerPatterns.any { textLower.contains(it) }

        val secondaryMatchCount = secondaryTradingTerms.count { textLower.contains(it) }
        val buySellMatchCount = buySellTerms.count { textLower.contains(it) }

        // Non-trading receipt guard: normal shopping/bank receipts should NOT be classified as trading
        val isReceiptOrInvoice = (textLower.contains("receipt") || textLower.contains("invoice") || textLower.contains("فاتورة") || textLower.contains("إيصال") || textLower.contains("مشتريات")) &&
                !hasVisualChart && !hasStrongTerm

        if (!isReceiptOrInvoice) {
            if (hasStrongTerm || hasTicker) {
                categories.add(CATEGORY_TRADING)
            } else if (hasVisualChart && (secondaryMatchCount >= 1 || buySellMatchCount >= 1)) {
                categories.add(CATEGORY_TRADING)
            } else if (secondaryMatchCount >= 2 && buySellMatchCount >= 1) {
                categories.add(CATEGORY_TRADING)
            }
        }

        // Fallback: Default heuristic if still empty
        if (categories.isEmpty()) {
            if (pathLower.contains("dcim") || pathLower.contains("camera")) {
                // Check if likely a photo containing people or objects
                if (faces.isNotEmpty()) categories.add(CATEGORY_PERSON) else categories.add(CATEGORY_OTHER)
            } else {
                categories.add(CATEGORY_OTHER)
            }
        }

        return categories
    }
}
