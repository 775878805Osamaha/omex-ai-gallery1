package com.omex.gallery.core.ai.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance production implementation of [ImagePreprocessor] for TFLite models.
 * Manages bitmap scaling, color space order (RGB/BGR), cropping, and tensor float normalization.
 */
class DefaultImagePreprocessor : ImagePreprocessor {

    companion object {
        private const val FLOAT_SIZE_BYTES = 4
        private val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    override fun preprocess(bitmap: Bitmap, options: PreprocessOptions): ByteBuffer {
        val scaledBitmap = prepareScaledBitmap(bitmap, options.targetWidth, options.targetHeight, options.keepAspectRatio)
        val buffer = convertBitmapToBuffer(scaledBitmap, options)
        if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }
        return buffer
    }

    override fun preprocessCrop(
        bitmap: Bitmap,
        cropLeft: Int,
        cropTop: Int,
        cropRight: Int,
        cropBottom: Int,
        options: PreprocessOptions
    ): ByteBuffer {
        val safeLeft = cropLeft.coerceIn(0, bitmap.width - 1)
        val safeTop = cropTop.coerceIn(0, bitmap.height - 1)
        val safeWidth = (cropRight - safeLeft).coerceIn(1, bitmap.width - safeLeft)
        val safeHeight = (cropBottom - safeTop).coerceIn(1, bitmap.height - safeTop)

        val croppedBitmap = Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
        val scaledBitmap = prepareScaledBitmap(croppedBitmap, options.targetWidth, options.targetHeight, options.keepAspectRatio)
        
        if (croppedBitmap != bitmap && croppedBitmap != scaledBitmap && !croppedBitmap.isRecycled) {
            croppedBitmap.recycle()
        }

        val buffer = convertBitmapToBuffer(scaledBitmap, options)
        if (scaledBitmap != bitmap && scaledBitmap != croppedBitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        return buffer
    }

    private fun prepareScaledBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        keepAspectRatio: Boolean
    ): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) {
            return source
        }

        if (!keepAspectRatio) {
            return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        }

        val targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetBitmap)

        val srcRect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val dstRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())

        val matrix = Matrix()
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER)

        canvas.drawBitmap(source, matrix, null)
        return targetBitmap
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap, options: PreprocessOptions): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val bufferSize = width * height * 3 * FLOAT_SIZE_BYTES
        
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var pixelIndex = 0
        for (i in 0 until height) {
            for (j in 0 until width) {
                val pixel = pixels[pixelIndex++]
                
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                val c0 = if (options.isBgrOrder) b else r
                val c1 = g
                val c2 = if (options.isBgrOrder) r else b

                when (options.normalization) {
                    NormalizationType.ZERO_TO_ONE -> {
                        byteBuffer.putFloat(c0)
                        byteBuffer.putFloat(c1)
                        byteBuffer.putFloat(c2)
                    }
                    NormalizationType.MINUS_ONE_TO_ONE -> {
                        byteBuffer.putFloat((c0 * 2.0f) - 1.0f)
                        byteBuffer.putFloat((c1 * 2.0f) - 1.0f)
                        byteBuffer.putFloat((c2 * 2.0f) - 1.0f)
                    }
                    NormalizationType.IMAGENET_MEAN_STD -> {
                        val m0 = IMAGENET_MEAN[0]
                        val m1 = IMAGENET_MEAN[1]
                        val m2 = IMAGENET_MEAN[2]
                        val s0 = IMAGENET_STD[0]
                        val s1 = IMAGENET_STD[1]
                        val s2 = IMAGENET_STD[2]

                        byteBuffer.putFloat((c0 - m0) / s0)
                        byteBuffer.putFloat((c1 - m1) / s1)
                        byteBuffer.putFloat((c2 - m2) / s2)
                    }
                    NormalizationType.NONE -> {
                        byteBuffer.putFloat(c0 * 255.0f)
                        byteBuffer.putFloat(c1 * 255.0f)
                        byteBuffer.putFloat(c2 * 255.0f)
                    }
                }
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }
}
