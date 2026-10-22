package io.openflux.android.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Tells when the phone's own network changes under a running connection: Wi-Fi turned off and mobile data took
 * over, or the other way round. The carriers' sockets stay on the network they were opened on; when it goes, they
 * are not told at once but hang, and the core never redials.
 *
 * It follows the real networks, never a VPN: with OpenFlux's VPN up the default network an app sees is that VPN,
 * and 3.10.2, which followed the default network, took the VPN coming up for a change and restarted the
 * connection, which brought the VPN down and up again, every few seconds. The network in use is the one Android
 * prefers among those up: Wi-Fi, then Ethernet, then mobile data.
 *
 * [onChange] gets the network now in use, once it moved from one network to another; a network that only went
 * away and came back as the same one is no change. The first network reported is where it starts.
 */
internal class DefaultNetworkWatch(context: Context, private val onChange: (Network) -> Unit) : AutoCloseable {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val tracker = DefaultNetworkTracker<Network>()
    private val up = PhysicalNetworks<Network>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val rank = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> return
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 1
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 2
                else -> 3
            }
            settle(up.put(network, rank))
        }

        override fun onLost(network: Network) {
            settle(up.remove(network))
        }
    }

    private fun settle(best: Network?) {
        if (best == null) return
        if (tracker.available(best)) onChange(best)
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    override fun close() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }
}

/** The real networks up and how Android prefers them (lower rank first); [put] and [remove] give the preferred one. */
internal class PhysicalNetworks<N : Any> {
    private val ranks = LinkedHashMap<N, Int>()

    @Synchronized
    fun put(network: N, rank: Int): N? {
        ranks[network] = rank
        return best()
    }

    @Synchronized
    fun remove(network: N): N? {
        ranks.remove(network)
        return best()
    }

    private fun best(): N? = ranks.entries.minByOrNull { it.value }?.key
}

/** Which network is in use, and whether a newly preferred one is a change from it (see [DefaultNetworkWatch]). */
internal class DefaultNetworkTracker<N : Any> {
    /** The last network in use: coming back on it is no change. */
    private var last: N? = null

    /** [network] is the one in use now; true when it differs from the one the connection was on. */
    @Synchronized
    fun available(network: N): Boolean {
        val before = last
        last = network
        return before != null && before != network
    }
}
