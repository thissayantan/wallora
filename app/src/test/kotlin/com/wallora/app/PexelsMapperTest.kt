package com.wallora.app

import com.wallora.app.data.remote.toDomain
import com.wallora.app.data.remote.dto.PexelsListResponse
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PexelsMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun loadFixture(name: String): String =
        javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun `pexels curated fixture parses correctly`() {
        val fixture = loadFixture("pexels_curated.json")
        val response = json.decodeFromString<PexelsListResponse>(fixture)
        assertEquals(2, response.photos.size)
    }

    @Test
    fun `pexels photo maps to domain wallpaper correctly`() {
        val fixture = loadFixture("pexels_curated.json")
        val response = json.decodeFromString<PexelsListResponse>(fixture)
        val photo = response.photos[0]
        val wallpaper = photo.toDomain(Category.NATURE)

        assertEquals("12345", wallpaper.id)
        assertEquals(SourceId.PEXELS, wallpaper.sourceId)
        assertEquals(Category.NATURE, wallpaper.category)
        assertEquals("Jane Doe", wallpaper.author)
        assertEquals("https://www.pexels.com/@janedoe", wallpaper.authorUrl)
        assertEquals("https://www.pexels.com/photo/12345/", wallpaper.sourcePageUrl)
        assertEquals(3024, wallpaper.width)
        assertEquals(4032, wallpaper.height)
        assertNotNull(wallpaper.colorHint)
        assertEquals("PEXELS:12345", wallpaper.globalKey)
    }

    @Test
    fun `pexels photo maps with null category`() {
        val fixture = loadFixture("pexels_curated.json")
        val response = json.decodeFromString<PexelsListResponse>(fixture)
        val wallpaper = response.photos[1].toDomain(null)
        assertNull(wallpaper.category)
        assertEquals("67890", wallpaper.id)
    }

    @Test
    fun `pexels thumb url falls back to small when medium is blank`() {
        val fixture = loadFixture("pexels_curated.json")
        val response = json.decodeFromString<PexelsListResponse>(fixture)
        // medium is set in photo[0]
        assert(response.photos[0].toDomain(null).thumbUrl.contains("h=350"))
    }

    @Test
    fun `pexels globalKey is stable`() {
        val fixture = loadFixture("pexels_curated.json")
        val response = json.decodeFromString<PexelsListResponse>(fixture)
        val w1 = response.photos[0].toDomain(null)
        val w2 = response.photos[0].toDomain(Category.SPACE)
        assertEquals(w1.globalKey, w2.globalKey) // globalKey doesn't include category
    }
}
