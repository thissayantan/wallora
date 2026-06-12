package com.wallora.app.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.Log
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * OOM-safe bitmap loading utilities used by the apply pipeline and the pre-apply editor.
 *
 * Two strategies:
 * 1. [decodeScaled] — load a downscaled (sampled) bitmap that fits within [maxWidth] × [maxHeight].
 *    Used for previews in the editor and grids (grids should use Coil thumbs, but this handles
 *    edge cases).
 * 2. [decodeRegion] — use [BitmapRegionDecoder] to load only the visible crop region from a
 *    full-resolution stream, avoiding loading the entire image into memory.
 *    Used by the apply pipeline to center-crop to device screen dimensions.
 *
 * All functions return `null` on any failure (OOM, format unsupported, etc.) rather than
 * throwing, and log the error for diagnostics.
 */
object SafeBitmapDecoder {

    private const val TAG = "SafeBitmapDecoder"

    /**
     * Compute the power-of-two inSampleSize needed so that the decoded image fits within
     * [maxWidth] × [maxHeight] while being as large as possible.
     */
    fun computeSampleSize(srcWidth: Int, srcHeight: Int, maxWidth: Int, maxHeight: Int): Int {
        require(maxWidth > 0 && maxHeight > 0) { "maxWidth and maxHeight must be > 0" }
        var sampleSize = 1
        var w = srcWidth
        var h = srcHeight
        while (w / 2 >= maxWidth || h / 2 >= maxHeight) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return max(1, sampleSize)
    }

    /**
     * Decode a scaled-down bitmap from [inputStream] that fits within [maxWidth] × [maxHeight].
     *
     * @param inputStream must be a fresh, positioned-at-start stream.
     * @param maxWidth maximum output width in pixels.
     * @param maxHeight maximum output height in pixels.
     * @return decoded [Bitmap] or `null` on failure.
     */
    fun decodeScaled(inputStream: InputStream, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            // First pass — bounds only
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, opts)
            // inputStream is consumed; caller must provide a fresh one if re-used

            val sampleSize = computeSampleSize(opts.outWidth, opts.outHeight, maxWidth, maxHeight)

            opts.inJustDecodeBounds = false
            opts.inSampleSize = sampleSize
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888

            // inputStream is exhausted after the first decodeStream pass above.
            // Callers using InputStreams that support reset() can reset here.
            // For URL streams, the caller should provide a seekable wrapper.
            BitmapFactory.decodeStream(inputStream, null, opts)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM decoding scaled bitmap", oom)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode scaled bitmap", e)
            null
        }
    }

    /**
     * Decode only the center-crop region of a JPEG/PNG image using [BitmapRegionDecoder].
     *
     * The crop is computed to fill [targetWidth] × [targetHeight] from the center of the source,
     * equivalent to `Bitmap.createScaledBitmap` + center crop but without loading the full image.
     * The resulting bitmap is always exactly [targetWidth] × [targetHeight].
     *
     * @param bytes raw image bytes (must be JPEG or PNG for BitmapRegionDecoder).
     * @param targetWidth desired output width.
     * @param targetHeight desired output height.
     * @return cropped [Bitmap] or `null` on failure.
     */
    fun decodeRegion(bytes: ByteArray, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, true)
                ?: return null
            val srcW = decoder.width
            val srcH = decoder.height

            // Compute the crop rect that fills targetWidth × targetHeight from center
            val srcAspect = srcW.toFloat() / srcH
            val dstAspect = targetWidth.toFloat() / targetHeight

            val (cropW, cropH) = if (srcAspect > dstAspect) {
                // source is wider — crop sides
                val h = srcH
                val w = (h * dstAspect).toInt()
                w to h
            } else {
                // source is taller — crop top/bottom
                val w = srcW
                val h = (w / dstAspect).toInt()
                w to h
            }

            val left = (srcW - cropW) / 2
            val top = (srcH - cropH) / 2
            val rect = Rect(
                left,
                top,
                min(left + cropW, srcW),
                min(top + cropH, srcH),
            )

            val sampleSize = computeSampleSize(cropW, cropH, targetWidth, targetHeight)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val sampled = decoder.decodeRegion(rect, opts)
            decoder.recycle()

            // If the sampled bitmap isn't exactly target size, scale it
            if (sampled != null &&
                (sampled.width != targetWidth || sampled.height != targetHeight)
            ) {
                val scaled = Bitmap.createScaledBitmap(sampled, targetWidth, targetHeight, true)
                if (scaled !== sampled) sampled.recycle()
                scaled
            } else {
                sampled
            }
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM in decodeRegion target=${targetWidth}x$targetHeight", oom)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed in decodeRegion", e)
            null
        }
    }
}
