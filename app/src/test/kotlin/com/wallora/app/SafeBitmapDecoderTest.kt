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
}
