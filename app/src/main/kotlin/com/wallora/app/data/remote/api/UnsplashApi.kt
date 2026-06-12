package com.wallora.app.data.remote.api

import com.wallora.app.data.remote.dto.UnsplashPhoto
import com.wallora.app.data.remote.dto.UnsplashSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface UnsplashApi {

    @GET("photos")
    suspend fun getPhotos(
        @Query("order_by") orderBy: String = "popular",
        @Query("per_page") perPage: Int = PAGE_SIZE,
        @Query("page") page: Int = 1,
    ): List<UnsplashPhoto>

    @GET("search/photos")
    suspend fun search(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = PAGE_SIZE,
        @Query("page") page: Int = 1,
        @Query("orientation") orientation: String = "portrait",
    ): UnsplashSearchResponse

    /** Ping the download tracking endpoint (required by Unsplash API terms). */
    @GET
    suspend fun trackDownload(@Url downloadLocation: String): okhttp3.ResponseBody

    companion object {
        const val PAGE_SIZE = 20
    }
}
