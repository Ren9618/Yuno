package com.yuno

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * YouTube answers in the app's language now ([ContentLanguage]), so these are the Japanese forms
 * of the metadata Yuno parses: 「1.2万回視聴」, 「3時間前」, and the account channel list
 * (`account/accounts_list`) whose brand items carry a page id and YouTube's own switch URL.
 */
class JapaneseParseTest {
    @Test fun viewCounts() {
        assertEquals(12_000L, InnerTube.parseViews("1.2万回視聴"))
        assertEquals(120_000L, InnerTube.parseViews("12万回視聴"))
        assertEquals(123_450_000L, InnerTube.parseViews("1億2345万回視聴"))
        assertEquals(340_000_000L, InnerTube.parseViews("3.4億回視聴"))
        assertEquals(0L, InnerTube.parseViews("視聴回数はありません"))
        // The English forms must keep working (the extractor still answers in English).
        assertEquals(1_234_567L, InnerTube.parseViews("1,234,567 views"))
        assertEquals(1_200_000L, InnerTube.parseViews("1.2M views"))
        assertEquals(0L, InnerTube.parseViews("No views"))
        assertEquals(-1L, InnerTube.parseViews(null))
        assertEquals(-1L, InnerTube.parseViews(""))
    }

    @Test fun metadataLines() {
        assertTrue(InnerTube.looksLikeViews("1.2万回視聴"))
        assertTrue(InnerTube.looksLikeViews("1,234 views"))
        assertFalse(InnerTube.looksLikeViews("Yunoa"))
        assertTrue(InnerTube.looksLikeAge("3時間前"))
        assertTrue(InnerTube.looksLikeAge("2日前"))
        assertTrue(InnerTube.looksLikeAge("たった今"))
        assertTrue(InnerTube.looksLikeAge("3 days ago"))
        assertFalse(InnerTube.looksLikeAge("Yunoa"))
        assertFalse(InnerTube.looksLikeAge("動画"))
    }

    @Test fun ages() {
        assertEquals(10_800_000L, NewVideos.ageMs("3時間前"))
        assertEquals(172_800_000L, NewVideos.ageMs("2日前"))
        assertEquals(0L, NewVideos.ageMs("たった今"))
        // English still parses: the extractor's fallback and NewPipe's own pages use it.
        assertEquals(10_800_000L, NewVideos.ageMs("Streamed 3 hours ago"))
        assertNull(NewVideos.ageMs("2024/01/01"))
        assertNull(NewVideos.ageMs(null))
    }

    private fun runs(text: String) = JSONObject().put("runs", JSONArray().put(JSONObject().put("text", text)))

    private fun accountItem(
        name: String,
        handle: String?,
        selected: Boolean,
        pageId: String? = null,
        signinUrl: String? = null,
    ): JSONObject {
        val item = JSONObject()
            .put("accountName", runs(name))
            .put("isSelected", selected)
            .put("accountPhoto", JSONObject().put("thumbnails", JSONArray().put(JSONObject().put("url", "//yt3.example/avatar.jpg"))))
        handle?.let { item.put("channelHandle", runs(it)) }
        if (pageId != null || signinUrl != null) {
            val tokens = JSONArray()
            pageId?.let { tokens.put(JSONObject().put("pageIdToken", JSONObject().put("pageId", it))) }
            signinUrl?.let { tokens.put(JSONObject().put("accountSigninToken", JSONObject().put("signinUrl", it))) }
            item.put(
                "serviceEndpoint",
                JSONObject().put(
                    "selectActiveIdentityEndpoint",
                    JSONObject().put("supportedTokens", tokens),
                ),
            )
        }
        return JSONObject().put("accountItem", item)
    }

    @Test fun brandAccounts() {
        val root = JSONObject().put(
            "actions",
            JSONArray().put(
                JSONObject().put(
                    "accountItems",
                    JSONArray()
                        // The personal channel has no page id and no switch URL.
                        .put(accountItem("Yuno", "@yuno", selected = false))
                        .put(
                            accountItem(
                                "Yuno Music",
                                "@yunomusic",
                                selected = true,
                                pageId = "123456789012345678901",
                                signinUrl = "https://accounts.google.com/SignOutOptions?pageId=123456789012345678901",
                            ),
                        )
                        // A protocol-relative URL must become absolute.
                        .put(accountItem("Yuno Labs", null, selected = false, pageId = "987654321098765432109", signinUrl = "//accounts.google.com/SignOutOptions?pageId=2")),
                ),
            ),
        )
        val accounts = Account.parseBrandAccounts(root)
        assertEquals(3, accounts.size)

        val personal = accounts[0]
        assertEquals("Yuno", personal.name)
        assertEquals("@yuno", personal.handle)
        assertEquals("https://yt3.example/avatar.jpg", personal.avatar)
        assertNull(personal.pageId)
        assertNull(personal.signinUrl)
        assertFalse(personal.selected)

        val brand = accounts[1]
        assertEquals("Yuno Music", brand.name)
        assertEquals("@yunomusic", brand.handle)
        assertEquals("123456789012345678901", brand.pageId)
        assertEquals("https://accounts.google.com/SignOutOptions?pageId=123456789012345678901", brand.signinUrl)
        assertTrue(brand.selected)

        val labs = accounts[2]
        assertNull(labs.handle)
        assertEquals("https://accounts.google.com/SignOutOptions?pageId=2", labs.signinUrl)
    }

    @Test fun brandAccountsEmpty() {
        assertTrue(Account.parseBrandAccounts(JSONObject()).isEmpty())
    }
}
