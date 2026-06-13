package com.wallora.app

import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.domain.rotation.PickResult
import com.wallora.app.domain.rotation.RotationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RotationEngine] — no-repeat logic, exhaustion reset, window sizing.
 */
class RotationEngineTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeWallpaper(id: String): Wallpaper = Wallpaper(
        id = id,
        sourceId = SourceId.PEXELS,
        thumbUrl = "https://example.com/thumb/$id.jpg",
        fullUrl = "https://example.com/full/$id.jpg",
        width = 1080,
        height = 1920,
        author = "Test Author",
        authorUrl = "",
        sourcePageUrl = "",
        colorHint = null,
    )

    private val sampleCandidates = (1..10).map { fakeWallpaper("w$it") }

    // ── Basic selection ───────────────────────────────────────────────────────

    @Test
    fun `pickNext returns Empty when candidates are empty`() {
        val result = RotationEngine.pickNext(emptyList(), emptySet())
        assertEquals(PickResult.Empty, result)
    }

    @Test
    fun `pickNext with no recent history returns a wallpaper`() {
        val result = RotationEngine.pickNext(sampleCandidates, emptySet(), seed = 0L)
        assertTrue(result is PickResult.Found)
    }

    @Test
    fun `pickNext with seed is deterministic`() {
        val r1 = RotationEngine.pickNext(sampleCandidates, emptySet(), seed = 3L)
        val r2 = RotationEngine.pickNext(sampleCandidates, emptySet(), seed = 3L)
        assertEquals(r1, r2)
    }

    // ── No-repeat logic ───────────────────────────────────────────────────────

    @Test
    fun `pickNext avoids recently-seen wallpapers`() {
        // Mark all but the last as recent
        val recent = sampleCandidates.dropLast(1).map { it.globalKey }.toSet()
        val result = RotationEngine.pickNext(sampleCandidates, recent, seed = 0L)
        val found = result as PickResult.Found
        // Only the last candidate is unseen — must pick it
        assertEquals(sampleCandidates.last().globalKey, found.wallpaper.globalKey)
        assertFalse(found.wasExhausted)
    }

    @Test
    fun `pickNext exhaustion resets and picks from full pool`() {
        // All candidates are "recent" — engine should reset and pick from full pool
        val recent = sampleCandidates.map { it.globalKey }.toSet()
        val result = RotationEngine.pickNext(sampleCandidates, recent, seed = 2L)
        val found = result as PickResult.Found
        assertTrue(found.wasExhausted)
        assertTrue(sampleCandidates.any { it.globalKey == found.wallpaper.globalKey })
    }

    @Test
    fun `no-repeat exhaustion loop eventually covers all candidates`() {
        // Simulate repeated calls, accumulating seen keys. After N calls, all N candidates
        // should have been picked at least once (within 2× N attempts to account for randomness).
        val candidates = (1..5).map { fakeWallpaper("loop$it") }
        val seen = mutableSetOf<String>()
        var seed = 0L
        repeat(candidates.size * 3) {
            val recent = seen.toList().takeLast(candidates.size - 1).toSet()
            val result = RotationEngine.pickNext(candidates, recent, seed = seed++)
            if (result is PickResult.Found) seen.add(result.wallpaper.globalKey)
        }
        assertEquals("All 5 candidates should appear in seen set", 5, seen.size)
    }

    // ── Window sizing ─────────────────────────────────────────────────────────

    @Test
    fun `noRepeatWindow is zero for single-item playlist`() {
        assertEquals(0, RotationEngine.noRepeatWindow(1))
    }

    @Test
    fun `noRepeatWindow is candidatesSize minus 1 for small playlists`() {
        assertEquals(4, RotationEngine.noRepeatWindow(5, maxWindowSize = 30))
    }

    @Test
    fun `noRepeatWindow is capped at maxWindowSize`() {
        assertEquals(10, RotationEngine.noRepeatWindow(100, maxWindowSize = 10))
    }

    @Test
    fun `noRepeatWindow with single candidate allows repeat`() {
        val single = listOf(fakeWallpaper("only"))
        val recent = setOf(single[0].globalKey) // it's already seen
        val result = RotationEngine.pickNext(single, recent, seed = 0L)
        val found = result as PickResult.Found
        // The only candidate must be returned despite being "recent" (exhaustion reset)
        assertEquals("only", found.wallpaper.id)
        assertTrue(found.wasExhausted)
    }
}
