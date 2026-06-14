package com.wallora.app.data.remote.api

import com.wallora.app.data.remote.dto.PixabayResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApi {

    /**
     * Pixabay image search. The API key is passed as a query param (not a header).
     * orientation=vertical + min_height/min_width guarantee portrait-format results.
     * safesearch=true enforces SFW content.
     */
    @GET("api/")
    suspend fun search(
        @Query("key") key: String,
        @Query("q") query: String,
        @Query("image_type") imageType: String = "photo",
        @Query("orientation") orientation: String = "vertical",
        @Query("order") order: String = "popular",
        @Query("safesearch") safesearch: Boolean = true,
        @Query("min_width") minWidth: Int = 1080,
        @Query("min_height") minHeight: Int = 1920,
        @Query("per_page") perPage: Int = PAGE_SIZE,
        @Query("page") page: Int = 1,
    ): PixabayResponse

    companion object {
        const val PAGE_SIZE = 20
    }
}
