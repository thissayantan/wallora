package com.wallora.app.domain.model

/**
 * Normalized wallpaper domain model — source-agnostic.
 *
 * @param id Unique identifier within the source (e.g. Pexels photo ID, Wallhaven hash).
 * @param sourceId Which [SourceId] produced this wallpaper.
 * @param thumbUrl URL of a thumbnail suitable for grid display.
 * @param fullUrl URL of the full-resolution image.
 * @param width Full-resolution width in pixels.
 * @param height Full-resolution height in pixels.
 * @param author Display name of the photographer/creator.
 * @param authorUrl Link to the author's profile page (required for Unsplash attribution).
 * @param sourcePageUrl Link back to the original source page.
 * @param colorHint Dominant color hint as 0xAARRGGBB int, or null if unknown.
 * @param category Category this wallpaper was fetched for (may be null for search results).
 * @param tags Optional list of tags / keywords from the source API.
 */
data class Wallpaper(
    val id: String,
    val sourceId: SourceId,
    val thumbUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val author: String,
    val authorUrl: String,
    val sourcePageUrl: String,
    val colorHint: Int? = null,
    val category: Category? = null,
    val tags: List<String> = emptyList(),
) {
    /** Stable unique key across all sources for deduplication. */
    val globalKey: String get() = "${sourceId.name}:$id"
}
