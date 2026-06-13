package com.wallora.app.domain.rotation

import com.wallora.app.domain.model.Wallpaper

/**
 * Pure stateless helper for no-repeat wallpaper rotation.
 *
 * Holds no state — the caller is responsible for persisting history. This makes
 * the logic trivially unit-testable without mocks.
 */
object RotationEngine {

    /**
     * Pick the next wallpaper from [candidates], excluding any whose [Wallpaper.globalKey]
     * is in [recentKeys].
     *
     * - If candidates list is empty: returns null.
     * - If all candidates are in [recentKeys] (exhausted): picks randomly from the full
     *   [candidates] list (resets the no-repeat window implicitly for the caller).
     * - Otherwise: picks randomly from the remaining (non-recent) candidates.
     *
     * @param candidates  full list of available wallpapers (playlist).
     * @param recentKeys  set of globalKeys that have been shown recently (no-repeat window).
     * @param seed        optional seed for reproducible tests. If null, uses [Wallpaper.globalKey]
     *                    hashing for a deterministic but varied pick.
     * @return the selected wallpaper, or null if [candidates] is empty.
     */
    fun pickNext(
        candidates: List<Wallpaper>,
        recentKeys: Set<String>,
        seed: Long? = null,
    ): PickResult {
        if (candidates.isEmpty()) return PickResult.Empty

        val unseen = candidates.filter { it.globalKey !in recentKeys }
        // All candidates exhausted — reset and pick from the full list
        val pool = unseen.ifEmpty { candidates }

        val index = if (seed != null) {
            (seed % pool.size).toInt().let { if (it < 0) it + pool.size else it }
        } else {
            pool.indices.random()
        }

        return PickResult.Found(
            wallpaper = pool[index],
            wasExhausted = unseen.isEmpty(),
        )
    }

    /**
     * Determine the no-repeat window size: the smaller of [candidates].size − 1 or
     * [maxWindowSize]. A window of 0 disables no-repeat (single-item playlists).
     */
    fun noRepeatWindow(candidatesSize: Int, maxWindowSize: Int = 30): Int =
        maxOf(0, minOf(candidatesSize - 1, maxWindowSize))
}

/** Result of [RotationEngine.pickNext]. */
sealed class PickResult {
    /** No candidates available. */
    data object Empty : PickResult()

    /** A wallpaper was selected. */
    data class Found(
        val wallpaper: Wallpaper,
        /** True if the pool was exhausted and the no-repeat window was implicitly reset. */
        val wasExhausted: Boolean,
    ) : PickResult()
}
