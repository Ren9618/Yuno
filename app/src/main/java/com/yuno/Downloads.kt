package com.yuno

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log

/**
 * Saving a video's own file (the fork): the chosen stream's URL handed to Android's Download
 * Manager, which shows the progress in its notification and puts the file away — in a Yuno
 * folder inside Downloads on Android 10 and later, in the app's own download folder before
 * that (writing the public one needs a storage permission the app doesn't ask for). The URLs
 * are the same signed, expiring ones playback plays; the download sheet asks for them fresh
 * and a 403 there just reads as a failure, with the external downloader as the other way.
 */
object Downloads {
    private const val TAG = "Yuno"

    /** "My Video (360p).mp4": the title made safe for a file name and not too long. */
    fun fileName(title: String, tag: String?, ext: String): String {
        val base = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").replace(Regex(" +"), " ")
            .trim().take(120).ifBlank { "video" }
        val mid = if (tag.isNullOrBlank()) "" else " ($tag)"
        return "$base$mid.$ext"
    }

    /**
     * Hands the file to the Download Manager. False (nothing shown anywhere) when the phone
     * has no download manager or it refused the request; the sheet then says so and the
     * external downloader row is still there.
     */
    fun save(context: Context, title: String, url: String, fileName: String): Boolean {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription(title)
            .setMimeType(mimeOf(fileName))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // The player's own agent, so googlevideo answers the way it answers the app.
            .addRequestHeader("User-Agent", Net.USER_AGENT)
        if (Build.VERSION.SDK_INT >= 29) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Yuno/$fileName")
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        return try {
            manager.enqueue(request) >= 0
        } catch (e: Exception) {
            Log.w(TAG, "download $fileName", e)
            false
        }
    }

    private fun mimeOf(fileName: String): String = when {
        fileName.endsWith(".mp4") -> "video/mp4"
        fileName.endsWith(".m4a") -> "audio/mp4"
        fileName.endsWith(".webm") -> "video/webm"
        fileName.endsWith(".opus") -> "audio/ogg"
        else -> "application/octet-stream"
    }
}
