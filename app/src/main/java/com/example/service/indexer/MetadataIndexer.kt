package com.example.service.indexer

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import com.example.core.data.local.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for parsing EXIF camera attributes, exposure details, and geo-location coordinates from media files.
 */
class MetadataIndexer(private val context: Context) {

    suspend fun extractExif(item: MediaItemEntity): MediaItemEntity = withContext(Dispatchers.IO) {
        if (item.isVideo) {
            return@withContext item.copy(isIndexed = true)
        }

        try {
            val uri = Uri.parse(item.uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)

                val latLong = FloatArray(2)
                val hasGps = exif.getLatLong(latLong)

                val lat = if (hasGps) latLong[0].toDouble() else null
                val lng = if (hasGps) latLong[1].toDouble() else null

                return@withContext item.copy(
                    cameraMake = make,
                    cameraModel = model,
                    iso = iso?.let { "ISO $it" },
                    aperture = aperture?.let { "f/$it" },
                    exposureTime = exposure?.let { "${it}s" },
                    focalLength = focal?.let { "${it}mm" },
                    latitude = lat,
                    longitude = lng,
                    isIndexed = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        item.copy(isIndexed = true)
    }
}
