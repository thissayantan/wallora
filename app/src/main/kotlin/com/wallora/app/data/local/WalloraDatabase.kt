package com.wallora.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wallora.app.data.local.dao.FavoriteDao
import com.wallora.app.data.local.dao.HistoryDao
import com.wallora.app.data.local.dao.RecentSearchDao
import com.wallora.app.data.local.dao.WallpaperDao
import com.wallora.app.data.local.entity.FavoriteEntity
import com.wallora.app.data.local.entity.HistoryEntity
import com.wallora.app.data.local.entity.RecentSearchEntity
import com.wallora.app.data.local.entity.WallpaperEntity

@Database(
    entities = [
        WallpaperEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        RecentSearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WalloraDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun recentSearchDao(): RecentSearchDao
}
