package com.yuno

import android.app.Activity
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.nio.ByteBuffer
import kotlin.system.exitProcess

/**
 * Debug-only probe of the device's raw framework MediaCodec stack. The Codec2 client path
 * was proven to fault at codec create ([MediaCodec.createByCodecName] died on
 * `c2.android.aac.encoder` at the same address the media3 and NDK crashes use), and
 * `OMX.google.*` names turned out to be aliases for C2 components, so both client paths
 * crash. The remaining question is whether the fault is inherited through the app zygote
 * or hits every process: [DecoderTest.run] can also be driven through a fresh `app_process`
 * exec (not a zygote fork) as a standalone main:
 *
 * ```
 * su -c 'CP=$(pm path --user 11 com.yuno | head -1 | sed "s/^package://"); \
 *        CLASSPATH="$CP" app_process /system/bin --nice-name=decoderprobe com.yuno.DecoderTest'
 * ```
 *
 * A pass there would mean the corruption is zygote-inherited and a decoder helper process
 * could bypass it; a crash there means the ROM's media client stack is broken everywhere.
 *
 * Stages, each logged line by line; the last line before a native crash marks the call:
 * 1. `[omx-aac]` create/configure/start `OMX.google.aac.decoder` (an alias for the C2 one)
 * 2. `[omx-avc]` create/configure/start `OMX.google.h264.decoder` (no surface, buffers)
 * 3. `[omx-mp3]` create/configure/start `OMX.google.mp3.decoder`
 * 4. `[c2-aac]`  the control — the C2 decoder media3 died on.
 *
 * Registered only in debug builds (app/src/debug/AndroidManifest.xml). Launch:
 * `adb shell am start --user 11 -n com.yuno/.DecoderTestActivity`
 */
class DecoderTestActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply {
            setPadding(48, 120, 48, 48)
            setTextIsSelectable(true)
        }
        setContentView(status)
        say("probe starts, pid=${android.os.Process.myPid()}")
        Thread({ DecoderTest.run(::say) }, "DecoderProbe").apply { start() }
    }

    /** One log line per step; the last line before a native crash marks the faulting call. */
    private fun say(message: String) {
        Log.i(DecoderTest.TAG, message)
        runOnUiThread {
            status.text = "${status.text}${if (status.text.isEmpty()) "" else "\n"}$message"
        }
    }
}

/** The probe itself: shared by the debug activity and the standalone `app_process` main. */
object DecoderTest {
    const val TAG = "DecoderTest"

    /** Entry for `app_process … com.yuno.DecoderTest`: same stages, stdout instead of UI. */
    @JvmStatic
    fun main(args: Array<String>) {
        run { message ->
            println(message)
            Log.i(TAG, message)
        }
        exitProcess(0)
    }

    fun run(log: (String) -> Unit) {
        log("device=${Build.DEVICE} android=${Build.VERSION.RELEASE} pid=${android.os.Process.myPid()}")
        // How this process mapped the media client libs: in app processes the writable
        // tail (BSS) of libmedia.so sits in an unmapped hole, and the C2 client init
        // writes into it. The fresh app_process exec maps it properly — that difference
        // is the whole bug.
        runCatching {
            java.io.File("/proc/self/maps").bufferedReader().useLines { lines ->
                lines.filter { "libmedia" in it || "libcodec2" in it || "libstagefright" in it }
                    .forEach { log("[maps] $it") }
            }
        }
        val names = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.joinToString(",") { it.name }
        log("codecs: $names")
        stage(log, "[omx-aac]", "OMX.google.aac.decoder") { audioProbe(it) }
        stage(log, "[omx-avc]", "OMX.google.h264.decoder") { videoProbe(it) }
        stage(log, "[omx-mp3]", "OMX.google.mp3.decoder") { audioProbe(it) }
        // The control: the C2 decoder media3 crashed on, untouched by any renderer.
        stage(log, "[c2-aac]", "c2.android.aac.decoder") { audioProbe(it) }
        log("probe done")
    }

    private fun stage(log: (String) -> Unit, tag: String, codecName: String, body: (String) -> Unit) {
        try {
            body(codecName)
            log("$tag OK ($codecName)")
        } catch (t: Throwable) {
            log("$tag FAILED ($codecName): ${t.javaClass.simpleName}: ${t.message}")
            Log.w(TAG, "$tag failed", t)
        }
    }

    /** create → configure (csd for AAC) → start → release, no buffers moved. */
    private fun audioProbe(codecName: String) {
        Log.i(TAG, "create $codecName")
        val codec = MediaCodec.createByCodecName(codecName)
        try {
            val format = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2)
            // AudioSpecificConfig: AAC-LC, 44.1 kHz, stereo (0x12 0x20).
            format.setByteBuffer("csd-0", ByteBuffer.wrap(byteArrayOf(0x12, 0x20)))
            Log.i(TAG, "configure (44.1kHz stereo, csd-0 2B)")
            codec.configure(format, null, null, 0)
            Log.i(TAG, "start")
            codec.start()
            Log.i(TAG, "stop+release")
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
    }

    /** create → configure (no surface, no csd) → start → release, buffers only. */
    private fun videoProbe(codecName: String) {
        Log.i(TAG, "create $codecName")
        val codec = MediaCodec.createByCodecName(codecName)
        try {
            val format = MediaFormat.createVideoFormat("video/avc", 320, 240)
            Log.i(TAG, "configure 320x240, no surface")
            codec.configure(format, null, null, 0)
            Log.i(TAG, "start")
            codec.start()
            Log.i(TAG, "stop+release")
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
    }
}
