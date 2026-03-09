/*
 * Copyright (c) 2024 Proton AG
 * This file is part of Proton AG and Proton Mail.
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

package ch.protonmail.android.mailsession.data.mapper

import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountAvatarInfo
import ch.protonmail.android.mailsession.domain.model.AccountState
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.StoredAccount
import uniffi.mail_uniffi.StoredAccountState

internal fun StoredAccount.toAccount() = Account(
    userId = UserId(userId()),
    name = details().name,
    state = when (state()) {
        is StoredAccountState.NotReady -> AccountState.NotReady
        is StoredAccountState.LoggedIn -> AccountState.Ready
        is StoredAccountState.LoggedOut -> AccountState.Disabled
        is StoredAccountState.NeedMbp -> AccountState.TwoPasswordNeeded
        is StoredAccountState.NeedTfa -> AccountState.TwoFactorNeeded
        is StoredAccountState.NeedNewPass -> AccountState.NewPassNeeded
    },
    primaryAddress = details().email,
    avatarInfo = details().avatarInformation.let { AccountAvatarInfo(it.text, it.color) }
)
