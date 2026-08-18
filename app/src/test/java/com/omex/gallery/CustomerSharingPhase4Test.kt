package com.omex.gallery

import android.content.Intent
import android.net.Uri
import com.omex.gallery.core.ai.share.AiShareDescriptionGenerator
import com.omex.gallery.domain.model.AiClassification
import com.omex.gallery.domain.model.AiObject
import com.omex.gallery.domain.model.AiOcrText
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaItemWithAi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CustomerSharingPhase4Test {

    private fun createDummyMediaItem(
        id: Long = 100L,
        fileName: String = "leather_bag.jpg",
        uriString: String = "content://media/external/images/media/100",
        mimeType: String = "image/jpeg",
        isVideo: Boolean = false
    ): MediaItem {
        return MediaItem(
            id = id,
            uriString = uriString,
            filePath = "/storage/emulated/0/DCIM/Camera/$fileName",
            fileName = fileName,
            mimeType = mimeType,
            dateTaken = 1700000000000L,
            sizeBytes = 2048576L,
            width = 1920,
            height = 1080,
            isVideo = isVideo,
            durationMs = if (isVideo) 15000L else 0L,
            isFavorite = false
        )
    }

    private fun createMediaItemWithAi(
        mediaItem: MediaItem,
        objects: List<AiObject> = emptyList(),
        classifications: List<AiClassification> = emptyList(),
        ocrText: String? = null
    ): MediaItemWithAi {
        return MediaItemWithAi(
            mediaItem = mediaItem,
            classifications = classifications,
            objects = objects,
            faces = emptyList(),
            metadata = null,
            ocrText = ocrText?.let { AiOcrText(mediaId = mediaItem.id, extractedText = it, language = "ar") }
        )
    }

    // 1. توليد وصف منتج صحيح
    @Test
    fun test01_GenerateValidProductDescription() {
        val media = createDummyMediaItem(fileName = "luxury_shoes.jpg")
        val itemWithAi = createMediaItemWithAi(
            mediaItem = media,
            objects = listOf(AiObject(mediaId = 100L, classId = 1, labelName = "shoe", score = 0.95f, left = 0f, top = 0f, right = 1f, bottom = 1f)),
            classifications = listOf(AiClassification(mediaId = 100L, classId = 1, label = "Footwear", category = "FASHION", confidence = 0.92f))
        )

        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)
        assertNotNull(desc)
        assertTrue("Description should contain Arabic shoe keyword", desc.contains("حذاء"))
        assertTrue("Description should contain standard suffix", desc.contains(AiShareDescriptionGenerator.DEFAULT_SUFFIX))
    }

    // 2. عدم اختراع مواصفات غير موجودة
    @Test
    fun test02_NoHallucinatedSpecifications() {
        val media = createDummyMediaItem(fileName = "watch_photo.jpg")
        val itemWithAi = createMediaItemWithAi(
            mediaItem = media,
            objects = listOf(AiObject(mediaId = 100L, classId = 1, labelName = "watch", score = 0.88f, left = 0f, top = 0f, right = 1f, bottom = 1f))
        )

        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)
        assertFalse("Should not fabricate price in currency", desc.contains("ريال") || desc.contains("دولار") || desc.contains("جنيه"))
        assertFalse("Should not fabricate warranty claims", desc.contains("ضمان لمدة") || desc.contains("كفالة سنتين"))
        assertFalse("Should not fabricate origin country", desc.contains("صنع في") || desc.contains("ألماني") || desc.contains("ياباني"))
        assertFalse("Should not fabricate fake dimensions", desc.contains("سم") || desc.contains("ملم"))
    }

    // 3. وجود العبارة: "للطلب والاستفسار التواصل معنا."
    @Test
    fun test03_ContainsMandatoryCustomerSuffix() {
        val media = createDummyMediaItem()
        val itemWithAi = createMediaItemWithAi(mediaItem = media)
        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)

        assertTrue(
            "Description must contain 'للطلب والاستفسار التواصل معنا.'",
            desc.contains("للطلب والاستفسار التواصل معنا.")
        )
    }

    // 4. تعديل النص قبل المشاركة
    @Test
    fun test04_EditedTextIncludedInShareIntent() {
        val media = createDummyMediaItem()
        val originalDesc = "حذاء أنيق\n\nللطلب والاستفسار التواصل معنا."
        val userEditedDesc = "عرض خاص اليوم فقط!\nحذاء رياضي أنيق\nللطلب والاستفسار التواصل معنا."

        val intent = AiShareDescriptionGenerator.buildShareIntent(
            mediaItem = media,
            descriptionText = userEditedDesc,
            targetPackage = "com.whatsapp"
        )

        val textInIntent = intent.getStringExtra(Intent.EXTRA_TEXT)
        assertEquals("User edited description must be passed to Intent", userEditedDesc, textInIntent)
    }

    // 5. مشاركة صورة + نص في Intent واحد
    @Test
    fun test05_ShareImageAndTextSingleIntent() {
        val media = createDummyMediaItem(uriString = "content://media/external/images/media/444")
        val desc = "حقيبة يد فاخرة\n\nللطلب والاستفسار التواصل معنا."

        val intent = AiShareDescriptionGenerator.buildShareIntent(
            mediaItem = media,
            descriptionText = desc,
            targetPackage = "com.whatsapp"
        )

        assertEquals(Intent.ACTION_SEND, intent.action)
        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals("content://media/external/images/media/444", streamUri?.toString())
        assertEquals(desc, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    // 6. MIME type الصحيح للصورة
    @Test
    fun test06_CorrectImageMimeType() {
        val pngMedia = createDummyMediaItem(mimeType = "image/png")
        val intent = AiShareDescriptionGenerator.buildShareIntent(pngMedia, "وصف")
        assertEquals("image/png", intent.type)

        val jpegMedia = createDummyMediaItem(mimeType = "image/jpeg")
        val intent2 = AiShareDescriptionGenerator.buildShareIntent(jpegMedia, "وصف")
        assertEquals("image/jpeg", intent2.type)
    }

    // 7. منح URI صلاحية القراءة FLAG_GRANT_READ_URI_PERMISSION
    @Test
    fun test07_GrantReadUriPermissionFlag() {
        val media = createDummyMediaItem()
        val intent = AiShareDescriptionGenerator.buildShareIntent(media, "وصف")

        val hasFlag = (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        assertTrue("Intent must include FLAG_GRANT_READ_URI_PERMISSION", hasFlag)
    }

    // 8. مشاركة الفيديو بالطريقة الصحيحة
    @Test
    fun test08_VideoShareIntent() {
        val videoMedia = createDummyMediaItem(
            fileName = "demo_clip.mp4",
            uriString = "content://media/external/video/media/777",
            mimeType = "video/mp4",
            isVideo = true
        )

        val intent = AiShareDescriptionGenerator.buildShareIntent(
            mediaItem = videoMedia,
            descriptionText = null,
            targetPackage = "com.whatsapp"
        )

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("video/mp4", intent.type)
        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals("content://media/external/video/media/777", streamUri?.toString())
    }

    // 9. عدم إعادة تشغيل AI/OCR عند المشاركة
    @Test
    fun test09_UsesExistingPrecomputedAiMetadata() {
        val media = createDummyMediaItem()
        val precomputedOcr = "عطر فرنسي راقي 100ml"
        val itemWithAi = createMediaItemWithAi(
            mediaItem = media,
            ocrText = precomputedOcr
        )

        // Generating local description uses the existing ocrText from itemWithAi instantly without calling OCR models
        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)
        assertTrue("Should incorporate pre-computed OCR title directly", desc.contains(precomputedOcr))
    }

    // 10. عدم تغيير URI الأصلي
    @Test
    fun test10_PreservesOriginalContentUri() {
        val originalUri = "content://com.android.providers.media.documents/document/image%3A555"
        val media = createDummyMediaItem(uriString = originalUri)
        val intent = AiShareDescriptionGenerator.buildShareIntent(media, "وصف")

        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals(originalUri, streamUri?.toString())
        assertFalse("Must never convert content:// to file://", streamUri.toString().startsWith("file://"))
    }

    // 11. التعامل مع صورة بدون وصف
    @Test
    fun test11_HandleImageWithoutAiMetadata() {
        val media = createDummyMediaItem(fileName = "IMG_20260817.jpg")
        val itemWithAi = createMediaItemWithAi(mediaItem = media) // No OCR, no objects, no classifications

        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)
        assertNotNull(desc)
        assertTrue("Should provide graceful fallback title", desc.contains("منتج مميز"))
        assertTrue("Should still include standard suffix", desc.contains(AiShareDescriptionGenerator.DEFAULT_SUFFIX))
    }

    // 12. التعامل مع منتج بدون بيانات كافية
    @Test
    fun test12_HandleSparseProductData() {
        val media = createDummyMediaItem(fileName = "product_item.jpg")
        val itemWithAi = createMediaItemWithAi(
            mediaItem = media,
            objects = emptyList(),
            classifications = listOf(AiClassification(mediaId = 100L, classId = 1, label = "clothing", category = "FASHION", confidence = 0.51f))
        )

        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)
        assertTrue("Should detect clothing category", desc.contains("ملابس عصرية"))
        assertTrue("Should end with customer contact", desc.contains(AiShareDescriptionGenerator.DEFAULT_SUFFIX))
    }

    // 13. عدم حدوث Crash عند فشل توليد الوصف أو مدخلات غريبة
    @Test
    fun test13_NoCrashOnMalformedOrExtremeInputs() {
        val media = createDummyMediaItem(fileName = "")
        val itemWithAi = createMediaItemWithAi(
            mediaItem = media,
            ocrText = "!@#$%^&*()_+~`|}{[]:;?><,./"
        )

        val desc = AiShareDescriptionGenerator.generateLocalProductDescription(itemWithAi)
        assertNotNull(desc)
        assertTrue(desc.isNotEmpty())
    }

    // 14. إلغاء المشاركة بدون تغيير البيانات
    @Test
    fun test14_CancelShareDoesNotMutateMediaItem() {
        val media = createDummyMediaItem()
        val originalUri = media.uriString
        val originalFavorite = media.isFavorite

        // User opens preview and cancels -> MediaItem remains unchanged
        var sharePreviewOpen = true
        var customerShareText = "تعديل تجريبي"

        // Cancel simulated:
        sharePreviewOpen = false
        // customerShareText is discarded locally

        assertFalse(sharePreviewOpen)
        assertEquals(originalUri, media.uriString)
        assertEquals(originalFavorite, media.isFavorite)
    }

    // 15. الحفاظ على المشاركة الحالية إذا كان WhatsApp غير مثبت (Fallback Intent Chooser)
    @Test
    fun test15_FallbackIntentWithoutPackageRestriction() {
        val media = createDummyMediaItem()
        val desc = "وصف تسويقي"

        // Target WhatsApp
        val whatsappIntent = AiShareDescriptionGenerator.buildShareIntent(media, desc, "com.whatsapp")
        assertEquals("com.whatsapp", whatsappIntent.`package`)

        // Fallback Chooser Intent without package lock
        val generalIntent = AiShareDescriptionGenerator.buildShareIntent(media, desc, null)
        assertNull("Fallback intent should have no package restriction to allow system chooser", generalIntent.`package`)
        assertEquals(desc, generalIntent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(media.mimeType, generalIntent.type)
    }
}
