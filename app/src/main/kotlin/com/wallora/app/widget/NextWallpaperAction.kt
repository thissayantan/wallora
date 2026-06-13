package com.wallora.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.wallora.app.domain.usecase.NextWallpaperResult
import com.wallora.app.domain.usecase.WallpaperTarget
import dagger.hilt.android.EntryPointAccessors

/**
 * Glance [ActionCallback] that applies the next wallpaper in-place.
 *
 * Glance callbacks are not Hilt-injected; we use [EntryPointAccessors] to reach
 * [NextWallpaperUseCase] and the application-scoped [CoroutineScope].
 *
 * The coroutine is launched on the app scope so it survives the callback lifecycle.
 */
class NextWallpaperAction : ActionCallback {

    companion object {
        private const val TAG = "NextWallpaperAction"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WalloraEntryPoint::class.java,
        )
        val useCase = entryPoint.nextWallpaperUseCase()
        // Invoke directly — onAction is already a suspend function called from a coroutine
        Log.d(TAG, "Widget tapped — applying next wallpaper")
        when (val result = useCase(WallpaperTarget.BOTH)) {
            is NextWallpaperResult.Applied -> Log.d(TAG, "Applied: ${result.wallpaper.globalKey}")
            is NextWallpaperResult.NoPlaylist -> Log.w(TAG, "No playlist")
            is NextWallpaperResult.Failure -> Log.e(TAG, "Failed: ${result.message}")
        }
    }
}
