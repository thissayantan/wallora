package com.wallora.app.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import com.wallora.app.R
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile — "Next wallpaper".
 * Available on API 24+ (Android 7.0+). Tapping the tile calls [NextWallpaperUseCase].
 */
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.N)
class WalloraQsTileService : TileService() {

    companion object {
        private const val TAG = "WalloraQsTile"
    }

    @Inject lateinit var nextWallpaperUseCase: NextWallpaperUseCase

    override fun onTileAdded() {
        updateTile(Tile.STATE_INACTIVE)
    }

    override fun onStartListening() {
        updateTile(Tile.STATE_INACTIVE)
    }

    override fun onClick() {
        // Show the tile as active while working
        updateTile(Tile.STATE_ACTIVE)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "QS tile tapped — rotating wallpaper")
                val result = nextWallpaperUseCase(WallpaperTarget.BOTH)
                Log.d(TAG, "QS tile result: $result")
            } catch (e: Exception) {
                Log.e(TAG, "QS tile rotation failed", e)
            } finally {
                updateTile(Tile.STATE_INACTIVE)
            }
        }
    }

    private fun updateTile(state: Int) {
        val tile = qsTile ?: return
        tile.state = state
        tile.label = getString(R.string.qs_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.app_name)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_next)
        tile.updateTile()
    }
}
