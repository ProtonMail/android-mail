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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@SuppressLint("MissingPermission")
class NetworkManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkManager() {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Compare-and-set rather than a volatile flag: register() is reached from every new collector of
    // observe(), which is app-scoped and collected from more than one place, so two of them landing
    // together would both pass a plain check and registerDefaultNetworkCallback would throw.
    private val registered = AtomicBoolean(false)

    @Volatile
    private var _networkStatus: NetworkStatus = determineCurrentNetworkStatus()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLost(network: Network) {
            super.onLost(network)
            updateNetworkStatus()
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateNetworkStatus()
        }
    }

    override val networkStatus: NetworkStatus
        get() = _networkStatus

    override val activeNetwork: Network?
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        get() = connectivityManager.activeNetwork

    /**
     * Deliberately independent of [networkStatus]: "disconnected" is a valid status but not a
     * valid answer to "does this connection cost the user money", and anything we cannot inspect
     * has to be assumed to cost money.
     */
    override val isMetered: Boolean
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        get() {
            val activeNetwork = connectivityManager.activeNetwork ?: return true
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return true
            return capabilities.isMetered()
        }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun register() {
        if (registered.compareAndSet(false, true)) {
            // Get current state before registering
            _networkStatus = determineCurrentNetworkStatus()
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    override fun unregister() {
        if (registered.compareAndSet(true, false)) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun updateNetworkStatus() {
        val newStatus = determineCurrentNetworkStatus()

        if (_networkStatus != newStatus) {
            _networkStatus = newStatus
            notifyObservers(newStatus)
        }
    }

    private fun determineCurrentNetworkStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            return NetworkStatus.Disconnected
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            return NetworkStatus.Disconnected
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        return when {
            !hasInternet -> NetworkStatus.Disconnected
            capabilities.isMetered() -> NetworkStatus.Metered
            else -> NetworkStatus.Unmetered
        }
    }

    // NET_CAPABILITY_NOT_METERED describes the transport, and a VPN commonly advertises it
    // regardless of what it tunnels over - Proton users frequently run Proton VPN, and getting
    // this wrong means backfilling a mailbox over cellular. isActiveNetworkMetered resolves
    // through the tunnel, so treating either signal saying "metered" as metered covers the VPN
    // case without having to recognise the transport.
    private fun NetworkCapabilities.isMetered(): Boolean =
        !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            connectivityManager.isActiveNetworkMetered
}
