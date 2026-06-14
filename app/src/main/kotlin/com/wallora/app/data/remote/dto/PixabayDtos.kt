package com.wallora.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PixabayResponse(
    val total: Long = 0L,
    @SerialName("totalHits") val totalHits: Long = 0L,
    val hits: List<PixabayHit> = emptyList(),
)

@Serializable
data class PixabayHit(
    val id: Long = 0L,
    val tags: String = "",
    @SerialName("webformatURL") val webformatURL: String = "",   // ~640 px wide — thumb
    @SerialName("largeImageURL") val largeImageURL: String = "", // ~1280 px wide — full
    @SerialName("imageWidth") val imageWidth: Int = 0,
    @SerialName("imageHeight") val imageHeight: Int = 0,
    @SerialName("user") val user: String = "",
    @SerialName("pageURL") val pageURL: String = "",
    @SerialName("userImageURL") val userImageURL: String = "",
)
