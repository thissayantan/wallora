package com.wallora.app.domain.model

/**
 * A single page of results from a [WallpaperSource].
 *
 * @param items The wallpapers on this page.
 * @param nextPage Opaque cursor for the next page, or null if this is the last page.
 *   For integer-paged APIs this is just the next page number as a String.
 *   For cursor-based APIs (e.g. Reddit) it is the `after` token.
 */
data class Page<T>(
    val items: List<T>,
    val nextPage: String?,
)
