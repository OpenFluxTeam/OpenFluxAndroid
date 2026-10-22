package io.openflux.android.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

/**
 * Tells when the phone's own network changes under a running connection: Wi-Fi turned off and mobile data took
 * over, or the other way round. The carriers' sockets stay on the network they were opened on; when it goes, they
 * are not told at once but hang, and the core never redials. OpenFlux is left out of its own VPN, so the default
 * network this app sees is the real one, not the VPN.
 *
 * [onChange] gets the network now in use, once the default moved from one network to another; a network that
 * only went away and came back as the same one is no change. The first network reported is where it starts.
 */
internal class DefaultNetworkWatch(context: Context, private val onChange: (Network) -> Unit) : AutoCloseable {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val tracker = DefaultNetworkTracker<Network>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (tracker.available(network)) onChange(network)
        }

        override fun onLost(network: Network) {
            tracker.lost(network)
        }
    }

    init {
        cm.registerDefaultNetworkCallback(callback)
    }

    override fun close() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }
}

/** Which default network is in use, and whether a newly reported one is a change from it (see [DefaultNetworkWatch]). */
internal class DefaultNetworkTracker<N : Any> {
    /** The network in use; null while there is none (lost, the next not reported yet). */
    private var current: N? = null
    /** The last network in use, kept through a loss: coming back on it is no change. */
    private var last: N? = null

    /** [network] is the default now; true when it differs from the one the connection was on. */
    @Synchronized
    fun available(network: N): Boolean {
        val before = last
        current = network
        last = network
        return before != null && before != network
    }

    @Synchronized
    fun lost(network: N) {
        if (network == current) current = null
    }
}
