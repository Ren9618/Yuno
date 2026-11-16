package com.yuno

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * The community-API extras ported from Morphe's light set (2026-11-16):
 * Return YouTube Dislike's dislike counts, DeArrow's community titles/thumbnails,
 * and sharing links without tracking parameters. The mute, loop and copy buttons
 * belong to the player and are only shown while their switches here are on.
 *
 * The lookups send just the video id, exactly what YouTube itself already knows.
 */
object Enhancers {
    private const val PREFS = "enhancers"
    private const val KEY_SANITIZE = "sanitize_links"
    private const val KEY_MUTE = "mute_button"
    private const val KEY_LOOP = "loop_button"
    private const val KEY_COPY = "copy_buttons"

    private var prefs: SharedPreferences? = null

    /** Shared links lose the tracking parameters ("Sanitize sharing links"). */
    var sanitizeLinks by mutableStateOf(true)
        private set
    /** The player's mute pill. */
    var showMute by mutableStateOf(true)
        private set
    /** The player's loop pill. */
    var showLoop by mutableStateOf(true)
        private set
    /** The player's copy pill (title, description, link). */
    var showCopy by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs = it }
        sanitizeLinks = p.getBoolean(KEY_SANITIZE, true)
        showMute = p.getBoolean(KEY_MUTE, true)
        showLoop = p.getBoolean(KEY_LOOP, true)
        showCopy = p.getBoolean(KEY_COPY, true)
    }

    fun updateSanitizeLinks(on: Boolean) {
        sanitizeLinks = on
        prefs?.edit()?.putBoolean(KEY_SANITIZE, on)?.apply()
    }

    fun updateMuteButton(on: Boolean) {
        showMute = on
        prefs?.edit()?.putBoolean(KEY_MUTE, on)?.apply()
    }

    fun updateLoopButton(on: Boolean) {
        showLoop = on
        prefs?.edit()?.putBoolean(KEY_LOOP, on)?.apply()
    }

    fun updateCopyButtons(on: Boolean) {
        showCopy = on
        prefs?.edit()?.putBoolean(KEY_COPY, on)?.apply()
    }

    /**
     * [url] without the tracking parameters: `si` (YouTube's share tracker), `feature`
     * and the `utm_*` ad ones. Only http(s) links with a query are touched.
     */
    internal fun link(url: String): String = sanitizedLink(url, sanitizeLinks)

    /** [sanitizedLink] with the switch passed in, so tests stay free of any UI state. */
    internal fun sanitizedLink(url: String, sanitize: Boolean): String {
        if (!sanitize) return url
        val question = url.indexOf('?')
        if (question < 0) return url
        val base = url.substring(0, question)
        if (!base.startsWith("http://") && !base.startsWith("https://")) return url
        val kept = url.substring(question + 1).substringBefore('#').split('&').filter { it.isNotEmpty() }.filter { key ->
            !TRACKING.matches(key.substringBefore('=').lowercase())
        }
        val fragment = url.substringAfter('#', "")
        return buildString {
            append(base)
            if (kept.isNotEmpty()) append('?').append(kept.joinToString("&"))
            if (fragment.isNotEmpty()) append('#').append(fragment)
        }
    }

    /** `si`, `si_*` and the `utm_*` families, plus the analytics ones. */
    private val TRACKING = Regex("^(si|si_.+|utm_.+|_ga|_hs|feature)$")
}

/**
 * Return YouTube Dislike (returnyoutubedislikeapi.com): the dislike counts YouTube took
 * away, estimated by its users. The count sits under the player's own dislike button;
 * a video without estimates shows none. The account's own dislike still goes to YouTube.
 */
object ReturnYouTubeDislike {
    private const val KEY_ENABLED = "enabled"

