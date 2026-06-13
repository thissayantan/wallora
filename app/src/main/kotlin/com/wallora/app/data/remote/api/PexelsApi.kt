package com.wallora.app.data.remote.api

import com.wallora.app.data.remote.dto.PexelsListResponse
import com.wallora.app.data.remote.dto.PexelsSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PexelsApi {

    /** Curated photos — used for browsing without a specific query. */
    @GET("v1/curated")
    suspend fun getCurated(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = PAGE_SIZE,
        @Query("orientation") orientation: String = "portrait",
    ): PexelsListResponse

    /** Keyword search. */
    @GET("v1/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = PAGE_SIZE,
        @Query("orientation") orientation: String = "portrait",
    ): PexelsSearchResponse

    companion object {
        const val PAGE_SIZE = 20
    }
}
