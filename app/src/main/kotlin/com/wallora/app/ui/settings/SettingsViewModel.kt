package com.wallora.app.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.worker.AlarmScheduleCalculator
import com.wallora.app.worker.AlarmScheduler
import com.wallora.app.worker.RotationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val TAG = "SettingsViewModel"

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

    /** Human-readable "next change" time, e.g. "Today 14:30" or "Tomorrow 08:00". */
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

    // ── Write operations ──────────────────────────────────────────────────────

    fun setRotationEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setRotationEnabled(enabled)
        val intervalMs = settingsRepository.rotationIntervalMs.stateIn(viewModelScope, SharingStarted.Eagerly, 3_600_000L).value
        val wifiOnly = settingsRepository.rotationWifiOnly.stateIn(viewModelScope, SharingStarted.Eagerly, false).value
        val chargingOnly = settingsRepository.rotationChargingOnly.stateIn(viewModelScope, SharingStarted.Eagerly, false).value
        val times = settingsRepository.rotationTimes.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet()).value
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
        val wifiOnly = rotationWifiOnly.value
        val chargingOnly = rotationChargingOnly.value
        if (rotationEnabled.value) {
            RotationWorker.schedule(context, ms, wifiOnly, chargingOnly)
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
        if (current.isNotEmpty() && rotationEnabled.value) {
            alarmScheduler.scheduleNext(context, current)
        }
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
}
