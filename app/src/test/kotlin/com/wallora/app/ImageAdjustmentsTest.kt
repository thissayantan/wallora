package com.wallora.app

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import com.wallora.app.data.util.ImageAdjustments
import com.wallora.app.domain.model.EditParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ImageAdjustments].
 * Tests ColorMatrix math, blur bounds, and apply pipeline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ImageAdjustmentsTest {

    // ── ColorMatrix math ──────────────────────────────────────────────────────

    @Test
    fun `buildColorMatrix identity produces no-op matrix`() {
        val matrix = ImageAdjustments.buildColorMatrix(
            brightness = 0f,
            contrast = 1f,
            saturation = 1f,
        )
        val values = FloatArray(20)
        matrix.getArray().copyInto(values)

        // Diagonal of the R/G/B block must equal contrast = 1.0
        assertEquals(1f, values[0], 0.01f)  // R scale
        assertEquals(1f, values[6], 0.01f)  // G scale
        assertEquals(1f, values[12], 0.01f) // B scale
        // Translation terms should be ~0
        assertEquals(0f, values[4], 1.0f)   // R translate (saturation may add small cross-terms)
        assertEquals(0f, values[9], 1.0f)
        assertEquals(0f, values[14], 1.0f)
    }

    @Test
    fun `buildColorMatrix high brightness shifts translation positive`() {
        val matrix = ImageAdjustments.buildColorMatrix(
            brightness = 0.5f, // +0.5 = +127 per channel
            contrast = 1f,
            saturation = 1f,
        )
        val values = matrix.getArray()
        // With saturation=1 (identity sat), brightness +0.5 adds ~127 to R/G/B translation
        assertTrue("R translate should be positive", values[4] > 100f)
        assertTrue("G translate should be positive", values[9] > 100f)
        assertTrue("B translate should be positive", values[14] > 100f)
    }

    @Test
    fun `buildColorMatrix contrast 2 doubles diagonal`() {
        val matrix = ImageAdjustments.buildColorMatrix(
            brightness = 0f,
            contrast = 2f,
            saturation = 1f,
        )
        val values = matrix.getArray()
        // With saturation=1 (near-identity), diagonal is roughly contrast
        assertTrue("R scale near 2", values[0] > 1.8f)
        assertTrue("G scale near 2", values[6] > 1.8f)
        assertTrue("B scale near 2", values[12] > 1.8f)
    }

    @Test
    fun `buildColorMatrix saturation 0 produces near-grayscale`() {
        val matrix = ImageAdjustments.buildColorMatrix(
            brightness = 0f,
            contrast = 1f,
            saturation = 0f,
        )
        val values = matrix.getArray()
        // In grayscale mode, R/G/B rows should sum to roughly 1 using luminance weights
        // Standard luminance: 0.213R + 0.715G + 0.072B ≈ 1
        val rSum = values[0] + values[1] + values[2]
        val gSum = values[5] + values[6] + values[7]
        val bSum = values[10] + values[11] + values[12]
        assertEquals(1f, rSum, 0.05f)
        assertEquals(1f, gSum, 0.05f)
        assertEquals(1f, bSum, 0.05f)
    }

    // ── Blur radius bounds ────────────────────────────────────────────────────

    @Test
    fun `applyStackBlur minimum radius 1 succeeds`() {
        val src = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        src.eraseColor(0xFF336699.toInt())
        val result = ImageAdjustments.applyStackBlur(src, radius = 1)
        assertNotNull(result)
        assertEquals(50, result.width)
        assertEquals(50, result.height)
    }

    @Test
    fun `applyStackBlur large radius does not OOM or crash`() {
        val src = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        src.eraseColor(0xFFAA4488.toInt())
        // max allowed radius is 50 — should complete without exception
        val result = ImageAdjustments.applyStackBlur(src, radius = 25)
        assertNotNull(result)
        assertEquals(200, result.width)
        assertEquals(200, result.height)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `applyStackBlur radius 0 throws`() {
        val src = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        ImageAdjustments.applyStackBlur(src, radius = 0)
    }

    // ── Full apply pipeline ───────────────────────────────────────────────────

    @Test
    fun `apply with identity EditParams returns same-size bitmap`() {
        val src = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888)
        src.eraseColor(0xFFFFFFFF.toInt())
        val params = EditParams() // default: blur=0, brightness=0, contrast=1, sat=1, pan=0
        val result = ImageAdjustments.apply(src, params)
        assertEquals(100, result.width)
        assertEquals(150, result.height)
    }

    @Test
    fun `apply with blur EditParams blurs image without resizing`() {
        val src = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        src.eraseColor(0xFF123456.toInt())
        val params = EditParams(blur = 5f)
        val result = ImageAdjustments.apply(src, params)
        assertEquals(80, result.width)
        assertEquals(80, result.height)
    }

    @Test
    fun `apply with pan offsets does not crash`() {
        val src = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        val params = EditParams(panX = 0.3f, panY = -0.2f)
        val result = ImageAdjustments.apply(src, params)
        assertEquals(60, result.width)
        assertEquals(60, result.height)
    }
}
