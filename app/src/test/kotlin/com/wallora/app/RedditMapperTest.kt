package com.wallora.app

import com.wallora.app.data.remote.isDirectImagePost
import com.wallora.app.data.remote.toDomain
import com.wallora.app.data.remote.dto.RedditListingResponse
import com.wallora.app.domain.model.SourceId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun loadFixture(name: String): String =
        javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun `reddit fixture parses correctly`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        assertEquals(5, response.data.children.size)
        assertEquals("t3_abc123", response.data.after)
    }

    @Test
    fun `isDirectImagePost accepts i_redd_it jpg post`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val post = response.data.children[0].data  // img001 — i.redd.it jpg
        assertTrue(isDirectImagePost(post))
    }

    @Test
    fun `isDirectImagePost rejects NSFW post`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val post = response.data.children[1].data  // nsfw001 — over_18=true
        assertFalse(isDirectImagePost(post))
    }

    @Test
    fun `isDirectImagePost rejects video post`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val post = response.data.children[2].data  // video001 — is_video=true
        assertFalse(isDirectImagePost(post))
    }

    @Test
    fun `isDirectImagePost rejects gallery post`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val post = response.data.children[3].data  // gallery001 — is_gallery=true
        assertFalse(isDirectImagePost(post))
    }

    @Test
    fun `isDirectImagePost accepts imgur direct image`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val post = response.data.children[4].data  // imgur001 — i.imgur.com png
        assertTrue(isDirectImagePost(post))
    }

    @Test
    fun `reddit post maps to domain wallpaper correctly`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val post = response.data.children[0].data  // img001
        val wallpaper = post.toDomain(null)

        assertNotNull(wallpaper)
        assertEquals("img001", wallpaper!!.id)
        assertEquals(SourceId.REDDIT, wallpaper.sourceId)
        assertEquals("nature_fan", wallpaper.author)
        assertEquals("https://i.redd.it/img001.jpg", wallpaper.fullUrl)
        assertTrue(wallpaper.sourcePageUrl.contains("EarthPorn"))
        assertEquals("REDDIT:img001", wallpaper.globalKey)
        assertEquals(3024, wallpaper.width)
        assertEquals(4032, wallpaper.height)
    }

    @Test
    fun `reddit filtered posts only 2 of 5 pass`() {
        val fixture = loadFixture("reddit_hot.json")
        val response = json.decodeFromString<RedditListingResponse>(fixture)
        val valid = response.data.children
            .map { it.data }
            .filter { isDirectImagePost(it) }
        assertEquals(2, valid.size)  // img001 and imgur001
        assertEquals("img001", valid[0].id)
        assertEquals("imgur001", valid[1].id)
    }
}
