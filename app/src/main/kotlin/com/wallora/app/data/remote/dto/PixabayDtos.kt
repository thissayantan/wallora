package com.wallora.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PixabayResponse(
    val total: Long = 0L,
    val totalHits: Long = 0L,
    val hits: List<PixabayHit> = emptyList(),
)

@Serializable
data class PixabayHit(
    val id: Long = 0L,
    val tags: String = "",
    val webformatURL: String = "",   // ~640 px wide — thumb
    val largeImageURL: String = "",  // ~1280 px wide — full
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val user: String = "",
    val pageURL: String = "",
)
