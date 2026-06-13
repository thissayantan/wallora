package com.wallora.app.data.remote.api

import com.wallora.app.data.remote.dto.RedditListingResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditApi {

    @GET("r/{subreddit}/top.json")
    suspend fun getTop(
        @Path("subreddit") subreddit: String,
        @Query("t") timeRange: String = "month",   // top posts of the past month
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("after") after: String? = null,
    ): RedditListingResponse

    @GET("r/{subreddit}/search.json")
    suspend fun search(
        @Path("subreddit") subreddit: String,
        @Query("q") query: String,
        @Query("restrict_sr") restrictSr: Boolean = true,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("after") after: String? = null,
        @Query("sort") sort: String = "relevance",
    ): RedditListingResponse

    companion object {
        const val PAGE_SIZE = 25
    }
}
