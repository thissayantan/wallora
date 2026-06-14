package com.wallora.app.di

import com.wallora.app.BuildConfig
import com.wallora.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
        collectKey(settingsRepository.userPexelsKey, BuildConfig.PEXELS_API_KEY) { effectivePexelsKey = it }
        collectKey(settingsRepository.userUnsplashKey, BuildConfig.UNSPLASH_ACCESS_KEY) { effectiveUnsplashKey = it }
        collectKey(settingsRepository.userWallhavenKey, BuildConfig.WALLHAVEN_API_KEY) { effectiveWallhavenKey = it }
        collectKey(settingsRepository.userPixabayKey, BuildConfig.PIXABAY_API_KEY) { effectivePixabayKey = it }
    }

    /** Collects a user-key flow and assigns the effective key (user override, else BuildConfig fallback). */
    private fun collectKey(userKey: Flow<String>, fallback: String, assign: (String) -> Unit) {
        appScope.launch {
            userKey.collect { assign(it.ifBlank { fallback }) }
        }
    }
}
