package com.wallora.app.data.remote

import com.wallora.app.data.remote.api.RedditApi
import com.wallora.app.data.remote.dto.RedditPostData
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditSource @Inject constructor(
    private val api: RedditApi,
    private val settingsRepository: SettingsRepository,
) : WallpaperSource {

    override val id: SourceId = SourceId.REDDIT
    override val isConfigured: Boolean = true

    override suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper> {
        val after = page.takeIf { it != "1" }
        val subs = if (categories.isEmpty()) {
            // Use user-configured subreddit pool
            settingsRepository.userSubreddits.first()
                .ifEmpty { SettingsRepository.DEFAULT_SUBREDDITS }
        } else {
            categories.flatMap { it.subreddits }.distinct()
        }
        // Multi-reddit notation: r/sub1+sub2 — up to 5 combined
        val subreddit = subs.take(5).joinToString("+")
        val resp = api.getTop(subreddit, after = after)
        val wallpapers = resp.data.children
            .map { it.data }
            .filter { isDirectImagePost(it) && isPortraitCompatible(it) }
            .mapNotNull { it.toDomain(categories.firstOrNull()) }
        return Page(items = wallpapers, nextPage = resp.data.after)
    }

    override suspend fun search(query: String, page: String): Page<Wallpaper> {
        val subreddit = settingsRepository.userSubreddits.first()
            .ifEmpty { SettingsRepository.DEFAULT_SUBREDDITS }
            .take(5).joinToString("+")
        val after = page.takeIf { it != "1" }
        val resp = api.search(subreddit, query, after = after)
        val wallpapers = resp.data.children
            .map { it.data }
            .filter { isDirectImagePost(it) && isPortraitCompatible(it) }
            .mapNotNull { it.toDomain(null) }
        return Page(items = wallpapers, nextPage = resp.data.after)
    }
}

internal fun isDirectImagePost(post: RedditPostData): Boolean {
    if (post.over18) return false
    if (post.isVideo) return false
    if (post.isGallery == true) return false

    val url = post.urlOverridden ?: post.url
    val lc = url.lowercase()
    return lc.matches(Regex(".*\\.(jpg|jpeg|png|webp)(\\?.*)?$")) &&
        (lc.contains("i.redd.it") || lc.contains("i.imgur.com") || lc.contains("imgur.com/"))
}

/** Only include images that are reasonably portrait-compatible (not ultra-wide). */
internal fun isPortraitCompatible(post: RedditPostData): Boolean {
    val preview = post.preview?.images?.firstOrNull()
    val source = preview?.source ?: return true // no metadata → allow
    val w = source.width
    val h = source.height
    if (w <= 0 || h <= 0) return true
    // Accept images where height is at least 70% of width (portrait or square-ish)
    return h.toFloat() / w.toFloat() >= 0.7f
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

private fun String.unescapeUrl(): String = replace("&amp;", "&")
