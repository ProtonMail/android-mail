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

package ch.protonmail.android.mailnotifications.domain.handler

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailnotifications.data.repository.NotificationTokenRepository
import ch.protonmail.android.mailnotifications.domain.usecase.DismissEmailNotificationsForUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

internal class AccountStateAwareNotificationHandler @Inject constructor(
    private val accountManager: AccountManager,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val dismissEmailNotificationsForUser: DismissEmailNotificationsForUser,
    @AppScope private val coroutineScope: CoroutineScope
) : NotificationHandler {

    override fun handle() {
        coroutineScope.launch {
            accountManager.onAccountStateChanged(true).collect {
                when (it.state) {
                    AccountState.Ready -> notificationTokenRepository.bindTokenToUser(it.userId)
                    AccountState.Disabled,
                    AccountState.Removed -> dismissEmailNotificationsForUser(it.userId)
                    else -> Unit
                }
            }
        }
    }
}
