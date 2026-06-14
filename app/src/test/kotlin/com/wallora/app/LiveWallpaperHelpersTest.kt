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
    // Symmetric pan: bitmap destRect is centered by CropCalculator (overflow/2 on each side).
    // translateX(0) = +overflow/2 (pan right, left edge flush), 0.5 = 0, 1 = -overflow/2.

    @Test
    fun `parallax translateX at xOffset 0 shifts half overflow right (left edge flush)`() {
        val bitmapW = 1404
        val surfaceW = 1080
        val halfOverflow = (bitmapW - surfaceW) / 2f
        val result = ParallaxMath.translateX(xOffset = 0f, bitmapWidth = bitmapW, surfaceWidth = surfaceW)
        assertEquals(halfOverflow, result, 0.01f)
    }

    @Test
    fun `parallax translateX at xOffset 1 shifts half overflow left (right edge flush)`() {
        val bitmapW = 1404
        val surfaceW = 1080
        val halfOverflow = (bitmapW - surfaceW) / 2f
        val result = ParallaxMath.translateX(xOffset = 1f, bitmapWidth = bitmapW, surfaceWidth = surfaceW)
        assertEquals(-halfOverflow, result, 0.01f)
    }

    @Test
    fun `parallax translateX at xOffset 0_5 returns 0 (centred)`() {
        val result = ParallaxMath.translateX(0.5f, 1404, 1080)
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `parallax translateX when bitmap equals surface width returns 0`() {
        val result = ParallaxMath.translateX(0.5f, 1080, 1080)
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `parallax fixedOffset returns 0 (bitmap already centred by CropCalculator)`() {
        // fixedOffsetTranslateX is 0 because the bitmap is already centered in the destRect
        assertEquals(0f, ParallaxMath.fixedOffsetTranslateX(1404, 1080), 0.01f)
    }

    @Test
    fun `parallax clampTranslateX clamps to positive half overflow`() {
        val bitmapW = 1404
        val surfaceW = 1080
        val halfOverflow = (bitmapW - surfaceW) / 2f
        val clamped = ParallaxMath.clampTranslateX(1000f, bitmapW, surfaceW)
        assertEquals(halfOverflow, clamped, 0.01f)
    }

    @Test
    fun `parallax clampTranslateX clamps to negative half overflow`() {
        val bitmapW = 1404
        val surfaceW = 1080
        val halfOverflow = (bitmapW - surfaceW) / 2f
        val clamped = ParallaxMath.clampTranslateX(-1000f, bitmapW, surfaceW)
        assertEquals(-halfOverflow, clamped, 0.01f)
    }

    @Test
    fun `parallax surface is always fully covered at every xOffset`() {
        // Realistic dimensions: surface is 1080×2400 (portrait phone), bitmap is 1.3W × H
        // (decoded by SafeBitmapDecoder to targetWidth=1404, targetHeight=2400).
        // CropCalculator.centerCropRect sees bmpAspect (1404/2400) > surfAspect (1080/2400),
        // so it fits to height → destRect spans -overflow/2 .. surfaceW + overflow/2.
        // Symmetric pan (translate = (0.5-xOffset) * overflow) guarantees full coverage.
        val surfaceW = 1080
        val surfaceH = 2400
        val bitmapW = (surfaceW * ParallaxMath.PARALLAX_SCALE).toInt() // 1404
        val bitmapH = surfaceH                                           // 2400
        val destRect = CropCalculator.centerCropRect(bitmapW, bitmapH, surfaceW, surfaceH)
        for (step in 0..10) {
            val xOffset = step / 10f
            val translate = ParallaxMath.clampTranslateX(
                ParallaxMath.translateX(xOffset, bitmapW, surfaceW),
                bitmapW, surfaceW
            )
            val effectiveLeft = destRect.left + translate.toInt()
            val effectiveRight = destRect.right + translate.toInt()
            assertTrue(
                "Left edge exposed at xOffset=$xOffset: effectiveLeft=$effectiveLeft",
                effectiveLeft <= 0
            )
            assertTrue(
                "Right edge exposed at xOffset=$xOffset: effectiveRight=$effectiveRight",
                effectiveRight >= surfaceW
            )
        }
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
