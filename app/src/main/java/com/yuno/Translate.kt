package com.yuno

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale

/**
 * Comment translation: a comment's text turned into the app's language, via Google
 * Translate's public web endpoint (`translate_a/single` with `client=gtx` — the same one
 * community tools use; no key, no account, and the comments are public anyway). Like the
 * other community extras ([Enhancers]), the request leaves Yuno itself, so the toggle-less
 * UI keeps it small: a per-comment "Translate" button that fills the answer in below.
 */
object Translator {
    /** The translated text, plus the source language Google detected ("en"). */
    data class Translation(val text: String, val source: String?)

    fun url(text: String, target: String): String =
        "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", "auto")
            .addQueryParameter("tl", target)
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", text.take(4000))
            .build().toString()

    /** The translation for [text] in [target] (a language code like "ja"), or null. */
    suspend fun translate(text: String, target: String): Translation? = withContext(Dispatchers.IO) {
        runCatching {
            Net.client.newCall(
                Request.Builder().url(url(text, target)).header("User-Agent", Net.USER_AGENT).build()
            ).execute().use { answer ->
                if (!answer.isSuccessful) return@use null
                parse(answer.body?.string().orEmpty())
            }
        }.getOrNull()
    }

    /** [parse] with everything passed in, so tests stay free of any network. */
    internal fun parse(body: String): Translation? {
        val json = runCatching { JSONArray(body) }.getOrNull() ?: return null
        val segments = json.optJSONArray(0) ?: return null
        val out = StringBuilder()
        for (i in 0 until segments.length()) {
            val segment = segments.optJSONArray(i) ?: return null
            out.append(segment.optString(0))
        }
        val text = out.toString()
        if (text.isBlank()) return null
        return Translation(text, json.optString(2).takeIf { it.isNotEmpty() })
    }

    /** The source language's name in the app's language ("en" → "英語" / "English"). */
    fun displaySource(tag: String): String =
        Locale.forLanguageTag(tag).getDisplayLanguage(AppLanguage.locale)
}
