package com.wallora.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wallora.app.BuildConfig
import com.wallora.app.data.remote.interceptor.ThrottleInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
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
