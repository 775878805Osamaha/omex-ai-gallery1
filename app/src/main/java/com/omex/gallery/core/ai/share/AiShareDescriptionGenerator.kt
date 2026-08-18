package com.omex.gallery.core.ai.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.omex.gallery.BuildConfig
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.MediaItemWithAi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object AiShareDescriptionGenerator {

    const val DEFAULT_SUFFIX = "للطلب والاستفسار التواصل معنا."
    const val WHATSAPP_PACKAGE = "com.whatsapp"

    /**
     * Generates a customer-ready product description in Arabic.
     * Uses Gemini Vision API if an API key is configured, otherwise falls back
     * to verified on-device ML metadata synthesis without fabricating information.
     */
    suspend fun generateProductDescription(
        context: Context?,
        mediaItemWithAi: MediaItemWithAi
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // Try Gemini API if API key is present and context is available
        if (context != null && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val geminiResult = callGeminiVisionApi(context, mediaItemWithAi, apiKey)
            if (!geminiResult.isNullOrBlank()) {
                return@withContext formatAndCleanDescription(geminiResult)
            }
        }

        // Fallback: Smart on-device metadata synthesis
        generateLocalProductDescription(mediaItemWithAi)
    }

    /**
     * Local synthesis strictly based on detected objects, OCR text, and classifications.
     * Guaranteed never to fabricate price, warranty, dimensions, material, or technical specs.
     */
    fun generateLocalProductDescription(mediaWithAi: MediaItemWithAi): String {
        val ocrText = mediaWithAi.ocrText?.extractedText?.trim() ?: ""
        val topObjects = mediaWithAi.objects.map { it.labelName.lowercase(Locale.ROOT) }
        val topClassifications = mediaWithAi.classifications.map { it.label.lowercase(Locale.ROOT) }

        // Determine title / main subject from confirmed data
        val itemTitle = when {
            ocrText.isNotEmpty() && ocrText.length in 3..45 && !ocrText.contains("\n") -> ocrText
            topObjects.isNotEmpty() -> translateLabelToArabic(topObjects.first())
            topClassifications.isNotEmpty() -> translateLabelToArabic(topClassifications.first())
            mediaWithAi.mediaItem.fileName.isNotBlank() &&
                    !mediaWithAi.mediaItem.fileName.startsWith("IMG_") &&
                    !mediaWithAi.mediaItem.fileName.startsWith("Screenshot_") &&
                    !mediaWithAi.mediaItem.fileName.startsWith("VID_") -> {
                mediaWithAi.mediaItem.fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            }
            else -> "منتج مميز"
        }

        // Detect specific content types (trading, document, generic product)
        val isTrading = topClassifications.any { it.contains("trading") || it.contains("chart") || it.contains("candlestick") } ||
                ocrText.contains("TradingView", ignoreCase = true) ||
                ocrText.contains("USDT", ignoreCase = true) ||
                ocrText.contains("BTC", ignoreCase = true) ||
                ocrText.contains("تداول", ignoreCase = true)

        val isDoc = topClassifications.any { it.contains("document") || it.contains("receipt") || it.contains("invoice") } ||
                ocrText.contains("invoice", ignoreCase = true) ||
                ocrText.contains("فاتورة", ignoreCase = true) ||
                ocrText.contains("إيصال", ignoreCase = true)

        val sb = StringBuilder()
        sb.append(itemTitle).append("\n\n")

        if (isTrading) {
            sb.append("مخطط تداول ورسم بياني تحليلي.")
        } else if (isDoc) {
            sb.append("مستند وبيانات توثيقية.")
        } else {
            // Short factual description based on identified tags
            val tagDescriptions = mutableListOf<String>()
            for (obj in topObjects.take(3)) {
                val arabicTag = translateLabelToArabic(obj)
                if (arabicTag != itemTitle && !tagDescriptions.contains(arabicTag)) {
                    tagDescriptions.add(arabicTag)
                }
            }
            if (tagDescriptions.isNotEmpty()) {
                sb.append("يتضمن: ").append(tagDescriptions.joinToString("، ")).append(".\n")
                sb.append("تصميم أنيق ومناسب للاستخدام اليومي.")
            } else {
                sb.append("منتج عالي الجودة بتصميم أنيق ومناسب.")
            }
        }

        sb.append("\n\n").append(DEFAULT_SUFFIX)
        return sb.toString().trim()
    }

    /**
     * Builds an Android Intent for sharing media (image or video) with an optional text caption.
     * Grants read URI permission and respects the target package when specified.
     */
    fun buildShareIntent(
        mediaItem: MediaItem,
        descriptionText: String? = null,
        targetPackage: String? = null
    ): Intent {
        val mime = if (mediaItem.mimeType.isNotBlank()) mediaItem.mimeType else {
            if (mediaItem.isVideo) "video/mp4" else "image/jpeg"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaItem.uriString))
            if (!descriptionText.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, descriptionText)
            }
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun callGeminiVisionApi(
        context: Context,
        mediaWithAi: MediaItemWithAi,
        apiKey: String
    ): String? {
        return try {
            val uri = Uri.parse(mediaWithAi.mediaItem.uriString)
            val bitmap = loadScaledBitmap(context, uri, maxDimension = 512) ?: return null

            val base64Image = bitmapToBase64(bitmap)
            if (base64Image.isEmpty()) return null

            val systemPrompt = """
                أنت مساعد ذكي متخصص في كتابة وصف مبيعات احترافي للمنتجات الظاهرة بالصور والمخصصة للمشاركة للعملاء عبر واتساب.
                
                التعليمات الصارمة:
                1. صف المنتج الظاهر في الصورة فقط وبدقة ودون أي مبالغة.
                2. يمنع منعاً باتاً اختراع: السعر، الماركة، الموديل، المواصفات الفنية، الأبعاد، الخامات، الضمان، أو بلد المنشأ.
                3. اكتب بأسلوب تسويقي أنيق وقصير جداً باللغة العربية (1 إلى 3 جمل قصيرة).
                4. يجب أن ينتهي النص تماماً بهذه العبارة في سطر منفصل:
                $DEFAULT_SUFFIX
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemPrompt)
                            })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

            conn.outputStream.use { os ->
                os.write(jsonPayload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "")
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAndCleanDescription(rawText: String): String {
        var text = rawText.trim()
        if (!text.endsWith(DEFAULT_SUFFIX)) {
            text = text.removeSuffix(DEFAULT_SUFFIX).trim()
            text = "$text\n\n$DEFAULT_SUFFIX"
        }
        return text
    }

    fun translateLabelToArabic(label: String): String {
        val l = label.lowercase(Locale.ROOT)
        return when {
            l.contains("shoe") || l.contains("sneaker") || l.contains("boot") -> "حذاء أنيق"
            l.contains("bag") || l.contains("handbag") || l.contains("backpack") -> "حقيبة متميزة"
            l.contains("watch") || l.contains("smartwatch") -> "ساعة أنيقة"
            l.contains("phone") || l.contains("mobile") || l.contains("cell") -> "هاتف ذكي"
            l.contains("laptop") || l.contains("computer") -> "جهاز كمبيوتر"
            l.contains("bottle") -> "عبوة أنيقة"
            l.contains("box") || l.contains("package") -> "علبة مميزة"
            l.contains("perfume") || l.contains("cosmetic") -> "عطر ومستحضرات"
            l.contains("shirt") || l.contains("dress") || l.contains("clothing") || l.contains("t-shirt") -> "ملابس عصرية"
            l.contains("car") || l.contains("vehicle") || l.contains("automobile") -> "سيارة"
            l.contains("food") || l.contains("meal") || l.contains("dish") -> "وجبة طعام"
            l.contains("pizza") -> "بيتزا"
            l.contains("coffee") || l.contains("tea") -> "مشروب ساخن"
            l.contains("glasses") || l.contains("sunglasses") -> "نظارة"
            l.contains("ring") || l.contains("necklace") || l.contains("jewelry") -> "مجوهرات وإكسسوارات"
            l.contains("candlestick") || l.contains("chart") -> "رسم بياني"
            l.contains("document") || l.contains("receipt") -> "مستند"
            else -> "منتج مميز"
        }
    }

    private fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream.close()

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val secondStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(secondStream, null, decodeOptions).also {
                secondStream?.close()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
