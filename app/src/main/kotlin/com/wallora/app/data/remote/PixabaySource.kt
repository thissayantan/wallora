package com.wallora.app.data.remote

import com.wallora.app.data.remote.api.PixabayApi
import com.wallora.app.data.remote.dto.PixabayHit
import com.wallora.app.di.UserKeyCache
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PixabaySource @Inject constructor(
    private val api: PixabayApi,
    private val userKeyCache: UserKeyCache,
) : WallpaperSource {

    override val id: SourceId = SourceId.PIXABAY
    override val isConfigured: Boolean get() = userKeyCache.effectivePixabayKey.isNotBlank()

    override suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        val query = if (categories.isEmpty()) "wallpaper"
                    else categories.joinToString(" ") { it.pixabayQuery }
        return fetchPage(query, pageNum)
    }

    override suspend fun search(query: String, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        return fetchPage(query, pageNum)
    }

    private suspend fun fetchPage(query: String, pageNum: Int): Page<Wallpaper> {
        val key = userKeyCache.effectivePixabayKey
        val resp = api.search(key = key, query = query, page = pageNum)
        val items = resp.hits
            .filter { it.isPortraitCompatible() }
            .map { it.toDomain() }
        val hasMore = (pageNum.toLong() * PixabayApi.PAGE_SIZE) < resp.totalHits
        return Page(
            items = items,
            nextPage = if (hasMore) (pageNum + 1).toString() else null,
        )
    }
}

/** Pixabay's orientation=vertical filter can return near-square images; enforce aspect ratio. */
private fun PixabayHit.isPortraitCompatible(): Boolean {
    if (imageWidth <= 0 || imageHeight <= 0) return true // no data → let it through
    return imageHeight.toFloat() / imageWidth.toFloat() >= 0.7f
}

internal fun PixabayHit.toDomain(): Wallpaper = Wallpaper(
    id = id.toString(),
    sourceId = SourceId.PIXABAY,
    thumbUrl = webformatURL,
    fullUrl = largeImageURL.ifBlank { webformatURL },
    width = imageWidth,
    height = imageHeight,
    author = user,
    authorUrl = "", // Pixabay doesn't provide a direct user profile URL in the free API
    sourcePageUrl = pageURL,
    tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
)
