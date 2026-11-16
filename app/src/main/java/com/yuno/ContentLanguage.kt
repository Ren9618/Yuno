package com.yuno

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * The language YouTube itself answers in (`hl`, and `gl` for the country): the app's language
 * unless one is picked in Settings › Language › YouTube content.
 *
 * InnerTube requests (Home, Subscriptions, channel pages, search, the video page's description)
 * carry it, so a Japanese user really sees YouTube as a Japanese user does: Japanese tab names,
 * chips, view counts and dates («1.2万回視聴», «3 時間前»). Descriptions are still the creator's
 * own text; YouTube only translates them when the creator added a translation.
 *
 * NewPipeExtractor's stream extraction stays on English (see [AppLanguage]): its error handling
 * recognises YouTube's English refusal messages (bot check, age, members-only). Those pages are
 * only used for what they are good at (stream URLs, captions); the Japanese description comes
 * from InnerTube ([InnerTube.videoDescription]).
 */
object ContentLanguage {
    /** A language YouTube answers in, named in its own language. */
    class Choice(val tag: String, val name: String)

    /** The same list as the app's own languages, so picking one is familiar. */
    val choices
        get() = AppLanguage.choices.map { Choice(it.tag, it.name) }

    /** The name of the auto option (follow the app's language). */
    val autoName: String get() = str(R.string.language_youtube_auto)

    private const val PREFS = "content_language"
    private const val KEY_TAG = "tag"

    private lateinit var app: Application
    private var ready = false

    /** The picked tag, "" = follow the app's language. */
    var picked by mutableStateOf("")
        private set

    /** Bumped when [hl]/[gl] move, so screens holding YouTube data can reload. */
    var generation by mutableIntStateOf(0)
        private set

    fun init(application: Application) {
        app = application
        ready = true
        refresh()
    }

    /** Re-reads the pick (for example after the app's own language changed). */
    fun refresh() {
        if (!ready) return
        val before = hl to gl
        picked = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, "").orEmpty()
        changed(before)
    }

    fun pick(tag: String) {
        if (tag == picked) return
        val before = hl to gl
        picked = tag
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TAG, tag).apply()
        changed(before)
    }

    /** The language YouTube is asked in, e.g. "ja". */
    val hl: String
        get() {
            val locale = if (picked.isEmpty()) AppLanguage.locale else Locale.forLanguageTag(picked)
            return locale.language.ifBlank { "en" }
        }

    /** The country YouTube is asked as, e.g. "JP": the pick's region, else the language's home. */
    val gl: String
        get() {
            val locale = if (picked.isEmpty()) AppLanguage.locale else Locale.forLanguageTag(picked)
            return locale.country.ifBlank { COUNTRY_OF_LANGUAGE[locale.language] ?: "US" }
        }

    /** The language tag for YouTube pages a person reads. */
    val tag: String get() = if (picked.isNotEmpty()) picked else AppLanguage.webTag

    /** Everything YouTube served in the old language is stale: drop the cached stream pages. */
    private fun changed(before: Pair<String, String>) {
        if (before == (hl to gl)) return
        generation++
        StreamCache.invalidateAll()
    }

    /**
     * Where each of the app's languages is most at home. Regions matter to YouTube's catalogue
     * (trending, search ranking, available videos); the app's language list has no regions of its own.
     */
    private val COUNTRY_OF_LANGUAGE = mapOf(
        "en" to "US",
        "es" to "ES",
        "pt" to "BR",
        "fr" to "FR",
        "de" to "DE",
        "it" to "IT",
        "id" to "ID",
        "tr" to "TR",
        "vi" to "VN",
        "ru" to "RU",
        "ar" to "SA",
        "hi" to "IN",
        "ja" to "JP",
    )
}

/** An `Accept-Language` header for YouTube requests: the content language first, English as fallback. */
fun acceptLanguage(hl: String = ContentLanguage.hl): String = when (hl) {
    "en" -> "en-US,en;q=0.9"
    else -> "$hl,en-US;q=0.8,en;q=0.7"
}
