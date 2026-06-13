package com.wallora.app.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.wallora.app.di.ApplicationScope
import com.wallora.app.domain.usecase.NextWallpaperResult
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * No-UI trampoline activity for the "Next wallpaper" shortcut / launcher gesture / Tasker.
 *
 * Design:
 * - Theme.Wallora.Trampoline: translucent, no animation, excluded from the recents list.
 * - Calls [NextWallpaperUseCase] on the application-scoped [CoroutineScope] so the work
 *   survives [finish()] which is called immediately after launch.
 * - Exported with action [ACTION_NEXT_WALLPAPER] for Tasker and deep-linking.
 *
 * Usage from Nova Launcher:
 *   Long-press → Shortcuts → Wallora → "Next wallpaper"
 *
 * Usage from Tasker:
 *   Action: Send Intent, Action: com.wallora.app.action.NEXT_WALLPAPER, Package: com.wallora.app
 */
@AndroidEntryPoint
class NextWallpaperActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NextWallpaperActivity"
        const val ACTION_NEXT_WALLPAPER = "com.wallora.app.action.NEXT_WALLPAPER"
    }

    @Inject lateinit var nextWallpaperUseCase: NextWallpaperUseCase
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Received next-wallpaper trigger")
        // Launch on applicationScope: work outlives this Activity (finish() below).
        applicationScope.launch {
            val result = nextWallpaperUseCase(WallpaperTarget.BOTH)
            when (result) {
                is NextWallpaperResult.Applied -> Log.d(TAG, "Applied: ${result.wallpaper.globalKey}")
                is NextWallpaperResult.NoPlaylist -> Log.w(TAG, "No playlist — nothing to apply")
                is NextWallpaperResult.Failure -> Log.e(TAG, "Failed: ${result.message}")
            }
        }
        // Finish immediately so no UI is ever shown
        finish()
    }
}
