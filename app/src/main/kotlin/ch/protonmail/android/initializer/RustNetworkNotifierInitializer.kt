/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.initializer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import ch.protonmail.android.di.NetworkManagerEntryPoint
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.OsNetworkStatus
import uniffi.mail_uniffi.OsNetworkType

internal class RustNetworkObserverInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPointAccessor = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NetworkManagerEntryPoint::class.java
        )

        val networkManager = entryPointAccessor.networkManager()
        val mailSession = entryPointAccessor.mailSession()
        val appScope = entryPointAccessor.appScope()

        mailSession.observeNetwork(networkManager, appScope)
        networkManager.refreshOnForeground(appScope)
        networkManager.refreshOnRestrictionChange(context.applicationContext, appScope)
    }

    // Rust defaults to UNMETERED, which is the unsafe answer, so it has to be told the real one
    // before anything can ask the orchestrator to start. observe() emits the current status on
    // subscribe, so the first collection below does that.
    //
    // Deliberately app-scoped rather than tied to ProcessLifecycleOwner: the indexing foreground
    // service runs while the app is backgrounded, and that is exactly when Rust must not be told
    // a stale network type.
    private fun MailSession.observeNetwork(networkManager: NetworkManager, appScope: CoroutineScope) {
        appScope.launch {
            networkManager
                .observe()
                .distinctUntilChanged()
                .collect { status ->
                    Timber.d("NetworkStatus updated to $status")

                    when (status) {
                        NetworkStatus.Unmetered,
                        NetworkStatus.Metered -> updateOsNetworkStatus(OsNetworkStatus.ONLINE)
                        NetworkStatus.Disconnected -> updateOsNetworkStatus(OsNetworkStatus.OFFLINE)
                    }

                    val type = status.toOsNetworkType()
                    Timber.d("OsNetworkType updated to $type")
                    updateOsNetworkType(type)
                }
        }
    }

    // Derived from the status rather than read again. The status already carries metered-ness, and
    // a second read answers for a later instant, which is how Rust ends up with a pair that
    // contradicts itself. `Disconnected` says nothing about cost, so it is reported as metered and
    // nothing decides to backfill over what might be cellular.
    private fun NetworkStatus.toOsNetworkType(): OsNetworkType = when (this) {
        NetworkStatus.Unmetered -> OsNetworkType.UNMETERED
        NetworkStatus.Metered,
        NetworkStatus.Disconnected -> OsNetworkType.METERED
    }

    // Doze and App Standby make the platform report no active network at all, so the app tells Rust
    // it is OFFLINE. The only connectivity signal on the way out is onBlockedStatusChanged, and a
    // device that drops it leaves Rust pinned to OFFLINE. Re-reading here bounds that to the next
    // time the user opens the app.
    //
    // Runs on [appScope] rather than the lifecycle scope: the read costs three binder calls.
    private fun NetworkManager.refreshOnForeground(appScope: CoroutineScope) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    appScope.launch { refresh() }
                }
            }
        )
    }

    // The restriction changing is the event the app actually cares about, and these two broadcasts
    // are the platform announcing it directly rather than through the network. They cover the
    // window ON_START cannot: content indexing runs as a foreground service for as long as an hour
    // with the app backgrounded, and that whole time is one stale OFFLINE away from no sync at all.
    private fun NetworkManager.refreshOnRestrictionChange(context: Context, appScope: CoroutineScope) {
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            addAction(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    appScope.launch { refresh() }
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(RustMailCommonInitializer::class.java)
}
