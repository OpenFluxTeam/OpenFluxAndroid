package io.openflux.android.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** When the phone's own network counts as changed under a connection (DefaultNetworkWatch). */
class DefaultNetworkTrackerTest {
    @Test
    fun theFirstNetworkIsWhereItStarts() {
        val t = DefaultNetworkTracker<String>()
        assertFalse(t.available("wifi"))
    }

    @Test
    fun anotherNetworkIsAChange() {
        val t = DefaultNetworkTracker<String>()
        t.available("wifi")
        assertTrue(t.available("cell"))
        assertTrue(t.available("wifi"))
    }

    @Test
    fun theSameNetworkAgainIsNoChange() {
        val t = DefaultNetworkTracker<String>()
        t.available("wifi")
        assertFalse(t.available("wifi"))
    }

    @Test
    fun wifiIsPreferredOverMobileDataAsAndroidDoes() {
        val up = PhysicalNetworks<String>()
        assertEquals("cell", up.put("cell", 2))
        assertEquals("wifi", up.put("wifi", 0))
        // Capabilities change often (signal, validation): the same networks keep the same choice.
        assertEquals("wifi", up.put("cell", 2))
    }

    @Test
    fun wifiOffLeavesMobileDataThenNothing() {
        val up = PhysicalNetworks<String>()
        up.put("wifi", 0)
        up.put("cell", 2)
        assertEquals("cell", up.remove("wifi"))
        assertNull(up.remove("cell"))
    }

    /** What 3.10.2 did in a loop: the VPN going down and up is no change of the real network. */
    @Test
    fun theVpnComingAndGoingChangesNothing() {
        val t = DefaultNetworkTracker<String>()
        val up = PhysicalNetworks<String>()
        assertFalse(t.available(up.put("wifi", 0)!!))
        repeat(5) { assertFalse(t.available(up.put("wifi", 0)!!)) }
    }
}
