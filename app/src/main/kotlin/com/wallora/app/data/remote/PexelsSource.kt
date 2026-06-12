package com.wallora.app.data.remote

import com.wallora.app.BuildConfig
import com.wallora.app.data.remote.api.PexelsApi
import com.wallora.app.data.remote.dto.PexelsPhoto
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
) : WallpaperSource {

    override val id: SourceId = SourceId.PEXELS
    override val isConfigured: Boolean get() = BuildConfig.PEXELS_API_KEY.isNotBlank()

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

/** Convert a Pexels photo DTO to the domain [Wallpaper] model. */
internal fun PexelsPhoto.toDomain(category: Category?): Wallpaper {
    val colorHint = avgColor?.removePrefix("#")?.toLongOrNull(16)?.toInt()
    return Wallpaper(
        id = id.toString(),
        sourceId = SourceId.PEXELS,
        thumbUrl = src.medium.ifBlank { src.small },
        fullUrl = src.original.ifBlank { src.large2x },
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
