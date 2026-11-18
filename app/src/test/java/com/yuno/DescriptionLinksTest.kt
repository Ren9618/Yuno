package com.yuno

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fork's clickable descriptions ([DescriptionLinks]): where the links sit in the text,
 * what they open, and what stays out of them.
 */
class DescriptionLinksTest {
    @Test fun aPlainUrlIsALink() {
        val text = "look: https://example.com/x?a=1 end"
        assertEquals(listOf("https://example.com/x?a=1"), descriptionLinks(text).map { it.url })
        assertEquals(text.indexOf("https"), descriptionLinks(text).first().start)
        assertEquals(text.indexOf(" end"), descriptionLinks(text).first().end)
    }

    @Test fun theSentencesPunctuationStaysOutOfTheLink() {
        assertEquals("https://example.com/x", descriptionLinks("見て https://example.com/x。").single().url)
        assertEquals("https://example.com/x", descriptionLinks("(https://example.com/x)").single().url)
        assertEquals("https://example.com/x", descriptionLinks("https://example.com/x,").single().url)
    }

    @Test fun sentencesWithoutASchemeGetNoLinks() {
        assertTrue(descriptionLinks("example.com と www.example.com のみ").isEmpty())
        assertTrue(descriptionLinks("ただの文章").isEmpty())
    }

    @Test fun everyLinkIsFoundLeftToRight() {
        val text = "https://a.b/one 中割り https://c.d/two?x=1#f https://e.f/three"
        assertEquals(
            listOf("https://a.b/one", "https://c.d/two?x=1#f", "https://e.f/three"),
            descriptionLinks(text).map { it.url },
        )
        assertTrue(descriptionLinks(text).zipWithNext().all { (a, b) -> a.end < b.start })
    }

    @Test fun timestampsAreFoundWithTheirTimes() {
        val text = "0:00 始まり\n1:23 二つ目\n1:02:03 長い章"
        assertEquals(listOf(0L, 83_000L, 3_723_000L), descriptionTimestamps(text).map { it.ms })
        assertEquals(text.indexOf("0:00"), descriptionTimestamps(text).first().start)
        assertEquals(text.indexOf("0:00") + 4, descriptionTimestamps(text).first().end)
    }

    @Test fun timestampsInsideUrlsStayPartOfTheUrl() {
        val text = "see https://example.com/watch?v=x&t=1:23 for the good part and 0:45 here"
        val links = descriptionLinks(text)
        assertEquals(listOf(45_000L), descriptionTimestamps(text, links).map { it.ms })
    }

    @Test fun clockReadingsAreNotTimestamps() {
        assertTrue(descriptionTimestamps("0:60 と 9:61 は時計の読み").isEmpty())
        assertTrue(descriptionTimestamps("v0:00x は語の一部").isEmpty())
    }
}
