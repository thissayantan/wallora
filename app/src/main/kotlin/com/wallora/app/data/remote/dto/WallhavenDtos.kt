package com.wallora.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WallhavenResponse(
    val data: List<WallhavenWallpaper> = emptyList(),
    val meta: WallhavenMeta? = null,
)

@Serializable
data class WallhavenWallpaper(
    val id: String = "",
    val url: String = "",                // source page URL (wallhaven.cc/w/…)
    @SerialName("short_url") val shortUrl: String = "",
    val views: Int = 0,
    val favorites: Int = 0,
    val source: String = "",
    val purity: String = "sfw",
    val category: String = "general",
    @SerialName("dimension_x") val dimensionX: Int = 0,
    @SerialName("dimension_y") val dimensionY: Int = 0,
    val resolution: String = "",
    val ratio: String = "",
    @SerialName("file_size") val fileSize: Long = 0L,
    @SerialName("file_type") val fileType: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val colors: List<String> = emptyList(),
    val path: String = "",               // full-res URL
    val thumbs: WallhavenThumbs = WallhavenThumbs(),
    val tags: List<WallhavenTag> = emptyList(),
    val uploader: WallhavenUploader? = null,
)

@Serializable
data class WallhavenThumbs(
    val large: String = "",
    val original: String = "",
    val small: String = "",
)

@Serializable
data class WallhavenTag(
    val id: Int = 0,
    val name: String = "",
    val alias: String = "",
    val category: String = "",
    val purity: String = "",
)

@Serializable
data class WallhavenUploader(
    val username: String = "",
    @SerialName("group") val group: String = "",
    val avatar: WallhavenAvatar? = null,
)

@Serializable
data class WallhavenAvatar(
    @SerialName("200px") val px200: String = "",
)

@Serializable
data class WallhavenMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val per_page: Int = 24,
    val total: Int = 0,
    val query: String? = null,
    @SerialName("seed") val seed: String? = null,
)
