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

package ch.protonmail.android.mailcommon.domain.network

import java.util.concurrent.CopyOnWriteArraySet
import android.Manifest
import android.net.Network
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Ported from core.network.data.
 *
 * Allows checking connectivity state (disconnected/metered/unmetered).
 */
abstract class NetworkManager {

    // Mutated from the connectivity callback thread while being iterated from collectors, so it
    // has to tolerate concurrent modification.
    private val observers = CopyOnWriteArraySet<NetworkCallback>()

    /**
     * @return current [NetworkStatus]
     *
     * Answered from what the system callbacks have reported, so reading it is cheap. It is only as
     * fresh as the last callback the app received. [refresh] is what re-reads the system.
     */
    @get:RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    abstract val networkStatus: NetworkStatus

    /**
     * @return whether the active connection costs the user money.
     *
     * Answered independently of [networkStatus]: `Disconnected` says nothing about cost, and an
     * unknown connection is reported as metered so callers fail safe.
     */
    abstract val isMetered: Boolean

    /** @return current active [Network] **/
    abstract val activeNetwork: Network?

    /** @return `true` if network connectivity is available */
    fun isConnectedToNetwork() = networkStatus != NetworkStatus.Disconnected

    /**
     * @return [Flow] of [NetworkStatus] - current state and changes
     */
    fun observe() = callbackFlow {
        // Ensure we're registered before creating the observer
        if (observers.isEmpty()) {
            register()
        }

        val observer: NetworkCallback = { trySend(it) }
        addObserver(observer)
        trySend(networkStatus)
        awaitClose { removeObserver(observer) }
    }

    /** Registers system's network state observer */
    abstract fun register()

    /** Unregisters system's network state observer */
    abstract fun unregister()

    /**
     * Re-reads the current status from the system and pushes it to every collector of [observe].
     *
     * This is the way out of a block the app was never told had lifted. While the app's network
     * access is blocked - Doze, App Standby, Data Saver - the platform reports no active network,
     * and the callback for the unblock edge does not reach every device. A read that sees a usable
     * network means the block is gone.
     *
     * Call it when the app returns to the foreground, and when the platform reports that a
     * restriction changed. It emits even when the status did not change, and it reads the system
     * synchronously, so do not call it from the main thread.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    abstract fun refresh()

    protected fun notifyObservers(status: NetworkStatus) = observers.forEach { it(status) }

    private fun addObserver(callback: NetworkCallback) {
        if (observers.isEmpty())
            register()
        observers.add(callback)
    }

    private fun removeObserver(callback: NetworkCallback) {
        observers.remove(callback)
        if (observers.isEmpty())
            unregister()
    }
}

enum class NetworkStatus { Unmetered, Metered, Disconnected }

internal typealias NetworkCallback = (NetworkStatus) -> Unit
