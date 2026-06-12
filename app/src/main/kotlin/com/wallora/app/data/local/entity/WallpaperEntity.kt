package com.wallora.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper

/** Cached wallpaper page entry with TTL. */
@Entity(
    tableName = "wallpaper_cache",
    indices = [Index("cacheKey"), Index("cachedAt")],
)
data class WallpaperEntity(
    @PrimaryKey val globalKey: String,      // "${sourceId}:${id}"
    val sourceId: String,                   // SourceId.name
    val id: String,
    val thumbUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val author: String,
    val authorUrl: String,
    val sourcePageUrl: String,
    val colorHint: Int?,
    val category: String?,                  // Category.name or null
    val tags: String,                       // JSON array of tags
    val cacheKey: String,                   // "sourceId:category:page" for lookup
    val cachedAt: Long,                     // System.currentTimeMillis()
) {
    fun toDomain(): Wallpaper = Wallpaper(
        id = id,
        sourceId = SourceId.valueOf(sourceId),
        thumbUrl = thumbUrl,
        fullUrl = fullUrl,
        width = width,
        height = height,
        author = author,
        authorUrl = authorUrl,
        sourcePageUrl = sourcePageUrl,
        colorHint = colorHint,
        category = category?.let { runCatching { Category.valueOf(it) }.getOrNull() },
        tags = if (tags.isEmpty()) emptyList()
               else tags.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") },
    )

    companion object {
        fun fromDomain(
            wallpaper: Wallpaper,
            cacheKey: String,
            cachedAt: Long,
        ): WallpaperEntity = WallpaperEntity(
            globalKey = wallpaper.globalKey,
            sourceId = wallpaper.sourceId.name,
            id = wallpaper.id,
            thumbUrl = wallpaper.thumbUrl,
            fullUrl = wallpaper.fullUrl,
            width = wallpaper.width,
            height = wallpaper.height,
            author = wallpaper.author,
            authorUrl = wallpaper.authorUrl,
            sourcePageUrl = wallpaper.sourcePageUrl,
            colorHint = wallpaper.colorHint,
            category = wallpaper.category?.name,
            tags = wallpaper.tags.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" },
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }
}
