package com.wallora.app.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wallora.app.data.local.dao.FavoriteDao
import com.wallora.app.data.local.dao.HistoryDao
import com.wallora.app.data.local.dao.WallpaperDao
import com.wallora.app.data.local.entity.FavoriteEntity
import com.wallora.app.data.local.entity.HistoryEntity
import com.wallora.app.data.paging.MultiSourcePagingSource
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards WallpaperSource>,
    private val wallpaperDao: WallpaperDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val settingsRepository: SettingsRepository,
) {

    companion object {
        private const val TAG = "WallpaperRepository"
        private const val PAGE_SIZE = 20
        private const val CACHE_TTL_MS = 3_600_000L // 1 hour
    }

    /** Browse wallpapers by categories — returns a Paging 3 flow. */
    fun browse(
        categories: List<Category>,
        enabledSources: Set<SourceId>,
    ): Flow<PagingData<Wallpaper>> {
        val activeSources = sources.filter { it.isConfigured && it.id in enabledSources }
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2,
            ),
            pagingSourceFactory = {
                MultiSourcePagingSource(
                    sources = activeSources,
                    categories = categories,
                    query = null,
                    wallpaperDao = wallpaperDao,
                    cacheTtlMs = CACHE_TTL_MS,
                )
            },
        ).flow
    }

    /** Search across all enabled sources — returns a Paging 3 flow. */
    fun search(
        query: String,
        enabledSources: Set<SourceId>,
    ): Flow<PagingData<Wallpaper>> {
        val activeSources = sources.filter { it.isConfigured && it.id in enabledSources }
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                MultiSourcePagingSource(
                    sources = activeSources,
                    categories = emptyList(),
                    query = query,
                    wallpaperDao = wallpaperDao,
                    cacheTtlMs = CACHE_TTL_MS,
                )
            },
        ).flow
    }

    // ── Favorites ────────────────────────────────────────────────────────────

    fun observeFavorites(): Flow<List<Wallpaper>> =
        favoriteDao.observeAll().map { list ->
            list.map { entity ->
                Wallpaper(
                    id = entity.id,
                    sourceId = SourceId.valueOf(entity.sourceId),
                    thumbUrl = entity.thumbUrl,
                    fullUrl = entity.fullUrl,
                    width = entity.width,
                    height = entity.height,
                    author = entity.author,
                    authorUrl = entity.authorUrl,
                    sourcePageUrl = entity.sourcePageUrl,
                    colorHint = entity.colorHint,
                )
            }
        }

    fun observeIsFavorite(globalKey: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(globalKey)

    suspend fun addFavorite(wallpaper: Wallpaper) {
        favoriteDao.insert(
            FavoriteEntity(
                globalKey = wallpaper.globalKey,
                sourceId = wallpaper.sourceId.name,
                id = wallpaper.id,
                thumbUrl = wallpaper.thumbUrl,
                fullUrl = wallpaper.fullUrl,
                width = wallpaper.width,
                height = wallpaper.height,
                author = wallpaper.author,
                authorUrl = wallpaper.authorUrl,
                sourcePageUrl = wallpaper.sourcePageUrl,
                colorHint = wallpaper.colorHint,
                tags = wallpaper.tags.joinToString(","),
                addedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun removeFavorite(globalKey: String) = favoriteDao.delete(globalKey)

    // ── History ──────────────────────────────────────────────────────────────

    fun observeHistory(): Flow<List<Wallpaper>> =
        historyDao.observeAll().map { list ->
            list.map { entity ->
                Wallpaper(
                    id = entity.id,
                    sourceId = SourceId.valueOf(entity.sourceId),
                    thumbUrl = entity.thumbUrl,
                    fullUrl = entity.fullUrl,
                    width = entity.width,
                    height = entity.height,
                    author = entity.author,
                    authorUrl = entity.authorUrl,
                    sourcePageUrl = entity.sourcePageUrl,
                    colorHint = entity.colorHint,
                )
            }
        }

    suspend fun addToHistory(wallpaper: Wallpaper) {
        historyDao.insert(
            HistoryEntity(
                globalKey = wallpaper.globalKey,
                sourceId = wallpaper.sourceId.name,
                id = wallpaper.id,
                thumbUrl = wallpaper.thumbUrl,
                fullUrl = wallpaper.fullUrl,
                width = wallpaper.width,
                height = wallpaper.height,
                author = wallpaper.author,
                authorUrl = wallpaper.authorUrl,
                sourcePageUrl = wallpaper.sourcePageUrl,
                colorHint = wallpaper.colorHint,
                tags = wallpaper.tags.joinToString(","),
                setAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clearHistory() = historyDao.deleteAll()

    /**
     * Return the most-recent [limit] applied wallpaper globalKeys (newest first).
     * Used by [com.wallora.app.domain.rotation.RotationEngine] for no-repeat logic.
     */
    suspend fun getRecentHistoryKeys(limit: Int): List<String> =
        historyDao.getAll()
            .sortedByDescending { it.setAt }
            .take(limit)
            .map { it.globalKey }

    /** Snapshot of all favorited wallpapers (newest first). Used by rotation playlist. */
    suspend fun getFavoritesSnapshot(): List<Wallpaper> =
        favoriteDao.getAll().sortedByDescending { it.addedAt }.map { entity ->
            Wallpaper(
                id = entity.id,
                sourceId = SourceId.valueOf(entity.sourceId),
                thumbUrl = entity.thumbUrl,
                fullUrl = entity.fullUrl,
                width = entity.width,
                height = entity.height,
                author = entity.author,
                authorUrl = entity.authorUrl,
                sourcePageUrl = entity.sourcePageUrl,
                colorHint = entity.colorHint,
            )
        }

    // ── Cache maintenance ─────────────────────────────────────────────────────

    suspend fun clearCache() {
        wallpaperDao.deleteAll()
        Log.d(TAG, "Cache cleared")
    }

    suspend fun evictExpiredCache() {
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MS
        wallpaperDao.deleteExpired(cutoff)
    }
}
