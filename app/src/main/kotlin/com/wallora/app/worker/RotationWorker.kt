package com.wallora.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wallora.app.domain.usecase.NextWallpaperResult
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that runs the interval-based wallpaper rotation.
 * Injected via Hilt's [HiltWorker] / [AssistedInject] pattern.
 */
@HiltWorker
class RotationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val nextWallpaperUseCase: NextWallpaperUseCase,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RotationWorker"
        const val WORK_NAME = "wallora_interval_rotation"

        /**
         * Schedule (or reschedule) the periodic rotation worker.
         *
         * @param wifiOnly  require an unmetered network.
         * @param chargingOnly  require the device to be charging.
         * @param intervalMs  repeat interval in ms (minimum 15 minutes enforced by WorkManager).
         */
        fun schedule(
            context: Context,
            intervalMs: Long,
            wifiOnly: Boolean,
            chargingOnly: Boolean,
        ) {
            val interval = maxOf(intervalMs, TimeUnit.MINUTES.toMillis(15))
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresCharging(chargingOnly)
                .build()

            val request = PeriodicWorkRequestBuilder<RotationWorker>(
                interval, TimeUnit.MILLISECONDS,
                // flex period = 20% of interval (WorkManager minimum is 5 min)
                maxOf(TimeUnit.MINUTES.toMillis(5), interval / 5), TimeUnit.MILLISECONDS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
            Log.d(TAG, "Scheduled rotation every ${interval / 60_000}m wifiOnly=$wifiOnly chargingOnly=$chargingOnly")
        }

        /** Cancel any scheduled rotation work. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Rotation work cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Rotation worker fired")
        return when (val result = nextWallpaperUseCase(WallpaperTarget.BOTH)) {
            is NextWallpaperResult.Applied -> {
                Log.d(TAG, "Applied: ${result.wallpaper.globalKey}")
                Result.success()
            }
            is NextWallpaperResult.NoPlaylist -> {
                Log.w(TAG, "No playlist — skipping rotation")
                Result.success() // not a retry-worthy failure
            }
            is NextWallpaperResult.Failure -> {
                Log.e(TAG, "Rotation failed: ${result.message}")
                Result.retry()
            }
        }
    }
}
