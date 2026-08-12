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
        path: String = "/storage/emulated/0/DCIM/test.jpg",
        name: String = "test.jpg"
    ) = MediaItemEntity(
        id = id,
        uriString = "file://$path",
        filePath = path,
        fileName = name,
        mimeType = "image/jpeg",
        isVideo = false,
        width = 1080,
        height = 1080,
        sizeBytes = 1024L,
        dateTaken = System.currentTimeMillis(),
        dateModified = System.currentTimeMillis()
    )

    @Test
    fun test1_TradingViewChart() {
        val item = dummyMediaItem(name = "chart_analysis.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "TradingView BTC/USD Technical Analysis")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)
        
        assertTrue("TradingView chart should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test2_MetaTraderChart() {
        val item = dummyMediaItem(name = "mt5_forex.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "MetaTrader 5 EUR/USD Order Buy Sell Stop Loss Take Profit")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("MetaTrader chart should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test3_CandlestickChartWithIndicators() {
        val item = dummyMediaItem(name = "candlestick_chart.png")
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "candlestick chart", category = "financial chart", confidence = 0.95f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "RSI Moving Average Support Resistance")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, ocrText = ocr)

        assertTrue("Candlestick chart with indicators should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test4_CryptoTradingScreenshot() {
        val item = dummyMediaItem(path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_Binance_Crypto.png", name = "Screenshot_Binance_Crypto.png")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Binance Crypto P&L +125% Futures Leverage 20x BTC/USDT")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("Crypto trading screenshot should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Crypto trading screenshot should also be SCREENSHOT", categories.contains(CategoryClassifier.CATEGORY_SCREENSHOT))
    }

    @Test
    fun test5_ForexChart() {
        val item = dummyMediaItem(name = "forex_xauusd.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "تداول فوركس XAU/USD تحليل السوق نقطة الدخول جني الأرباح")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertTrue("Forex chart should be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
    }

    @Test
    fun test6_NormalBankReceiptContainingBuy() {
        val item = dummyMediaItem(name = "bank_receipt.jpg")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "إيصال تم تحويل المبلغ شراء بقيمة 500 ريال بنك الراجحي")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertFalse("Normal bank receipt containing 'شراء' must NOT be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Bank receipt should be classified as DOCUMENT", categories.contains(CategoryClassifier.CATEGORY_DOCUMENT))
    }

    @Test
    fun test7_NormalShoppingScreenshot() {
        val item = dummyMediaItem(path = "/storage/emulated/0/Pictures/Screenshots/Screenshot_Amazon.png", name = "Screenshot_Amazon.png")
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "سلة المشتريات اطلب الآن توصيل مجاني السعر 150 ريال")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, ocrText = ocr)

        assertFalse("Normal shopping screenshot must NOT be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Normal shopping screenshot should be SCREENSHOT", categories.contains(CategoryClassifier.CATEGORY_SCREENSHOT))
    }

    @Test
    fun test8_PersonViewingTradingChart() {
        val item = dummyMediaItem(name = "person_chart.jpg")
        val faces = listOf(DetectedFaceEntity(id = 1L, mediaId = 1L, left = 0f, top = 0f, right = 100f, bottom = 100f, confidence = 0.98f))
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 1, label = "person", category = "human", confidence = 0.95f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "TradingView BTC/USD Chart Analysis")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, faces = faces, ocrText = ocr)

        assertTrue("Person viewing trading chart should be TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Person viewing trading chart should also be PERSON", categories.contains(CategoryClassifier.CATEGORY_PERSON))
    }

    @Test
    fun test9_ProductImageUnrelatedToTrading() {
        val item = dummyMediaItem(name = "running_shoes.jpg")
        val objects = listOf(DetectedObjectEntity(id = 1L, mediaId = 1L, classId = 1, labelName = "shoe", score = 0.9f, left = 0f, top = 0f, right = 50f, bottom = 50f))
        val classifications = listOf(ImageClassificationEntity(mediaId = 1L, classId = 2, label = "footwear", category = "product", confidence = 0.92f))
        val ocr = OcrTextEntity(mediaId = 1L, extractedText = "Nike Running Shoes Air Max Red Size 42")
        val categories = CategoryClassifier.classifyMediaItem(mediaItem = item, classifications = classifications, objects = objects, ocrText = ocr)

        assertFalse("Unrelated product must NOT be classified as TRADING", categories.contains(CategoryClassifier.CATEGORY_TRADING))
        assertTrue("Product image should be PRODUCT", categories.contains(CategoryClassifier.CATEGORY_PRODUCT))
    }
}
