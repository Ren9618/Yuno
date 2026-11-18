package com.yuno

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The signed-in feeds can't be fetched in tests, but they're built from the same renderers
 * as these public responses (captured 2026-10-04): a playlist page (lockupViewModel +
 * continuationItemViewModel), search (videoRenderer + lockupViewModel) and watch-next.
 */
class InnerTubeParseTest {
    private fun fixture(name: String) = JSONObject(javaClass.classLoader!!.getResource("$name.json")!!.readText())

    @Test fun playlistHeaderFromFlow() {
        val root = JSONObject(
            """{"header":{"pageHeaderViewModel":{
                "metadata":{"contentMetadataViewModel":{"avatarStackViewModel":{"text":{"content":"by Example Channel"}}}},
                "description":{"descriptionPreviewViewModel":{"description":{"content":"A long description"}}}
            }}}""",
        )
        val header = InnerTube.parsePlaylistHeader(root)
        assertEquals("Example Channel", header.owner)
        assertEquals("A long description", header.description)
    }

    @Test fun playlistPage() {
        val page = InnerTube.parseFeed(fixture("playlist"))
        assertTrue("expected ~100 videos, got ${page.videos.size}", page.videos.size >= 90)
        assertNotNull("playlist continuation token", page.next)
        page.videos.forEach { v ->
            assertEquals(11, v.id.length)
            assertTrue("title for ${v.id}", v.title.isNotBlank())
        }
        assertTrue("most items have a duration", page.videos.count { it.durationSec > 0 } >= page.videos.size * 0.9)
        assertTrue(
            "lockup feeds must not keep 336px hqdefault URLs",
            page.videos.none { it.thumbnail.orEmpty().contains("/hqdefault") },
        )
        assertTrue(
            "playlist rows should carry channel avatars",
            page.videos.count { it.avatar != null } >= page.videos.size * 0.9,
        )
        val header = InnerTube.parsePlaylistHeader(fixture("playlist"))
        assertEquals("Kurzgesagt – In a Nutshell", header.owner)
        assertEquals("", header.description)
        page.videos.filter { "/ytc/" in it.avatar.orEmpty() }.forEach { v ->
            val edge = Regex("=s(\\d+)").find(v.avatar!!)!!.groupValues[1].toInt()
            assertTrue("ytc avatar must not be over-sharpened: ${v.avatar}", edge <= 96)
        }
    }

    @Test fun ultrawideQualityTiers() {
        assertEquals(2160, qualityTier(3840, 1634))
        assertEquals(1080, qualityTier(1920, 818))
        assertEquals(720, qualityTier(1280, 544))
        assertEquals(1080, qualityTier(1920, 1080))
    }

    @Test fun normalizeYtcToGoogleusercontent() {
        val ggpht = "https://yt3.ggpht.com/ytc/AIdro_n1Ribd7LwdP_qKtqWL3ZDfIgv9M1d6g78VwpHGXVR2Ir4=s6"
        val out = normalizeChannelAvatarUrl(ggpht)
        assertTrue(out.contains("googleusercontent.com/ytc/"))
        assertTrue(out.contains("-c-k-c0x00ffffff-no-rj"))
    }

    @Test fun sharpenYtcChannelAvatar() {
        val raw = "https://yt3.ggpht.com/ytc/AIdro_n1Ribd7LwdP_qKtqWL3ZDfIgv9M1d6g78VwpHGXVR2Ir4=s6"
        val out = sharpenChannelAvatarUrl(raw, 176)
        assertTrue(out.contains("googleusercontent.com/ytc/"))
        assertTrue(out.contains("=s176"))
        val broken = raw.replace("=s6", "=s176")
        assertTrue(sharpenChannelAvatarUrl(broken, 48).contains("=s88"))
    }

    @Test fun searchPageKeepsMixedOrder() {
        val page = InnerTube.parseSearch(fixture("search"))
        assertTrue("search hits", page.hits.isNotEmpty())
        assertNotNull("search continuation", page.next)
    }

