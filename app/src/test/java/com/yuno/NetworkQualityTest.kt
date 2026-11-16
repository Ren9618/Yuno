package com.yuno

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The fork's per-network quality ([NetworkQuality]): the connection's own ceiling rides above
 * the player's pick, and Auto on that connection (or an unknown one) leaves the player's pick.
 */
class NetworkQualityTest {
    @Test fun theConnectionsOwnCeilingComesFirst() {
        assertEquals(480, NetworkQuality.effective(global = 1080, wifi = 480, mobile = 720, kind = NetworkQuality.Kind.Wifi))
        assertEquals(720, NetworkQuality.effective(global = 1080, wifi = 480, mobile = 720, kind = NetworkQuality.Kind.Mobile))
    }

    @Test fun autoOnTheConnectionLeavesThePlayersPick() {
        assertEquals(1080, NetworkQuality.effective(global = 1080, wifi = null, mobile = 720, kind = NetworkQuality.Kind.Wifi))
        assertEquals(1080, NetworkQuality.effective(global = 1080, wifi = 480, mobile = null, kind = NetworkQuality.Kind.Mobile))
    }

    @Test fun anUnknownConnectionLeavesThePlayersPick() {
        assertNull(NetworkQuality.effective(global = null, wifi = 480, mobile = 720, kind = null))
        assertEquals(1080, NetworkQuality.effective(global = 1080, wifi = 480, mobile = 720, kind = null))
    }

    @Test fun nothingSetIsAutoEverywhere() {
        assertNull(NetworkQuality.effective(global = null, wifi = null, mobile = null, kind = NetworkQuality.Kind.Wifi))
        assertNull(NetworkQuality.effective(global = null, wifi = null, mobile = null, kind = NetworkQuality.Kind.Mobile))
    }
}
