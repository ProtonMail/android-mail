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

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.di.NetworkManagerEntryPoint
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import dagger.hilt.android.EntryPointAccessors
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

        // Rust defaults to UNMETERED, which is the unsafe answer, so it has to be told the real one
        // before anything can ask the orchestrator to start. observe() emits the current status on
        // subscribe, so the first collection below does that - off the main thread, which matters
        // because reading the type costs three binder calls and this runs during App Startup.
        //
        // Deliberately app-scoped rather than tied to ProcessLifecycleOwner: the indexing foreground
        // service runs while the app is backgrounded, and that is exactly when Rust must not be told
        // a stale network type.
        entryPointAccessor.appScope().launch {
            networkManager
                .observe()
                .distinctUntilChanged()
                .collect { status ->
                    Timber.d("NetworkStatus updated to $status")

                    when (status) {
                        NetworkStatus.Unmetered,
                        NetworkStatus.Metered -> mailSession.updateOsNetworkStatus(OsNetworkStatus.ONLINE)
                        NetworkStatus.Disconnected -> mailSession.updateOsNetworkStatus(OsNetworkStatus.OFFLINE)
                    }

                    // Read separately from [status]: `Disconnected` is the right answer for OFFLINE
                    // and no answer at all for "does this connection cost money".
                    mailSession.pushNetworkType(networkManager)
                }
        }
    }

    private fun MailSession.pushNetworkType(networkManager: NetworkManager) {
        val type = if (networkManager.isMetered) OsNetworkType.METERED else OsNetworkType.UNMETERED
        Timber.d("OsNetworkType updated to $type")
        updateOsNetworkType(type)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(RustMailCommonInitializer::class.java)
}