    /** Flow keeps a Shorts strip as a shelf. Flattening it would turn All into a list of shorts. */
    @Test fun searchKeepsShortsShelves() {
        val page = InnerTube.parseSearch(fixture("search"))
        val shelves = page.hits.filterIsInstance<SearchHit.Shelf>()
        assertEquals(2, shelves.size)
        assertTrue(shelves.all { it.kind == SearchShelfKind.SHORTS && it.videos.isNotEmpty() })
        assertTrue(page.hits.none { it is SearchHit.Video && it.short })
        assertTrue(page.hits.any { it is SearchHit.Channel })
        assertTrue(page.hits.any { it is SearchHit.Playlist })
        val shorts = page.hits.asShortsResults()
        val shelfIds = shelves.flatMap { it.videos }.map { it.id }.toSet()
        val shortIds = shorts.map { (it as SearchHit.Video).item.id }.toSet()
        assertTrue(shortIds.containsAll(shelfIds))
        assertTrue(shorts.all { it is SearchHit.Video && it.short })
    }

    @Test fun searchResults() {
        val page = InnerTube.parseFeed(fixture("search"))
        assertTrue("got ${page.videos.size}", page.videos.size >= 10)
        assertNotNull("search continuation", page.next)
        assertTrue("channel names", page.videos.count { it.channel.isNotBlank() } >= page.videos.size * 0.8)
        assertTrue("view counts", page.videos.count { it.views > 0 } >= page.videos.size * 0.8)
        assertTrue(
            "high-res thumbnails from InnerTube, not hqdefault placeholders",
            page.videos.count { !it.thumbnail.orEmpty().endsWith("/hqdefault.jpg") } >= page.videos.size * 0.8,
        )
    }

    @Test fun shortsAreKeptBesideTheVideosNotInThem() {
        val page = InnerTube.parseFeed(fixture("search"))
        // The search page's shorts shelf: its shorts land in [FeedPage.shorts], never in [videos]
        // (the history reads the same feed shape and shows them as their own row).
        assertTrue("got ${page.shorts.size} shorts", page.shorts.isNotEmpty())
        assertTrue("shorts carry their ids", page.shorts.all { it.id.length == 11 })
        assertTrue("shorts stay out of the videos", page.videos.none { v -> page.shorts.any { it.id == v.id } })
    }

    @Test fun shortsOnlyPlaylists() {
        fun item(id: String, dur: Long, live: Boolean = false) =
            VideoItem(id, "t", "c", null, null, null, dur, 1, null, live)
        // A short's card carries no duration at all (a lockup) or at most the cap; anything
        // longer, or a live stream, is a video's playlist.
        assertTrue(listOf(item("abcdefghijk", 0L), item("lmnopqrstu", 31L)).shortsOnly())
        assertTrue(listOf(item("abcdefghijk", 62L)).shortsOnly())
        assertFalse(listOf(item("abcdefghijk", 63L)).shortsOnly())
        assertFalse(listOf(item("abcdefghijk", 0L, live = true)).shortsOnly())
        assertFalse(emptyList<VideoItem>().shortsOnly())
    }

    @Test fun savedShortsChoices() {
        val saved = PlaylistChoice("PL123", "保存したショート動画", contains = true, privacy = "PRIVATE")
        val english = PlaylistChoice("PL123", "Saved shorts", contains = false, privacy = "PUBLIC")
        val other = PlaylistChoice("PL456", "Watch later", contains = null, privacy = null)
        assertTrue(saved.isSavedShorts)
        assertTrue(english.isSavedShorts)
        assertFalse(other.isSavedShorts)
    }

