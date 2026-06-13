package com.wallora.app.service.helpers

import android.graphics.Rect
import kotlin.math.roundToInt

/**
 * Pure helper for center-crop destination rect calculations used by the live wallpaper
 * engine when drawing bitmaps onto the surface canvas.
 */
object CropCalculator {

    /**
     * Compute the destination [Rect] that center-crops [bitmapWidth]×[bitmapHeight] onto
     * a [surfaceWidth]×[surfaceHeight] canvas, maintaining the bitmap's aspect ratio.
     *
     * The returned rect will always be >= the surface size (or equal if aspect ratios match).
     *
     * @param bitmapWidth   source bitmap width.
     * @param bitmapHeight  source bitmap height.
     * @param surfaceWidth  canvas / surface width.
     * @param surfaceHeight canvas / surface height.
     * @return              [Rect] to pass as `destRect` to [android.graphics.Canvas.drawBitmap].
     */
    fun centerCropRect(
        bitmapWidth: Int,
        bitmapHeight: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ): Rect {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return Rect(0, 0, surfaceWidth, surfaceHeight)

        val bmpAspect = bitmapWidth.toFloat() / bitmapHeight
        val surfAspect = surfaceWidth.toFloat() / surfaceHeight

        val destW: Int
        val destH: Int

        if (bmpAspect > surfAspect) {
            // Bitmap is wider relative to height — fit to height
            destH = surfaceHeight
            destW = (surfaceHeight * bmpAspect).roundToInt()
        } else {
            // Bitmap is taller relative to width — fit to width
            destW = surfaceWidth
            destH = (surfaceWidth / bmpAspect).roundToInt()
        }

        val offsetX = (destW - surfaceWidth) / 2
        val offsetY = (destH - surfaceHeight) / 2

        return Rect(-offsetX, -offsetY, destW - offsetX, destH - offsetY)
    }

    /**
     * Compute the desired minimum width for [WallpaperService.Engine.setDesiredMinimumWidth].
     * Returns [surfaceWidth] × [parallaxScale] rounded to the nearest int.
     */
    fun desiredMinimumWidth(surfaceWidth: Int, parallaxScale: Float = ParallaxMath.PARALLAX_SCALE): Int =
        (surfaceWidth * parallaxScale).roundToInt()
}
