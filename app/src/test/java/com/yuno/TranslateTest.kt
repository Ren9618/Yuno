package com.yuno

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Google Translate's public gtx answer ([Translator.parse]): the translated text joined
 * from its segments, the detected source language, and the shapes that answer "no".
 */
class TranslateTest {
    @Test fun readsTheTranslationAndTheDetectedSource() {
        val answer = Translator.parse("""[[["こんにちは","Hello",null,null,10]],null,"en"]""")
        assertEquals("こんにちは", answer?.text)
        assertEquals("en", answer?.source)
    }

    @Test fun joinsEverySegment() {
        val answer = Translator.parse("""[[["こん","Hel"],["にちは","lo"]],null,"en"]""")
        assertEquals("こんにちは", answer?.text)
        assertEquals("en", answer?.source)
    }

    @Test fun noSourceLanguageWhenAbsent() {
        val answer = Translator.parse("""[[["こんにちは","Hello"]]]""")
        assertEquals("こんにちは", answer?.text)
        assertNull(answer?.source)
    }

    @Test fun shapesThatAnswerNothing() {
        assertNull(Translator.parse("not json"))
        assertNull(Translator.parse("""[null,null,"en"]"""))
        assertNull(Translator.parse("""[[[]]]"""))
    }

    @Test fun theUrlCarriesTheRequest() {
        val url = Translator.url("Hello", "ja")
        assertTrue(url.contains("client=gtx"))
        assertTrue(url.contains("sl=auto"))
        assertTrue(url.contains("tl=ja"))
        assertTrue(url.contains("dt=t"))
        assertTrue(url.contains("q=Hello"))
    }
}
