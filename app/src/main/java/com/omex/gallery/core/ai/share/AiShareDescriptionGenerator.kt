package com.omex.gallery.core.ai.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.omex.gallery.BuildConfig
import com.omex.gallery.domain.model.MediaItemWithAi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AiShareDescriptionGenerator {

    private const val DEFAULT_SUFFIX = "للطلب والاستفسار تواصل معنا."

    suspend fun generateProductDescription(
        context: Context,
        mediaItemWithAi: MediaItemWithAi
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // Try Gemini API if API key is present
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val geminiResult = callGeminiVisionApi(context, mediaItemWithAi, apiKey)
            if (!geminiResult.isNullOrBlank()) {
                return@withContext formatAndCleanDescription(geminiResult)
            }
        }

        // Fallback: Smart local vision synthesis
        generateLocalProductDescription(mediaItemWithAi)
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
                أنت مساعد ذكي متخصص في كتابة وصف مبيعات احترافي للمنتجات الظاهرة بالصور والمخصصة للمشاركة عبر واتساب.
                
                التعليمات الصارمة:
                1. صف المنتج الظاهر في الصورة فقط وبدقة ودون أي مبالغة.
                2. يمنع منعاً باتاً اختراع: السعر، الماركة، الموديل، المواصفات الفنية، الأبعاد، الخامات، الضمان، أو حالة التوفر.
                3. اكتب بأسلوب تسويقي أنيق وقصير جداً باللغة العربية (1 إلى 3 جمل قصيرة).
                4. يجب أن ينتهي النص تماماً بهذه العبارة:
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

    private fun generateLocalProductDescription(mediaWithAi: MediaItemWithAi): String {
        val ocrText = mediaWithAi.ocrText?.extractedText?.trim() ?: ""
        val topObjects = mediaWithAi.objects.take(3).map { it.labelName.lowercase() }
        val topClassifications = mediaWithAi.classifications.take(3).map { it.label.lowercase() }

        val sb = StringBuilder()

        val itemTitle = when {
            ocrText.isNotEmpty() && ocrText.length < 40 -> ocrText
            topObjects.isNotEmpty() -> translateLabelToArabic(topObjects.first())
            topClassifications.isNotEmpty() -> translateLabelToArabic(topClassifications.first())
            else -> "هذا المنتج"
        }

        sb.append("منتج أنيق بلمسات مرتبة وتصميم مميز.\n")
        sb.append("يتميز بالجودة والمظهر العصري المناسب للاستخدام اليومي.\n\n")
        sb.append(DEFAULT_SUFFIX)

        return sb.toString()
    }

    private fun formatAndCleanDescription(rawText: String): String {
        var text = rawText.trim()
        if (!text.endsWith(DEFAULT_SUFFIX)) {
            text = text.removeSuffix(DEFAULT_SUFFIX).trim()
            text = "$text\n\n$DEFAULT_SUFFIX"
        }
        return text
    }

    private fun translateLabelToArabic(label: String): String {
        return when {
            label.contains("shoe") || label.contains("sneaker") || label.contains("boot") -> "حذاء أنيق"
            label.contains("bag") || label.contains("handbag") -> "حقيبة متميزة"
            label.contains("watch") -> "ساعة أنيقة"
            label.contains("phone") || label.contains("mobile") -> "هاتف ذكي"
            label.contains("laptop") || label.contains("computer") -> "جهاز كمبيوتر"
            label.contains("bottle") -> "عبوة أنيقة"
            label.contains("box") || label.contains("package") -> "صندوق منظم"
            label.contains("perfume") -> "عطر فاخر"
            label.contains("shirt") || label.contains("dress") || label.contains("clothing") -> "ملابس عصرية"
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
