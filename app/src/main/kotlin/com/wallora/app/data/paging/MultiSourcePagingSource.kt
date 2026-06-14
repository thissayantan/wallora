package com.wallora.app.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.wallora.app.data.local.dao.WallpaperDao
import kotlinx.coroutines.CancellationException
import com.wallora.app.data.local.entity.WallpaperEntity
import com.wallora.app.domain.WallpaperSource
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Wallpaper

/**
 * Multi-source fan-out PagingSource.
 *
 * For each page load:
 * 1. Check Room TTL cache for all sources at this page.
 * 2. For any source whose cache has expired (or doesn't exist), fetch from network.
 * 3. Insert fresh items into Room cache.
 * 4. Round-robin interleave results from all sources.
 * 5. Deduplicate by [Wallpaper.globalKey].
 *
 * Key type: a [PageKey] holding per-source cursors so each source can page
 * independently without blocking the others.
 */
class MultiSourcePagingSource(
    private val sources: List<WallpaperSource>,
    private val categories: List<Category>,
    private val query: String?,
    private val wallpaperDao: WallpaperDao,
    private val cacheTtlMs: Long,
) : PagingSource<MultiSourcePagingSource.PageKey, Wallpaper>() {

    /**
     * Tracks globalKeys already emitted by this PagingSource instance.
     * Scoped to the instance lifetime so dedup spans all pages in a generation
     * (resets on refresh when a new instance is created). Thread-safe because
     * Paging 3 may call load() from multiple threads in theory.
     */
    private val seenKeys: MutableSet<String> =
        java.util.Collections.synchronizedSet(HashSet())

    /** Holds a map of sourceId → next-page cursor for that source. */
    data class PageKey(val cursors: Map<String, String>)

    companion object {
        private const val TAG = "MultiSourcePagingSource"
        val FIRST_PAGE = PageKey(emptyMap())
    }

    override fun getRefreshKey(state: PagingState<PageKey, Wallpaper>): PageKey? = null

    override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, Wallpaper> {
        val key = params.key ?: FIRST_PAGE
        val resultLists = mutableListOf<List<Wallpaper>>()
        val nextCursors = mutableMapOf<String, String>()

        for (source in sources) {
            val sourceKey = source.id.name
            val cursor = key.cursors[sourceKey] ?: "1"

            // Check TTL cache first
            val cacheKey = buildCacheKey(source.id.name, cursor)
            val minTimestamp = System.currentTimeMillis() - cacheTtlMs
            val cached = wallpaperDao.getByCacheKey(cacheKey, minTimestamp)

            val items: List<Wallpaper>
            val nextCursor: String?

            if (cached.isNotEmpty()) {
                items = cached.map { it.toDomain() }
                // For cached pages, assume there's a next page unless it's clearly empty
                nextCursor = cursor.toIntOrNull()?.plus(1)?.toString()
                Log.d(TAG, "${source.id}: cache hit (${items.size} items)")
            } else {
                try {
                    val page = if (query != null) source.search(query, cursor)
                               else source.browse(categories, cursor)
                    items = page.items
                    nextCursor = page.nextPage

                    val now = System.currentTimeMillis()
                    wallpaperDao.insertAll(items.map { WallpaperEntity.fromDomain(it, cacheKey, now) })
                    Log.d(TAG, "${source.id}: network fetch (${items.size} items)")
                } catch (e: CancellationException) {
                    throw e  // never swallow — flatMapLatest cancels in-flight loads on category change
                } catch (e: Exception) {
                    Log.w(TAG, "${source.id}: fetch failed, skipping source", e)
                    // Don't advance cursor on failure — retry on next page load
                    continue
                }
            }

            resultLists.add(items)
            if (nextCursor != null) nextCursors[sourceKey] = nextCursor
        }

        // Round-robin interleave results from all sources
        val interleaved = roundRobinInterleave(resultLists)

        // Cross-page dedup: filter by seenKeys so the same wallpaper never appears
        // twice across pages. distinctBy would only catch within-page duplicates;
        // cross-page duplicates cause an IllegalArgumentException in LazyStaggeredGrid.
        val deduped = interleaved.filter { seenKeys.add(it.globalKey) }

        val nextKey = if (nextCursors.isEmpty()) null else PageKey(nextCursors)

        return LoadResult.Page(
            data = deduped,
            prevKey = null,
            nextKey = nextKey,
        )
    }

    private fun buildCacheKey(sourceId: String, page: String): String {
        val catPart = if (query != null) "search:$query" else categories.joinToString(",") { it.name }
        return "$sourceId:$catPart:$page"
    }

    /** Round-robin interleave: take one item at a time from each list, in turn. */
    private fun roundRobinInterleave(lists: List<List<Wallpaper>>): List<Wallpaper> {
        val result = mutableListOf<Wallpaper>()
        val iters = lists.map { it.iterator() }
        while (iters.any { it.hasNext() }) {
            for (iter in iters) {
                if (iter.hasNext()) result.add(iter.next())
            }
        }
        return result
    }
}
