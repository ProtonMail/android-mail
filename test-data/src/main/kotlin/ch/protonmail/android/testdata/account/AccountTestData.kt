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

package ch.protonmail.android.testdata.account

import ch.protonmail.android.testdata.session.SessionTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserTestData
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.SessionState
import me.proton.core.network.domain.session.SessionId

object AccountTestData {

    val Primary = build()

    val PrimaryNotReady = build(
        sessionId = null,
        sessionState = null,
        state = AccountState.NotReady
    )

    fun build(
        sessionId: SessionId? = SessionTestData.Primary.sessionId,
        sessionState: SessionState? = SessionState.Authenticated,
        state: AccountState = AccountState.Ready
    ) = Account(
        details = AccountDetails(session = null),
        email = UserTestData.Primary.email,
        sessionId = sessionId,
        sessionState = sessionState,
        state = state,
        userId = UserIdTestData.Primary,
        username = UserTestData.Primary.name ?: UserTestData.Primary.userId.id
    )
}
