/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.logging

import io.sentry.Sentry
import io.sentry.protocol.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import me.proton.core.accountmanager.data.AccountStateHandlerCoroutineScope
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryUserObserver @Inject constructor(
    @AccountStateHandlerCoroutineScope
    internal val scope: CoroutineScope,
    internal val accountManager: AccountManager,
) {

    fun start() = accountManager.getPrimaryUserId()
        .map { userId ->
            val user = User().apply { id = userId?.id }
            Sentry.setUser(user)
        }
        .launchIn(scope)
}
