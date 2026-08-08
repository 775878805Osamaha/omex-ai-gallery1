package com.omex.gallery.core.indexer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Container holding exact and perceptual hashes calculated for a media file.
 */
data class MediaHashes(
    val sha256: String,
    val dHash: Long,
    val pHash: Long
)

/**
 * High-performance hash engine for SHA-256 byte checksums, dHash, and pHash (DCT-based).
 */
class HashGenerator(private val context: Context) {

    /**
     * Calculates SHA-256, dHash, and pHash for an image URI.
     */
    suspend fun generateHashes(contentUri: Uri): MediaHashes = withContext(Dispatchers.IO) {
        val sha256 = calculateSha256(contentUri)
        val bitmap = loadDownsampledBitmap(contentUri, targetSize = 64)

        if (bitmap == null) {
            return@withContext MediaHashes(sha256 = sha256, dHash = 0L, pHash = 0L)
        }

        try {
            val dHashVal = computeDHash(bitmap)
            val pHashVal = computePHash(bitmap)
            MediaHashes(sha256 = sha256, dHash = dHashVal, pHash = pHashVal)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Calculates SHA-256 hash of stream content.
     */
    fun calculateSha256(contentUri: Uri): String {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(contentUri) ?: return ""
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Computes dHash (Difference Hash) using a 9x8 grayscale downsample.
     */
    fun computeDHash(srcBitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(srcBitmap, 9, 8, true)
        var hash = 0L

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val leftPixel = scaled.getPixel(col, row)
                val rightPixel = scaled.getPixel(col + 1, row)

                val leftLuma = getLuminance(leftPixel)
                val rightLuma = getLuminance(rightPixel)

                if (leftLuma > rightLuma) {
                    val bitPos = row * 8 + col
                    hash = hash or (1L shl bitPos)
                }
            }
        }

        if (scaled != srcBitmap && !scaled.isRecycled) {
            scaled.recycle()
        }
        return hash
    }

    /**
     * Computes pHash (Perceptual Hash) using 32x32 grayscale DCT reduction.
     */
    fun computePHash(srcBitmap: Bitmap): Long {
        val size = 32
        val scaled = Bitmap.createScaledBitmap(srcBitmap, size, size, true)
        val matrix = Array(size) { DoubleArray(size) }

        for (y in 0 until size) {
            for (x in 0 until size) {
                matrix[y][x] = getLuminance(scaled.getPixel(x, y)).toDouble()
            }
        }

        if (scaled != srcBitmap && !scaled.isRecycled) {
            scaled.recycle()
        }

        // Apply 2D DCT
        val dct = applyDct2D(matrix, size)

        // Extract top-left 8x8 matrix (excluding DC term at [0][0])
        var sum = 0.0
        val lowFreq = DoubleArray(64)
        var count = 0

        for (u in 0 until 8) {
            for (v in 0 until 8) {
                if (u == 0 && v == 0) continue
                val valDct = dct[u][v]
                lowFreq[count++] = valDct
                sum += valDct
            }
        }

        val avg = sum / 63.0
        var hash = 0L

        for (i in 0 until 63) {
            if (lowFreq[i] > avg) {
                hash = hash or (1L shl i)
            }
        }

        return hash
    }

    /**
     * Computes Hamming Distance (number of differing bit positions) between two 64-bit hashes.
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    private fun getLuminance(colorInt: Int): Int {
        val r = Color.red(colorInt)
        val g = Color.green(colorInt)
        val b = Color.blue(colorInt)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private fun loadDownsampledBitmap(contentUri: Uri, targetSize: Int): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(contentUri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var sampleSize = 1
            while (options.outWidth / sampleSize > targetSize * 2 || options.outHeight / sampleSize > targetSize * 2) {
                sampleSize *= 2
            }

            inputStream = context.contentResolver.openInputStream(contentUri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } catch (e: Exception) {
            null
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun applyDct2D(f: Array<DoubleArray>, N: Int): Array<DoubleArray> {
        val F = Array(N) { DoubleArray(N) }
        val c1 = sqrt(1.0 / N)
        val c2 = sqrt(2.0 / N)

        for (u in 0 until N) {
            for (v in 0 until N) {
                var sum = 0.0
                for (i in 0 until N) {
                    for (j in 0 until N) {
                        sum += f[i][j] *
                                cos(((2 * i + 1) * u * PI) / (2.0 * N)) *
                                cos(((2 * j + 1) * v * PI) / (2.0 * N))
                    }
                }
                val alphaU = if (u == 0) c1 else c2
                val alphaV = if (v == 0) c1 else c2
                F[u][v] = alphaU * alphaV * sum
            }
        }
        return F
    }
}
