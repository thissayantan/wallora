package com.wallora.app.domain

import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Page
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper

/**
 * Pluggable wallpaper source interface. All sources produce the same [Wallpaper] model.
 *
 * Sources should fail soft: throw exceptions rather than returning partial results, so
 * the repository can catch them per-source without blanking the whole grid.
 */
interface WallpaperSource {
    val id: SourceId

    /**
     * Whether this source is configured (API key present, etc.). If false, the source
     * should be visually disabled in Settings with a "key missing" hint.
     */
    val isConfigured: Boolean

    /**
     * Browse wallpapers by category.
     *
     * @param categories Selected categories (OR-combined). Empty = all/general.
     * @param page Page cursor (use "1" for the first page; returns [Page.nextPage] for subsequent).
     */
    suspend fun browse(categories: List<Category>, page: String): Page<Wallpaper>

    /**
     * Keyword search.
     *
     * @param query Search terms.
     * @param page Page cursor.
     */
    suspend fun search(query: String, page: String): Page<Wallpaper>
}
