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

package ch.protonmail.android.testdata

import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState.Disabled
import me.proton.core.account.domain.entity.AccountState.NotReady
import me.proton.core.account.domain.entity.AccountState.Ready
import me.proton.core.account.domain.entity.AccountState.Removed
import me.proton.core.account.domain.entity.AccountType.Internal
import me.proton.core.account.domain.entity.SessionDetails
import me.proton.core.account.domain.entity.SessionState.Authenticated
import me.proton.core.account.domain.entity.SessionState.SecondFactorNeeded
import me.proton.core.network.domain.session.SessionId

object AccountTestData {
    private const val RAW_USERNAME = "username"
    private const val RAW_EMAIL = "email@protonmail.ch"
    private const val INITIAL_EVENT_ID = "event_id"

    val readyAccount = Account(
        userId = UserIdTestData.userId,
        username = RAW_USERNAME,
        email = RAW_EMAIL,
        state = Ready,
        sessionId = SessionId(UserIdTestData.userId.id),
        sessionState = Authenticated,
        details = AccountDetails(
            null,
            SessionDetails(
                initialEventId = INITIAL_EVENT_ID,
                requiredAccountType = Internal,
                secondFactorEnabled = true,
                twoPassModeEnabled = true,
                passphrase = null,
                password = null,
                fido2AuthenticationOptionsJson = null
            )
        )
    )

    val notReadyAccount = Account(
        userId = UserIdTestData.userId,
        username = RAW_USERNAME,
        email = RAW_EMAIL,
        state = NotReady,
        sessionId = SessionId(UserIdTestData.userId.id),
        sessionState = SecondFactorNeeded,
        details = AccountDetails(
            null,
            SessionDetails(
                initialEventId = INITIAL_EVENT_ID,
                requiredAccountType = Internal,
                secondFactorEnabled = true,
                twoPassModeEnabled = true,
                passphrase = null,
                password = null,
                fido2AuthenticationOptionsJson = null
            )
        )
    )

    val disabledAccount = Account(
        userId = UserIdTestData.userId,
        username = RAW_USERNAME,
        email = RAW_EMAIL,
        state = Disabled,
        sessionId = SessionId(UserIdTestData.userId.id),
        sessionState = null,
        details = AccountDetails(
            null,
            SessionDetails(
                initialEventId = INITIAL_EVENT_ID,
                requiredAccountType = Internal,
                secondFactorEnabled = true,
                twoPassModeEnabled = true,
                passphrase = null,
                password = null,
                fido2AuthenticationOptionsJson = null
            )
        )
    )

    val removedAccount = Account(
        userId = UserIdTestData.userId,
        username = RAW_USERNAME,
        email = RAW_EMAIL,
        state = Removed,
        sessionId = null,
        sessionState = null,
        details = AccountDetails(
            null,
            SessionDetails(
                initialEventId = INITIAL_EVENT_ID,
                requiredAccountType = Internal,
                secondFactorEnabled = true,
                twoPassModeEnabled = true,
                passphrase = null,
                password = null,
                fido2AuthenticationOptionsJson = null
            )
        )
    )
}
