package com.wallora.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedditListingResponse(
    val kind: String = "",
    val data: RedditListingData = RedditListingData(),
)

@Serializable
data class RedditListingData(
    val after: String? = null,
    val before: String? = null,
    val dist: Int = 0,
    val children: List<RedditPost> = emptyList(),
)

@Serializable
data class RedditPost(
    val kind: String = "",
    val data: RedditPostData = RedditPostData(),
)

@Serializable
data class RedditPostData(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val subreddit: String = "",
    val permalink: String = "",
    val url: String = "",
    @SerialName("url_overridden_by_dest") val urlOverridden: String? = null,
    @SerialName("post_hint") val postHint: String? = null,
    @SerialName("over_18") val over18: Boolean = false,
    @SerialName("is_video") val isVideo: Boolean = false,
    @SerialName("is_gallery") val isGallery: Boolean? = false,
    @SerialName("domain") val domain: String = "",
    val preview: RedditPreview? = null,
    @SerialName("score") val score: Int = 0,
    @SerialName("ups") val ups: Int = 0,
)

@Serializable
data class RedditPreview(
    val images: List<RedditPreviewImage> = emptyList(),
    val enabled: Boolean = true,
)

@Serializable
data class RedditPreviewImage(
    val source: RedditPreviewResolution = RedditPreviewResolution(),
    val resolutions: List<RedditPreviewResolution> = emptyList(),
    val id: String = "",
)

@Serializable
data class RedditPreviewResolution(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)
