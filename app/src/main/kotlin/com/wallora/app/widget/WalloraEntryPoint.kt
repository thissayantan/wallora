package com.wallora.app.widget

import com.wallora.app.di.ApplicationScope
import com.wallora.app.domain.usecase.NextWallpaperUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * Hilt entry point for Glance widget callbacks.
 *
 * Glance [ActionCallback] instances are not Hilt-injected, so dependencies must be
 * retrieved manually via [dagger.hilt.android.EntryPointAccessors.fromApplication].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WalloraEntryPoint {
    fun nextWallpaperUseCase(): NextWallpaperUseCase

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
