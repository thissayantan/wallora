package com.wallora.app.service.helpers

/**
 * Tracks alpha interpolation for a cross-fade transition between two bitmaps.
 *
 * Usage (per render frame):
 * ```
 * val alpha = animator.currentAlpha()        // [0..1], 0 = old bitmap, 1 = new bitmap
 * if (!animator.isComplete()) animator.advance()
 * ```
 *
 * This is deliberately free of Android/Compose dependencies so it can be unit-tested.
 */
class CrossfadeAnimator(
    /** Total duration of the cross-fade in milliseconds. */
    private val durationMs: Long = 400L,
) {
    private var startTimeMs: Long = -1L
    private var completed = false

    fun start(nowMs: Long = System.currentTimeMillis()) {
        startTimeMs = nowMs
        completed = false
    }

    /** Returns the interpolated alpha [0..1] for the NEW bitmap. */
    fun currentAlpha(nowMs: Long = System.currentTimeMillis()): Float {
        if (startTimeMs < 0) {
            start(nowMs)
            return 0f
        }
        val elapsed = (nowMs - startTimeMs).coerceAtLeast(0L)
        val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
        if (progress >= 1f) completed = true
        return progress
    }

    fun isComplete(): Boolean = completed

    fun reset() {
        startTimeMs = -1L
        completed = false
    }
}
