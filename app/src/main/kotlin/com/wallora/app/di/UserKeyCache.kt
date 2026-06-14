package com.wallora.app.di

import com.wallora.app.BuildConfig
import com.wallora.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches effective API keys in @Volatile fields so OkHttp interceptors can read them
 * synchronously on every request. Merges user-supplied keys (from DataStore) with the
 * compile-time BuildConfig fallbacks — user key wins when set.
 */
@Singleton
class UserKeyCache @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    @Volatile var effectivePexelsKey: String = BuildConfig.PEXELS_API_KEY
        private set
    @Volatile var effectiveUnsplashKey: String = BuildConfig.UNSPLASH_ACCESS_KEY
        private set
    @Volatile var effectiveWallhavenKey: String = BuildConfig.WALLHAVEN_API_KEY
        private set
    @Volatile var effectivePixabayKey: String = BuildConfig.PIXABAY_API_KEY
        private set

    init {
        appScope.launch {
            settingsRepository.userPexelsKey.collect { userKey ->
                effectivePexelsKey = userKey.ifBlank { BuildConfig.PEXELS_API_KEY }
            }
        }
        appScope.launch {
            settingsRepository.userUnsplashKey.collect { userKey ->
                effectiveUnsplashKey = userKey.ifBlank { BuildConfig.UNSPLASH_ACCESS_KEY }
            }
        }
        appScope.launch {
            settingsRepository.userWallhavenKey.collect { userKey ->
                effectiveWallhavenKey = userKey.ifBlank { BuildConfig.WALLHAVEN_API_KEY }
            }
        }
        appScope.launch {
            settingsRepository.userPixabayKey.collect { userKey ->
                effectivePixabayKey = userKey.ifBlank { BuildConfig.PIXABAY_API_KEY }
            }
        }
    }
}
