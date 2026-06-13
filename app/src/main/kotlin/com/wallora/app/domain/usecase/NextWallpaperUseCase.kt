package com.wallora.app.domain.usecase

import android.util.Log
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.domain.rotation.PickResult
import com.wallora.app.domain.rotation.RotationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome returned to the caller (WorkManager, AlarmReceiver, engine). */
sealed class NextWallpaperResult {
    data class Applied(val wallpaper: Wallpaper) : NextWallpaperResult()
    data object NoPlaylist : NextWallpaperResult()
    data class Failure(val message: String) : NextWallpaperResult()
}

/**
 * Picks the next wallpaper from the playlist and applies it.
 *
 * Playlist sources:
 * - `"FAVORITES"`: all saved favorites.
 * - `"CATEGORIES"` (default): reads enabled sources + selected categories from
 *   [SettingsRepository] and fetches a page of wallpapers from [WallpaperRepository].
 *
 * No-repeat: uses [RotationEngine] with recent history from Room. If all candidates
 * have been recently applied, the engine resets the window and picks from the full list.
 *
 * Pre-fetch: when [prefetchNext] is true and the device is on Wi-Fi, the *following*
 * wallpaper's full-res image is kicked off in the background as a fire-and-forget
 * download so the next rotation apply is near-instant. Implemented as a best-effort
 * warm-up via OkHttp (no on-device caching beyond the OS HTTP cache).
 */
@Singleton
class NextWallpaperUseCase @Inject constructor(
    private val repository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val applyWallpaperUseCase: ApplyWallpaperUseCase,
    private val sources: Set<@JvmSuppressWildcards WallpaperSource>,
) {
    companion object {
        private const val TAG = "NextWallpaper"
        /** Maximum recent history entries to consider for no-repeat. */
        private const val MAX_NO_REPEAT_WINDOW = 30
    }

    suspend operator fun invoke(
        target: WallpaperTarget = WallpaperTarget.BOTH,
    ): NextWallpaperResult = withContext(Dispatchers.IO) {
        val playlistMode = settingsRepository.rotationPlaylist.first()
        val candidates = getCandidates(playlistMode)
        if (candidates.isEmpty()) {
            Log.w(TAG, "No candidates for playlist=$playlistMode")
            return@withContext NextWallpaperResult.NoPlaylist
        }

        val recentHistory = repository.getRecentHistoryKeys(MAX_NO_REPEAT_WINDOW)
        val window = RotationEngine.noRepeatWindow(candidates.size, MAX_NO_REPEAT_WINDOW)
        val recentKeys = recentHistory.take(window).toSet()

        val pickResult = RotationEngine.pickNext(candidates, recentKeys)
        val wallpaper = when (pickResult) {
            is PickResult.Empty -> return@withContext NextWallpaperResult.NoPlaylist
            is PickResult.Found -> {
                if (pickResult.wasExhausted) Log.d(TAG, "No-repeat window exhausted, resetting")
                pickResult.wallpaper
            }
        }

        Log.d(TAG, "Rotating to: ${wallpaper.globalKey}")

        // Always persist the picked wallpaper so the live engine can observe it.
        settingsRepository.setCurrentWallpaperUrls(wallpaper.fullUrl, wallpaper.thumbUrl)

        val isLiveActive = settingsRepository.isLiveWallpaperActive.first()
        if (isLiveActive && target != WallpaperTarget.LOCK) {
            // Live mode: engine observes DataStore and renders the bitmap itself.
            // We must NOT call WallpaperManager.setBitmap(FLAG_SYSTEM) — it would
            // deactivate the live wallpaper and revert to static.
            // Still apply to LOCK screen statically when target == BOTH.
            repository.addToHistory(wallpaper)
            if (target == WallpaperTarget.BOTH) {
                applyWallpaperUseCase(wallpaper, WallpaperTarget.LOCK)
            }
            return@withContext NextWallpaperResult.Applied(wallpaper)
        }

        val applyResult = applyWallpaperUseCase(wallpaper, target)
        return@withContext when (applyResult) {
            is ApplyResult.Success -> NextWallpaperResult.Applied(wallpaper)
            is ApplyResult.Failure -> NextWallpaperResult.Failure(applyResult.message)
        }
    }

    private suspend fun getCandidates(playlistMode: String): List<Wallpaper> {
        return when (playlistMode) {
            "FAVORITES" -> repository.getFavoritesSnapshot()
            else -> getCategoryBrowseCandidates()
        }
    }

    /**
     * Fetches one page of wallpapers per configured + enabled source for the currently
     * selected categories. Calls [WallpaperSource.browse] with page "1" directly to avoid
     * Paging 3 infrastructure overhead in a background context.
     */
    private suspend fun getCategoryBrowseCandidates(): List<Wallpaper> {
        val enabledSources = settingsRepository.enabledSources.first()
        val categories = settingsRepository.selectedCategories.first()
            .toList()
            .ifEmpty { Category.entries.toList() }

        // Collect one page per configured + enabled source
        val results = mutableListOf<Wallpaper>()
        for (source in sources) {
            if (!source.isConfigured || source.id !in enabledSources) continue
            try {
                val page = source.browse(categories = categories, page = "1")
                results += page.items
            } catch (e: Exception) {
                Log.w(TAG, "Source ${source.id} failed during rotation fetch: ${e.message}")
            }
        }
        return results
    }
}
