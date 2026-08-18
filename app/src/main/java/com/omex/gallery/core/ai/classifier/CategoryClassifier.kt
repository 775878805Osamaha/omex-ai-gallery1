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
        val isScreenshot = pathLower.contains("screenshot") || nameLower.contains("screenshot") ||
            pathLower.contains("screen_shot") || nameLower.contains("screen_shot") ||
            pathLower.contains("لقطة") || nameLower.contains("لقطة") ||
            classifications.any { it.label.lowercase().contains("screenshot") || it.category.lowercase().contains("screenshot") }
        if (isScreenshot) {
            categories.add(CATEGORY_SCREENSHOT)
        }

        // 2. DOCUMENT / Note / Receipt Detection
        val docKeywords = listOf(
            "document", "doc", "pdf", "invoice", "receipt", "bill", "contract", "report", "paper", "sheet",
            "prescription", "medical", "notes", "handwritten", "certificate", "statement", "letter", "prescription",
            "فاتورة", "تقرير", "مستند", "عقد", "ورقة", "ايصال", "إيصال", "شهادة", "روشتة", "تقرير طبي", "ملاحظات", "كشف حساب", "تحليل"
        )
        val docVisualLabels = listOf(
            "document", "paper", "text", "page", "sheet", "receipt", "invoice", "prescription",
            "certificate", "handwriting", "handwritten", "contract", "bill", "letter", "statement"
        )
        val hasDocVisual = classifications.any { cls ->
            val label = cls.label.lowercase()
            val cat = cls.category.lowercase()
            docVisualLabels.any { label.contains(it) || cat.contains(it) }
        } || objects.any { obj ->
            val label = obj.labelName.lowercase()
            docVisualLabels.any { label.contains(it) }
        }

        val textWordCount = textLower.split("\\s+".toRegex()).count { it.isNotBlank() }
        val isDocument = docKeywords.any { pathLower.contains(it) || nameLower.contains(it) } ||
            hasDocVisual ||
            textWordCount > 25 ||
            docKeywords.any { textLower.contains(it) }

        if (isDocument) {
            categories.add(CATEGORY_DOCUMENT)
        }

        // 3. PERSON Detection
        val hasFace = faces.isNotEmpty()
        val hasPersonObject = objects.any {
            val label = it.labelName.lowercase()
            label.contains("person") || label.contains("human") || label.contains("man") ||
            label.contains("woman") || label.contains("child") || label.contains("boy") || label.contains("girl")
        }
        val hasPersonClassification = classifications.any {
            val label = it.label.lowercase()
            val cat = it.category.lowercase()
            label.contains("person") || label.contains("human") || cat.contains("human") ||
            cat.contains("person") || label.contains("portrait") || label.contains("selfie")
        }
        val hasPerson = hasFace || hasPersonObject || hasPersonClassification ||
            pathLower.contains("selfie") || nameLower.contains("selfie") ||
            pathLower.contains("portrait") || nameLower.contains("portrait")

        if (hasPerson) {
            categories.add(CATEGORY_PERSON)
        }

        // 4. CAR Detection
        val carLabels = listOf("car", "vehicle", "automobile", "truck", "bus", "jeep", "sedan", "suv", "van", "motorcycle", "سيارة", "مركبة", "شاحنة")
        if (carLabels.any { pathLower.contains(it) || nameLower.contains(it) } ||
            objects.any { obj -> carLabels.any { obj.labelName.lowercase().contains(it) } } ||
            classifications.any { cls -> carLabels.any { cls.label.lowercase().contains(it) || cls.category.lowercase().contains(it) } }
        ) {
            categories.add(CATEGORY_CAR)
        }

        // 5. FOOD Detection
        val foodLabels = listOf(
            "food", "meal", "dish", "cuisine", "pizza", "sandwich", "burger", "fruit", "cake", "bread",
            "coffee", "tea", "drink", "restaurant", "apple", "banana", "orange", "dessert", "snack",
            "طعام", "وجبة", "أكل", "مطعم", "مشروب"
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

        // 9. TRADING Detection (Visual + OCR + Contextual Rules)
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

        var isTrading = false
        if (!isReceiptOrInvoice) {
            if (hasStrongTerm || hasTicker) {
                categories.add(CATEGORY_TRADING)
                isTrading = true
            } else if (hasVisualChart && (secondaryMatchCount >= 1 || buySellMatchCount >= 1)) {
                categories.add(CATEGORY_TRADING)
                isTrading = true
            } else if (secondaryMatchCount >= 2 && buySellMatchCount >= 1) {
                categories.add(CATEGORY_TRADING)
                isTrading = true
            }
        }

        // 10. PRODUCT Detection (Visual & Physical Evidence Priority)
        // Strict physical merchandise labels - excludes generic human attire (clothing, shirt, pants, dress)
        val specificPhysicalProductLabels = listOf(
            "shoe", "sneaker", "boot", "sandal", "footwear", "heel",
            "bottle", "package", "packaging", "perfume", "cosmetics", "lotion", "shampoo", "lipstick", "makeup",
            "smartphone", "cellphone", "mobile phone", "phone", "iphone", "samsung", "ipad", "smartwatch", "wristwatch", "watch",
            "headphones", "earphones", "headset", "earbuds", "speaker", "camera", "gadget", "tablet", "laptop", "computer",
            "keyboard", "mouse", "toy", "furniture", "chair", "table", "couch", "sofa", "bed", "appliance", "television", "tv",
            "handbag", "backpack", "wallet", "purse", "sunglasses", "glasses", "jewelry", "ring", "necklace", "earring", "bracelet",
            "cup", "mug", "merchandise", "retail product", "consumer product", "item", "goods", "box",
            "منتج", "منتجات", "سلعة", "بضاعة", "مقتنيات", "عطر", "حذاء", "حقيبة", "ساعة", "نظارة", "هاتف", "جوال"
        )

        val generalProductCategoryLabels = listOf(
            "product", "merchandise", "retail", "consumer goods", "goods", "item", "shopping",
            "منتج", "سلعة", "بضاعة", "منتجات"
        )

        // Visual Evidence 1: Specific physical product detected as an object
        val hasDetectedPhysicalObject = objects.any { obj ->
            val objLabel = obj.labelName.lowercase()
            specificPhysicalProductLabels.any { objLabel.contains(it) }
        }

        // Visual Evidence 2: Visual classification explicitly identifies physical product
        val hasClassifiedPhysicalProduct = classifications.any { cls ->
            val clsLabel = cls.label.lowercase()
            val clsCat = cls.category.lowercase()
            // Exclude document, paper, text, receipt, chart from product visual evidence
            val isDocOrChartLabel = docVisualLabels.any { clsLabel.contains(it) || clsCat.contains(it) } ||
                visualChartLabels.any { clsLabel.contains(it) || clsCat.contains(it) }

            !isDocOrChartLabel && (
                specificPhysicalProductLabels.any { clsLabel.contains(it) } ||
                generalProductCategoryLabels.any { clsCat == it || clsLabel == it }
            )
        }

        // Visual Evidence 3: Explicit product file path / name / folder
        val productPathKeywords = listOf(
            "product", "products", "item", "items", "merchandise", "goods", "shop", "store",
            "ecommerce", "amazon", "noon", "jumia", "aliexpress", "ebay", "market",
            "منتج", "منتجات", "سلعة", "بضاعة", "مبيعات", "متجر", "تسوق"
        )
        val hasProductFileName = productPathKeywords.any { pathLower.contains(it) || nameLower.contains(it) } && !isScreenshot

        // Evidence 4: Strong product keywords in OCR text (e.g. price tags, discount, buy now)
        val productOcrKeywords = listOf(
            "price:", "price :", "egp", "sar", "aed", "السعر", "سعر", "جنيه", "ريال", "درهم",
            "discount", "offer", "sale", "order now", "buy now", "add to cart",
            "خصم", "عروض", "عرض", "تخفيض", "تخفيضات", "اطلب الان", "شراء"
        )
        val hasProductOcr = !isDocument && productOcrKeywords.any { textLower.contains(it) }

        val hasVisualProductEvidence = hasDetectedPhysicalObject || hasClassifiedPhysicalProduct || hasProductFileName || hasProductOcr

        // Documents, medical papers, receipts, handwritten notes, and trading charts must NOT be PRODUCT
        // unless an actual distinct physical product object is visually detected.
        val isPureDocumentOrNote = isDocument && !hasDetectedPhysicalObject && !hasProductFileName

        // If it's a person portrait without standalone physical product evidence, do not classify as PRODUCT
        val isPersonPortrait = hasPerson && !hasDetectedPhysicalObject && !hasProductFileName

        if (hasVisualProductEvidence && !isTrading && !isPureDocumentOrNote && !isPersonPortrait) {
            categories.add(CATEGORY_PRODUCT)
        }

        // Fallback: Default heuristic if still empty
        if (categories.isEmpty()) {
            if (pathLower.contains("dcim") || pathLower.contains("camera")) {
                if (faces.isNotEmpty()) categories.add(CATEGORY_PERSON) else categories.add(CATEGORY_OTHER)
            } else {
                categories.add(CATEGORY_OTHER)
            }
        }

        return categories
    }
}
