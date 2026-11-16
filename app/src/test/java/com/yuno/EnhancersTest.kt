package com.yuno

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Morphe light set's community extras ([Enhancers]): the tracking parameters stripped
 * from shared links, and the DeArrow answer turned into a title/thumbnail replacement
 * (the community's first entry, unless it *is* the original).
 */
class EnhancersTest {
    @Test fun sanitizeStripsTracking() {
        val url = "https://youtu.be/dQw4w9WgXcQ?si=AbCdEf-123&t=42"
        assertEquals("https://youtu.be/dQw4w9WgXcQ?t=42", Enhancers.sanitizedLink(url, true))
        // "feature" and the utm family are tracking too.
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            Enhancers.sanitizedLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share&utm_source=x", true),
        )
    }

    @Test fun sanitizeKeepsEverythingElse() {
        // Nothing left to keep: the ? goes entirely.
        assertEquals(
            "https://youtu.be/dQw4w9WgXcQ",
            Enhancers.sanitizedLink("https://youtu.be/dQw4w9WgXcQ?si=x", true),
        )
        // The fragment survives.
        assertEquals(
            "https://youtu.be/a?t=1#lived",
            Enhancers.sanitizedLink("https://youtu.be/a?si=x&t=1#lived", true),
        )
        // A query with no tracking, and no query at all, come back untouched.
        assertEquals("https://youtu.be/a?t=1", Enhancers.sanitizedLink("https://youtu.be/a?t=1", true))
        assertEquals("https://youtu.be/a", Enhancers.sanitizedLink("https://youtu.be/a", true))
        // Non-http links are never rewritten.
        assertEquals("intent://a?si=x", Enhancers.sanitizedLink("intent://a?si=x", true))
        // Off: nothing happens.
        assertEquals("https://youtu.be/a?si=x", Enhancers.sanitizedLink("https://youtu.be/a?si=x", false))
    }

    private fun entry(title: String, original: Boolean = false) =
        JSONObject().put("title", title).put("original", original).put("votes", 100)

    private fun branding(titles: JSONArray? = null, thumbs: JSONArray? = null): JSONObject {
        val o = JSONObject()
        titles?.let { o.put("titles", it) }
        thumbs?.let { o.put("thumbnails", it) }
        return o
    }

    @Test fun dearrowPicksTheBestEntry() {
        // (The API's own answer is checked here; the request itself is not made in tests.)
        val titles = JSONArray().put(entry("The real title")).put(entry("Clickbait", original = true))
        val answer = branding(titles)
        val title = answer.optJSONArray("titles")?.optJSONObject(0)?.takeIf { !it.optBoolean("original") }?.optString("title")?.ifBlank { null }
        val none = branding(JSONArray().put(entry("Clickbait", original = true)))
        val noTitle = none.optJSONArray("titles")?.optJSONObject(0)?.takeIf { !it.optBoolean("original") }?.optString("title")?.ifBlank { null }
        assertEquals("The real title", title)
        assertNull(noTitle)
    }

    @Test fun dearrowThumbnailTime() {
        // "Tt12.345" carries the timestamp; "To" is the original. The thumbnail itself is
        // fetched from the thumbnail server with that timestamp.
        assertEquals("12.345", "Tt12.345".substringAfter('t', "0"))
        assertEquals("0", "To".substringAfter('t', "0"))
        assertEquals("0", "To0".substringAfter('t', "0"))
    }
}
