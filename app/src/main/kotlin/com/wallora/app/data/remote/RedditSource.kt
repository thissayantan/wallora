package com.wallora.app.data.remote

import com.wallora.app.data.remote.api.RedditApi
import com.wallora.app.data.remote.dto.RedditPostData
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditSource @Inject constructor(
    private val api: RedditApi,
) : WallpaperSource {

    override val id: SourceId = SourceId.REDDIT
    override val isConfigured: Boolean = true // No key needed for SFW public listing

    override suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper> {
        val subs = if (categories.isEmpty()) {
            listOf("wallpaper", "wallpapers")
        } else {
            categories.flatMap { it.subreddits }.distinct()
        }
        // Multi-reddit notation: r/sub1+sub2 — fetch the first combo of 3 subreddits
        val subreddit = subs.take(3).joinToString("+")
        val after = page.takeIf { it != "1" }
        val resp = api.getHot(subreddit, after = after)
        val wallpapers = resp.data.children
            .map { it.data }
            .filter { isDirectImagePost(it) }
            .mapNotNull { it.toDomain(categories.firstOrNull()) }
        return Page(
            items = wallpapers,
            nextPage = resp.data.after,
        )
    }

    override suspend fun search(query: String, page: String): Page<Wallpaper> {
        val subreddit = "wallpaper+wallpapers+EarthPorn"
        val after = page.takeIf { it != "1" }
        val resp = api.search(subreddit, query, after = after)
        val wallpapers = resp.data.children
            .map { it.data }
            .filter { isDirectImagePost(it) }
            .mapNotNull { it.toDomain(null) }
        return Page(
            items = wallpapers,
            nextPage = resp.data.after,
        )
    }
}

/**
 * Predicate: only include direct image posts (i.redd.it or direct imgur).
 * Excludes: galleries, videos, NSFW, reddit text posts, external links.
 */
internal fun isDirectImagePost(post: RedditPostData): Boolean {
    if (post.over18) return false
    if (post.isVideo) return false
    if (post.isGallery == true) return false

    val url = post.urlOverridden ?: post.url
    val lc = url.lowercase()
    return lc.matches(Regex(".*\\.(jpg|jpeg|png|webp)(\\?.*)?$")) &&
        (lc.contains("i.redd.it") || lc.contains("i.imgur.com") || lc.contains("imgur.com/"))
}

internal fun RedditPostData.toDomain(category: Category?): Wallpaper? {
    val imageUrl = urlOverridden ?: url
    val preview = preview?.images?.firstOrNull()
    val source = preview?.source
    val thumbUrl = preview?.resolutions?.lastOrNull { it.width <= 640 }?.url?.unescapeUrl()
        ?: source?.url?.unescapeUrl()
        ?: imageUrl
    val width = source?.width ?: 1080
    val height = source?.height ?: 1920
    if (imageUrl.isBlank()) return null
    return Wallpaper(
        id = id,
        sourceId = SourceId.REDDIT,
        thumbUrl = thumbUrl,
        fullUrl = imageUrl,
        width = width,
        height = height,
        author = author,
        authorUrl = "https://www.reddit.com/u/$author",
        sourcePageUrl = "https://www.reddit.com$permalink",
        colorHint = null,
        category = category,
        tags = listOf(subreddit),
    )
}

/** Reddit HTML-encodes preview URLs with &amp; → fix that. */
private fun String.unescapeUrl(): String = replace("&amp;", "&")
