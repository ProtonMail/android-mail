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

package ch.protonmail.android.mailnotifications.domain

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.ExistingWorkPolicy
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailnotifications.data.FirebaseNotificationsTokenChannel
import ch.protonmail.android.mailnotifications.data.local.RegisterDeviceTokenWorker
import ch.protonmail.android.mailnotifications.data.remote.FirebaseMessagingProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class FirebaseMessagingTokenLifecycleObserver @Inject constructor(
    private val firebaseMessagingProxy: FirebaseMessagingProxy,
    private val firebaseNotificationsTokenChannel: FirebaseNotificationsTokenChannel,
    private val enqueuer: Enqueuer,
    @AppScope private val coroutineScope: CoroutineScope
) : DefaultLifecycleObserver {

    private var tokenFlowJob: Job? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        tokenFlowJob = coroutineScope.launch {
            firebaseNotificationsTokenChannel.tokenFlow.distinctUntilChanged().collect { token ->
                Timber.tag(LogTag).d("Received new token, enqueueing registration...")
                enqueuer.enqueueUniqueWork(
                    workerId = RegisterDeviceTokenWorker.UniqueWorkerId,
                    worker = RegisterDeviceTokenWorker::class.java,
                    existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                    constraints = enqueuer.buildDefaultConstraints(),
                    params = RegisterDeviceTokenWorker.params(token)
                )
            }
        }

        coroutineScope.launch {
            val token = firebaseMessagingProxy.fetchToken().getOrElse {
                Timber.tag(LogTag).d("Unable to fetch token")
                return@launch
            }

            firebaseNotificationsTokenChannel.sendToken(token)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        tokenFlowJob?.cancel()
    }

    companion object {
        private const val LogTag = "Register device token"
    }
}
