package com.yuno

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.services.youtube.ItagItem
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.VideoStream

/**
 * The decoder policy ([CodecSettings]): which codec family a mime or video stream belongs
 * to, which decoders count as software, and what the switches let through.
 */
class CodecSettingsTest {
    @Test fun mimeFamilies() {
        assertEquals("avc", CodecSettings.familyOf("video/avc"))
        assertEquals("hevc", CodecSettings.familyOf("video/hevc"))
        assertEquals("vp9", CodecSettings.familyOf("video/x-vnd.on2.vp9"))
        assertEquals("av1", CodecSettings.familyOf("video/av01"))
        // Audio mimes and unknown ones belong to no family: the video switches leave them alone.
        assertNull(CodecSettings.familyOf("audio/mp4a-latm"))
        assertNull(CodecSettings.familyOf("audio/opus"))
        assertNull(CodecSettings.familyOf(null))
    }

    @Test fun softwareDecodersByFlagOrName() {
        assertTrue(CodecSettings.isSoftware(true, "c2.exynos.h264.decoder"))
        assertTrue(CodecSettings.isSoftware(false, "c2.android.avc.decoder"))
        assertTrue(CodecSettings.isSoftware(false, "OMX.google.h264.decoder"))
        assertFalse(CodecSettings.isSoftware(false, "c2.exynos.h264.decoder"))
        assertFalse(CodecSettings.isSoftware(false, "OMX.qcom.video.decoder.avc"))
    }

    @Test fun policyRules() {
        val avcOff = setOf("avc")
        // A disabled family is out entirely: every decoder for it, hardware or software.
        assertFalse(CodecSettings.allows(false, avcOff, false, "c2.exynos.h264.decoder", "video/avc"))
        assertFalse(CodecSettings.allows(false, avcOff, true, "c2.android.avc.decoder", "video/avc"))
        // Software-only keeps the software decoders for every mime…
        assertTrue(CodecSettings.allows(true, emptySet(), true, "c2.android.aac.decoder", "audio/mp4a-latm"))
        // …but a disabled family is still out even in software.
        assertFalse(CodecSettings.allows(true, avcOff, true, "c2.android.avc.decoder", "video/avc"))
        // …and drops the hardware ones.
        assertFalse(CodecSettings.allows(true, emptySet(), false, "c2.exynos.h264.decoder", "video/avc"))
        // No switches on: nothing is filtered.
        assertTrue(CodecSettings.allows(false, emptySet(), false, "c2.exynos.h264.decoder", "video/avc"))
    }

    @Test fun streamFamilies() {
        assertEquals("avc", StreamSelection.familyOf(stream("avc1.640028", MediaFormat.MPEG_4)))
        assertEquals("vp9", StreamSelection.familyOf(stream("vp09.00.10.08", MediaFormat.WEBM)))
        assertEquals("av1", StreamSelection.familyOf(stream("av01.0.04M.08", MediaFormat.WEBM)))
        assertEquals("hevc", StreamSelection.familyOf(stream("hvc1.1.6.L153.B0", MediaFormat.MPEG_4)))
        // No codec string: the container decides (YouTube's mp4 muxed rungs are AVC).
        assertEquals("avc", StreamSelection.familyOf(stream(null, MediaFormat.MPEG_4)))
        assertEquals("vp9", StreamSelection.familyOf(stream(null, MediaFormat.WEBM)))
    }

    private fun stream(codec: String?, format: MediaFormat): VideoStream {
        val itag = ItagItem(137, ItagItem.ItagType.VIDEO_ONLY, format, "720p")
        itag.setCodec(codec)
        return VideoStream.Builder()
            .setId("137")
            .setContent("https://example.test/v", true)
            .setMediaFormat(format)
            .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP)
            .setIsVideoOnly(true)
            .setResolution("720p")
            .setItagItem(itag)
            .build()
    }
}
