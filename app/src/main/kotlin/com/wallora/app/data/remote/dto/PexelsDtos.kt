package com.wallora.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PexelsListResponse(
    val photos: List<PexelsPhoto> = emptyList(),
    @SerialName("next_page") val nextPage: String? = null,
    @SerialName("total_results") val totalResults: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 15,
)

@Serializable
data class PexelsSearchResponse(
    val photos: List<PexelsPhoto> = emptyList(),
    @SerialName("next_page") val nextPage: String? = null,
    @SerialName("total_results") val totalResults: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 15,
)

@Serializable
data class PexelsPhoto(
    val id: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val url: String = "",                    // source page URL
    @SerialName("avg_color") val avgColor: String? = null,
    val photographer: String = "",
    @SerialName("photographer_url") val photographerUrl: String = "",
    @SerialName("photographer_id") val photographerId: Int = 0,
    val src: PexelsSrc = PexelsSrc(),
    val alt: String = "",
)

@Serializable
data class PexelsSrc(
    val original: String = "",
    val large2x: String = "",
    val large: String = "",
    val medium: String = "",
    val small: String = "",
    val portrait: String = "",
    val landscape: String = "",
    val tiny: String = "",
)
