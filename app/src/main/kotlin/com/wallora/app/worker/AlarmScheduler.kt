package com.wallora.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels the exact-alarm rotation triggers via [AlarmManager].
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] when exact alarms are permitted,
 * falling back to [AlarmManager.setAndAllowWhileIdle] (inexact) otherwise. This
 * satisfies the [android.Manifest.permission.SCHEDULE_EXACT_ALARM] graceful flow
 * required on Android 12+ (SDK 31+).
 *
 * Only one alarm is pending at a time — [RotationAlarmReceiver] re-registers the
 * *next* alarm after firing (chained alarms pattern).
 */
@Singleton
class AlarmScheduler @Inject constructor() {

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val REQUEST_CODE = 1001
    }

    /**
     * Schedule the next rotation alarm from a set of daily "HH:mm" trigger times.
     *
     * @param context  application context.
     * @param times    set of "HH:mm" strings; empty = do nothing.
     * @return         true if an alarm was scheduled.
     */
    fun scheduleNext(context: Context, times: Set<String>): Boolean {
        val triggerMs = AlarmScheduleCalculator.nextTrigger(
            times = times,
            nowMs = System.currentTimeMillis(),
        ) ?: run {
            Log.d(TAG, "No trigger times configured")
            return false
        }

        val intent = buildIntent(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Graceful inexact fallback
            Log.w(TAG, "canScheduleExactAlarms=false — using inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
        }

        Log.d(TAG, "Alarm scheduled at epoch=$triggerMs (${java.util.Date(triggerMs)})")
        return true
    }

    /** Cancel any pending rotation alarm. */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildIntent(context))
        Log.d(TAG, "Alarm cancelled")
    }

    private fun buildIntent(context: Context): PendingIntent {
        val intent = Intent(context, RotationAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
