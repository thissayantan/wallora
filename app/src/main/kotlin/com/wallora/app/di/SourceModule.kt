package com.wallora.app.di

import com.wallora.app.data.remote.PexelsSource
import com.wallora.app.data.remote.WallhavenSource
import com.wallora.app.data.remote.RedditSource
import com.wallora.app.data.remote.UnsplashSource
import com.wallora.app.data.remote.api.PexelsApi
import com.wallora.app.data.remote.api.WallhavenApi
import com.wallora.app.data.remote.api.RedditApi
import com.wallora.app.data.remote.api.UnsplashApi
import com.wallora.app.domain.WallpaperSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SourceModule {

    @Provides
    @Singleton
    fun providePexelsApi(@Named(RETROFIT_PEXELS) retrofit: Retrofit): PexelsApi =
        retrofit.create(PexelsApi::class.java)

    @Provides
    @Singleton
    fun provideWallhavenApi(@Named(RETROFIT_WALLHAVEN) retrofit: Retrofit): WallhavenApi =
        retrofit.create(WallhavenApi::class.java)

    @Provides
    @Singleton
    fun provideRedditApi(@Named(RETROFIT_REDDIT) retrofit: Retrofit): RedditApi =
        retrofit.create(RedditApi::class.java)

    @Provides
    @Singleton
    fun provideUnsplashApi(@Named(RETROFIT_UNSPLASH) retrofit: Retrofit): UnsplashApi =
        retrofit.create(UnsplashApi::class.java)

    // Multibinding: set of all sources so the repository can iterate them
    @Provides
    @Singleton
    @IntoSet
    fun bindPexelsSource(source: PexelsSource): WallpaperSource = source

    @Provides
    @Singleton
    @IntoSet
    fun bindWallhavenSource(source: WallhavenSource): WallpaperSource = source

    @Provides
    @Singleton
    @IntoSet
    fun bindRedditSource(source: RedditSource): WallpaperSource = source

    @Provides
    @Singleton
    @IntoSet
    fun bindUnsplashSource(source: UnsplashSource): WallpaperSource = source
}
