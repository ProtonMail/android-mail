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

package me.proton.android.core.accountmanager.presentation.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.core.account.domain.model.CoreAccountState
import me.proton.android.core.account.domain.usecase.ObserveCoreAccounts
import me.proton.android.core.accountmanager.domain.usecase.GetAccountAvatarItem
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountItem
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountListItem
import me.proton.core.util.kotlin.takeIfNotBlank
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetPrimaryAccountResult
import javax.inject.Inject

class ObserveAccountListItems @Inject constructor(
    private val getAccountAvatarItem: GetAccountAvatarItem,
    private val mailSessionInterface: MailSession,
    private val observeCoreAccounts: ObserveCoreAccounts
) {

    suspend operator fun invoke(): Flow<List<AccountListItem>> = observeCoreAccounts().map { accounts ->
        val primaryStoredAccount = when (val result = mailSessionInterface.getPrimaryAccount()) {
            is MailSessionGetPrimaryAccountResult.Error -> null
            is MailSessionGetPrimaryAccountResult.Ok -> result.v1
        }
        accounts.map { account ->
            val accountName = account.displayName?.takeIfNotBlank() ?: account.nameOrAddress
            val avatarItem = getAccountAvatarItem(account.userId)
            val accountItem = AccountItem(
                userId = account.userId,
                name = accountName,
                email = account.primaryEmailAddress,
                initials = avatarItem?.initials,
                color = avatarItem?.color
            )
            when (account.state) {
                CoreAccountState.Ready -> if (account.userId.id == primaryStoredAccount?.userId()) {
                    AccountListItem.Ready.Primary(accountItem)
                } else {
                    AccountListItem.Ready(accountItem)
                }

                else -> AccountListItem.Disabled(accountItem)
            }
        }
    }
}
