package com.wallora.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import javax.inject.Inject

/**
 * Triggered by [AlarmScheduler] at user-specified rotation times.
 *
 * Uses [goAsync] to perform the wallpaper rotation on a coroutine, then
 * re-chains the *next* alarm so rotation keeps firing each day without a
 * permanent WakeLock or repeating alarms.
 */
@AndroidEntryPoint
class RotationAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RotationAlarmReceiver"
    }

    @Inject lateinit var nextWallpaperUseCase: NextWallpaperUseCase
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Alarm fired — rotating wallpaper")
                nextWallpaperUseCase(WallpaperTarget.BOTH)

                // Re-schedule the next alarm (chained; no AlarmManager.INTERVAL_DAY repeat)
                val times = settingsRepository.rotationTimes.first()
                alarmScheduler.scheduleNext(context, times)
            } catch (e: Exception) {
                Log.e(TAG, "Rotation alarm failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
