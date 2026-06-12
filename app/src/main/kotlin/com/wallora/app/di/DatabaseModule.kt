package com.wallora.app.di

import android.content.Context
import androidx.room.Room
import com.wallora.app.data.local.WalloraDatabase
import com.wallora.app.data.local.dao.FavoriteDao
import com.wallora.app.data.local.dao.HistoryDao
import com.wallora.app.data.local.dao.RecentSearchDao
import com.wallora.app.data.local.dao.WallpaperDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): WalloraDatabase =
        Room.databaseBuilder(context, WalloraDatabase::class.java, "wallora.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWallpaperDao(db: WalloraDatabase): WallpaperDao = db.wallpaperDao()

    @Provides
    fun provideFavoriteDao(db: WalloraDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: WalloraDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideRecentSearchDao(db: WalloraDatabase): RecentSearchDao = db.recentSearchDao()
}
