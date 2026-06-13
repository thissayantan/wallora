package com.wallora.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.model.SourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists all user settings via DataStore (Preferences).
 * All writes are suspend functions; all reads are [Flow]s for reactive UI.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    // ── Source toggles ──────────────────────────────────────────────────────
    private val enabledSourcesKey = stringSetPreferencesKey("enabled_sources")

    val enabledSources: Flow<Set<SourceId>> = dataStore.data.map { prefs ->
        val raw = prefs[enabledSourcesKey] ?: SourceId.entries.map { it.name }.toSet()
        raw.mapNotNull { runCatching { SourceId.valueOf(it) }.getOrNull() }.toSet()
    }

    suspend fun setSourceEnabled(source: SourceId, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[enabledSourcesKey]?.toMutableSet()
                ?: SourceId.entries.map { it.name }.toMutableSet()
            if (enabled) current.add(source.name) else current.remove(source.name)
            prefs[enabledSourcesKey] = current
        }
    }

    // ── Category defaults ────────────────────────────────────────────────────
    private val selectedCategoriesKey = stringSetPreferencesKey("selected_categories")

    val selectedCategories: Flow<Set<Category>> = dataStore.data.map { prefs ->
        // null = all categories
        prefs[selectedCategoriesKey]
            ?.mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun setSelectedCategories(categories: Set<Category>) {
        dataStore.edit { prefs ->
            prefs[selectedCategoriesKey] = categories.map { it.name }.toSet()
        }
    }

    // ── Rotation settings ────────────────────────────────────────────────────
    private val rotationEnabledKey = booleanPreferencesKey("rotation_enabled")
    private val rotationIntervalMsKey = longPreferencesKey("rotation_interval_ms")
    private val rotationTimesKey = stringSetPreferencesKey("rotation_times")       // "HH:mm" strings
    private val rotationOnUnlockKey = booleanPreferencesKey("rotation_on_unlock")
    private val rotationWifiOnlyKey = booleanPreferencesKey("rotation_wifi_only")
    private val rotationChargingOnlyKey = booleanPreferencesKey("rotation_charging_only")
    private val rotationPlaylistKey = stringPreferencesKey("rotation_playlist")    // "FAVORITES" | "CATEGORIES"

    val rotationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[rotationEnabledKey] ?: false
    }
    val rotationIntervalMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[rotationIntervalMsKey] ?: 3_600_000L // 1 hour default
    }
    val rotationTimes: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[rotationTimesKey] ?: emptySet()
    }
    val rotationOnUnlock: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[rotationOnUnlockKey] ?: false
    }
    val rotationWifiOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[rotationWifiOnlyKey] ?: false
    }
    val rotationChargingOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[rotationChargingOnlyKey] ?: false
    }
    val rotationPlaylist: Flow<String> = dataStore.data.map { prefs ->
        prefs[rotationPlaylistKey] ?: "CATEGORIES"
    }

    suspend fun setRotationEnabled(enabled: Boolean) =
        dataStore.edit { it[rotationEnabledKey] = enabled }

    suspend fun setRotationIntervalMs(ms: Long) =
        dataStore.edit { it[rotationIntervalMsKey] = ms }

    suspend fun setRotationTimes(times: Set<String>) =
        dataStore.edit { it[rotationTimesKey] = times }

    suspend fun setRotationOnUnlock(enabled: Boolean) =
        dataStore.edit { it[rotationOnUnlockKey] = enabled }

    suspend fun setRotationWifiOnly(enabled: Boolean) =
        dataStore.edit { it[rotationWifiOnlyKey] = enabled }

    suspend fun setRotationChargingOnly(enabled: Boolean) =
        dataStore.edit { it[rotationChargingOnlyKey] = enabled }

    suspend fun setRotationPlaylist(playlist: String) =
        dataStore.edit { it[rotationPlaylistKey] = playlist }

    // ── Current wallpaper (for live engine observability) ────────────────────
    private val currentWallpaperFullUrlKey = stringPreferencesKey("current_wallpaper_full_url")
    private val currentWallpaperThumbUrlKey = stringPreferencesKey("current_wallpaper_thumb_url")

    /**
     * The full-res and thumb URL of the most recently applied wallpaper.
     * The live wallpaper engine observes this to reload its bitmap on rotation.
     * Null when no wallpaper has been persisted yet (fresh install).
     */
    val currentWallpaperUrls: Flow<Pair<String, String>?> = dataStore.data.map { prefs ->
        val full = prefs[currentWallpaperFullUrlKey] ?: return@map null
        full to (prefs[currentWallpaperThumbUrlKey] ?: "")
    }

    suspend fun setCurrentWallpaperUrls(fullUrl: String, thumbUrl: String) {
        dataStore.edit { prefs ->
            prefs[currentWallpaperFullUrlKey] = fullUrl
            prefs[currentWallpaperThumbUrlKey] = thumbUrl
        }
    }

    // ── Gesture & live wallpaper ─────────────────────────────────────────────
    private val doubleTapGestureKey = booleanPreferencesKey("double_tap_gesture")
    private val parallaxEnabledKey = booleanPreferencesKey("parallax_enabled")
    private val isLiveWallpaperActiveKey = booleanPreferencesKey("live_wallpaper_active")

    val doubleTapGestureEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[doubleTapGestureKey] ?: false  // DEFAULT OFF: most launchers consume the gesture
    }
    val parallaxEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[parallaxEnabledKey] ?: true  // DEFAULT ON per spec
    }
    val isLiveWallpaperActive: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[isLiveWallpaperActiveKey] ?: false
    }

    suspend fun setDoubleTapGesture(enabled: Boolean) =
        dataStore.edit { it[doubleTapGestureKey] = enabled }

    suspend fun setParallaxEnabled(enabled: Boolean) =
        dataStore.edit { it[parallaxEnabledKey] = enabled }

    suspend fun setLiveWallpaperActive(active: Boolean) =
        dataStore.edit { it[isLiveWallpaperActiveKey] = active }

    // ── EditParams (default look for live mode) ───────────────────────────────
    private val editBlurKey = floatPreferencesKey("edit_blur")
    private val editBrightnessKey = floatPreferencesKey("edit_brightness")
    private val editContrastKey = floatPreferencesKey("edit_contrast")
    private val editSaturationKey = floatPreferencesKey("edit_saturation")
    private val editPanXKey = floatPreferencesKey("edit_pan_x")
    private val editPanYKey = floatPreferencesKey("edit_pan_y")

    val defaultEditParams: Flow<EditParams> = dataStore.data.map { prefs ->
        EditParams(
            blur = prefs[editBlurKey] ?: 0f,
            brightness = prefs[editBrightnessKey] ?: 0f,
            contrast = prefs[editContrastKey] ?: 1f,
            saturation = prefs[editSaturationKey] ?: 1f,
            panX = prefs[editPanXKey] ?: 0f,
            panY = prefs[editPanYKey] ?: 0f,
        )
    }

    suspend fun setDefaultEditParams(params: EditParams) = dataStore.edit { prefs ->
        prefs[editBlurKey] = params.blur
        prefs[editBrightnessKey] = params.brightness
        prefs[editContrastKey] = params.contrast
        prefs[editSaturationKey] = params.saturation
        prefs[editPanXKey] = params.panX
        prefs[editPanYKey] = params.panY
    }

    // ── Theme ────────────────────────────────────────────────────────────────
    private val themeKey = stringPreferencesKey("theme")  // "SYSTEM" | "LIGHT" | "DARK"

    val theme: Flow<String> = dataStore.data.map { prefs -> prefs[themeKey] ?: "SYSTEM" }

    suspend fun setTheme(theme: String) = dataStore.edit { it[themeKey] = theme }

    // ── Cache management ─────────────────────────────────────────────────────
    private val cacheTtlMsKey = longPreferencesKey("cache_ttl_ms")

    val cacheTtlMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[cacheTtlMsKey] ?: 3_600_000L
    }

    suspend fun setCacheTtlMs(ms: Long) = dataStore.edit { it[cacheTtlMsKey] = ms }

    // ── User API keys (override BuildConfig at runtime) ──────────────────────
    private val userPexelsKeyKey = stringPreferencesKey("user_pexels_key")
    private val userUnsplashKeyKey = stringPreferencesKey("user_unsplash_key")
    private val userWallhavenKeyKey = stringPreferencesKey("user_wallhaven_key")

    val userPexelsKey: Flow<String> = dataStore.data.map { it[userPexelsKeyKey] ?: "" }
    val userUnsplashKey: Flow<String> = dataStore.data.map { it[userUnsplashKeyKey] ?: "" }
    val userWallhavenKey: Flow<String> = dataStore.data.map { it[userWallhavenKeyKey] ?: "" }

    suspend fun setUserPexelsKey(key: String) = dataStore.edit { it[userPexelsKeyKey] = key }
    suspend fun setUserUnsplashKey(key: String) = dataStore.edit { it[userUnsplashKeyKey] = key }
    suspend fun setUserWallhavenKey(key: String) = dataStore.edit { it[userWallhavenKeyKey] = key }

    // ── User Reddit subreddits ───────────────────────────────────────────────
    private val userSubredditsKey = stringSetPreferencesKey("user_subreddits")

    val userSubreddits: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[userSubredditsKey]?.toList()?.sorted() ?: DEFAULT_SUBREDDITS
    }

    suspend fun setUserSubreddits(subs: Set<String>) =
        dataStore.edit { it[userSubredditsKey] = subs }

    // ── Custom category keywords ─────────────────────────────────────────────
    private val customKeywordsKey = stringSetPreferencesKey("custom_keywords")

    val customKeywords: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[customKeywordsKey] ?: emptySet()
    }

    suspend fun setCustomKeywords(keywords: Set<String>) =
        dataStore.edit { it[customKeywordsKey] = keywords }

    // ── Prefetched next wallpaper (for instant gesture apply) ────────────────
    private val prefetchedFullUrlKey = stringPreferencesKey("prefetched_full_url")
    private val prefetchedThumbUrlKey = stringPreferencesKey("prefetched_thumb_url")

    val prefetchedWallpaperUrls: Flow<Pair<String, String>?> = dataStore.data.map { prefs ->
        val full = prefs[prefetchedFullUrlKey] ?: return@map null
        full to (prefs[prefetchedThumbUrlKey] ?: "")
    }

    suspend fun setPrefetchedWallpaperUrls(fullUrl: String, thumbUrl: String) {
        dataStore.edit { prefs ->
            prefs[prefetchedFullUrlKey] = fullUrl
            prefs[prefetchedThumbUrlKey] = thumbUrl
        }
    }

    suspend fun clearPrefetchedWallpaperUrls() {
        dataStore.edit { prefs ->
            prefs.remove(prefetchedFullUrlKey)
            prefs.remove(prefetchedThumbUrlKey)
        }
    }

    companion object {
        val DEFAULT_SUBREDDITS = listOf("iWallpaper")
    }
}
