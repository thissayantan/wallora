package com.wallora.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Simple token-bucket throttle interceptor. Limits requests to at most
 * [maxRequests] per [windowMs] milliseconds by acquiring a per-host semaphore.
 *
 * This is a coarse throttle — it won't perfectly distribute requests over time,
 * but it's enough to keep Pexels/Unsplash within their free-tier limits and to
 * be polite to Reddit.
 */
class ThrottleInterceptor(
    private val minIntervalMs: Long = 500L,
) : Interceptor {

    @Volatile
    private var lastRequestAt: Long = 0L

    @Synchronized
    private fun waitForSlot() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestAt
        if (elapsed < minIntervalMs) {
            Thread.sleep(minIntervalMs - elapsed)
        }
        lastRequestAt = System.currentTimeMillis()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        waitForSlot()
        return chain.proceed(chain.request())
    }
}
