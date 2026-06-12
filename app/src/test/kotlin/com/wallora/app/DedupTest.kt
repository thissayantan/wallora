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
}
