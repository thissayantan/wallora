package com.wallora.app.data.remote.api

import com.wallora.app.data.remote.dto.WallhavenResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApi {

    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String = "",
        @Query("categories") categories: String = "100",
        @Query("purity") purity: String = "100",
        @Query("atleast") atleast: String = "1080x1920",
        @Query("ratios") ratios: String = "portrait",    // portrait-only (9x16, 10x16, 3x4 etc.)
        @Query("sorting") sorting: String = "toplist",   // toplist → highest-rated content
        @Query("topRange") topRange: String = "1y",       // past year — wider variety than 6M
        @Query("order") order: String = "desc",
        @Query("page") page: Int = 1,
    ): WallhavenResponse

    companion object {
        const val PAGE_SIZE = 24
    }
}
