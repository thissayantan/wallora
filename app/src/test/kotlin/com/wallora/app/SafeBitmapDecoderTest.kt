package com.wallora.app

import com.wallora.app.data.util.SafeBitmapDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeBitmapDecoderTest {

    @Test
    fun `computeSampleSize returns 1 when image fits target`() {
        // Image is 100x100, target is 200x200 → no downscale needed
        assertEquals(1, SafeBitmapDecoder.computeSampleSize(100, 100, 200, 200))
    }

    @Test
    fun `computeSampleSize returns 2 for 2x oversized image`() {
        // 2000x4000 image, target 1000x2000 → need to halve → sampleSize 2
        assertEquals(2, SafeBitmapDecoder.computeSampleSize(2000, 4000, 1000, 2000))
    }

    @Test
    fun `computeSampleSize returns power of 2`() {
        // 4320x7680 (8K portrait), target 1080x1920 → need 4x downscale
        val size = SafeBitmapDecoder.computeSampleSize(4320, 7680, 1080, 1920)
        assertEquals(4, size)
    }

    @Test
    fun `computeSampleSize returns 1 for exact match`() {
        assertEquals(1, SafeBitmapDecoder.computeSampleSize(1080, 1920, 1080, 1920))
    }

    @Test
    fun `computeSampleSize is always at least 1`() {
        val size = SafeBitmapDecoder.computeSampleSize(1, 1, 1000, 1000)
        assertTrue(size >= 1)
    }

    @Test
    fun `computeSampleSize handles extreme aspect ratio`() {
        // Very tall image, wide target → width constraint is not binding, height is
        val size = SafeBitmapDecoder.computeSampleSize(100, 10000, 100, 500)
        assertTrue(size >= 2)  // need to reduce height from 10000 to ≤500
    }

    // A3: GPU max-texture-size safety tests.
    // Root cause of the black preview on Pixel 6 Pro: full-res originals (e.g. Wallhaven
    // 5120×2880) exceed the GPU max texture size (typically 4096 or 8192px). A HARDWARE
    // Bitmap that large cannot be uploaded by Compose RecordingCanvas → renders black.
    // Fix: allowHardware(false) on the detail ImageRequest. The engine decode path uses
    // computeSampleSize which guarantees the pre-scaled bitmap is < 2× the target dimension,
    // well within GPU limits, before the final createScaledBitmap step.

    @Test
    fun `computeSampleSize applies meaningful downsampling for 5K wallhaven image`() {
        // Wallhaven full-res: 5120×2880, Pixel 6 Pro display: 1440×3120.
        // Width 5120 >> 1440, so sampleSize must be ≥ 2 (some downsampling happens).
        val size = SafeBitmapDecoder.computeSampleSize(5120, 2880, 1440, 3120)
        assertTrue("Expected sampleSize ≥ 2 for 5K→1440p, got $size", size >= 2)
    }

    @Test
    fun `computeSampleSize applies meaningful downsampling for 8K image`() {
        // 8K: 7680×4320, display 1080×1920 → both dimensions 4–7× larger → sampleSize ≥ 4
        val size = SafeBitmapDecoder.computeSampleSize(7680, 4320, 1080, 1920)
        assertTrue("Expected sampleSize ≥ 4 for 8K→1080p, got $size", size >= 4)
    }

    @Test
    fun `computeSampleSize keeps decoded dims within 2x target (pre-scale pass invariant)`() {
        // The algorithm guarantees: w/sampleSize < 2*maxW AND h/sampleSize < 2*maxH.
        // This is the termination condition (loop exits when next halving would go below max).
        // A subsequent createScaledBitmap step trims to exact target size.
        val srcW = 5120; val srcH = 2880
        val maxW = 1440; val maxH = 3120
        val sampleSize = SafeBitmapDecoder.computeSampleSize(srcW, srcH, maxW, maxH)
        val decodedW = srcW / sampleSize
        val decodedH = srcH / sampleSize
        assertTrue("decodedW=$decodedW must be < 2*$maxW=${2 * maxW}", decodedW < 2 * maxW)
        assertTrue("decodedH=$decodedH must be < 2*$maxH=${2 * maxH}", decodedH < 2 * maxH)
    }
}
