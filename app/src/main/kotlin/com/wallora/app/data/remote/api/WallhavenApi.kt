package com.wallora.app.data.remote.api

import com.wallora.app.data.remote.dto.WallhavenResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApi {

    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String = "",
        @Query("categories") categories: String = "100", // 1=general,0=anime,0=people → "100"
        @Query("purity") purity: String = "100",         // 1=sfw, 0=sketchy, 0=nsfw → "100"
        @Query("atleast") atleast: String = "1080x1920", // portrait HD
        @Query("sorting") sorting: String = "relevance",
        @Query("order") order: String = "desc",
        @Query("page") page: Int = 1,
    ): WallhavenResponse

    companion object {
        const val PAGE_SIZE = 24
    }
}
