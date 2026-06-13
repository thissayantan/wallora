package com.wallora.app.service.helpers

/**
 * Pure helper for parallax translation calculations in the live wallpaper engine.
 *
 * The over-wide bitmap approach: bitmap is [PARALLAX_SCALE]× wider than the surface.
 * As the launcher scrolls (xOffset 0→1), the bitmap translates from its left edge to
 * its right edge, creating a parallax effect.
 */
object ParallaxMath {

    /** Bitmap is rendered 1.3× wider than the surface to allow parallax travel. */
    const val PARALLAX_SCALE = 1.3f

    /**
     * Compute the horizontal translation in pixels to apply to the bitmap.
     *
     * @param xOffset       launcher scroll position [0..1]. 0 = leftmost page, 1 = rightmost.
     * @param bitmapWidth   width of the rendered bitmap in pixels.
     * @param surfaceWidth  width of the wallpaper surface in pixels.
     * @return              translation in pixels (negative = shift left, positive = shift right).
     */
    fun translateX(xOffset: Float, bitmapWidth: Int, surfaceWidth: Int): Float {
        val overflowPixels = (bitmapWidth - surfaceWidth).coerceAtLeast(0)
        if (overflowPixels == 0) return 0f
        // At xOffset=0: translate -overflow (left edge visible)
        // At xOffset=1: translate 0 (right edge visible)
        // Invert so content moves left as pages scroll right
        return -(xOffset * overflowPixels)
    }

    /**
     * Compute how wide an over-wide bitmap should be to allow smooth parallax.
     *
     * @param surfaceWidth   surface width in pixels.
     * @param pageCount      number of launcher home screen pages (used to compute step size).
     *                       If 0, defaults to 1 (fixed-offset fallback).
     * @return               desired bitmap width in pixels.
     */
    fun overwideBitmapWidth(surfaceWidth: Int, pageCount: Int = 1): Int {
        val pages = pageCount.coerceAtLeast(1)
        return (surfaceWidth * PARALLAX_SCALE).toInt()
    }

    /**
     * Fixed-offset fallback translation when no xOffset events are received (e.g.,
     * certain launchers always report xOffset=0).
     *
     * Centres the bitmap on the surface.
     */
    fun fixedOffsetTranslateX(bitmapWidth: Int, surfaceWidth: Int): Float {
        val overflow = (bitmapWidth - surfaceWidth).coerceAtLeast(0)
        return -(overflow / 2f)
    }

    /**
     * Clamp [translateX] so the bitmap never shows a gap at either side.
     */
    fun clampTranslateX(translateX: Float, bitmapWidth: Int, surfaceWidth: Int): Float {
        val minTranslate = -(bitmapWidth - surfaceWidth).toFloat().coerceAtLeast(0f)
        return translateX.coerceIn(minTranslate, 0f)
    }
}
