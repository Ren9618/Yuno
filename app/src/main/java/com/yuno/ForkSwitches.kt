package com.yuno

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The switches for the fork's own additions (Settings › Fork features), grouped on that page:
 * the short's linked-video and sound rows, the library's saved-shorts pin and history shelf,
 * the player's chapter marks, comment translation, and the bot check's background recovery.
 * They are the fork's, so they can be turned off without touching the app's own settings;
 * every one is on by default. A switch only hides the fork's layer — the page, comments and
 * chapters the app already loads are not fetched differently.
 */
object ForkSwitches {
    private const val KEY_SHORT_LINKED = "fork_short_linked"
    private const val KEY_SHORT_SOUND = "fork_short_sound"
    private const val KEY_SAVED_SHORTS = "fork_saved_shorts_pin"
    private const val KEY_HISTORY_SHORTS = "fork_history_shorts"
    private const val KEY_CHAPTERS = "fork_chapter_marks"
    private const val KEY_TRANSLATION = "fork_comment_translation"
    private const val KEY_BOT_RECOVERY = "fork_bot_recovery"

    private var prefs: SharedPreferences? = null

    /** The short's linked-video chip (the author's blue-circle row). */
    var shortLinkedVideo by mutableStateOf(true)
        private set
    /** The short's sound chip and the wall of other shorts that use it. */
    var shortSound by mutableStateOf(true)
        private set
    /** YouTube's saved-shorts playlist pinned in Library. */
    var savedShortsPin by mutableStateOf(true)
        private set
    /** History's Shorts shelf beside the videos. */
    var historyShortsShelf by mutableStateOf(true)
        private set
    /** The scrubber's chapter notches and the description's tappable timestamps. */
    var chapterMarks by mutableStateOf(true)
        private set
    /** The Translate button under comments. */
    var commentTranslation by mutableStateOf(true)
        private set
    /** Waiting a bot check out in the background and reopening the video by itself. */
    var botCheckRecovery by mutableStateOf(true)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
        shortLinkedVideo = read(KEY_SHORT_LINKED)
        shortSound = read(KEY_SHORT_SOUND)
        savedShortsPin = read(KEY_SAVED_SHORTS)
        historyShortsShelf = read(KEY_HISTORY_SHORTS)
        chapterMarks = read(KEY_CHAPTERS)
        commentTranslation = read(KEY_TRANSLATION)
        botCheckRecovery = read(KEY_BOT_RECOVERY)
    }

    private fun read(key: String): Boolean = prefs?.getBoolean(key, true) != false

    private fun save(key: String, on: Boolean) {
        prefs?.edit()?.putBoolean(key, on)?.apply()
    }

    fun updateShortLinkedVideo(on: Boolean) {
        if (on == shortLinkedVideo) return
        shortLinkedVideo = on
        save(KEY_SHORT_LINKED, on)
    }

    fun updateShortSound(on: Boolean) {
        if (on == shortSound) return
        shortSound = on
        save(KEY_SHORT_SOUND, on)
    }

    fun updateSavedShortsPin(on: Boolean) {
        if (on == savedShortsPin) return
        savedShortsPin = on
        save(KEY_SAVED_SHORTS, on)
    }

    fun updateHistoryShortsShelf(on: Boolean) {
        if (on == historyShortsShelf) return
        historyShortsShelf = on
        save(KEY_HISTORY_SHORTS, on)
    }

    fun updateChapterMarks(on: Boolean) {
        if (on == chapterMarks) return
        chapterMarks = on
        save(KEY_CHAPTERS, on)
    }

    fun updateCommentTranslation(on: Boolean) {
        if (on == commentTranslation) return
        commentTranslation = on
        save(KEY_TRANSLATION, on)
    }

    fun updateBotCheckRecovery(on: Boolean) {
        if (on == botCheckRecovery) return
        botCheckRecovery = on
        save(KEY_BOT_RECOVERY, on)
    }
}
