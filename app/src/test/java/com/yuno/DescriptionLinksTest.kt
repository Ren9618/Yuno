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
}
