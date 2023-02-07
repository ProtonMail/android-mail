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

package ch.protonmail.android.uitest.rule

import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.mailcommon.domain.sample.SessionSample
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.proton.core.account.domain.entity.Account
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.network.domain.session.Session
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import org.junit.rules.TestWatcher
import org.junit.runner.Description

sealed class LoginTestRule : TestWatcher()

fun createMockLoginTestRule(
    accountManager: () -> AccountManager,
    userManager: () -> UserManager,
    account: Account = AccountSample.Primary,
    session: Session = SessionSample.Primary,
    user: User = UserSample.Primary
): MockLoginTestRule = MockLoginTestRule(
    accountManager = accountManager,
    userManager = userManager,
    account = account,
    session = session,
    user = user
)

/**
 * A [LoginTestRule] that inject an Account to bypass the login flow.
 * It also removes the account after the test is finished.
 */
class MockLoginTestRule(
    accountManager: () -> AccountManager,
    userManager: () -> UserManager,
    private val account: Account,
    private val session: Session,
    private val user: User
) : LoginTestRule() {

    private val accountManager by lazy { accountManager() }
    private val userManager by lazy { userManager() }

    override fun starting(description: Description) {
        super.starting(description)
        runBlocking(Dispatchers.Main) {
            accountManager.addAccount(account, session)
            userManager.addUser(user, emptyList())
        }
    }

    override fun finished(description: Description) {
        super.finished(description)
        runBlocking(Dispatchers.Main) {
            accountManager.removeAccount(account.userId)
        }
    }
}

/**
 * Empty implementation of [LoginTestRule]
 */
object NoLoginTestRule : LoginTestRule()
