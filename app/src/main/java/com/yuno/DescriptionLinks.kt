package com.yuno

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

/**
 * The fork's clickable descriptions (a short's 概要欄, the video's own): the words stay as
 * they are, and every `https://…` in them becomes a link the browser opens. The scan is
 * plain text handling, so [descriptionLinks] tests without a device.
 */

/** A link inside a description: the url to open and where it sits in the text. */
internal data class DescriptionLink(val url: String, val start: Int, val end: Int)

/** A description's `m:ss` / `h:mm:ss` timestamp: where it seeks to and where it sits in the text. */
internal data class DescriptionTimestamp(val ms: Long, val start: Int, val end: Int)

/**
 * Every `http(s)://…` run in [text], left to right. The sentence's trailing punctuation
 * (「。」、 a closing bracket) stays out of the link — it opens nowhere.
 */
internal fun descriptionLinks(text: String): List<DescriptionLink> {
    if (!text.contains("://")) return emptyList()
    val links = mutableListOf<DescriptionLink>()
    val matcher = LINK.matcher(text)
    while (matcher.find()) {
        var url = matcher.group()
        while (url.length > "http://".length && url.last() in LINK_TRAILING) url = url.dropLast(1)
        links += DescriptionLink(url, matcher.start(), matcher.start() + url.length)
    }
    return links
}

/**
 * Every chapter timestamp (`m:ss` or `h:mm:ss`) in [text], left to right — the timestamps a
 * creator typed into their 概要欄. One that sits inside one of [links] (a url that carries a
 * `t=1:23` in it) is part of the link, not its own seek; a minute part over 59 for seconds is
 * a clock reading, not a chapter, and goes nowhere.
 */
internal fun descriptionTimestamps(text: String, links: List<DescriptionLink> = descriptionLinks(text)): List<DescriptionTimestamp> {
    if (!text.contains(':')) return emptyList()
    val stamps = mutableListOf<DescriptionTimestamp>()
    val matcher = TIMESTAMP.matcher(text)
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        if (links.any { link -> start < link.end && link.start < end }) continue
        val parts = matcher.group().split(':').map { it.toInt() }
        val ms = when {
            parts.size == 3 && parts[1] < 60 && parts[2] < 60 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            parts.size == 2 && parts[1] < 60 -> (parts[0] * 60L + parts[1]) * 1000L
            else -> continue
        }
        stamps += DescriptionTimestamp(ms, start, end)
    }
    return stamps
}

/**
 * A description's [text] with its links clickable ([onOpen] answers to a tap) and — when
 * [onSeek] is given (a video's own 概要欄; shorts keep it null) — its timestamps seeking
 * there, all tinted [color]. [onSeek] sits before [onOpen] so calls can keep their tap
 * lambda trailing.
 */
internal fun linkifiedDescription(text: String, color: Color, onSeek: ((Long) -> Unit)? = null, onOpen: (String) -> Unit): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val links = descriptionLinks(text)
        links.forEach { link ->
            addLink(
                LinkAnnotation.Url(
                    link.url,
                    TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline)),
                    { onOpen(link.url) },
                ),
                link.start,
                link.end,
            )
        }
        if (onSeek != null) {
            descriptionTimestamps(text, links).forEach { stamp ->
                addLink(
                    LinkAnnotation.Clickable(
                        "seek:${stamp.ms}",
                        TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline)),
                        { onSeek(stamp.ms) },
                    ),
                    stamp.start,
                    stamp.end,
                )
            }
        }
    }

/** YouTube's own dark-theme link blue; a link reads as a link on the short's shade and the player's paper. */
internal val DescriptionLinkBlue = Color(0xFF3EA6FF)

private val LINK = Pattern.compile("https?://\\S+")
private const val LINK_TRAILING = ".,;:!?、。)）」』】〉\"'"

/** A chapter timestamp: `m:ss`, optionally `h:mm:ss`, not glued to digits, letters or another clock around it. */
private val TIMESTAMP = Pattern.compile("(?<![0-9A-Za-z:])([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)(?![0-9])")
