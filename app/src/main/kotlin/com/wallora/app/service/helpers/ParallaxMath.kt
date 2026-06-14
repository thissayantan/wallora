package com.wallora.app.service.helpers

/**
 * Pure helper for parallax translation calculations in the live wallpaper engine.
 *
 * The over-wide bitmap approach: bitmap is [PARALLAX_SCALE]× wider than the surface.
 * [CropCalculator.centerCropRect] centers the bitmap on the surface, leaving overflow/2
 * hidden on EACH side. [translateX] pans symmetrically so:
 *   xOffset=0 (leftmost page)  → +overflow/2 (left edge flush with surface)
 *   xOffset=0.5 (center)       →  0          (bitmap stays centred)
 *   xOffset=1 (rightmost page) → -overflow/2 (right edge flush with surface)
 *
 * This guarantees the surface is always fully covered at every offset — there is no gap
 * at either edge regardless of how many home screen pages the launcher shows.
 */
object ParallaxMath {

    /** Bitmap is rendered 1.3× wider than the surface to allow parallax travel. */
    const val PARALLAX_SCALE = 1.3f

    /**
     * Compute the horizontal translation in pixels to apply to the bitmap.
     *
     * Uses a symmetric formula that matches the centered dest rect produced by
     * [CropCalculator.centerCropRect]: the half-overflow on each side becomes exactly the
     * maximum pan range in each direction, so neither edge is ever exposed.
     *
     * @param xOffset       launcher scroll position [0..1]. 0 = leftmost page, 1 = rightmost.
     * @param bitmapWidth   width of the rendered bitmap in pixels.
     * @param surfaceWidth  width of the wallpaper surface in pixels.
     * @return              translation in pixels (negative = shift left, positive = shift right).
     */
    fun translateX(xOffset: Float, bitmapWidth: Int, surfaceWidth: Int): Float {
        val overflowPixels = (bitmapWidth - surfaceWidth).coerceAtLeast(0)
        if (overflowPixels == 0) return 0f
        // Symmetric pan around the centered bitmap: at 0.5 translate=0, at 0 translate=+half, at 1 translate=-half
        return (0.5f - xOffset) * overflowPixels
    }

    /**
     * Compute how wide an over-wide bitmap should be to allow smooth parallax.
     *
     * @param surfaceWidth   surface width in pixels.
     * @param pageCount      number of launcher home screen pages (unused; kept for API compat).
     * @return               desired bitmap width in pixels.
     */
    fun overwideBitmapWidth(surfaceWidth: Int, pageCount: Int = 1): Int {
        return (surfaceWidth * PARALLAX_SCALE).toInt()
    }

    /**
     * Fixed-offset fallback translation when no xOffset events are received (e.g.,
     * certain launchers always report xOffset=0).
     *
     * Centres the bitmap on the surface — equivalent to [translateX] at xOffset=0.5.
     */
    fun fixedOffsetTranslateX(bitmapWidth: Int, surfaceWidth: Int): Float {
        return 0f  // bitmap is already centred by centerCropRect; no additional translation needed
    }

    /**
     * Clamp [translateX] so the bitmap never exposes a gap at either edge.
     * Valid range is ±(overflow/2) to match the symmetric pan formula.
     */
    fun clampTranslateX(translateX: Float, bitmapWidth: Int, surfaceWidth: Int): Float {
        val halfOverflow = (bitmapWidth - surfaceWidth).toFloat().coerceAtLeast(0f) / 2f
        return translateX.coerceIn(-halfOverflow, halfOverflow)
    }
}