    /**
     * The author's linked video (the blue-circle row), read off the short's page: a button
     * tapping a reelWatchEndpoint is another short, one tapping a watchEndpoint is a video,
     * and a page without the carousel (an outside link only, or none) answers null.
     */
    @Test fun shortLinkedVideo() {
        fun page(vararg buttons: String) = """<html><script>var ytInitialData = {"overlay":{
            "reelCarouselViewModel":{"buttonViewModels":[${buttons.joinToString()}]}
        }};</script></html>"""
        // Live pages carry the title inside the second `buttonViewModel`, right beside `onTap`.
        fun short(id: String) = """{"reelCarouselButtonViewModel":{"buttonViewModel":{"buttonViewModel":{
            "onTap":{"innertubeCommand":{"reelWatchEndpoint":{"videoId":"$id",
            "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/$id/frame0.jpg","width":1080,"height":1920}]}}}},
            "titleFormatted":{"content":"映画再現⁉︎ 一番くじ vol.2"},
            "accessibilityText":"映画再現⁉︎ 一番くじ vol.2"}}}}"""
        fun video(id: String) = """{"reelCarouselButtonViewModel":{"buttonViewModel":{"buttonViewModel":{
            "onTap":{"innertubeCommand":{"watchEndpoint":{"videoId":"$id"}}},
            "titleFormatted":{"content":"A full-length video"}}}}}"""
        fun outside(url: String) = """{"reelCarouselButtonViewModel":{"buttonViewModel":{"buttonViewModel":{
            "onTap":{"innertubeCommand":{"urlEndpoint":{"url":"$url"}}},
            "titleFormatted":{"content":"The shop"}}}}}"""
        // An older capture keeps the title beside the nested button instead: still parsed.
        fun flatShort(id: String) = """{"reelCarouselButtonViewModel":{"buttonViewModel":{
            "buttonViewModel":{"onTap":{"innertubeCommand":{"reelWatchEndpoint":{"videoId":"$id"}}}},
            "titleFormatted":{"content":"フラットな古い形"},
            "accessibilityText":"フラットな古い形"}}}"""
        val short = InnerTube.parseShortLinkedVideo(page(short("ILUzMJlFluQ")))
        assertEquals("ILUzMJlFluQ", short?.id)
        assertEquals("映画再現⁉︎ 一番くじ vol.2", short?.title)
        assertEquals(true, short?.short)
        assertEquals("https://i.ytimg.com/vi/ILUzMJlFluQ/frame0.jpg", short?.thumbnail)
        val video = InnerTube.parseShortLinkedVideo(page(video("abcdefghijk"), short("ILUzMJlFluQ"), outside("https://example.com")))
        assertEquals("abcdefghijk", video?.id)
        assertEquals(false, video?.short)
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", video?.thumbnail)
        // The outside link is not a video: nothing plays, so nothing comes back.
        assertNull(InnerTube.parseShortLinkedVideo(page(outside("https://example.com"))))
        assertNull(InnerTube.parseShortLinkedVideo("<html>no data here</html>"))
        assertEquals("フラットな古い形", InnerTube.parseShortLinkedVideo(page(flatShort("ILUzMJlFluQ")))?.title)
    }

    /**
     * A playlist of shorts (the Saved shorts one): YouTube's browse answers with the shorts
     * beside an empty videos list, which showed as a blank playlist. The fold moves them into
     * the videos, and a page that has videos (any other playlist) is left as it is.
     */
    @Test fun shortsPlaylistFoldsIntoVideos() {
        val root = JSONObject(
            """{"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[
                {"shortsLockupViewModel":{"entityId":"shorts-video-1","accessibilityText":"A short - play Short",
                 "onTap":{"innertubeCommand":{"reelWatchEndpoint":{"videoId":"n2sn4kkkGQM"}}},
                 "overlayMetadata":{"primaryText":{"content":"The Deadly Power of a Coin-Sized Black Hole"},
                                    "secondaryText":{"content":"20 million views"}}}}]}}]}""",
        )
        val page = InnerTube.parseFeed(root)
        assertTrue("shorts beside the videos, got ${page.videos.size} videos and ${page.shorts.size} shorts",
            page.videos.isEmpty() && page.shorts.size == 1)
        val folded = page.withShortsAsVideos()
        assertEquals(page.shorts, folded.videos)
        assertTrue("plays as shorts, got $folded", folded.videos.shortsOnly())
        // Any other playlist has videos: the fold changes nothing.
        val withVideos = page.copy(videos = listOf(page.shorts.first())).withShortsAsVideos()
        assertEquals(1, withVideos.videos.size)
        assertEquals(1, withVideos.shorts.size)
    }

    /** The Saved shorts playlist is known by its title (its id differs across accounts). */
    @Test fun savedShortsByTitle() {
        assertTrue(isSavedShortsTitle("保存したショート動画"))
        assertTrue(isSavedShortsTitle("Saved shorts"))
        assertTrue(isSavedShortsTitle("SAVED SHORTS"))
        assertFalse(isSavedShortsTitle("Watch later"))
        assertFalse(isSavedShortsTitle("あとで見る"))
    }

