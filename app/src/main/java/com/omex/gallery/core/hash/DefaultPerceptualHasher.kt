package com.omex.gallery.core.hash

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Production implementation of [PerceptualHasher] providing aHash, dHash, and pHash (DCT-based).
 */
class DefaultPerceptualHasher : PerceptualHasher {

    override fun computeHash(bitmap: Bitmap, algorithm: HashAlgorithm): HashResult {
        val hashValue = when (algorithm) {
            HashAlgorithm.AVERAGE_HASH -> computeAverageHash(bitmap)
            HashAlgorithm.DIFFERENCE_HASH -> computeDifferenceHash(bitmap)
            HashAlgorithm.PERCEPTUAL_HASH -> computePhashDct(bitmap)
        }

        val binaryString = hashValue.toUnsignedString()
        return HashResult(
            hashValue = hashValue,
            binaryString = binaryString,
            algorithm = algorithm
        )
    }

    override fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    override fun isDuplicate(hash1: Long, hash2: Long, threshold: Int): Boolean {
        return hammingDistance(hash1, hash2) <= threshold
    }

    /**
     * Average Hash (aHash):
     * 1. Scale down to 8x8 grayscale.
     * 2. Compute mean pixel brightness value.
     * 3. Set bit 1 if pixel >= mean, else 0.
     */
    private fun computeAverageHash(bitmap: Bitmap): Long {
        val scaled = scaleToGrayscale(bitmap, 8, 8)
        val pixels = IntArray(64)
        scaled.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        if (scaled != bitmap && !scaled.isRecycled) scaled.recycle()

        var sum = 0L
        val grays = IntArray(64)
        for (i in 0 until 64) {
            val gray = pixels[i] and 0xFF
            grays[i] = gray
            sum += gray
        }

        val avg = sum / 64.0
        var hash = 0L
        for (i in 0 until 64) {
            if (grays[i] >= avg) {
                hash = hash or (1L shl (63 - i))
            }
        }
        return hash
    }

    /**
     * Difference Hash (dHash):
     * 1. Scale down to 9x8 grayscale.
     * 2. Compare adjacent horizontal pixel intensities (left > right -> 1, else 0).
     */
    private fun computeDifferenceHash(bitmap: Bitmap): Long {
        val scaled = scaleToGrayscale(bitmap, 9, 8)
        val pixels = IntArray(72)
        scaled.getPixels(pixels, 0, 9, 0, 0, 9, 8)
        if (scaled != bitmap && !scaled.isRecycled) scaled.recycle()

        var hash = 0L
        var bitIndex = 0

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val left = pixels[row * 9 + col] and 0xFF
                val right = pixels[row * 9 + col + 1] and 0xFF

                if (left > right) {
                    hash = hash or (1L shl (63 - bitIndex))
                }
                bitIndex++
            }
        }
        return hash
    }

    /**
     * Perceptual Hash (pHash) using Discrete Cosine Transform (DCT):
     * 1. Scale down to 32x32 grayscale.
     * 2. Compute 2D DCT matrix.
     * 3. Extract top-left 8x8 low-frequency matrix (excluding DC coefficient at 0,0).
     * 4. Compute median/mean of low frequencies and generate 64-bit fingerprint.
     */
    private fun computePhashDct(bitmap: Bitmap): Long {
        val size = 32
        val scaled = scaleToGrayscale(bitmap, size, size)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled != bitmap && !scaled.isRecycled) scaled.recycle()

        val matrix = Array(size) { DoubleArray(size) }
        for (r in 0 until size) {
            for (c in 0 until size) {
                matrix[r][c] = (pixels[r * size + c] and 0xFF).toDouble()
            }
        }

        val dctMatrix = applyDct2D(matrix, size)

        // Take top-left 8x8 low frequencies
        val lowFreq = DoubleArray(64)
        var sum = 0.0
        var count = 0

        for (u in 0 until 8) {
            for (v in 0 until 8) {
                if (u == 0 && v == 0) continue // Skip DC component
                val valDct = dctMatrix[u][v]
                lowFreq[u * 8 + v] = valDct
                sum += valDct
                count++
            }
        }

        val avg = sum / count
        var hash = 0L

        for (i in 0 until 64) {
            if (lowFreq[i] >= avg) {
                hash = hash or (1L shl (63 - i))
            }
        }

        return hash
    }

    private fun applyDct2D(matrix: Array<DoubleArray>, size: Int): Array<DoubleArray> {
        val dct = Array(size) { DoubleArray(size) }

        val c = DoubleArray(size) { if (it == 0) 1.0 / sqrt(2.0) else 1.0 }

        for (u in 0 until 8) {
            for (v in 0 until 8) {
                var sum = 0.0
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        sum += matrix[x][y] *
                                cos((2 * x + 1) * u * PI / (2 * size)) *
                                cos((2 * y + 1) * v * PI / (2 * size))
                    }
                }
                dct[u][v] = 0.25 * c[u] * c[v] * sum
            }
        }

        return dct
    }

    private fun scaleToGrayscale(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()

        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)

        val scaledSource = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        canvas.drawBitmap(scaledSource, 0f, 0f, paint)

        if (scaledSource != source && !scaledSource.isRecycled) {
            scaledSource.recycle()
        }

        return grayscaleBitmap
    }

    private fun Long.toUnsignedString(): String {
        return String.format("%64s", java.lang.Long.toBinaryString(this)).replace(' ', '0')
    }
}
