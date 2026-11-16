package com.yuno

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A bot check that outlasts the quick retries ([BotCheck], about 40 s) is waited out in the
 * background: one quiet load after every stretch in [DelaysMs], and again the moment the
 * default network changes — the check rides on the address, which is what the player's
 * airplane-mode hint is about. Listeners decide what a recovered load means for them:
 * the player state picks the video stuck on the error panel back up (the cache now holds
 * the real streams), and the service swaps a playing 360p fallback's source for the real
 * ones. [StreamCache] reports every fresh real load through [stop], so a wait started
 * before the video was retried by hand doesn't re-swap it later. A wait that runs out its
 * tries re-arms once more on the next network change ([REARM_MS] long), trying right away,
 * so the airplane toggle the panel suggests keeps working by itself.
 */
object BotCheckRecovery {
    private const val TAG = "Yuno"

    /** Waits between background loads: about 7½ minutes in four tries. */
    private val DelaysMs = longArrayOf(30_000L, 60_000L, 120_000L, 240_000L)

    /** How long after running out its tries a wait still re-arms on a network change. */
    private const val REARM_MS = 10 * 60 * 1000L

    /** Exhausted waits, by video with when they gave up: a network change gives one more. */
    private val rearmable = ConcurrentHashMap<String, Long>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val jobs = ConcurrentHashMap<String, Job>()

    /** Bumped when the default network changes; every wait watches it for a shortcut. */
    private val nudge = MutableStateFlow(0L)

    fun addListener(listener: (String) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners -= listener
    }

    /** Keeps trying [id] in the background, from scratch if a wait was already running.
     *  [rightAway]: skip the first stretch — a re-arm, the network has just changed. */
    fun schedule(id: String, rightAway: Boolean = false) {
        jobs.remove(id)?.cancel()
        jobs[id] = scope.launch {
            var first = rightAway
            for (wait in DelaysMs) {
                if (!first) {
                    val seen = nudge.value
                    // A network change (airplane mode, Wi-Fi to data) shortens the wait: the
                    // fresh address is the one thing that makes the check pass sooner.
                    withTimeoutOrNull(wait) { nudge.first { it != seen } }
                }
                first = false
                // A change may have cut the wait short: the fresh address needs a moment to
                // come up (loading the instant the network reappears just fails to connect).
                delay(3_000)
                if (attempt(id)) return@launch
            }
            Log.i(TAG, "bot check for $id; the waiting gave up (a network change tries again)")
            rearmable[id] = System.currentTimeMillis()
            jobs.remove(id)
        }
    }

    /**
     * The default network changed: the check rides on the address — every wait tries again
     * right away, and one that already ran out its tries ([REARM_MS] recent) gets a fresh
     * wait on top, so the panel's airplane-mode toggle keeps working by itself.
     */
    fun networkChanged() {
        nudge.update { it + 1 }
        val now = System.currentTimeMillis()
        val rearm = mutableListOf<String>()
        for ((id, gaveUpAt) in rearmable) {
            if (now - gaveUpAt > REARM_MS) rearmable.remove(id, gaveUpAt) else rearm += id
        }
        rearm.forEach { schedule(it, rightAway = true) }
    }

    /** Real streams for [id] arrived some other way (a retry by hand): no waiting needed. */
    fun stop(id: String) {
        jobs.remove(id)?.cancel()
        rearmable.remove(id)
    }

    /**
     * One quiet load. A refused one throws ([StreamCache] never keeps a refused load as
     * its answer), which is just "still checked": the next stretch tries again. A load
     * that returns is the real thing — the check passed for this video.
     */
    private suspend fun attempt(id: String): Boolean {
        try {
            StreamCache.getOrLoad(id)
        } catch (e: SignInConfirmNotBotException) {
            return false
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Type and message inline: release logs don't always carry the stack trace.
            Log.w(TAG, "background load for $id failed: ${e.javaClass.simpleName}: ${e.message}")
            return false
        }
        Log.i(TAG, "bot check for $id passed in the background")
        stop(id)
        listeners.forEach { listener ->
            runCatching { listener(id) }
        }
        return true
    }
}
