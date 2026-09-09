/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailcommon.data.network

import java.util.concurrent.atomic.AtomicBoolean
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class NetworkManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkManager() {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Every new collector of observe() calls register(), and two landing together would both pass a plain check.
    private val registered = AtomicBoolean(false)

    // Serialises publishing. The callbacks arrive on one thread, but refresh() runs on the app scope
    // and its read costs up to three binder calls. Without this a slow read can publish after a
    // newer one has already gone out.
    private val statusLock = Any()

    // What the collectors last saw, and what [networkStatus] answers while registered. Written under
    // [statusLock]. If read without it: locking the read would park the main thread behind the binder
    // calls refresh() makes.
    @Volatile
    private var publishedStatus: NetworkStatus = NetworkStatus.Disconnected

    /*
     * The network status is two facts, tracked apart. [describedStatus] is what the network is, from the
     * capabilities the system hands us. [accessBlocked] is whether this app may use it.
     *
     * Doze, App Standby and Data Saver move the second without touching the first, and a blocked
     * app is shown no active network, so one read cannot tell them apart.
     *
     * No callback reads [connectivityManager]. The platform documents that a synchronous call made
     * from inside one can return a stale or null object, and a null reads as `Disconnected`.
     */
    @Volatile
    private var describedNetwork: Network? = null

    @Volatile
    private var describedStatus: NetworkStatus = NetworkStatus.Disconnected

    @Volatile
    private var accessBlocked: Boolean = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        // The capabilities follow immediately, so there is nothing to publish yet.
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            trackDescribedNetwork(network)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)

            trackDescribedNetwork(network)
            describedStatus = networkCapabilities.toDescribedStatus()
            publish()
        }

        override fun onLost(network: Network) {
            super.onLost(network)

            // Only if the network we hold is the one that went away. A handover can report the old
            // network as lost after the new one was described, and wiping on that reports
            // `Disconnected` over a live connection.
            if (network != describedNetwork) return

            describedNetwork = null
            describedStatus = NetworkStatus.Disconnected
            publish()
        }

        /**
         * Doze, App Standby and Data Saver block the app's network access without touching the
         * network. This is the only callback the system fires on either edge.
         *
         * The flag is stored rather than turned into a status. A block changes what the app may do,
         * not what the network is, and both edges need the other fact intact to be answered.
         *
         * [network] is not checked. A default network callback describes one network at a time, and
         * a block left over from a replaced network is cleared in [trackDescribedNetwork].
         */
        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
            super.onBlockedStatusChanged(network, blocked)

            accessBlocked = blocked
            publish()
        }

        // A blocked state belongs to a network, and the system only guarantees the callback on the
        // blocked edge, so a network we have not seen starts unblocked. Keeping the old network's
        // block is what pins a wifi-to-cellular handover to `Disconnected`.
        private fun trackDescribedNetwork(network: Network) {
            if (network == describedNetwork) return

            describedNetwork = network
            accessBlocked = false
        }
    }

    /**
     * The tracked status while registered, a fresh read otherwise.
     *
     * Callers read this from the main thread and the system read costs up to three binder calls.
     * [refresh] is what re-reads it. An unregistered manager has nothing tracked to answer with.
     */
    override val networkStatus: NetworkStatus
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        get() = if (registered.get()) publishedStatus else readCurrentStatus()

    override val activeNetwork: Network?
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        get() = connectivityManager.activeNetwork

    /**
     * Independent of [networkStatus] on purpose. `Disconnected` is a valid status and not an answer
     * to "does this connection cost the user money". Anything we cannot inspect is assumed to.
     */
    override val isMetered: Boolean
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        get() {
            val activeNetwork = connectivityManager.activeNetwork ?: return true
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return true
            return capabilities.isActiveNetworkMetered()
        }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun register() {
        if (registered.compareAndSet(false, true)) {
            synchronized(statusLock) {
                // Seeds both facts until the first onCapabilitiesChanged replaces them, so a
                // blocked edge arriving first has something to answer with.
                val status = readCurrentStatus()
                publishedStatus = status
                describedStatus = status
                accessBlocked = false

                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            }
        }
    }

    override fun unregister() {
        if (registered.compareAndSet(true, false)) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun refresh() {
        synchronized(statusLock) {
            val status = readCurrentStatus()

            // A read that sees a usable network outranks a tracked block. A blocked app is shown no
            // active network, so seeing one means the block is gone, callback or not.
            //
            // A read that sees nothing only decides what is published. It cannot tell a blocked app
            // from a device with no network, and overwriting [describedStatus] would lose the
            // network the app is blocked from.
            if (status != NetworkStatus.Disconnected) {
                describedStatus = status
                accessBlocked = false
            }

            publishedStatus = status
            notifyObservers(status)
        }
    }

    // Both facts are read inside the lock. Reading them first would let a [refresh] landing in
    // between publish its answer and have this one overwrite it.
    private fun publish() {
        synchronized(statusLock) {
            notifyStatus(if (accessBlocked) NetworkStatus.Disconnected else describedStatus)
        }
    }

    private fun notifyStatus(status: NetworkStatus) {
        synchronized(statusLock) {
            if (publishedStatus == status) return
            publishedStatus = status
            notifyObservers(status)
        }
    }

    private fun readCurrentStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkStatus.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkStatus.Disconnected

        return when {
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkStatus.Disconnected
            capabilities.isActiveNetworkMetered() -> NetworkStatus.Metered
            else -> NetworkStatus.Unmetered
        }
    }

    // The capabilities handed to a callback, and nothing else. Corroborating them with
    // isActiveNetworkMetered needs activeNetwork read as non-null first, or the two describe
    // different networks - and a blocked app has none, which makes every network metered.
    private fun NetworkCapabilities.toDescribedStatus(): NetworkStatus = when {
        !hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkStatus.Disconnected
        !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkStatus.Metered
        else -> NetworkStatus.Unmetered
    }

    // NET_CAPABILITY_NOT_METERED describes the transport, and a VPN can advertise it regardless of
    // what it tunnels over. Proton users often run VPN, and getting this wrong means
    // backfilling a mailbox over cellular, so either signal saying "metered" wins.
    // isActiveNetworkMetered resolves through the tunnel. Only valid on the active network.
    private fun NetworkCapabilities.isActiveNetworkMetered(): Boolean =
        !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            connectivityManager.isActiveNetworkMetered
}
