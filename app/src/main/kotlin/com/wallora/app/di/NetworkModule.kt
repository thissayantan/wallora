package com.wallora.app.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wallora.app.BuildConfig
import com.wallora.app.data.remote.interceptor.ThrottleInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private const val TIMEOUT_SECONDS = 30L

/** Hilt qualifier names for per-source Retrofit instances. */
const val RETROFIT_PEXELS = "pexels"
const val RETROFIT_WALLHAVEN = "wallhaven"
const val RETROFIT_REDDIT = "reddit"
const val RETROFIT_UNSPLASH = "unsplash"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Shared OkHttpClient for full-res image downloads.
     * 150 MB disk cache so subsequent rotations to the same image are instant.
     * A network interceptor forces Cache-Control: max-age=86400 on responses whose
     * servers don't set cache headers (some CDNs omit them on direct URL hits).
     */
    @Singleton
    @Provides
    fun provideDownloadClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cache(Cache(File(context.cacheDir, "wallpaper_http_cache"), 150L * 1024L * 1024L))
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Honour server headers; only inject if missing
                if (response.header("Cache-Control") == null) {
                    response.newBuilder()
                        .header("Cache-Control", "max-age=86400")
                        .build()
                } else response
            }
            .build()

    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun buildLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }

    private fun baseClientBuilder(throttleMs: Long = 500L): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(buildLoggingInterceptor())
            .addInterceptor(ThrottleInterceptor(throttleMs))

    @Singleton
    @Provides
    @Named(RETROFIT_PEXELS)
    fun providePexelsRetrofit(json: Json): Retrofit {
        val client = baseClientBuilder(throttleMs = 500L)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", BuildConfig.PEXELS_API_KEY)
                    .build()
                chain.proceed(request)
            }.build()
        return Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Singleton
    @Provides
    @Named(RETROFIT_WALLHAVEN)
    fun provideWallhavenRetrofit(json: Json): Retrofit {
        val client = baseClientBuilder(throttleMs = 1_000L)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    if (BuildConfig.WALLHAVEN_API_KEY.isNotBlank()) {
                        header("X-API-Key", BuildConfig.WALLHAVEN_API_KEY)
                    }
                }.build()
                chain.proceed(req)
            }.build()
        return Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Singleton
    @Provides
    @Named(RETROFIT_REDDIT)
    fun provideRedditRetrofit(json: Json): Retrofit {
        // Reddit requires a distinctive User-Agent to avoid 403 from automated request detection
        val client = baseClientBuilder(throttleMs = 2_000L) // polite for Reddit
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "android:com.wallora.app:v1.0 (by /u/wallora_app)")
                    .build()
                chain.proceed(req)
            }.build()
        return Retrofit.Builder()
            .baseUrl("https://www.reddit.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Singleton
    @Provides
    @Named(RETROFIT_UNSPLASH)
    fun provideUnsplashRetrofit(json: Json): Retrofit {
        val client = baseClientBuilder(throttleMs = 1_200L) // Unsplash 50 req/hr free tier
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept-Version", "v1")
                    .header("Authorization", "Client-ID ${BuildConfig.UNSPLASH_ACCESS_KEY}")
                    .build()
                chain.proceed(req)
            }.build()
        return Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
