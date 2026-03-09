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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import ch.protonmail.android.di.NetworkManagerEntryPoint
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import uniffi.mail_uniffi.OsNetworkStatus

internal class RustNetworkObserverInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPointAccessor = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NetworkManagerEntryPoint::class.java
        )

        val networkManager = entryPointAccessor.networkManager()
        val mailSession = entryPointAccessor.mailSession()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var networkMonitoringJob: Job? = null

            override fun onStart(owner: LifecycleOwner) {
                Timber.d("App foregrounded - starting NetworkStatus check")

                networkMonitoringJob = CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    networkManager
                        .observe()
                        .distinctUntilChanged()
                        .collect { status ->
                            Timber.d("NetworkStatus updated to $status")

                            when (status) {
                                NetworkStatus.Unmetered,
                                NetworkStatus.Metered -> {
                                    mailSession.updateOsNetworkStatus(OsNetworkStatus.ONLINE)
                                }

                                NetworkStatus.Disconnected -> {
                                    mailSession.updateOsNetworkStatus(OsNetworkStatus.OFFLINE)
                                }
                            }
                        }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                Timber.d("App backgrounded - pausing NetworkStatus check")
                networkMonitoringJob?.cancel()
            }
        })
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(RustMailCommonInitializer::class.java)
}
