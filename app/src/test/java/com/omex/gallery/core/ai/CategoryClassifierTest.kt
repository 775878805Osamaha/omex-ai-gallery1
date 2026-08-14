package com.omex.gallery.core.ai

import com.omex.gallery.core.ai.classifier.CategoryClassifier
import com.omex.gallery.core.data.local.DetectedFaceEntity
import com.omex.gallery.core.data.local.DetectedObjectEntity
import com.omex.gallery.core.data.local.ImageClassificationEntity
import com.omex.gallery.core.data.local.MediaItemEntity
import com.omex.gallery.core.data.local.OcrTextEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryClassifierTest {

    private fun dummyMediaItem(
        id: Long = 1L,
        path: String = "/storage/emulated/0/DCIM/Camera/IMG_20260101.jpg",
        name: String = "IMG_20260101.jpg"
    ) = MediaItemEntity(
        id = id,
        uriString = "content://media/external/images/media/$id",
        filePath = path,
        fileName = name,
        mimeType = "image/jpeg",
        isVideo = false,
        width = 1920,
        height = 1080,
        sizeBytes = 1024 * 1024,
        dateTaken = System.currentTimeMillis(),
        dateModified = System.currentTimeMillis()
    )

    @Test
    fun test1_TradingViewChartClassification() {
        val item = dummyMediaItem(name = "tradingview_btc_analysis.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "TradingView BTCUSDT 1D Technical Analysis Bullish Flag")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("TradingView chart should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test2_MetaTraderChartClassification() {
        val item = dummyMediaItem(name = "chart_screenshot.png")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "MetaTrader 5 EURUSD H4 Stop Loss Take Profit Buy Limit")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("MetaTrader chart should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test3_CandlestickVisualClassification() {
        val item = dummyMediaItem(name = "market_overview.jpg")
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 1L, classId = 1, label = "candlestick chart", category = "chart", confidence = 0.95f)
        )
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Support and resistance breakout entry point target")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, ocrText = ocr)

        assertTrue("Candlestick chart with indicators should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test4_CryptoScreenshotMultiCategory() {
        val item = dummyMediaItem(path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_Binance.png", name = "Screenshot_Binance.png")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Binance Futures Position Long ETHUSDT Leverage 10x")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("Crypto trading screenshot should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Crypto trading screenshot should also be SCREENSHOT", categories.contains(CategoryClassifier.CATEGORY_SCREENSHOT))
    }

    @Test
    fun test5_ArabicForexTextClassification() {
        val item = dummyMediaItem(name = "analysis_arabic.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "تداول العملات الفوركس تحليل فني لزوج الذهب مقابل الدولار وقف الخسارة")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("Forex chart should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test6_ShoppingReceiptNotTrading() {
        val item = dummyMediaItem(name = "supermarket_receipt.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "فاتورة شراء سوبر ماركت كارفور تم شراء حليب وخبز السعر الإجمالي 150 جنيه")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertFalse("Normal bank receipt containing 'شراء' must NOT be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Bank receipt should be classified as DOCUMENT", categories.contains(CategoryClassifier.CATEGORY_DOCUMENT))
    }

    @Test
    fun test7_ECommerceProductNotTrading() {
        val item = dummyMediaItem(path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_Amazon.png", name = "Screenshot_Amazon.png")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Amazon Order Summary Buy Now Wireless Headphones Price 50 USD")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertFalse("Normal shopping screenshot must NOT be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Normal shopping screenshot should be SCREENSHOT", categories.contains(CategoryClassifier.CATEGORY_SCREENSHOT))
    }

    @Test
    fun test8_PersonWithTradingChart() {
        val item = dummyMediaItem(name = "trader_working.jpg")
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 1L, classId = 1, label = "financial chart", category = "chart", confidence = 0.90f)
        )
        val faces = listOf(
            DetectedFaceEntity(id = 1L, mediaId = 1L, left = 10f, top = 10f, right = 50f, bottom = 50f, confidence = 0.98f)
        )
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Bitcoin bull market analysis")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, faces = faces, ocrText = ocr)

        assertTrue("Person viewing trading chart should be TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Person viewing trading chart should also be PERSON", categories.contains(CategoryClassifier.CATEGORY_PERSON))
    }

    @Test
    fun test9_UnrelatedProductImageNotTrading() {
        val item = dummyMediaItem(name = "perfume_bottle.jpg")
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 1L, classId = 1, label = "perfume bottle", category = "cosmetics", confidence = 0.95f)
        )
        val objects = listOf(
            DetectedObjectEntity(id = 1L, mediaId = 1L, classId = 1, labelName = "bottle", score = 0.92f, left = 0f, top = 0f, right = 100f, bottom = 100f)
        )
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Eau De Parfum 100ml Special Offer")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, objects = objects, ocrText = ocr)

        assertFalse("Unrelated product must NOT be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Product image should be PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }

    @Test
    fun test10_RealProductImageAppearsInProducts() {
        val item = dummyMediaItem(name = "perfume_bottle.jpg")
        val objects = listOf(DetectedObjectEntity(id = 1L, mediaId = 1L, classId = 1, labelName = "bottle", score = 0.95f, left = 10f, top = 10f, right = 80f, bottom = 90f))
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 2, label = "perfume", category = "cosmetics", confidence = 0.94f))
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, objects = objects)

        assertTrue("Real product image must appear in PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
        assertFalse("Real product without person should not be PERSON", categories.contains(CategoryClassifier.CATEGORY_PERSON))
        assertFalse("Real product should not be DOCUMENT", categories.contains(CategoryClassifier.CATEGORY_DOCUMENT))
    }

    @Test
    fun test11_PersonPhotoDoesNotAppearInProducts() {
        val item = dummyMediaItem(name = "portrait_friend.jpg")
        val faces = listOf(DetectedFaceEntity(id = 1L, mediaId = 1L, left = 20f, top = 20f, right = 80f, bottom = 80f, confidence = 0.99f))
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 1L, classId = 1, label = "person", category = "human", confidence = 0.98f),
            ImageClassificationEntity(mediaId = 1L, classId = 2, label = "clothing", category = "apparel", confidence = 0.90f),
            ImageClassificationEntity(mediaId = 1L, classId = 3, label = "shirt", category = "apparel", confidence = 0.88f)
        )
        val objects = listOf(DetectedObjectEntity(id = 1L, mediaId = 1L, classId = 1, labelName = "person", score = 0.97f, left = 0f, top = 0f, right = 100f, bottom = 100f))
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, objects = objects, faces = faces)

        assertTrue("Person photo must be classified as PERSON", categories.contains(CategoryClassifier.CATEGORY_PERSON))
        assertFalse("Person wearing clothes must NOT be classified as PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }

    @Test
    fun test12_HandwrittenNotesDoNotAppearInProducts() {
        val item = dummyMediaItem(name = "meeting_notes.jpg")
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "handwriting", category = "document", confidence = 0.93f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Notes from project kickoff: total budget 5000 USD, timeline 3 months, items required: 5 units.")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, ocrText = ocr)

        assertTrue("Handwritten notes must be classified as DOCUMENT", categories.contains(CategoryClassifier.CATEGORY_DOCUMENT))
        assertFalse("Handwritten notes must NOT appear in PRODUCT even with prices/words in OCR", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }

    @Test
    fun test13_MedicalDocumentDoesNotAppearInProducts() {
        val item = dummyMediaItem(name = "medical_prescription.jpg")
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "prescription", category = "document", confidence = 0.96f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "روشتة طبية دكتور أحمد علاج ضغط الدم السعر 120 جنيه صيدلية العزبي")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, ocrText = ocr)

        assertTrue("Medical prescription must be classified as DOCUMENT", categories.contains(CategoryClassifier.CATEGORY_DOCUMENT))
        assertFalse("Medical prescription must NOT appear in PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }

    @Test
    fun test14_BankPaymentReceiptDoesNotAppearInProducts() {
        val item = dummyMediaItem(name = "pos_payment_receipt.jpg")
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "receipt", category = "paper", confidence = 0.95f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "فاتورة دفع إلكتروني إيصال سداد المبلغ 350 SAR تم الدفع بنجاح كود العملية 98234")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, ocrText = ocr)

        assertTrue("Bank payment receipt must be classified as DOCUMENT", categories.contains(CategoryClassifier.CATEGORY_DOCUMENT))
        assertFalse("Bank payment receipt must NOT appear in PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
        assertFalse("Bank payment receipt must NOT appear in TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test15_TradingScreenshotDoesNotAppearInProducts() {
        val item = dummyMediaItem(path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_Trading_BTC.png", name = "Screenshot_Trading_BTC.png")
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "candlestick chart", category = "chart", confidence = 0.94f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Binance Futures BTC/USDT Long Position Price 95,000 $ Stop Loss Take Profit")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, ocrText = ocr)

        assertTrue("Trading screenshot must be TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Trading screenshot must be SCREENSHOT", categories.contains(CategoryClassifier.CATEGORY_SCREENSHOT))
        assertFalse("Trading screenshot must NOT appear in PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }

    @Test
    fun test16_PersonHoldingProductBothCategories() {
        val item = dummyMediaItem(name = "person_with_new_smartphone.jpg")
        val faces = listOf(DetectedFaceEntity(id = 1L, mediaId = 1L, left = 20f, top = 10f, right = 50f, bottom = 40f, confidence = 0.98f))
        val objects = listOf(
            DetectedObjectEntity(id = 1L, mediaId = 1L, classId = 1, labelName = "person", score = 0.95f, left = 0f, top = 0f, right = 100f, bottom = 100f),
            DetectedObjectEntity(id = 2L, mediaId = 1L, classId = 2, labelName = "smartphone", score = 0.91f, left = 40f, top = 50f, right = 60f, bottom = 80f)
        )
        val classifications = listOf(
            ImageClassificationEntity(mediaId = 1L, classId = 1, label = "person", category = "human", confidence = 0.95f),
            ImageClassificationEntity(mediaId = 1L, classId = 2, label = "gadget", category = "electronics", confidence = 0.89f)
        )
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, objects = objects, faces = faces)

        assertTrue("Person holding a clearly detected product should have PERSON", categories.contains(CategoryClassifier.CATEGORY_PERSON))
        assertTrue("Person holding a clearly detected product should also have PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }

    @Test
    fun test17_ProductImageWithOcrTextConfirmedByVisualEvidence() {
        val item = dummyMediaItem(name = "sony_headphones_box.jpg")
        val objects = listOf(DetectedObjectEntity(id = 1L, mediaId = 1L, classId = 1, labelName = "headphones", score = 0.96f, left = 10f, top = 10f, right = 90f, bottom = 90f))
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "electronics", category = "product", confidence = 0.93f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Sony WH-1000XM5 Wireless Noise Canceling Headphones $399.99 Special Offer")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, objects = objects, ocrText = ocr)

        assertTrue("Product image with OCR confirmed by visual evidence must be PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
        assertFalse("Product box should not be classified as TRADING despite currency and offer", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }
}
