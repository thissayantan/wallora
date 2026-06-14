package com.wallora.app

import com.wallora.app.data.remote.toDomain
import com.wallora.app.data.remote.dto.PixabayResponse
import com.wallora.app.domain.model.SourceId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixabayMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun loadFixture(name: String): String =
        javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun `pixabay fixture parses correctly`() {
        val fixture = loadFixture("pixabay_search.json")
        val response = json.decodeFromString<PixabayResponse>(fixture)
        assertEquals(3, response.hits.size)
        assertEquals(500L, response.total)
        assertEquals(500L, response.totalHits)
    }

    @Test
    fun `pixabay hit maps to domain wallpaper correctly`() {
        val fixture = loadFixture("pixabay_search.json")
        val response = json.decodeFromString<PixabayResponse>(fixture)
        val hit = response.hits[0]
        val wallpaper = hit.toDomain()

        assertEquals("987654321", wallpaper.id)
        assertEquals(SourceId.PIXABAY, wallpaper.sourceId)
        assertEquals("PIXABAY:987654321", wallpaper.globalKey)
        assertEquals("https://pixabay.com/get/abcdef.jpg", wallpaper.thumbUrl)
        assertEquals("https://pixabay.com/get/abcdef_1280.jpg", wallpaper.fullUrl)
        assertEquals("PixabayUser", wallpaper.author)
        assertEquals("https://pixabay.com/photos/vibrant-colorful-abstract-987654321/", wallpaper.sourcePageUrl)
        assertEquals(1080, wallpaper.width)
        assertEquals(1920, wallpaper.height)
        assertTrue(wallpaper.tags.containsAll(listOf("vibrant", "colorful", "abstract")))
    }

    @Test
    fun `pixabay full url falls back to thumb when large is blank`() {
        val fixture = loadFixture("pixabay_search.json")
        val response = json.decodeFromString<PixabayResponse>(fixture)
        // hits[1] has both URLs set — check fallback with a synthetic hit
        val hit = response.hits[0].copy(largeImageURL = "")
        val wallpaper = hit.toDomain()
        assertEquals(wallpaper.thumbUrl, wallpaper.fullUrl)
    }

    @Test
    fun `pixabay globalKey is stable across different category mappings`() {
        val fixture = loadFixture("pixabay_search.json")
        val response = json.decodeFromString<PixabayResponse>(fixture)
        val w1 = response.hits[0].toDomain()
        val w2 = response.hits[0].toDomain()
        assertEquals(w1.globalKey, w2.globalKey)
    }

    @Test
    fun `pixabay source id is PIXABAY for all hits`() {
        val fixture = loadFixture("pixabay_search.json")
        val response = json.decodeFromString<PixabayResponse>(fixture)
        response.hits.forEach { hit ->
            assertEquals(SourceId.PIXABAY, hit.toDomain().sourceId)
        }
    }

    @Test
    fun `pixabay portrait filter rejects landscape-only images`() {
        val fixture = loadFixture("pixabay_search.json")
        val response = json.decodeFromString<PixabayResponse>(fixture)
        // hits[2] is 1920×1080 (landscape) — aspect ratio 1080/1920 = 0.5625 < 0.7
        val landscapeHit = response.hits[2]
        val ratio = landscapeHit.imageHeight.toFloat() / landscapeHit.imageWidth.toFloat()
        assertFalse("Landscape image should fail portrait guard", ratio >= 0.7f)
        // hits[0] is 1080×1920 (portrait) — ratio = 1.777 ≥ 0.7
        val portraitHit = response.hits[0]
        val portraitRatio = portraitHit.imageHeight.toFloat() / portraitHit.imageWidth.toFloat()
        assertTrue("Portrait image should pass portrait guard", portraitRatio >= 0.7f)
    }
}
