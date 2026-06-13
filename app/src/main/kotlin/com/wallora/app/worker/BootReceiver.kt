package com.wallora.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wallora.app.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-registers WorkManager periodic work and AlarmManager exact alarms after
 * device reboot. Also handles MY_PACKAGE_REPLACED so alarms survive app updates.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rotationEnabled = settingsRepository.rotationEnabled.first()
                if (!rotationEnabled) {
                    Log.d(TAG, "Rotation disabled — nothing to re-register on boot")
                    return@launch
                }

                val intervalMs = settingsRepository.rotationIntervalMs.first()
                val wifiOnly = settingsRepository.rotationWifiOnly.first()
                val chargingOnly = settingsRepository.rotationChargingOnly.first()
                val times = settingsRepository.rotationTimes.first()

                // Re-schedule WorkManager periodic rotation
                RotationWorker.schedule(context, intervalMs, wifiOnly, chargingOnly)

                // Re-schedule exact-alarm rotation if times are configured
                if (times.isNotEmpty()) {
                    alarmScheduler.scheduleNext(context, times)
                    Log.d(TAG, "Re-scheduled exact alarm on boot for times: $times")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Boot re-registration failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
