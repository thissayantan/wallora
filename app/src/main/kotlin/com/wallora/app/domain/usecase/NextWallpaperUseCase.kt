package com.wallora.app.domain.usecase

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.di.ApplicationScope
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.domain.rotation.PickResult
import com.wallora.app.domain.rotation.RotationEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    @ApplicationContext private val context: Context,
    private val repository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val applyWallpaperUseCase: ApplyWallpaperUseCase,
    private val sources: Set<@JvmSuppressWildcards WallpaperSource>,
    private val okHttpClient: OkHttpClient,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "NextWallpaper"
        private const val MAX_NO_REPEAT_WINDOW = 30
    }

    /**
     * In-memory candidate cache. Avoids re-hitting the source APIs on every rotation
     * trigger. Populated on first call and refreshed in the background after each apply.
     */
    @Volatile private var candidateCache: List<Wallpaper> = emptyList()

    suspend operator fun invoke(
        target: WallpaperTarget = WallpaperTarget.BOTH,
    ): NextWallpaperResult = withContext(Dispatchers.IO) {
        val playlistMode = settingsRepository.rotationPlaylist.first()

        // Fast path: if a wallpaper was pre-fetched and cached to disk, apply it instantly.
        // This makes gesture-triggered changes feel immediate even after a process restart.
        val prefetched = settingsRepository.prefetchedWallpaperUrls.first()
        if (prefetched != null) {
            Log.d(TAG, "Using pre-fetched wallpaper: ${prefetched.first}")
            settingsRepository.clearPrefetchedWallpaperUrls()
            // Reconstruct a minimal Wallpaper so we can record history + kick prefetch
            val quickWallpaper = Wallpaper(
                id = "prefetch",
                sourceId = SourceId.WALLHAVEN,
                thumbUrl = prefetched.second,
                fullUrl = prefetched.first,
                width = 0, height = 0,
                author = "", authorUrl = "", sourcePageUrl = "",
                colorHint = null, category = null, tags = emptyList(),
            )
            settingsRepository.setCurrentWallpaperUrls(prefetched.first, prefetched.second)
            if (!isLiveWallpaperActive()) {
                applyWallpaperUseCase(quickWallpaper, target)
            } else {
                repository.addToHistory(quickWallpaper)
            }
            scheduleBackgroundPrefetch(playlistMode, quickWallpaper)
            return@withContext NextWallpaperResult.Applied(quickWallpaper)
        }

        // Normal path: pick from candidate cache or API
        val candidates = candidateCache.ifEmpty {
            getCandidates(playlistMode).also { candidateCache = it }
        }
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
        settingsRepository.setCurrentWallpaperUrls(wallpaper.fullUrl, wallpaper.thumbUrl)

        if (isLiveWallpaperActive() && target != WallpaperTarget.LOCK) {
            repository.addToHistory(wallpaper)
            if (target == WallpaperTarget.BOTH) {
                applyWallpaperUseCase(wallpaper, WallpaperTarget.LOCK)
            }
            scheduleBackgroundPrefetch(playlistMode, wallpaper)
            return@withContext NextWallpaperResult.Applied(wallpaper)
        }

        val applyResult = applyWallpaperUseCase(wallpaper, target)
        return@withContext when (applyResult) {
            is ApplyResult.Success -> {
                scheduleBackgroundPrefetch(playlistMode, wallpaper)
                NextWallpaperResult.Applied(wallpaper)
            }
            is ApplyResult.Failure -> NextWallpaperResult.Failure(applyResult.message)
        }
    }

    /**
     * Fire-and-forget: refresh the candidate list and warm the OkHttp disk cache for
     * the wallpaper that is most likely to be picked on the next rotation trigger.
     * Runs on [appScope] so it survives the caller finishing.
     */
    private fun scheduleBackgroundPrefetch(playlistMode: String, justApplied: Wallpaper) {
        appScope.launch(Dispatchers.IO) {
            try {
                val fresh = getCandidates(playlistMode)
                if (fresh.isNotEmpty()) candidateCache = fresh

                val updatedHistory = repository.getRecentHistoryKeys(MAX_NO_REPEAT_WINDOW)
                val window = RotationEngine.noRepeatWindow(fresh.size, MAX_NO_REPEAT_WINDOW)
                val nextPick = RotationEngine.pickNext(fresh, updatedHistory.take(window).toSet())
                if (nextPick is PickResult.Found) {
                    val next = nextPick.wallpaper
                    Log.d(TAG, "Prefetching: ${next.fullUrl}")
                    okHttpClient.newCall(Request.Builder().url(next.fullUrl).build()).execute().close()
                    // Persist the pre-fetched URL so the next invocation can apply it instantly
                    // (survives process restart — the image is already in OkHttp disk cache)
                    settingsRepository.setPrefetchedWallpaperUrls(next.fullUrl, next.thumbUrl)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Background prefetch skipped: ${e.message}")
            }
        }
    }

    /**
     * Returns true when Wallora's own live wallpaper service is the currently active wallpaper.
     * This is the real source of truth — no flag lifecycle to maintain. When true, the rotation
     * writes the new URL to DataStore and lets the live engine pick it up; it does NOT call
     * WallpaperManager.setBitmap(FLAG_SYSTEM), which would deactivate the live wallpaper.
     */
    private fun isLiveWallpaperActive(): Boolean = try {
        WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName
    } catch (_: Exception) {
        false
    }

    private suspend fun getCandidates(playlistMode: String): List<Wallpaper> =
        when (playlistMode) {
            "FAVORITES" -> repository.getFavoritesSnapshot()
            else -> getCategoryBrowseCandidates()
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