    @Test fun watchNextRelated() {
        val page = InnerTube.parseFeed(fixture("next"))
        assertTrue("got ${page.videos.size}", page.videos.size >= 10)
        assertTrue("channel names", page.videos.count { it.channel.isNotBlank() } >= page.videos.size * 0.8)
        // Khan Academy's "Fundraiser" badge has the same BADGE_COMMERCE style as movie offers.
        assertTrue("fundraiser video kept", page.videos.any { it.id == "-7Gl-yKF6Y4" })
    }

    @Test fun watchPageDescriptionAndRelated() {
        val page = InnerTube.parseWatchPage(fixture("next"))
        assertTrue("expected the description, got ${page.description?.take(40)}", !page.description.isNullOrBlank())
        assertTrue("expected the related feed, got ${page.related.size}", page.related.size >= 10)
        // The video itself (videoPrimaryInfoRenderer) must not come back as its own related row.
        assertTrue("no self row", page.related.none { it.title.isBlank() })
    }

    /** Less Bitter's Videos tab (captured 2026-10-06): its "Insider Edition" uploads are members-only. */
    @Test fun channelVideosDropMembersOnly() {
        val page = InnerTube.parseFeed(fixture("channel_videos"))
        assertTrue("got ${page.videos.size}", page.videos.size >= 10)
        assertTrue("members-only leaked: ${page.videos.filter { "Insider Edition" in it.title }.map { it.title }}",
            page.videos.none { "Insider Edition" in it.title })
        assertNotNull("channel continuation", page.next)
    }

    /**
     * HBO Max's channel page (captured 2026-10-13): 4 movies as gridVideoRenderer ("Try now" + rating)
     * and 4 Primetime lockups (BADGE_COMMERCE + rating), then 4 ordinary search results.
     */
    @Test fun moviesDropped() {
        val page = InnerTube.parseFeed(fixture("movies"))
        assertTrue("movies leaked: ${page.videos.filter { "HBO Max" in it.channel }.map { it.title }}",
            page.videos.none { "HBO Max" in it.channel })
        assertEquals("ordinary videos kept", 4, page.videos.size)
    }

    /** The Shorts feed (reel/reel_watch_sequence), captured anonymously 2026-10-28 (prefetch data stripped). */
    @Test fun shortsSequence() {
        val page = InnerTube.parseShorts(fixture("reel_sequence"))
        assertEquals(listOf("6x4mqgyvCZ8", "0fgRDkGLIkA", "yLMNlEddt1E", "vtmR8DikTx0", "ylMTylT1Hdw", "R-74QNLTgxA"), page.videos.map { it.id })
        page.videos.forEach { assertTrue("first frame for ${it.id}", it.thumbnail!!.startsWith("https://i.ytimg.com/vi/${it.id}/")) }
        assertNotNull("sequence continuation", page.next)
    }

    /** The seed that worked in the 2026-10-28 probe: id, then params { 3 = 5 }, then 10 = 25. */
    @Test fun reelSequenceParams() {
        val bytes = java.util.Base64.getDecoder().decode(InnerTube.reelSequenceParams("m7fHuAcbD0A"))
        assertEquals(listOf(0x0A, 11) + "m7fHuAcbD0A".map { it.code } + listOf(0x2A, 0x02, 0x18, 0x05, 0x50, 25), bytes.map { it.toInt() })
    }

    /**
     * Home (FEwhat_to_watch) with its category chips, captured 2026-11-02 for an anonymous visitor
     * with a few watches behind it (a fresh visitor gets only "Try searching to get started"),
     * trimmed to 8 lockups + the feed's continuation (ads, tracking, visitor data stripped).
     */
    @Test fun homeChips() {
        val root = fixture("home_chips")
        val chips = InnerTube.parseHomeChips(root)
        assertEquals("All", chips.first().title)
        assertEquals("the selected chip is the plain feed", null, chips.first().token)
        assertEquals("21 captured, Live left out", 20, chips.size)
        assertTrue(chips.drop(1).all { it.token != null })
        assertTrue(chips.map { it.title }.containsAll(listOf("Music", "Gaming", "Rhythm & Blues", "Recently uploaded")))
        assertTrue("live streams can't play", chips.none { it.title == "Live" })
        val page = InnerTube.parseFeed(root)
        assertEquals(8, page.videos.size)
        assertNotNull("feed continuation", page.next)
        assertTrue("a chip token taken for the next page", chips.none { it.token == page.next })
        // Search's filter chips (same chipCloudChipRenderer, in searchHeaderRenderer) aren't Home's.
        assertTrue(InnerTube.parseHomeChips(fixture("search")).isEmpty())
    }

