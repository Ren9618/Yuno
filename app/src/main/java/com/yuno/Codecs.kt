package com.yuno

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Which MediaCodec decoders the players may use (Morphe's codec switches, plus a way out for
 * devices whose vendor codecs crash natively — the same fault addr on this build's video *and*
 * audio decode, so the whole hardware set can be skipped).
 *
 * [softwareOnly] lets only software decoders be created (OMX.google.*, c2.android.*): every
 * stream decodes on the CPU, slower but alive. The four [disabled] families (AVC, HEVC, VP9,
 * AV1) instead take one video codec family out of stream picking, so playback falls back to
 * the next family that's still on — or to sound alone. Everything is read live: the renderers'
 * codec selector and the stream picking both check the switches as they go, so a flip applies
 * on the next video opened (or on the next decoder choice of the running one).
 *
 * Lives in the "playback" prefs like the other playback modes; [SyncedRenderersFactory] wraps
 * its selectors through [wrap], and [StreamSelection] drops disabled families from the streams
 * it offers.
 */
@OptIn(UnstableApi::class)
object CodecSettings {
    private const val KEY_SOFTWARE = "software_decoders_only"
    private const val KEY_SYNC = "codec_sync_adapter"
    private const val KEY_DISABLED_PREFIX = "codec_disabled_"
    private var prefs: SharedPreferences? = null

    /** The video codec families with a switch, in the Settings order. */
    val families = listOf("avc", "hevc", "vp9", "av1")

    /** True: only software decoders (OMX.google.*, c2.android.*) may be created. */
    var softwareOnly by mutableStateOf(false)
        private set

    /** True: MediaCodec runs through the synchronous adapter (no async callback loop). */
    var syncAdapters by mutableStateOf(false)
        private set

    /** Family ("avc"…) → true while its video streams are not offered. */
    var disabled by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
        softwareOnly = prefs?.getBoolean(KEY_SOFTWARE, false) == true
        syncAdapters = prefs?.getBoolean(KEY_SYNC, false) == true
        disabled = families.associateWith { prefs?.getBoolean(KEY_DISABLED_PREFIX + it, false) == true }
    }

    fun updateSoftwareOnly(on: Boolean) {
        softwareOnly = on
        prefs?.edit()?.putBoolean(KEY_SOFTWARE, on)?.apply()
    }

    fun updateSyncAdapters(on: Boolean) {
        syncAdapters = on
        prefs?.edit()?.putBoolean(KEY_SYNC, on)?.apply()
    }

    fun updateDisabled(family: String, on: Boolean) {
        if (family !in families) return
        disabled = disabled + (family to on)
        prefs?.edit()?.putBoolean(KEY_DISABLED_PREFIX + family, on)?.apply()
    }

    fun isDisabled(family: String): Boolean = disabled[family] == true
    val hasDisabled: Boolean get() = disabled.values.any { it }
    private val disabledFamilies: Set<String> get() = disabled.filterValues { it }.keys

    /**
     * The selector the renderers build with: unchanged while no switch is on, else one that
     * drops every candidate the policy rules out. An empty list marks the format unsupported,
     * so the player moves to another codec (or leaves the video track off).
     */
    fun wrap(delegate: MediaCodecSelector): MediaCodecSelector {
        if (!softwareOnly && !hasDisabled) return delegate
        return MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            delegate.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                .filter { allows(it) }
        }
    }

    /** The policy for one decoder candidate, under the current switches. */
    private fun allows(info: MediaCodecInfo): Boolean =
        allows(softwareOnly, disabledFamilies, info.softwareOnly, info.name, info.codecMimeType)

    /** The policy for one decoder candidate, given the switches (pure, so tests can check it). */
    fun allows(
        softwareOnlySwitch: Boolean,
        disabledFamilies: Set<String>,
        decoderSoftwareOnly: Boolean,
        decoderName: String,
        decoderCodecMimeType: String?,
    ): Boolean {
        familyOf(decoderCodecMimeType)?.let { if (it in disabledFamilies) return false }
        if (softwareOnlySwitch && !isSoftware(decoderSoftwareOnly, decoderName)) return false
        return true
    }

    /** A decoder is software when media3 says so, or by the software-only name prefixes. */
    fun isSoftware(decoderSoftwareOnly: Boolean, decoderName: String): Boolean =
        decoderSoftwareOnly ||
            decoderName.startsWith("OMX.google") ||
            decoderName.startsWith("c2.android") ||
            decoderName.startsWith("c2.google")

    /** The codec family a decoder mime belongs to ("video/avc", "video/x-vnd.on2.vp9", "video/av01"…). */
    fun familyOf(codecMimeType: String?): String? {
        val mime = codecMimeType ?: return null
        return when {
            "avc" in mime || "h264" in mime -> "avc"
            "hevc" in mime || "h265" in mime -> "hevc"
            "vp9" in mime || "vp09" in mime -> "vp9"
            "vp8" in mime -> "vp8"
            "av01" in mime || "av1" in mime -> "av1"
            else -> null
        }
    }
}
