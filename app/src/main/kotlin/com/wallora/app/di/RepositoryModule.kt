package com.wallora.app.di

import com.wallora.app.data.repository.WallpaperRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** WallpaperRepository is @Singleton-annotated and injected directly; no binding needed. */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
