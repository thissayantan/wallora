package com.wallora.app.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.data.util.ImageAdjustments
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import com.wallora.app.service.helpers.CrossfadeAnimator
import com.wallora.app.service.helpers.CropCalculator
import com.wallora.app.service.helpers.ParallaxMath
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Live wallpaper service for Wallora.
 *
 * Architecture (battery-safe):
 * - Draws ONLY on change events (offset, double-tap, new wallpaper), NOT in a
 *   continuous render loop.
 * - Bitmap is kept in memory while visible; released on surfaceDestroyed.
 * - All heavy work (download, decode, adjust) happens on Dispatchers.IO.
 */
@AndroidEntryPoint
class WalloraWallpaperService : WallpaperService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperRepository: WallpaperRepository
    @Inject lateinit var nextWallpaperUseCase: NextWallpaperUseCase

    override fun onCreateEngine(): Engine = WalloraEngine()

    inner class WalloraEngine : Engine() {

        private val TAG = "WalloraEngine"

        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val handler = Handler(Looper.getMainLooper())

        // ── Render state ──────────────────────────────────────────────────────

        /** Current drawn bitmap (already EditParams-adjusted). */
        private var currentBitmap: Bitmap? = null

        /** Next bitmap being cross-faded in. */
        private var nextBitmap: Bitmap? = null

        private var crossfadeAnimator = CrossfadeAnimator()

        /** Current horizontal scroll offset [0..1] from launcher. */
        private var xOffset = 0f
        private var xOffsetStep = 0f

        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private var editParams = EditParams.Default
        private var parallaxEnabled = true

        // ── Gesture ───────────────────────────────────────────────────────────

        private val gestureDetector = GestureDetector(
            applicationContext,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d(TAG, "Double-tap → next wallpaper")
                    engineScope.launch {
                        val gestureEnabled = settingsRepository.doubleTapGestureEnabled.first()
                        if (gestureEnabled) {
                            nextWallpaperUseCase(WallpaperTarget.HOME)
                        }
                    }
                    return true
                }
            }
        )

        // ── Lifecycle ─────────────────────────────────────────────────────────

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            // Observe settings changes for live updates
            engineScope.launch {
                settingsRepository.defaultEditParams.collect { params ->
                    editParams = params
                    drawFrame()
                }
            }
            engineScope.launch {
                settingsRepository.parallaxEnabled.collect { enabled ->
                    parallaxEnabled = enabled
                    drawFrame()
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "Surface created")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            Log.d(TAG, "Surface changed $width×$height")
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            currentBitmap?.recycle()
            currentBitmap = null
            nextBitmap?.recycle()
            nextBitmap = null
        }

        override fun onDestroy() {
            super.onDestroy()
            engineScope.cancel()
            handler.removeCallbacksAndMessages(null)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) drawFrame()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int,
        ) {
            if (parallaxEnabled) {
                this.xOffset = xOffset
                this.xOffsetStep = xOffsetStep
                drawFrame()
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
        }

        // ── Render ────────────────────────────────────────────────────────────

        /**
         * Draw the current frame onto the surface. Battery-safe: lock/draw/unlock only,
         * no looping timer.
         */
        fun drawFrame() {
            if (surfaceWidth == 0 || surfaceHeight == 0) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) renderOnCanvas(canvas)
            } catch (e: Exception) {
                Log.e(TAG, "Draw error", e)
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun renderOnCanvas(canvas: Canvas) {
            val bmp = currentBitmap
            if (bmp == null || bmp.isRecycled) {
                canvas.drawColor(android.graphics.Color.BLACK)
                return
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Cross-fade into next bitmap
            val next = nextBitmap
            if (next != null && !next.isRecycled) {
                val alpha = crossfadeAnimator.currentAlpha()
                paint.alpha = 255 - (alpha * 255).toInt()
                drawBitmapWithParallax(canvas, bmp, paint)
                paint.alpha = (alpha * 255).toInt()
                drawBitmapWithParallax(canvas, next, paint)

                if (crossfadeAnimator.isComplete()) {
                    currentBitmap?.recycle()
                    currentBitmap = next
                    nextBitmap = null
                    crossfadeAnimator.reset()
                } else {
                    // Schedule next frame for animation
                    handler.postDelayed({ drawFrame() }, 16L)
                }
            } else {
                paint.alpha = 255
                drawBitmapWithParallax(canvas, bmp, paint)
            }
        }

        private fun drawBitmapWithParallax(canvas: Canvas, bitmap: Bitmap, paint: Paint) {
            val destRect = CropCalculator.centerCropRect(
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                surfaceWidth = surfaceWidth,
                surfaceHeight = surfaceHeight,
            )

            val translateX = if (parallaxEnabled) {
                ParallaxMath.translateX(xOffset, bitmap.width, surfaceWidth)
            } else {
                0f
            }

            canvas.save()
            canvas.translate(translateX, 0f)
            canvas.drawBitmap(bitmap, null, destRect, paint)
            canvas.restore()
        }

        /** Called by [UserPresentReceiver] or rotation engine when a new wallpaper should be shown. */
        fun loadBitmap(bitmap: Bitmap) {
            val adjusted = if (editParams != EditParams.Default) {
                ImageAdjustments.apply(bitmap, editParams)
            } else bitmap

            if (currentBitmap == null) {
                currentBitmap = adjusted
                drawFrame()
            } else {
                nextBitmap = adjusted
                crossfadeAnimator = CrossfadeAnimator()
                drawFrame()
            }
        }
    }
}
