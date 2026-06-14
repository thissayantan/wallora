package com.wallora.app

import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import org.junit.Assert.assertEquals
import org.junit.Test

class DedupTest {

    private fun makeWallpaper(id: String, sourceId: SourceId) = Wallpaper(
        id = id,
        sourceId = sourceId,
        thumbUrl = "https://thumb/$id",
        fullUrl = "https://full/$id",
        width = 1080,
        height = 1920,
        author = "Author",
        authorUrl = "https://author",
        sourcePageUrl = "https://source/$id",
    )

    /** Simulates the dedup logic from MultiSourcePagingSource. */
    private fun deduplicate(items: List<Wallpaper>): List<Wallpaper> {
        val seen = mutableSetOf<String>()
        return items.filter { seen.add(it.globalKey) }
    }

    /** Round-robin interleave from multiple source lists. */
    private fun roundRobin(lists: List<List<Wallpaper>>): List<Wallpaper> {
        val result = mutableListOf<Wallpaper>()
        val iters = lists.map { it.iterator() }
        while (iters.any { it.hasNext() }) {
            for (iter in iters) {
                if (iter.hasNext()) result.add(iter.next())
            }
        }
        return result
    }

    @Test
    fun `dedup removes exact duplicates by globalKey`() {
        val p1 = makeWallpaper("1", SourceId.PEXELS)
        val p2 = makeWallpaper("1", SourceId.PEXELS) // duplicate
        val p3 = makeWallpaper("2", SourceId.PEXELS)
        val result = deduplicate(listOf(p1, p2, p3))
        assertEquals(2, result.size)
        assertEquals("PEXELS:1", result[0].globalKey)
        assertEquals("PEXELS:2", result[1].globalKey)
    }

    @Test
    fun `dedup does not conflate same id from different sources`() {
        val p1 = makeWallpaper("1", SourceId.PEXELS)
        val w1 = makeWallpaper("1", SourceId.WALLHAVEN)
        val result = deduplicate(listOf(p1, w1))
        assertEquals(2, result.size)
    }

    @Test
    fun `round robin interleaves evenly`() {
        val listA = listOf(makeWallpaper("a1", SourceId.PEXELS), makeWallpaper("a2", SourceId.PEXELS))
        val listB = listOf(makeWallpaper("b1", SourceId.WALLHAVEN), makeWallpaper("b2", SourceId.WALLHAVEN))
        val result = roundRobin(listOf(listA, listB))
        assertEquals(4, result.size)
        assertEquals("a1", result[0].id)
        assertEquals("b1", result[1].id)
        assertEquals("a2", result[2].id)
        assertEquals("b2", result[3].id)
    }

    @Test
    fun `round robin handles unequal list sizes`() {
        val listA = listOf(
            makeWallpaper("a1", SourceId.PEXELS),
            makeWallpaper("a2", SourceId.PEXELS),
            makeWallpaper("a3", SourceId.PEXELS),
        )
        val listB = listOf(makeWallpaper("b1", SourceId.WALLHAVEN))
        val result = roundRobin(listOf(listA, listB))
        assertEquals(4, result.size)
        // After b1 is exhausted, remaining a items are interleaved without b
        assertEquals("a1", result[0].id)
        assertEquals("b1", result[1].id)
        assertEquals("a2", result[2].id)
        assertEquals("a3", result[3].id)
    }

    @Test
    fun `dedup preserves order (first occurrence wins)`() {
        val items = listOf(
            makeWallpaper("1", SourceId.PEXELS),
            makeWallpaper("2", SourceId.WALLHAVEN),
            makeWallpaper("1", SourceId.PEXELS),  // duplicate
            makeWallpaper("3", SourceId.REDDIT),
        )
        val result = deduplicate(items)
        assertEquals(3, result.size)
        assertEquals("PEXELS:1", result[0].globalKey)
        assertEquals("WALLHAVEN:2", result[1].globalKey)
        assertEquals("REDDIT:3", result[2].globalKey)
    }

    // ---- Cross-page dedup regression (fast-scroll crash) ----
    // Before the fix, MultiSourcePagingSource.load() used distinctBy per page, so the same
    // wallpaper on two pages produced duplicate LazyStaggeredGrid keys → crash.
    // The fix uses a shared seenKeys set across load() calls on the same PagingSource instance.
    // These tests verify that algorithm directly.

    private fun crossPageDedupFilter(
        seenKeys: MutableSet<String>,
        items: List<Wallpaper>,
    ): List<Wallpaper> = items.filter { seenKeys.add(it.globalKey) }

    @Test
    fun `cross-page dedup filters wallpaper seen on a prior page`() {
        val seenKeys: MutableSet<String> =
            java.util.Collections.synchronizedSet(HashSet())
        val page1 = listOf(
            makeWallpaper("1", SourceId.PEXELS),
            makeWallpaper("42", SourceId.UNSPLASH),
        )
        val page2 = listOf(
            makeWallpaper("42", SourceId.UNSPLASH), // already seen in page1 → must be dropped
            makeWallpaper("99", SourceId.WALLHAVEN),
        )

        val result1 = crossPageDedupFilter(seenKeys, page1)
        val result2 = crossPageDedupFilter(seenKeys, page2)

        assertEquals(2, result1.size)
        assertEquals(1, result2.size)
        assertEquals("WALLHAVEN:99", result2[0].globalKey)
    }

    @Test
    fun `cross-page dedup accumulates seen keys across multiple pages`() {
        val seenKeys: MutableSet<String> =
            java.util.Collections.synchronizedSet(HashSet())
        val page1 = listOf(makeWallpaper("a", SourceId.PEXELS))
        val page2 = listOf(makeWallpaper("b", SourceId.PEXELS), makeWallpaper("a", SourceId.PEXELS))
        val page3 = listOf(makeWallpaper("a", SourceId.PEXELS), makeWallpaper("b", SourceId.PEXELS), makeWallpaper("c", SourceId.PEXELS))

        val r1 = crossPageDedupFilter(seenKeys, page1)
        val r2 = crossPageDedupFilter(seenKeys, page2)
        val r3 = crossPageDedupFilter(seenKeys, page3)

        assertEquals(1, r1.size) // a
        assertEquals(1, r2.size) // b; a dropped
        assertEquals(1, r3.size) // c; a+b dropped
        assertEquals("PEXELS:c", r3[0].globalKey)
    }

    @Test
    fun `fresh seenKeys set allows the same key again (simulates new PagingSource on refresh)`() {
        val seenKeys1: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())
        val seenKeys2: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())
        val items = listOf(makeWallpaper("1", SourceId.PEXELS))

        val r1 = crossPageDedupFilter(seenKeys1, items)
        val r2 = crossPageDedupFilter(seenKeys2, items) // new instance seenKeys

        assertEquals(1, r1.size)
        assertEquals(1, r2.size) // not filtered — separate set, simulates post-refresh generation
    }
}
