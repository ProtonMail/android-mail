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

package ch.protonmail.android.mailcommon.domain.sample

import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.SessionState
import me.proton.core.network.domain.session.SessionId

object AccountSample {

    val Primary = build()

    val PrimaryNotReady = build(
        sessionId = null,
        sessionState = null,
        state = AccountState.NotReady
    )

    fun build(
        sessionId: SessionId? = SessionIdSample.build(),
        sessionState: SessionState? = SessionState.Authenticated,
        state: AccountState = AccountState.Ready
    ) = Account(
        details = AccountDetails(session = null),
        email = UserAddressSample.build().email,
        sessionId = sessionId,
        sessionState = sessionState,
        state = state,
        userId = UserIdSample.Primary,
        username = UserAddressSample.build().displayName ?: UserIdSample.Primary.id
    )
}
