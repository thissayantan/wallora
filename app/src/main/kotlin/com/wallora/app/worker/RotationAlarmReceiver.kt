package com.wallora.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Triggered by AlarmManager at user-specified rotation times.
 * TODO(Phase 4): inject NextWallpaperUseCase via goAsync + coroutine scope.
 */
class RotationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO(Phase 4): trigger RotationEngine.rotate()
    }
}
