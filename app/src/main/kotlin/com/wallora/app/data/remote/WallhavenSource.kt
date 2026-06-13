package com.wallora.app.data.remote

import com.wallora.app.data.remote.api.WallhavenApi
import com.wallora.app.di.UserKeyCache
import com.wallora.app.data.remote.dto.WallhavenWallpaper
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallhavenSource @Inject constructor(
    private val api: WallhavenApi,
    private val userKeyCache: UserKeyCache,
) : WallpaperSource {

    override val id: SourceId = SourceId.WALLHAVEN
    // Wallhaven works without a key for SFW content; a user key unlocks more content
    override val isConfigured: Boolean = true

    override suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        val (query, catFlags) = buildQueryAndFlags(categories)
        val resp = api.search(
            query = query,
            categories = catFlags,
            page = pageNum,
        )
        val meta = resp.meta
        val hasMore = meta != null && meta.currentPage < meta.lastPage
        return Page(
            items = resp.data.filter { it.purity == "sfw" }.map { it.toDomain(categories.firstOrNull()) },
            nextPage = if (hasMore) (pageNum + 1).toString() else null,
        )
    }

    override suspend fun search(query: String, page: String): Page<Wallpaper> {
        val pageNum = page.toIntOrNull() ?: 1
        val resp = api.search(query = query, categories = "111", page = pageNum)
        val meta = resp.meta
        val hasMore = meta != null && meta.currentPage < meta.lastPage
        return Page(
            items = resp.data.filter { it.purity == "sfw" }.map { it.toDomain(null) },
            nextPage = if (hasMore) (pageNum + 1).toString() else null,
        )
    }

    private fun buildQueryAndFlags(categories: List<Category>): Pair<String, String> {
        if (categories.isEmpty()) return "" to "100" // general
        val queries = categories.map { it.wallhavenQuery }.distinct().joinToString(" ")
        // Use the most specific catFlags from the selected categories
        val hasAnime = categories.any { it.wallhavenCategories.contains("1") && it == Category.ANIME }
        val catFlags = if (hasAnime) "011" else "100"
        return queries to catFlags
    }
}

internal fun WallhavenWallpaper.toDomain(category: Category?): Wallpaper {
    val colorHint = colors.firstOrNull()?.removePrefix("#")?.toLongOrNull(16)?.toInt()
    return Wallpaper(
        id = id,
        sourceId = SourceId.WALLHAVEN,
        thumbUrl = thumbs.large.ifBlank { thumbs.original },
        fullUrl = path,
        width = dimensionX,
        height = dimensionY,
        author = uploader?.username ?: "Wallhaven",
        authorUrl = "https://wallhaven.cc/user/${uploader?.username ?: ""}",
        sourcePageUrl = url,
        colorHint = colorHint,
        category = category,
        tags = tags.map { it.name },
    )
}
