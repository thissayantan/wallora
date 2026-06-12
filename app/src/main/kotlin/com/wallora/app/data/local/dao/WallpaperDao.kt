package com.wallora.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wallora.app.data.local.entity.WallpaperEntity

@Dao
interface WallpaperDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallpapers: List<WallpaperEntity>)

    @Query("SELECT * FROM wallpaper_cache WHERE cacheKey = :key AND cachedAt > :minTimestamp ORDER BY rowid")
    suspend fun getByCacheKey(key: String, minTimestamp: Long): List<WallpaperEntity>

    @Query("SELECT * FROM wallpaper_cache WHERE globalKey = :globalKey LIMIT 1")
    suspend fun getByGlobalKey(globalKey: String): WallpaperEntity?

    @Query("DELETE FROM wallpaper_cache WHERE cachedAt <= :cutoffTimestamp")
    suspend fun deleteExpired(cutoffTimestamp: Long)

    @Query("DELETE FROM wallpaper_cache")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM wallpaper_cache")
    suspend fun count(): Int
}
