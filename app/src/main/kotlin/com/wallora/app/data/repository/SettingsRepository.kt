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
        val raw = prefs[selectedCategoriesKey]
        if (raw == null) emptySet() // empty = all categories
        else raw.mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }.toSet()
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

    // ── Gesture & live wallpaper ─────────────────────────────────────────────
    private val doubleTapGestureKey = booleanPreferencesKey("double_tap_gesture")
    private val parallaxEnabledKey = booleanPreferencesKey("parallax_enabled")
    private val isLiveWallpaperActiveKey = booleanPreferencesKey("live_wallpaper_active")

    val doubleTapGestureEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[doubleTapGestureKey] ?: true
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
        prefs[cacheTtlMsKey] ?: 3_600_000L // 1 hour TTL
    }

    suspend fun setCacheTtlMs(ms: Long) = dataStore.edit { it[cacheTtlMsKey] = ms }
}