    private var prefs: SharedPreferences? = null
    var enabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs = it }
        enabled = p.getBoolean(KEY_ENABLED, true)
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
    }

    private const val PREFS = "ryd"

    /** What the API answered for a video: its estimated dislikes. */
    class Votes(val likes: Long, val dislikes: Long)

    private val cache = ConcurrentHashMap<String, Votes>()

    fun cached(videoId: String): Votes? = cache[videoId]

    /** Blocking; call off the main thread. Never throws — no estimate is a valid answer. */
    fun fetch(videoId: String): Votes? {
        cache[videoId]?.let { return it }
        val votes = runCatching { request(videoId) }
            .onFailure { Log.w("Yuno", "Return YouTube Dislike lookup failed for $videoId", it) }
            .getOrNull()
        votes?.let { cache[videoId] = it } // a failure is not cached: a later open retries
        return votes
    }

    private fun request(videoId: String): Votes? {
        val url = "https://returnyoutubedislikeapi.com/votes?videoId=" + java.net.URLEncoder.encode(videoId, "UTF-8")
        val request = Request.Builder().url(url).header("User-Agent", Net.USER_AGENT).build()
        Net.client.newCall(request).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("Return YouTube Dislike ${r.code}")
            val o = JSONObject(r.body?.string().orEmpty())
            if (o.optBoolean("deleted")) return null
            val dislikes = o.optLong("dislikes", -1L)
            if (dislikes < 0) return null
            return Votes(o.optLong("likes", 0L), dislikes)
        }
    }
}

/**
 * DeArrow (dearrow.ajay.app): community-voted titles and thumbnails that say what a video
 * is, replacing the clickbait. Only the open video is rewritten; a video without a better
 * title or thumbnail keeps the creator's own.
 */
object DeArrow {
    private const val KEY_ENABLED = "enabled"

    private var prefs: SharedPreferences? = null
    var enabled by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs = it }
        enabled = p.getBoolean(KEY_ENABLED, false)
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
    }

    private const val PREFS = "dearrow"

    /** What the API answered for a video; null means "keep the creator's own". */
    class Branding(val title: String?, val thumbnailUrl: String?)

    private val cache = ConcurrentHashMap<String, Branding>()

    fun cached(videoId: String): Branding? = cache[videoId]

    /** Blocking; call off the main thread. Never throws — no branding is a valid answer. */
    fun fetch(videoId: String): Branding? {
        cache[videoId]?.let { return it }
        val branding = runCatching { request(videoId) }
            .onFailure { Log.w("Yuno", "DeArrow lookup failed for $videoId", it) }
            .getOrNull()
        branding?.let { cache[videoId] = it } // a failure is not cached: a later open retries
        return branding
    }

    private fun request(videoId: String): Branding? {
        val url = "https://dearrow.ajay.app/api/branding?videoID=" + java.net.URLEncoder.encode(videoId, "UTF-8")
        val request = Request.Builder().url(url).header("User-Agent", Net.USER_AGENT).build()
        Net.client.newCall(request).execute().use { r ->
            if (r.code == 404) return Branding(null, null) // no submissions: keep the original
            if (!r.isSuccessful) throw java.io.IOException("DeArrow ${r.code}")
            val o = JSONObject(r.body?.string().orEmpty())
            // The lists come in the community's order of preference; the first entry is the
            // best. One that *is* the original (or an empty list) means nothing better exists.
            val title = o.optJSONArray("titles")?.optJSONObject(0)
                ?.takeIf { !it.optBoolean("original") }
                ?.optString("title")?.ifBlank { null }
            val thumb = o.optJSONArray("thumbnails")?.optJSONObject(0)
                ?.takeIf { !it.optBoolean("original") }
                ?.optString("thumbnail")?.ifBlank { null }
                ?.let { "https://dearrow-thumb.ajay.app/api/v1/getThumbnail?videoID=$videoId&time=${timeOf(it)}" }
            return Branding(title, thumb)
        }
    }

    /** A thumbnail entry's timestamp ("Tt12.345" → 12.345 s, "To" → 0). */
    private fun timeOf(value: String): String =
        value.substringAfter('t', "0").toFloatOrNull()?.toString() ?: "0"
}
