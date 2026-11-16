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

/** A description's [text] with its links clickable ([onOpen] answers to a tap), tinted [color]. */
internal fun linkifiedDescription(text: String, color: Color, onOpen: (String) -> Unit): AnnotatedString =
    buildAnnotatedString {
        append(text)
        descriptionLinks(text).forEach { link ->
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
    }

/** YouTube's own dark-theme link blue; a link reads as a link on the short's shade and the player's paper. */
internal val DescriptionLinkBlue = Color(0xFF3EA6FF)

private val LINK = Pattern.compile("https?://\\S+")
private const val LINK_TRAILING = ".,;:!?、。)）」』】〉\"'"
