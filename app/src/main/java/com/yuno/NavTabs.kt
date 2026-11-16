package com.yuno

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The bottom tab bar's tabs and their order (Settings › Appearance › Tab bar): any of the
 * four tabs but Home can be hidden, and all of them can be reordered. Shorts answers to
 * [ShortsSetting] as well — with the Shorts switch off, it stays out of the bar whatever
 * this says.
 *
 * Saved in the "playback" preferences like the other switches, as one string: the full
 * order with a leading "-" on the hidden tabs — "Home,-Shorts,Subscriptions,Library".
 * Re-adding a tab puts it back at its place in this order.
 */
object NavTabs {
    private const val KEY = "nav_tabs"
    private var prefs: SharedPreferences? = null

    /** All four tabs in the bar's order, the hidden ones included. */
    var order by mutableStateOf(Tab.entries.toList())
        private set

    private var hidden by mutableStateOf<Set<Tab>>(emptySet())

    /** The tabs the bar shows, in [order]: its visible tabs, minus Shorts while its switch is off. */
    fun shown(): List<Tab> = order.filter { it !in hidden && (it != Tab.Shorts || ShortsSetting.enabled) }

    fun isHidden(tab: Tab): Boolean = tab in hidden

    fun init(context: Context) {
        prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
        val (savedOrder, savedHidden) = parseNavTabs(prefs?.getString(KEY, null))
        order = savedOrder
        hidden = savedHidden
    }

    /** [tab] shows in the bar again; with Shorts' own switch off it stays out until that is on too. */
    fun show(tab: Tab) {
        if (tab == Tab.Home || tab !in hidden) return
        hidden = hidden - tab
        persist()
    }

    fun hide(tab: Tab) {
        // Home is always in the bar — the app's floor, and with it so is at least one tab.
        if (tab == Tab.Home || tab in hidden) return
        hidden = hidden + tab
        persist()
    }

    /** Moves [tab] one slot up (delta −1) or down (delta 1) in [order]. */
    fun move(tab: Tab, delta: Int) {
        val from = order.indexOf(tab)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (from == to) return
        val list = order.toMutableList()
        list.removeAt(from)
        list.add(to, tab)
        order = list
        persist()
    }

    fun reset() {
        order = Tab.entries.toList()
        hidden = emptySet()
        persist()
    }

    private fun persist() {
        prefs?.edit()
            ?.putString(KEY, order.joinToString(",") { if (it in hidden) "-${it.name}" else it.name })
            ?.apply()
    }
}

/**
 * Reads the saved tab-bar string into its order and hidden set: names in order, "-" hiding
 * one. Unknown or repeated names are dropped, tabs missing from the string (a new one) join
 * at the end visible, a blank or unreadable string is the default, and Home is always
 * visible — it is the app's floor (Back lands there, at least one tab always shows).
 */
internal fun parseNavTabs(raw: String?): Pair<List<Tab>, Set<Tab>> {
    val default = Tab.entries.toList()
    if (raw.isNullOrBlank()) return default to emptySet()
    val order = LinkedHashSet<Tab>()
    val hidden = mutableSetOf<Tab>()
    raw.split(',').forEach { part ->
        val name = part.trim()
        if (name.isEmpty()) return@forEach
        val hide = name.startsWith("-")
        val tab = Tab.entries.firstOrNull { it.name == (if (hide) name.substring(1) else name) } ?: return@forEach
        if (order.add(tab) && hide) hidden += tab
    }
    if (order.isEmpty()) return default to emptySet()
    Tab.entries.forEach { if (it !in order) order += it }
    hidden.remove(Tab.Home)
    return order.toList() to hidden
}
