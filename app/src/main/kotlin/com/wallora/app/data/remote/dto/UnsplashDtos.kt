package com.wallora.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnsplashPhoto(
    val id: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val color: String? = null,
    val description: String? = null,
    @SerialName("alt_description") val altDescription: String? = null,
    val urls: UnsplashUrls = UnsplashUrls(),
    val links: UnsplashLinks = UnsplashLinks(),
    val user: UnsplashUser = UnsplashUser(),
    val tags: List<UnsplashTag> = emptyList(),
)

@Serializable
data class UnsplashUrls(
    val raw: String = "",
    val full: String = "",
    val regular: String = "",
    val small: String = "",
    val thumb: String = "",
)

@Serializable
data class UnsplashLinks(
    val self: String = "",
    val html: String = "",
    @SerialName("download") val download: String = "",
    @SerialName("download_location") val downloadLocation: String = "",
)

@Serializable
data class UnsplashUser(
    val id: String = "",
    val username: String = "",
    val name: String = "",
    val links: UnsplashUserLinks = UnsplashUserLinks(),
)

@Serializable
data class UnsplashUserLinks(
    val html: String = "",
)

@Serializable
data class UnsplashTag(
    val type: String = "",
    val title: String = "",
)

@Serializable
data class UnsplashSearchResponse(
    val total: Long = 0L,  // defensive: could exceed Int.MAX on broad queries
    @SerialName("total_pages") val totalPages: Int = 0,
    val results: List<UnsplashPhoto> = emptyList(),
)
