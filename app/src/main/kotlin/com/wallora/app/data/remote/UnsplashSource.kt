package com.wallora.app.data.remote

import com.wallora.app.BuildConfig
import com.wallora.app.data.remote.api.UnsplashApi
import com.wallora.app.data.remote.dto.UnsplashPhoto
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsplashSource @Inject constructor(
    private val api: UnsplashApi,
) : WallpaperSource {

    override val id: SourceId = SourceId.UNSPLASH
    override val isConfigured: Boolean get() = BuildConfig.UNSPLASH_ACCESS_KEY.isNotBlank()

    override suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        return if (categories.isEmpty()) {
            val photos = api.getPhotos(page = pageNum)
            Page(
                items = photos.map { it.toDomain(null) },
                nextPage = if (photos.size < UnsplashApi.PAGE_SIZE) null else (pageNum + 1).toString(),
            )
        } else {
            val query = categories.joinToString(" ") { it.unsplashQuery }
            val resp = api.search(query, page = pageNum)
            Page(
                items = resp.results.map { it.toDomain(categories.firstOrNull()) },
                nextPage = if (pageNum >= resp.totalPages) null else (pageNum + 1).toString(),
            )
        }
    }

    override suspend fun search(query: String, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        val resp = api.search(query, page = pageNum)
        return Page(
            items = resp.results.map { it.toDomain(null) },
            nextPage = if (pageNum >= resp.totalPages) null else (pageNum + 1).toString(),
        )
    }

    /**
     * Must be called whenever a user downloads/applies an Unsplash wallpaper.
     * Per Unsplash API guidelines this is required.
     */
    suspend fun trackDownload(downloadLocation: String) {
        try {
            api.trackDownload(downloadLocation).close()
        } catch (_: Exception) {
            // tracking failure must not block the user action
        }
    }
}

internal fun UnsplashPhoto.toDomain(category: Category?): Wallpaper {
    val colorHint = color?.removePrefix("#")?.toLongOrNull(16)?.toInt()
    return Wallpaper(
        id = id,
        sourceId = SourceId.UNSPLASH,
        thumbUrl = urls.small.ifBlank { urls.thumb },
        fullUrl = urls.full.ifBlank { urls.regular },
        width = width,
        height = height,
        author = user.name.ifBlank { user.username },
        authorUrl = user.links.html,
        sourcePageUrl = links.html,
        colorHint = colorHint,
        category = category,
        tags = tags.map { it.title },
    )
}