    /** "Your custom feed" opens a prompt box Skipii can't show (its page had no videos), so it's left out. */
    @Test fun promptChipDropped() {
        val root = fixture("home_chips")
        val bar = root.findObject("feedFilterChipBarRenderer")!!.getJSONArray("contents")
        bar.put(JSONObject(bar.getJSONObject(1).toString()).apply {
            getJSONObject("chipCloudChipRenderer").put("text", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Your custom feed"))))
        })
        val chips = InnerTube.parseHomeChips(root)
        assertEquals(20, chips.size)
        assertTrue(chips.none { it.title == "Your custom feed" })
    }

    /**
     * Live streams aren't played, so feeds leave them out: the HBO Max lockups badged LIVE /
     * "Now playing" on their thumbnails (movies.json) are live; a video whose channel is live right
     * now (`liveData` on the avatar only, home_chip_feed.json) is an ordinary upload and stays.
     */
    @Test fun liveDropped() {
        val lockups = mutableListOf<JSONObject>()
        fixture("movies").walk { key, o -> if (key == "lockupViewModel") lockups += o }
        val badged = lockups.filter { "THUMBNAIL_OVERLAY_BADGE_STYLE_LIVE" in it.toString() }
        assertEquals(4, badged.size)
        assertTrue(badged.all { InnerTube.isLive(it) })
        val feed = mutableListOf<JSONObject>()
        fixture("home_chip_feed").walk { key, o -> if (key == "lockupViewModel") feed += o }
        assertEquals("channel live now, video isn't", 2, feed.count { "liveData" in it.toString() })
        assertTrue(feed.none { InnerTube.isLive(it) })
        // Older renderers: the LIVE NOW badge, the time overlay styled LIVE, an upcoming premiere.
        fun renderer(extra: JSONObject.() -> Unit) = JSONObject().put("videoId", "abcdefghijk")
            .put("title", JSONObject().put("simpleText", "A stream")).apply(extra)
        val page = InnerTube.parseFeed(JSONObject().put("items", JSONArray()
            .put(JSONObject().put("videoRenderer", renderer {
                put("badges", JSONArray().put(JSONObject().put("metadataBadgeRenderer", JSONObject().put("style", "BADGE_STYLE_TYPE_LIVE_NOW"))))
            }))
            .put(JSONObject().put("videoRenderer", renderer {
                put("thumbnailOverlays", JSONArray().put(JSONObject().put("thumbnailOverlayTimeStatusRenderer", JSONObject().put("style", "LIVE"))))
            }))
            .put(JSONObject().put("videoRenderer", renderer { put("upcomingEventData", JSONObject().put("startTime", "1793700000")) }))
            .put(JSONObject().put("videoRenderer", renderer { put("videoId", "kept_video1") }))))
        assertEquals(listOf("kept_video1"), page.videos.map { it.id })
    }

    /** The "Music" chip's answer (same capture): reloadContinuationItemsCommand on the feed's body, its own continuation. */
    @Test fun homeChipFeed() {
        val page = InnerTube.parseFeed(fixture("home_chip_feed"))
        assertEquals(8, page.videos.size)
        assertTrue(page.next!!.startsWith("4qmFsgK"))
        page.videos.forEach { assertTrue("title for ${it.id}", it.title.isNotBlank()) }
    }

    @Test fun viewCounts() {
        assertEquals(1_234_567L, InnerTube.parseViews("1,234,567 views"))
        assertEquals(1_200_000L, InnerTube.parseViews("1.2M views"))
        assertEquals(36_000L, InnerTube.parseViews("36K views"))
        assertEquals(4_850_000L, InnerTube.parseViews("4.85M subscribers"))
        assertEquals(1_100L, InnerTube.parseViews("1.1K videos"))
        assertEquals(0L, InnerTube.parseViews("No views"))
        assertEquals(-1L, InnerTube.parseViews(null))
    }
}
