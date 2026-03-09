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

package me.proton.android.core.account.data.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.android.core.account.domain.model.CoreAccount
import me.proton.android.core.account.domain.model.CoreAccountState
import me.proton.android.core.account.domain.usecase.ObserveCoreAccounts
import me.proton.android.core.account.domain.usecase.ObservePrimaryCoreAccount
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetPrimaryAccountResult
import javax.inject.Inject

class ObservePrimaryCoreAccountImpl @Inject constructor(
    private val mailSession: MailSession,
    private val observeCoreAccounts: ObserveCoreAccounts
) : ObservePrimaryCoreAccount {

    override operator fun invoke(): Flow<CoreAccount?> = observeCoreAccounts().map { list ->
        val primaryAccount = when (val result = mailSession.getPrimaryAccount()) {
            is MailSessionGetPrimaryAccountResult.Error -> null
            is MailSessionGetPrimaryAccountResult.Ok -> result.v1
        }
        list.firstOrNull { it.state == CoreAccountState.Ready && it.userId.id == primaryAccount?.userId() }
    }.distinctUntilChanged()
}
