package io.openflux.android.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** When the phone's own network counts as changed under a connection (DefaultNetworkWatch). */
class DefaultNetworkTrackerTest {
    @Test
    fun theFirstNetworkIsWhereItStarts() {
        val t = DefaultNetworkTracker<String>()
        assertFalse(t.available("wifi"))
    }

    @Test
    fun wifiOffThenMobileDataIsAChange() {
        val t = DefaultNetworkTracker<String>()
        t.available("wifi")
        t.lost("wifi")
        assertTrue(t.available("cell"))
    }

    @Test
    fun aDirectSwitchIsAChange() {
        val t = DefaultNetworkTracker<String>()
        t.available("cell")
        assertTrue(t.available("wifi"))
    }

    @Test
    fun theSameNetworkBackIsNoChange() {
        val t = DefaultNetworkTracker<String>()
        t.available("wifi")
        t.lost("wifi")
        assertFalse(t.available("wifi"))
        assertFalse(t.available("wifi"))
    }

    @Test
    fun losingAnotherNetworkChangesNothing() {
        val t = DefaultNetworkTracker<String>()
        t.available("wifi")
        t.lost("cell")
        assertFalse(t.available("wifi"))
    }
}
