package com.wallora.app.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

/**
 * Normalized wallpaper domain model — source-agnostic.
 *
 * [Parcelable] so it can be passed through Navigation saved-state when opening the detail screen.
 *
 * [Stable]: all properties are vals; equal instances produce identical UI output (data class
 * equality is correct). [tags] is a List<String> which Compose can't infer as stable on its own,
 * so we declare stability explicitly. The list is effectively immutable for a given Wallpaper
 * instance (tags come from the API and never change after construction).
 */
@Stable
@Parcelize
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
) : Parcelable {
    /** Stable unique key across all sources for deduplication. */
    val globalKey: String get() = "${sourceId.name}:$id"
}
