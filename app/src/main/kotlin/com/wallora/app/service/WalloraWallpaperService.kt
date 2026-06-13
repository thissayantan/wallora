package com.wallora.app.service

import android.app.WallpaperColors
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.wallora.app.BuildConfig
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.data.util.ImageAdjustments
import com.wallora.app.data.util.SafeBitmapDecoder
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import com.wallora.app.service.helpers.CropCalculator
import com.wallora.app.service.helpers.CrossfadeAnimator
import com.wallora.app.service.helpers.ParallaxMath
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Live wallpaper service for Wallora.
 *
 * Architecture (battery-safe):
 * - Draws ONLY on change events (offset, double-tap, new wallpaper), NOT in a
 *   continuous render loop.
 * - Bitmap is kept in memory while visible; released on surfaceDestroyed.
 * - All heavy work (download, decode, adjust) happens on Dispatchers.IO.
 *
 * Bitmap wiring (A2-live fix):
 * - [SettingsRepository.currentWallpaperUrls] is observed in the engine scope.
 * - [NextWallpaperUseCase] writes the picked wallpaper's URL to that DataStore key.
 * - On each new emission the engine downloads + decodes to an over-wide bitmap and
 *   calls [loadBitmap], triggering a crossfade.
 * - On first surface ready with no persisted URL, the engine kicks [nextWallpaperUseCase]
 *   once to populate an initial wallpaper.
 *
 * Parallax (E1 fix):
 * - [setOffsetNotificationsEnabled] is called in [Engine.onCreate] so the launcher
 *   delivers real xOffset values.
 * - Bitmap is decoded at [ParallaxMath.PARALLAX_SCALE]× surface width so there is
 *   extra pixel travel for the translation.
 * - [setDesiredMinimumWidth] advertises the over-wide width so launchers don't fix
 *   xOffset at 0.5.
 */
@AndroidEntryPoint
class WalloraWallpaperService : WallpaperService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperRepository: WallpaperRepository
    @Inject lateinit var nextWallpaperUseCase: NextWallpaperUseCase
    @Inject lateinit var okHttpClient: OkHttpClient

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
        private var xOffset = 0.5f  // start centred until real offsets arrive
        private var xOffsetStep = 0f

        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private var editParams = EditParams.Default
        private var parallaxEnabled = true

        /** Guard: only kick nextWallpaperUseCase once when no URL is persisted. */
        private var hasKickedInitialLoad = false

        // ── On-unlock receiver ────────────────────────────────────────────────

        /** Dynamically registered — not in manifest (ACTION_USER_PRESENT can't be static on 26+). */
        private val userPresentReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                if (intent.action != android.content.Intent.ACTION_USER_PRESENT) return
                engineScope.launch {
                    val enabled = settingsRepository.rotationOnUnlock.first()
                    if (enabled) {
                        Log.d(TAG, "Screen unlocked — rotating wallpaper")
                        nextWallpaperUseCase(WallpaperTarget.HOME)
                    }
                }
            }
        }

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
            // Deliver real xOffset values from the launcher (E1 fix)
            setOffsetNotificationsEnabled(true)

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

            // Observe the current wallpaper URL — reload bitmap whenever it changes.
            // NextWallpaperUseCase writes here on every rotation pick (A2-live fix).
            engineScope.launch {
                settingsRepository.currentWallpaperUrls.collect { urls ->
                    if (urls != null && surfaceWidth > 0) {
                        loadWallpaperFromUrl(urls.first)
                    }
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

            // If no bitmap yet, trigger an initial load (surface is ready now).
            if (!hasKickedInitialLoad && currentBitmap == null) {
                hasKickedInitialLoad = true
                engineScope.launch {
                    val urls = settingsRepository.currentWallpaperUrls.first()
                    if (urls != null) {
                        // Collector above will handle it once surfaceWidth > 0; force it now.
                        loadWallpaperFromUrl(urls.first)
                    } else {
                        // No persisted wallpaper — pick a new one.
                        // NextWallpaperUseCase will persist the URL → collector fires → loadBitmap.
                        Log.d(TAG, "No persisted wallpaper — kicking next rotation")
                        nextWallpaperUseCase(WallpaperTarget.HOME)
                    }
                }
            }

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
            if (visible) {
                drawFrame()
                // Register on-unlock receiver only while engine is visible
                registerReceiver(
                    userPresentReceiver,
                    IntentFilter(android.content.Intent.ACTION_USER_PRESENT),
                )
            } else {
                try { unregisterReceiver(userPresentReceiver) } catch (_: IllegalArgumentException) { }
            }
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
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onOffsetsChanged xOffset=$xOffset step=$xOffsetStep")
                }
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
                canvas.drawColor(Color.BLACK)
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

            val rawTranslate = if (parallaxEnabled) {
                ParallaxMath.translateX(xOffset, bitmap.width, surfaceWidth)
            } else {
                0f
            }
            // Clamp so the bitmap never shows a gap at either edge
            val translateX = ParallaxMath.clampTranslateX(rawTranslate, bitmap.width, surfaceWidth)

            canvas.save()
            canvas.translate(translateX, 0f)
            canvas.drawBitmap(bitmap, null, destRect, paint)
            canvas.restore()
        }

        // ── WallpaperColors (Material You) ────────────────────────────────────

        override fun onComputeColors(): WallpaperColors? {
            val bmp = currentBitmap ?: return null
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
            return try {
                // Crop to the center-visible strip (removes the parallax over-width on both sides)
                // so color extraction reflects what the user actually sees, not the hidden margins.
                val visibleBmp = if (surfaceWidth > 0 && bmp.width > surfaceWidth) {
                    val startX = (bmp.width - surfaceWidth) / 2
                    Bitmap.createBitmap(bmp, startX, 0, surfaceWidth, bmp.height)
                } else bmp
                WallpaperColors.fromBitmap(visibleBmp)
            } catch (e: Exception) {
                Log.w(TAG, "WallpaperColors.fromBitmap failed", e)
                null
            }
        }

        /**
         * Load a new bitmap, applying crossfade if a current bitmap exists.
         * Must be called on the Main thread (touches currentBitmap / nextBitmap).
         */
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
            // Notify system of new dominant colors for Material You dynamic theming
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                notifyColorsChanged()
            }
        }

        /**
         * Download [fullUrl] and decode it to an over-wide bitmap suitable for parallax
         * rendering ([ParallaxMath.PARALLAX_SCALE] × surface width, ARGB_8888).
         * Calls [loadBitmap] on the Main thread when done.
         */
        private suspend fun loadWallpaperFromUrl(fullUrl: String) {
            val targetWidth = (surfaceWidth * ParallaxMath.PARALLAX_SCALE).roundToInt()
                .takeIf { it > 0 } ?: return
            val targetHeight = surfaceHeight.takeIf { it > 0 } ?: return

            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Loading wallpaper from URL: $fullUrl (${targetWidth}×${targetHeight})")
                    val request = Request.Builder().url(fullUrl).build()
                    val bytes = okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body?.bytes() else null
                    }
                    if (bytes == null) {
                        Log.w(TAG, "Failed to download wallpaper: $fullUrl")
                        return@withContext
                    }
                    val bitmap = SafeBitmapDecoder.decodeRegion(bytes, targetWidth, targetHeight)
                    if (bitmap == null) {
                        Log.w(TAG, "Failed to decode wallpaper: $fullUrl")
                        return@withContext
                    }
                    withContext(Dispatchers.Main) { loadBitmap(bitmap) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading wallpaper from URL: $fullUrl", e)
                }
            }
        }
    }
}
