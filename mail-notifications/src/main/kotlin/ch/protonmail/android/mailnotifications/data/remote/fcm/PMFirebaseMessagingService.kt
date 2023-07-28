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

package ch.protonmail.android.mailnotifications.data.remote.fcm

import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailnotifications.domain.FcmTokenPreferences
import ch.protonmail.android.mailnotifications.domain.ProcessPushNotificationDataWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.CoroutineScopeProvider
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PMFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var enqueuer: Enqueuer

    @Inject
    lateinit var fcmTokenPreferences: FcmTokenPreferences

    @Inject
    lateinit var scopeProvider: CoroutineScopeProvider

    @Inject
    lateinit var accountManager: AccountManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        scopeProvider.GlobalDefaultSupervisedScope.launch {
            fcmTokenPreferences.storeToken(token)
            accountManager.getAccounts()
                .map { accounts -> accounts.filter { account -> account.state == AccountState.Ready } }
                .collectLatest { accounts ->
                    accounts.map { account -> account.userId }.forEach { userId ->
                        enqueuer.enqueue<RegisterDeviceWorker>(RegisterDeviceWorker.params(userId))
                    }
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val uid = remoteMessage.data["UID"]
        val encryptedMessage = remoteMessage.data["encryptedMessage"]
        if (uid != null && encryptedMessage != null) {
            Timber.d("onMessageReceived: ${remoteMessage.data}")
            enqueuer.enqueue<ProcessPushNotificationDataWorker>(
                ProcessPushNotificationDataWorker.params(uid, encryptedMessage)
            )
        }
    }
}
