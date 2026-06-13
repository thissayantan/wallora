package com.wallora.app

import com.wallora.app.service.helpers.CrossfadeAnimator
import com.wallora.app.service.helpers.CropCalculator
import com.wallora.app.service.helpers.ParallaxMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])

/**
 * Unit tests for pure live-wallpaper helpers:
 * [ParallaxMath], [CrossfadeAnimator], [CropCalculator].
 */
class LiveWallpaperHelpersTest {

    // ── ParallaxMath ──────────────────────────────────────────────────────────

    @Test
    fun `parallax translateX at xOffset 0 shows left edge (no translation)`() {
        // At xOffset=0 (leftmost page), left edge of bitmap aligns with surface — no translation
        val result = ParallaxMath.translateX(xOffset = 0f, bitmapWidth = 1404, surfaceWidth = 1080)
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `parallax translateX at xOffset 1 shifts full overflow left`() {
        // At xOffset=1 (rightmost page), bitmap shifts left by full overflow to show right edge
        val bitmapW = 1404
        val surfaceW = 1080
        val overflow = bitmapW - surfaceW
        val result = ParallaxMath.translateX(xOffset = 1f, bitmapWidth = bitmapW, surfaceWidth = surfaceW)
        assertEquals(-overflow.toFloat(), result, 0.01f)
    }

    @Test
    fun `parallax translateX when bitmap equals surface width returns 0`() {
        val result = ParallaxMath.translateX(0.5f, 1080, 1080)
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `parallax fixedOffset centres bitmap`() {
        val bitmapW = 1404
        val surfaceW = 1080
        val expected = -((bitmapW - surfaceW) / 2f)
        assertEquals(expected, ParallaxMath.fixedOffsetTranslateX(bitmapW, surfaceW), 0.01f)
    }

    @Test
    fun `parallax clampTranslateX prevents positive translation`() {
        val clamped = ParallaxMath.clampTranslateX(100f, 1404, 1080)
        assertEquals(0f, clamped, 0.01f)
    }

    @Test
    fun `parallax clampTranslateX prevents overflow past left edge`() {
        val overflow = (1404 - 1080).toFloat()
        val clamped = ParallaxMath.clampTranslateX(-1000f, 1404, 1080)
        assertEquals(-overflow, clamped, 0.01f)
    }

    // ── CrossfadeAnimator ─────────────────────────────────────────────────────

    @Test
    fun `crossfade alpha starts at 0`() {
        val animator = CrossfadeAnimator(durationMs = 400L)
        val alpha = animator.currentAlpha(nowMs = 0L)
        assertEquals(0f, alpha, 0.001f)
    }

    @Test
    fun `crossfade alpha reaches 1 after duration`() {
        val animator = CrossfadeAnimator(durationMs = 400L)
        animator.currentAlpha(nowMs = 0L) // triggers start
        val alpha = animator.currentAlpha(nowMs = 400L)
        assertEquals(1f, alpha, 0.001f)
    }

    @Test
    fun `crossfade isComplete after full duration`() {
        val animator = CrossfadeAnimator(durationMs = 200L)
        animator.currentAlpha(nowMs = 0L)
        animator.currentAlpha(nowMs = 200L)
        assertTrue(animator.isComplete())
    }

    @Test
    fun `crossfade is not complete mid-animation`() {
        val animator = CrossfadeAnimator(durationMs = 400L)
        animator.currentAlpha(nowMs = 0L)
        animator.currentAlpha(nowMs = 100L)
        assertFalse(animator.isComplete())
    }

    @Test
    fun `crossfade reset clears state`() {
        val animator = CrossfadeAnimator(durationMs = 100L)
        animator.currentAlpha(nowMs = 0L)
        animator.currentAlpha(nowMs = 200L)
        assertTrue(animator.isComplete())
        animator.reset()
        assertFalse(animator.isComplete())
    }

    // ── CropCalculator ────────────────────────────────────────────────────────

    @Test
    fun `centerCropRect fills surface when aspect ratios match`() {
        val rect = CropCalculator.centerCropRect(1080, 1920, 1080, 1920)
        assertEquals(0, rect.left)
        assertEquals(0, rect.top)
        assertEquals(1080, rect.right)
        assertEquals(1920, rect.bottom)
    }

    @Test
    fun `centerCropRect handles wide bitmap on portrait surface`() {
        // Wide bitmap (16:9) on tall surface (9:16) — fit to HEIGHT so width > surface
        val rect = CropCalculator.centerCropRect(1920, 1080, 1080, 1920)
        // Fit to height: destH == surfaceHeight
        assertEquals(1920, rect.height())
        // Width must be wider than surface
        assertTrue(rect.width() >= 1080)
        // Should be centred horizontally (negative left)
        assertTrue(rect.left <= 0)
    }

    @Test
    fun `centerCropRect handles tall bitmap on wide surface`() {
        // Tall bitmap (9:20) on wide surface (16:9) — fit to WIDTH so height > surface
        val rect = CropCalculator.centerCropRect(1080, 2400, 1920, 1080)
        // Fit to width: destW == surfaceWidth
        assertEquals(1920, rect.width())
        // Height must be taller than surface
        assertTrue(rect.height() >= 1080)
    }

    @Test
    fun `desiredMinimumWidth is PARALLAX_SCALE times surface width`() {
        val expected = (1080 * ParallaxMath.PARALLAX_SCALE).toInt()
        assertEquals(expected, CropCalculator.desiredMinimumWidth(1080))
    }
}
