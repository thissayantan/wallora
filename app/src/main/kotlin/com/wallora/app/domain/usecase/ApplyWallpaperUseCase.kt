package com.wallora.app.domain.usecase

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.wallora.app.data.remote.UnsplashSource
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.data.util.ImageAdjustments
import com.wallora.app.data.util.SafeBitmapDecoder
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

enum class WallpaperTarget { HOME, LOCK, BOTH }

sealed class ApplyResult {
    data object Success : ApplyResult()
    data class Failure(val message: String) : ApplyResult()
}

/**
 * Applies a wallpaper to the device.
 *
 * Flow: download full-res bytes → [SafeBitmapDecoder.decodeRegion] center-crop to device
 * screen → [WallpaperManager.setBitmap] → add to history → ping Unsplash download endpoint.
 */
@Singleton
class ApplyWallpaperUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WallpaperRepository,
    private val unsplashSource: UnsplashSource,
    private val okHttpClient: OkHttpClient,
) {
    companion object { private const val TAG = "ApplyWallpaper" }

    suspend operator fun invoke(
        wallpaper: Wallpaper,
        target: WallpaperTarget,
        editedBitmap: Bitmap? = null, // pre-edited bitmap from the editor, if any
        editParams: EditParams? = null, // adjustments to apply after decoding
    ): ApplyResult = withContext(Dispatchers.IO) {
        try {
            val rawBitmap = editedBitmap ?: run {
                val (screenW, screenH) = screenSize()
                val bytes = downloadBytes(wallpaper.fullUrl) ?: return@withContext ApplyResult.Failure("Download failed")
                SafeBitmapDecoder.decodeRegion(bytes, screenW, screenH)
                    ?: return@withContext ApplyResult.Failure("Could not decode image")
            }
            val bitmap = if (editParams != null && editParams != EditParams.Default) {
                ImageAdjustments.apply(rawBitmap, editParams)
            } else {
                rawBitmap
            }

            val manager = WallpaperManager.getInstance(context)
            when (target) {
                WallpaperTarget.HOME -> manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                WallpaperTarget.LOCK -> manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                WallpaperTarget.BOTH -> manager.setBitmap(bitmap, null, true,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            }

            // Record in history (no-repeat logic for rotation)
            repository.addToHistory(wallpaper)

            // Ping Unsplash download endpoint (required by attribution terms)
            if (wallpaper.sourceId == SourceId.UNSPLASH) {
                unsplashSource.trackDownload(wallpaper.sourcePageUrl)
            }

            // Recycle intermediate bitmap if we allocated it
            if (editedBitmap == null || (editParams != null && editParams != EditParams.Default)) {
                bitmap.recycle()
            }

            ApplyResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply wallpaper: ${wallpaper.id}", e)
            ApplyResult.Failure(e.localizedMessage ?: "Unknown error")
        }
    }

    private fun downloadBytes(url: String): ByteArray? =
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $url", e)
            null
        }

    private fun screenSize(): Pair<Int, Int> =
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } catch (e: Exception) {
            // Fallback for Robolectric / tests
            1080 to 1920
        }
}
