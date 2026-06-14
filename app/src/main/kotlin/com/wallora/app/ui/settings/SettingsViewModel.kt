package com.wallora.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.wallora.app.BuildConfig
import com.wallora.app.R
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import com.wallora.app.worker.AlarmScheduleCalculator
import com.wallora.app.worker.AlarmScheduler
import com.wallora.app.worker.RotationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val nextWallpaperUseCase: NextWallpaperUseCase,
    private val downloadClient: OkHttpClient,
    private val alarmScheduler: AlarmScheduler,
    private val sources: Set<@JvmSuppressWildcards WallpaperSource>,
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    // ── Source toggles ────────────────────────────────────────────────────────

    val enabledSources: StateFlow<Set<SourceId>> =
        settingsRepository.enabledSources
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourceId.entries.toSet())

    /** Reactive map: SourceId → isConfigured (considers user-supplied keys). */
    val sourceConfiguredMap: StateFlow<Map<SourceId, Boolean>> = combine(
        settingsRepository.userPexelsKey,
        settingsRepository.userUnsplashKey,
        settingsRepository.userPixabayKey,
    ) { pexelsKey, unsplashKey, pixabayKey ->
        configuredMap(userPexels = pexelsKey, userUnsplash = unsplashKey, userPixabay = pixabayKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), configuredMap())

    fun setSourceEnabled(source: SourceId, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSourceEnabled(source, enabled)
    }

    /**
     * A source is configured when it needs no key (Wallhaven, Reddit) or when a key is present
     * from BuildConfig or the user. User-key args default to blank so the same builder serves
     * the StateFlow's initial value.
     */
    private fun configuredMap(
        userPexels: String = "",
        userUnsplash: String = "",
        userPixabay: String = "",
    ): Map<SourceId, Boolean> = mapOf(
        SourceId.PEXELS to (BuildConfig.PEXELS_API_KEY.isNotBlank() || userPexels.isNotBlank()),
        SourceId.UNSPLASH to (BuildConfig.UNSPLASH_ACCESS_KEY.isNotBlank() || userUnsplash.isNotBlank()),
        SourceId.WALLHAVEN to true,
        SourceId.REDDIT to true,
        SourceId.PIXABAY to (BuildConfig.PIXABAY_API_KEY.isNotBlank() || userPixabay.isNotBlank()),
    )

    // ── Category defaults ─────────────────────────────────────────────────────

    val selectedCategories: StateFlow<Set<Category>> =
        settingsRepository.selectedCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val customKeywords: StateFlow<Set<String>> =
        settingsRepository.customKeywords
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleCategory(category: Category) = viewModelScope.launch {
        val current = selectedCategories.value.toMutableSet()
        if (category in current) current.remove(category) else current.add(category)
        settingsRepository.setSelectedCategories(current)
    }

    fun addCustomKeyword(keyword: String) = viewModelScope.launch {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return@launch
        val current = customKeywords.value.toMutableSet()
        current.add(trimmed)
        settingsRepository.setCustomKeywords(current)
    }

    fun removeCustomKeyword(keyword: String) = viewModelScope.launch {
        val current = customKeywords.value.toMutableSet()
        current.remove(keyword)
        settingsRepository.setCustomKeywords(current)
    }

    // ── Reddit subreddits ─────────────────────────────────────────────────────

    val userSubreddits: StateFlow<List<String>> =
        settingsRepository.userSubreddits
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_SUBREDDITS)

    fun addSubreddit(name: String) = viewModelScope.launch {
        val trimmed = name.trim().removePrefix("r/").removePrefix("/r/")
        if (trimmed.isBlank()) return@launch
        val current = userSubreddits.value.toMutableSet()
        current.add(trimmed)
        settingsRepository.setUserSubreddits(current)
    }

    fun removeSubreddit(name: String) = viewModelScope.launch {
        val current = userSubreddits.value.toMutableSet()
        current.remove(name)
        if (current.isEmpty()) current.addAll(SettingsRepository.DEFAULT_SUBREDDITS)
        settingsRepository.setUserSubreddits(current)
    }

    fun resetSubreddits() = viewModelScope.launch {
        settingsRepository.setUserSubreddits(SettingsRepository.DEFAULT_SUBREDDITS.toSet())
    }

    // ── Rotation state ────────────────────────────────────────────────────────

    val rotationEnabled: StateFlow<Boolean> =
        settingsRepository.rotationEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val rotationIntervalMs: StateFlow<Long> =
        settingsRepository.rotationIntervalMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3_600_000L)

    val rotationPlaylist: StateFlow<String> =
        settingsRepository.rotationPlaylist
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "CATEGORIES")

    val rotationTimes: StateFlow<Set<String>> =
        settingsRepository.rotationTimes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val rotationWifiOnly: StateFlow<Boolean> =
        settingsRepository.rotationWifiOnly
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val rotationChargingOnly: StateFlow<Boolean> =
        settingsRepository.rotationChargingOnly
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val rotationOnUnlock: StateFlow<Boolean> =
        settingsRepository.rotationOnUnlock
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val nextChangeLabel: StateFlow<String> = combine(
        settingsRepository.rotationEnabled,
        settingsRepository.rotationTimes,
    ) { enabled, times ->
        if (!enabled || times.isEmpty()) return@combine ""
        val nextMs = AlarmScheduleCalculator.nextTrigger(times, System.currentTimeMillis())
            ?: return@combine ""
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowDay = System.currentTimeMillis() / 86_400_000L
        val nextDay = nextMs / 86_400_000L
        if (nextDay == nowDay) "Today ${fmt.format(Date(nextMs))}"
        else "Tomorrow ${fmt.format(Date(nextMs))}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setRotationEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setRotationEnabled(enabled)
        val intervalMs = rotationIntervalMs.value
        val wifiOnly = rotationWifiOnly.value
        val chargingOnly = rotationChargingOnly.value
        val times = rotationTimes.value
        if (enabled) {
            RotationWorker.schedule(context, intervalMs, wifiOnly, chargingOnly)
            if (times.isNotEmpty()) alarmScheduler.scheduleNext(context, times)
            // Apply a wallpaper immediately so the user sees feedback right away —
            // WorkManager's first run is ≥15 min so without this "nothing happens."
            _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.settings_rotation_applying)))
            nextWallpaperUseCase(WallpaperTarget.HOME)
        } else {
            RotationWorker.cancel(context)
            alarmScheduler.cancel(context)
        }
    }

    fun setRotationInterval(ms: Long) = viewModelScope.launch {
        settingsRepository.setRotationIntervalMs(ms)
        if (rotationEnabled.value) {
            RotationWorker.schedule(context, ms, rotationWifiOnly.value, rotationChargingOnly.value)
        }
    }

    fun setRotationPlaylist(playlist: String) = viewModelScope.launch {
        settingsRepository.setRotationPlaylist(playlist)
    }

    fun addRotationTime(time: String) = viewModelScope.launch {
        val current = rotationTimes.value.toMutableSet()
        current.add(time)
        settingsRepository.setRotationTimes(current)
        if (rotationEnabled.value) alarmScheduler.scheduleNext(context, current)
    }

    fun removeRotationTime(time: String) = viewModelScope.launch {
        val current = rotationTimes.value.toMutableSet()
        current.remove(time)
        settingsRepository.setRotationTimes(current)
        alarmScheduler.cancel(context)
        if (current.isNotEmpty() && rotationEnabled.value) alarmScheduler.scheduleNext(context, current)
    }

    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setRotationWifiOnly(enabled)
        if (rotationEnabled.value) {
            RotationWorker.schedule(context, rotationIntervalMs.value, enabled, rotationChargingOnly.value)
        }
    }

    fun setChargingOnly(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setRotationChargingOnly(enabled)
        if (rotationEnabled.value) {
            RotationWorker.schedule(context, rotationIntervalMs.value, rotationWifiOnly.value, enabled)
        }
    }

    fun setRotationOnUnlock(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setRotationOnUnlock(enabled)
    }

    // ── Parallax ──────────────────────────────────────────────────────────────

    val parallaxEnabled: StateFlow<Boolean> =
        settingsRepository.parallaxEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setParallaxEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setParallaxEnabled(enabled)
    }

    // ── Default look (EditParams) ─────────────────────────────────────────────

    val defaultEditParams: StateFlow<EditParams> =
        settingsRepository.defaultEditParams
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditParams.Default)

    fun resetDefaultEditParams() = viewModelScope.launch {
        settingsRepository.setDefaultEditParams(EditParams.Default)
    }

    // ── User API keys ─────────────────────────────────────────────────────────

    val userPexelsKey: StateFlow<String> =
        settingsRepository.userPexelsKey
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val userUnsplashKey: StateFlow<String> =
        settingsRepository.userUnsplashKey
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val userWallhavenKey: StateFlow<String> =
        settingsRepository.userWallhavenKey
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val userPixabayKey: StateFlow<String> =
        settingsRepository.userPixabayKey
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveUserPexelsKey(key: String) = viewModelScope.launch {
        settingsRepository.setUserPexelsKey(key.trim())
        _events.emit(SettingsEvent.ShowMessage("Pexels key saved"))
    }

    fun saveUserUnsplashKey(key: String) = viewModelScope.launch {
        settingsRepository.setUserUnsplashKey(key.trim())
        _events.emit(SettingsEvent.ShowMessage("Unsplash key saved"))
    }

    fun saveUserWallhavenKey(key: String) = viewModelScope.launch {
        settingsRepository.setUserWallhavenKey(key.trim())
        _events.emit(SettingsEvent.ShowMessage("Wallhaven key saved"))
    }

    fun saveUserPixabayKey(key: String) = viewModelScope.launch {
        settingsRepository.setUserPixabayKey(key.trim())
        _events.emit(SettingsEvent.ShowMessage("Pixabay key saved"))
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    val theme: StateFlow<String> =
        settingsRepository.theme
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    fun setTheme(theme: String) = viewModelScope.launch {
        settingsRepository.setTheme(theme)
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    fun clearCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            wallpaperRepository.clearCache()
            downloadClient.cache?.evictAll()
            Coil.imageLoader(context).diskCache?.clear()
        }
        _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.settings_clear_cache_done)))
    }
}
