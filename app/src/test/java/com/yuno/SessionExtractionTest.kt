package com.yuno

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The signed-in session riding the extractor's player requests ([SessionExtraction]): the
 * scope (only the player endpoints, never googlevideo), the web origin following the
 * request's own host (youtube.com's "Origin doesn't match Host" 400 otherwise), and how
 * the extractor's own cookies (its consent ones) merge with the session's.
 */
class SessionExtractionTest {
    private val session = mapOf(
        "SAPISID" to "sapisid",
        "__Secure-3PSID" to "ps3",
        "VISITOR_INFO1_LIVE" to "v1live",
    )
    private val auth = "SAPISIDHASH 1700000000_hash"

    /** Mirrors headersFor's scope and host-following origin for a URL, nothing app-bound. */
    private fun ride(url: String): Map<String, String>? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        val player = (httpUrl.host == "www.youtube.com" || httpUrl.host == "youtubei.googleapis.com") &&
            (httpUrl.encodedPath == "/youtubei/v1/player" ||
                httpUrl.encodedPath == "/youtubei/v1/reel/reel_item_watch")
        if (!player) return null
        return SessionExtraction.headers(true, auth, "0", session, null, "https://" + httpUrl.host)
    }

    @Test fun ridesOnlyThePlayerEndpoints() {
        // The real shapes the extractor sends: ANDROID's details go through the reel
        // endpoint on googleapis, VISIONOS plays on googleapis too, and the fork's own
        // re-ask is on www.
        assertNotNull(ride("https://youtubei.googleapis.com/youtubei/v1/reel/reel_item_watch?prettyPrint=false&t=x"))
        assertNotNull(ride("https://youtubei.googleapis.com/youtubei/v1/player?prettyPrint=false&t=x"))
        assertNotNull(ride("https://www.youtube.com/youtubei/v1/player?prettyPrint=false"))
        // Everything else stays anonymous: other InnerTube endpoints, and never googlevideo.
        assertNull(ride("https://www.youtube.com/youtubei/v1/next?key=x"))
        assertNull(ride("https://www.youtube.com/youtubei/v1/search"))
        assertNull(ride("https://rr4---sn-1giepjpx.googlevideo.com/videoplayback?id=a"))
        assertNull(ride("https://www.youtube.com/api/timedtext?v=a"))
        assertNull(ride("https://i.ytimg.com/vi/a/hqdefault.jpg"))
    }

    @Test fun theOriginFollowsTheHost() {
        // YouTube refuses a web origin that doesn't match the request's own host
        // ("Origin doesn't match Host"), so each host signs itself.
        val googleapis = ride("https://youtubei.googleapis.com/youtubei/v1/reel/reel_item_watch?prettyPrint=false")!!
        assertEquals("https://youtubei.googleapis.com", googleapis["Origin"])
        assertEquals("https://youtubei.googleapis.com", googleapis["X-Origin"])
        assertEquals("https://youtubei.googleapis.com/", googleapis["Referer"])
        val www = ride("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")!!
        assertEquals(Account.ORIGIN, www["Origin"])
        assertEquals(Account.ORIGIN, www["X-Origin"])
        assertEquals("https://www.youtube.com/", www["Referer"])
        // The signature and the session ride on either host.
        assertEquals(auth, googleapis["Authorization"])
        assertEquals(
            "SAPISID=sapisid; __Secure-3PSID=ps3; VISITOR_INFO1_LIVE=v1live",
            www["Cookie"],
        )
        assertEquals("0", www["X-Goog-AuthUser"])
    }

    @Test fun theExtractorsOwnCookiesStay() {
        // NewPipeExtractor puts consent cookies on its requests; the session adds to them
        // and wins where both carry the same name (the session's are the real login's).
        val headers = SessionExtraction.headers(
            true, auth, "0", session,
            extractorCookie = "SOCS=CAI; VISITOR_INFO1_LIVE=stale; OTHER=kept",
            origin = Account.ORIGIN,
        )!!
        assertEquals(
            "SOCS=CAI; VISITOR_INFO1_LIVE=v1live; OTHER=kept; SAPISID=sapisid; __Secure-3PSID=ps3",
            headers["Cookie"],
        )
    }

    @Test fun withoutASessionNothingChanges() {
        // The switch is off, or there is nothing to sign the request with: anonymous.
        assertNull(SessionExtraction.headers(false, auth, "0", session, null, Account.ORIGIN))
        assertNull(SessionExtraction.headers(true, null, "0", session, null, Account.ORIGIN))
        assertNull(SessionExtraction.headers(true, auth, "0", emptyMap(), null, Account.ORIGIN))
    }
}
