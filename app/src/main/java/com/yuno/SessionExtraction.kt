package com.yuno

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.schabi.newpipe.extractor.downloader.Request

/**
 * The yt-dlp remedy for the bot check: when someone is signed in, the player requests the
 * extractor makes ride that Google session — the session's cookies plus youtube.com's own
 * request signature ([Account.authorization], the same SAPISIDHASH youtube.com's pages
 * sign with). YouTube then reads them as signed-in requests, and the "Sign in to confirm
 * you're not a bot" refusal that anonymous requests on shared addresses get stops applying.
 * Nothing else changes: the streams still come from googlevideo, which is signed and not
 * cookie-bound, and every request without a session (signed out, no SAPISID) stays exactly
 * as anonymous as before, with [BotCheckRecovery] waiting the check out.
 *
 * The scope is the player requests only: the extractor's reel request (ANDROID's details),
 * both VISIONOS player calls (the extractor's own and [PlayerFallback]'s stand-in). Feeds,
 * search and comments stay anonymous, so nothing about what YouTube serves for them moves.
 * The web origin follows the request's own host — yt-dlp's rule, and YouTube checks it:
 * "Origin doesn't match Host" is its 400 for a web origin on the wrong host (the reel
 * endpoint answers exactly that for a www origin, 2026-11-16), so a googleapis request
 * signs itself with a googleapis origin and www with youtube.com's.
 * Switch: Enhancements › Use the signed-in session for extraction (on while signed in).
 */
object SessionExtraction {
    private const val TAG = "Yuno"

    /** The extractor's ANDROID player request (it goes through the shorts reel endpoint). */
    private const val REEL = "/youtubei/v1/reel/reel_item_watch"
    private const val PLAYER = "/youtubei/v1/player"

    /** Headers for an extractor request, or null to leave it anonymous. */
    fun headersFor(url: String, request: Request): Map<String, String>? {
        if (!isPlayerUrl(url)) return null
        if (!Enhancers.sessionExtraction || !Account.signedIn) return null
        val origin = originFor(url)
        val headers = headers(
            ride = true,
            auth = Account.authorization(origin),
            authUser = Account.authUser(),
            sessionCookies = Account.parseCookieHeader(Account.cookieHeader()),
            extractorCookie = request.headers()["Cookie"]?.joinToString("; "),
            origin = origin,
        ) ?: return null
        Log.i(TAG, "the player request rides the signed-in session")
        return headers
    }

    /** The same for [PlayerFallback]'s own player re-ask, which posts the endpoint directly. */
    fun headersForPlayerReAsk(): Map<String, String>? {
        if (!Enhancers.sessionExtraction || !Account.signedIn) return null
        return headers(
            ride = true,
            auth = Account.authorization(Account.ORIGIN),
            authUser = Account.authUser(),
            sessionCookies = Account.parseCookieHeader(Account.cookieHeader()),
            extractorCookie = null,
            origin = Account.ORIGIN,
        )
    }

    /** [headers] with everything passed in, so tests stay free of any app state. */
    internal fun headers(
        ride: Boolean,
        auth: String?,
        authUser: String,
        sessionCookies: Map<String, String>,
        extractorCookie: String?,
        origin: String,
    ): Map<String, String>? {
        if (!ride || auth == null) return null
        // Whatever the extractor itself put in the jar stays (its consent cookies); the
        // session only adds to it, and wins where both carry the same name.
        val jar = LinkedHashMap<String, String>()
        extractorCookie?.let { jar.putAll(Account.parseCookieHeader(it)) }
        jar.putAll(sessionCookies)
        if (jar.isEmpty()) return null
        return mapOf(
            "Cookie" to jar.entries.joinToString("; ") { "${it.key}=${it.value}" },
            "Authorization" to auth,
            "X-Goog-AuthUser" to authUser,
            "Origin" to origin,
            "X-Origin" to origin,
            "Referer" to "$origin/",
        )
    }

    /** The web origin is the host the request itself goes to; YouTube checks they match. */
    private fun originFor(url: String): String {
        val host = url.toHttpUrlOrNull()?.host ?: return Account.ORIGIN
        return "https://$host"
    }

    private fun isPlayerUrl(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        val path = httpUrl.encodedPath
        return (httpUrl.host == "www.youtube.com" || httpUrl.host == "youtubei.googleapis.com") &&
            (path == PLAYER || path == REEL)
    }
}
