package com.wallora.app.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallora.app.data.local.dao.WallpaperDao
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.model.SourceId
import com.wallora.app.worker.AlarmScheduleCalculator
import com.wallora.app.worker.AlarmScheduler
import com.wallora.app.worker.RotationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val alarmScheduler: AlarmScheduler,
    private val sources: Set<@JvmSuppressWildcards WallpaperSource>,
) : ViewModel() {

    private val TAG = "SettingsViewModel"

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    // ── Source toggles ────────────────────────────────────────────────────────

    val enabledSources: StateFlow<Set<SourceId>> =
        settingsRepository.enabledSources
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourceId.entries.toSet())

    /** Map of SourceId → isConfigured (API key present). */
    val sourceConfiguredMap: Map<SourceId, Boolean> =
        sources.associate { it.id to it.isConfigured }

    fun setSourceEnabled(source: SourceId, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSourceEnabled(source, enabled)
    }

    // ── Category defaults ─────────────────────────────────────────────────────

    val selectedCategories: StateFlow<Set<Category>> =
        settingsRepository.selectedCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleCategory(category: Category) = viewModelScope.launch {
        val current = selectedCategories.value.toMutableSet()
        if (category in current) current.remove(category) else current.add(category)
        settingsRepository.setSelectedCategories(current)
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

    val nextChangeLabel: StateFlow<String> =
        combine(
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

    // ── Gesture & parallax ────────────────────────────────────────────────────

    val doubleTapEnabled: StateFlow<Boolean> =
        settingsRepository.doubleTapGestureEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val parallaxEnabled: StateFlow<Boolean> =
        settingsRepository.parallaxEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setDoubleTapEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDoubleTapGesture(enabled)
    }

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

    // ── Theme ─────────────────────────────────────────────────────────────────

    val theme: StateFlow<String> =
        settingsRepository.theme
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    fun setTheme(theme: String) = viewModelScope.launch {
        settingsRepository.setTheme(theme)
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    fun clearCache() = viewModelScope.launch {
        wallpaperRepository.clearCache()
        _events.emit(SettingsEvent.ShowMessage("Cache cleared"))
    }
}
