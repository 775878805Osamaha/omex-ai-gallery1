package com.omex.gallery.core.indexer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Detailed EXIF and camera metadata attributes.
 */
data class ExifMetadata(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val orientation: Int = 0,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0
)

/**
 * Detailed video metadata attributes.
 */
data class VideoMetadata(
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val rotationDegrees: Int = 0,
    val frameRateFps: Float = 0f,
    val bitRateBps: Long = 0L,
    val mimeType: String = "video/mp4"
)

/**
 * Extracts rich metadata (EXIF camera details, GPS locations, video parameters) from URIs safely.
 */
class MetadataExtractor(private val context: Context) {

    /**
     * Extracts EXIF metadata for image content URIs.
     */
    suspend fun extractImageExif(contentUri: Uri): ExifMetadata = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(contentUri)
            if (inputStream == null) return@withContext ExifMetadata()

            val exif = ExifInterface(inputStream)

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { formatShutterSpeed(it) }
            val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" }
            val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
            val orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

            val latLongArray = exif.latLong
            val latitude = latLongArray?.getOrNull(0)
            val longitude = latLongArray?.getOrNull(1)

            ExifMetadata(
                cameraMake = make,
                cameraModel = model,
                aperture = aperture,
                iso = iso,
                exposureTime = exposure,
                focalLength = focal,
                flash = flash,
                latitude = latitude,
                longitude = longitude,
                orientation = orient,
                originalWidth = width,
                originalHeight = height
            )
        } catch (e: Exception) {
            ExifMetadata()
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Extracts video container metadata for video content URIs.
     */
    suspend fun extractVideoMetadata(contentUri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, contentUri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val width = widthStr?.toIntOrNull() ?: 0

            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val height = heightStr?.toIntOrNull() ?: 0

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotation = rotationStr?.toIntOrNull() ?: 0

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val bitrate = bitrateStr?.toLongOrNull() ?: 0L

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            VideoMetadata(
                durationMs = duration,
                width = width,
                height = height,
                rotationDegrees = rotation,
                bitRateBps = bitrate,
                mimeType = mime
            )
        } catch (e: Exception) {
            VideoMetadata()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun formatShutterSpeed(shutterStr: String): String {
        val speedVal = shutterStr.toDoubleOrNull() ?: return shutterStr
        return if (speedVal < 1.0 && speedVal > 0.0) {
            val reciprocal = (1.0 / speedVal).toInt()
            "1/$reciprocal s"
        } else {
            "${speedVal}s"
        }
    }
}
