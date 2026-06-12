package com.wallora.app

import com.wallora.app.data.remote.toDomain
import com.wallora.app.data.remote.dto.WallhavenResponse
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WallhavenMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun loadFixture(name: String): String =
        javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun `wallhaven fixture parses correctly`() {
        val fixture = loadFixture("wallhaven_search.json")
        val response = json.decodeFromString<WallhavenResponse>(fixture)
        assertEquals(2, response.data.size)
        assertNotNull(response.meta)
        assertEquals(1, response.meta!!.currentPage)
        assertEquals(10, response.meta!!.lastPage)
    }

    @Test
    fun `wallhaven wallpaper maps to domain correctly`() {
        val fixture = loadFixture("wallhaven_search.json")
        val response = json.decodeFromString<WallhavenResponse>(fixture)
        val item = response.data[0].toDomain(Category.NATURE)

        assertEquals("wq3g9k", item.id)
        assertEquals(SourceId.WALLHAVEN, item.sourceId)
        assertEquals(Category.NATURE, item.category)
        assertEquals(2160, item.width)
        assertEquals(3840, item.height)
        assertEquals("uploader_user", item.author)
        assertEquals("https://wallhaven.cc/w/wq3g9k", item.sourcePageUrl)
        assertEquals("WALLHAVEN:wq3g9k", item.globalKey)
        // tags
        assertEquals(listOf("nature", "landscape"), item.tags)
        // color hint from "#1a2b3c"
        assertNotNull(item.colorHint)
    }

    @Test
    fun `wallhaven item with null uploader uses fallback author`() {
        val fixture = loadFixture("wallhaven_search.json")
        val response = json.decodeFromString<WallhavenResponse>(fixture)
        val item = response.data[1].toDomain(null)
        assertEquals("Wallhaven", item.author)
        assertNull(item.category)
    }

    @Test
    fun `wallhaven thumb uses large thumb url`() {
        val fixture = loadFixture("wallhaven_search.json")
        val response = json.decodeFromString<WallhavenResponse>(fixture)
        val item = response.data[0].toDomain(null)
        assert(item.thumbUrl.contains("th.wallhaven.cc/lg"))
    }
}
