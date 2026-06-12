package com.wallora.app.data.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.wallora.app.domain.model.EditParams
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure image adjustment pipeline used by the pre-apply editor and live wallpaper render.
 *
 * Applies brightness / contrast / saturation via [ColorMatrix] and blur via a simple
 * downscale → box-blur → upscale approach (no RenderScript, no deprecated APIs).
 */
object ImageAdjustments {

    /**
     * Apply [EditParams] to [source] and return a new [Bitmap] (caller must recycle [source]
     * if it's no longer needed). The returned bitmap is always [ARGB_8888].
     *
     * [previewScale]: if < 1.0, the source is assumed to already be a downscaled preview
     * and blur radius is proportionally reduced.
     */
    fun apply(source: Bitmap, params: EditParams, previewScale: Float = 1.0f): Bitmap {
        var result = source

        // 1. Apply pan/crop offset — translate the canvas origin
        result = applyPan(result, params.panX, params.panY)

        // 2. Apply brightness / contrast / saturation via ColorMatrix
        if (params.brightness != 0f || params.contrast != 1f || params.saturation != 1f) {
            result = applyColorMatrix(result, params.brightness, params.contrast, params.saturation)
        }

        // 3. Apply blur — downscale→box-blur→upscale
        if (params.blur > 0f) {
            val adjustedRadius = (params.blur * previewScale).coerceIn(0f, 25f)
            if (adjustedRadius > 0f) {
                result = applyStackBlur(result, adjustedRadius.roundToInt().coerceAtLeast(1))
            }
        }

        return result
    }

    // ── Pan / crop ────────────────────────────────────────────────────────────

    /**
     * Translate the visible window by ([panX], [panY]) normalized coordinates.
     * Returns the source bitmap with the offset drawn onto a same-size canvas.
     * Pan values are clamped so the bitmap edge never passes the window edge.
     */
    fun applyPan(source: Bitmap, panX: Float, panY: Float): Bitmap {
        val w = source.width
        val h = source.height
        if (panX == 0f && panY == 0f) return source

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val dx = (panX * w * 0.1f).coerceIn(-w * 0.5f, w * 0.5f)
        val dy = (panY * h * 0.1f).coerceIn(-h * 0.5f, h * 0.5f)
        canvas.drawBitmap(source, dx, dy, null)
        return result
    }

    // ── ColorMatrix: brightness / contrast / saturation ──────────────────────

    /**
     * Build a [ColorMatrix] that applies brightness, contrast, and saturation independently.
     *
     * @param brightness -1.0 (black) to +1.0 (white). 0 = unchanged.
     * @param contrast 0.0 (flat gray) to 2.0 (max). 1.0 = unchanged.
     * @param saturation 0.0 (grayscale) to 2.0 (vivid). 1.0 = unchanged.
     */
    fun buildColorMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
        // Saturation matrix
        val sat = ColorMatrix()
        sat.setSaturation(saturation)

        // Scale (contrast) + translate (brightness):
        // newValue = contrast * oldValue + brightness * 255
        val bc = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness * 255f,
            0f, contrast, 0f, 0f, brightness * 255f,
            0f, 0f, contrast, 0f, brightness * 255f,
            0f, 0f, 0f, 1f, 0f,
        ))

        // Combine: first apply saturation, then contrast/brightness
        val combined = ColorMatrix()
        combined.setConcat(bc, sat)
        return combined
    }

    fun applyColorMatrix(source: Bitmap, brightness: Float, contrast: Float, saturation: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(buildColorMatrix(brightness, contrast, saturation))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    // ── Blur: downscale → box blur → upscale ─────────────────────────────────

    /**
     * Stack blur via downscale→render→upscale. Fast and OOM-safe.
     *
     * @param radius blur radius in pixels (1–25). Larger values downscale more.
     */
    fun applyStackBlur(source: Bitmap, radius: Int): Bitmap {
        require(radius in 1..50) { "radius must be in 1..50, got $radius" }

        val downscaleFactor = max(1, radius / 5) + 1
        val smallW = max(1, source.width / downscaleFactor)
        val smallH = max(1, source.height / downscaleFactor)

        // Downscale
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)

        // Apply box blur (simple 3-pass approximation of Gaussian)
        val blurred = boxBlur(small, radius / downscaleFactor + 1)
        small.recycle()

        // Upscale back to original size
        val result = Bitmap.createScaledBitmap(blurred, source.width, source.height, true)
        blurred.recycle()
        return result
    }

    /** Simple single-pass horizontal + vertical box blur. */
    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source
        val r = radius.coerceIn(1, 50)
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        horizontalBlur(pixels, w, h, r)
        verticalBlur(pixels, w, h, r)
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun horizontalBlur(pixels: IntArray, w: Int, h: Int, radius: Int) {
        for (y in 0 until h) {
            var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
            for (x in -radius..radius) {
                val xc = x.coerceIn(0, w - 1)
                val px = pixels[y * w + xc]
                rSum += (px shr 16 and 0xFF); gSum += (px shr 8 and 0xFF); bSum += (px and 0xFF); count++
            }
            for (x in 0 until w) {
                pixels[y * w + x] = (0xFF000000.toInt()) or
                    ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
                val remove = pixels[y * w + (x - radius).coerceIn(0, w - 1)]
                rSum -= (remove shr 16 and 0xFF); gSum -= (remove shr 8 and 0xFF); bSum -= (remove and 0xFF); count--
                val add = pixels[y * w + (x + radius + 1).coerceIn(0, w - 1)]
                rSum += (add shr 16 and 0xFF); gSum += (add shr 8 and 0xFF); bSum += (add and 0xFF); count++
            }
        }
    }

    private fun verticalBlur(pixels: IntArray, w: Int, h: Int, radius: Int) {
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
            for (y in -radius..radius) {
                val yc = y.coerceIn(0, h - 1)
                val px = pixels[yc * w + x]
                rSum += (px shr 16 and 0xFF); gSum += (px shr 8 and 0xFF); bSum += (px and 0xFF); count++
            }
            for (y in 0 until h) {
                pixels[y * w + x] = (0xFF000000.toInt()) or
                    ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
                val remove = pixels[(y - radius).coerceIn(0, h - 1) * w + x]
                rSum -= (remove shr 16 and 0xFF); gSum -= (remove shr 8 and 0xFF); bSum -= (remove and 0xFF); count--
                val add = pixels[(y + radius + 1).coerceIn(0, h - 1) * w + x]
                rSum += (add shr 16 and 0xFF); gSum += (add shr 8 and 0xFF); bSum += (add and 0xFF); count++
            }
        }
    }
}
