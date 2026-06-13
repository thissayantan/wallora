package com.wallora.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WalloraApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Singleton Coil ImageLoader — tuned for a wallpaper app:
     *  - Memory cache: 20% of max heap (default is 25%; 20% is gentler for the grid)
     *  - Disk cache: 250 MB in the app's cache dir (thumbnails + full-res previews)
     *  - Crossfade enabled by default (300 ms)
     *  - CachePolicy: ENABLED for both memory and disk
     */
    override fun newImageLoader(): ImageLoader {
        val maxHeapBytes = Runtime.getRuntime().maxMemory()
        val memoryCacheInt = (maxHeapBytes * 0.20).toLong()
            .coerceIn(32L * 1024 * 1024, 256L * 1024 * 1024)
            .toInt()

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(memoryCacheInt)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(300)
            .build()
    }
}
