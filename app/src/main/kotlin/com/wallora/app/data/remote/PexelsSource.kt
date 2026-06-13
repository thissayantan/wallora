package com.wallora.app.data.remote

import com.wallora.app.data.remote.api.PexelsApi
import com.wallora.app.data.remote.dto.PexelsPhoto
import com.wallora.app.di.UserKeyCache
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PexelsSource @Inject constructor(
    private val api: PexelsApi,
    private val userKeyCache: UserKeyCache,
) : WallpaperSource {

    override val id: SourceId = SourceId.PEXELS
    override val isConfigured: Boolean get() = userKeyCache.effectivePexelsKey.isNotBlank()

    override suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        return if (categories.isEmpty()) {
            val resp = api.getCurated(pageNum)
            Page(
                items = resp.photos.map { it.toDomain(null) },
                nextPage = if (resp.photos.size < PexelsApi.PAGE_SIZE) null else (pageNum + 1).toString(),
            )
        } else {
            val query = categories.joinToString(" OR ") { it.pexelsQuery }
            val resp = api.search(query, pageNum)
            Page(
                items = resp.photos.map { it.toDomain(categories.firstOrNull()) },
                nextPage = if (resp.photos.size < PexelsApi.PAGE_SIZE) null else (pageNum + 1).toString(),
            )
        }
    }

    override suspend fun search(query: String, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        val resp = api.search(query, pageNum)
        return Page(
            items = resp.photos.map { it.toDomain(null) },
            nextPage = if (resp.photos.size < PexelsApi.PAGE_SIZE) null else (pageNum + 1).toString(),
        )
    }
}

internal fun PexelsPhoto.toDomain(category: Category?): Wallpaper {
    val colorHint = avgColor?.removePrefix("#")?.toLongOrNull(16)?.toInt()
    // large2x is ~1920px wide — sufficient for any phone screen and ~5× smaller than original
    return Wallpaper(
        id = id.toString(),
        sourceId = SourceId.PEXELS,
        thumbUrl = src.medium.ifBlank { src.small },
        fullUrl = src.large2x.ifBlank { src.original },
        width = width,
        height = height,
        author = photographer,
        authorUrl = photographerUrl,
        sourcePageUrl = url,
        colorHint = colorHint,
        category = category,
        tags = if (alt.isNotBlank()) listOf(alt) else emptyList(),
    )
}
