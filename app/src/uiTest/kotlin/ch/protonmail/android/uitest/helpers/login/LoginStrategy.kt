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

package ch.protonmail.android.uitest.helpers.login

import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.mailcommon.domain.sample.SessionSample
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import me.proton.core.account.domain.entity.Account
import me.proton.core.network.domain.session.Session
import me.proton.core.user.domain.entity.User

internal sealed class LoginStrategy {

    sealed class LoggedIn(
        val account: Account,
        val session: Session,
        val user: User
    ) : LoginStrategy() {

        object PrimaryUser : LoggedIn(
            account = AccountSample.Primary,
            session = SessionSample.Primary,
            user = UserSample.Primary
        )
    }

    object LoggedOut : LoginStrategy()
}
