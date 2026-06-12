package com.wallora.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Re-registers WorkManager periodic work and AlarmManager exact alarms after
 * device reboot. Also handles MY_PACKAGE_REPLACED so alarms survive app updates.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // TODO(Phase 4): re-schedule WorkManager periodic rotation + exact alarms
        }
    }
}
