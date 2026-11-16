package com.yuno

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The saved tab-bar string ([NavTabs], [parseNavTabs]): the full order with a leading "-"
 * on the hidden tabs. Unknown or repeated names drop, missing tabs join at the end, a
 * blank or unreadable string is the default, and Home is never hidden.
 */
class NavTabsTest {
    @Test fun defaultWhenBlank() {
        val (order, hidden) = parseNavTabs(null)
        assertEquals(Tab.entries.toList(), order)
        assertTrue(hidden.isEmpty())
        val (blank, blankHidden) = parseNavTabs("")
        assertEquals(Tab.entries.toList(), blank)
        assertTrue(blankHidden.isEmpty())
    }

    @Test fun orderAndHiddenRoundTrip() {
        val (order, hidden) = parseNavTabs("Home,-Shorts,Subscriptions,Library")
        assertEquals(listOf(Tab.Home, Tab.Shorts, Tab.Subscriptions, Tab.Library), order)
        assertEquals(setOf(Tab.Shorts), hidden)
    }

    @Test fun unknownNamesDrop() {
        val (order, hidden) = parseNavTabs("Bogus,Home,Library,-Shorts,Subscriptions")
        assertEquals(listOf(Tab.Home, Tab.Library, Tab.Shorts, Tab.Subscriptions), order)
        assertEquals(setOf(Tab.Shorts), hidden)
    }

    @Test fun missingTabsJoinAtTheEnd() {
        val (order, hidden) = parseNavTabs("Library,Home")
        assertEquals(listOf(Tab.Library, Tab.Home, Tab.Shorts, Tab.Subscriptions), order)
        assertTrue(hidden.isEmpty())
    }

    @Test fun repeatedNameKeepsItsFirstSpot() {
        // The first mention wins: both its place in the order and its visibility.
        val (order, hidden) = parseNavTabs("Home,-Library,Shorts,Subscriptions,Library")
        assertEquals(listOf(Tab.Home, Tab.Library, Tab.Shorts, Tab.Subscriptions), order)
        assertTrue(Tab.Library in hidden)
    }

    @Test fun homeIsNeverHidden() {
        val (order, hidden) = parseNavTabs("-Home,-Shorts,Subscriptions,Library")
        assertEquals(listOf(Tab.Home, Tab.Shorts, Tab.Subscriptions, Tab.Library), order)
        assertFalse(Tab.Home in hidden)
        assertEquals(setOf(Tab.Shorts), hidden)
    }

    @Test fun gibberishIsTheDefault() {
        val (order, hidden) = parseNavTabs(",,,")
        assertEquals(Tab.entries.toList(), order)
        assertTrue(hidden.isEmpty())
    }
}
