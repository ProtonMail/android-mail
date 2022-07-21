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

package ch.protonmail.android.uitest.e2e.login

import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.android.robots.auth.login.MailboxPasswordRobot
import org.junit.Before
import org.junit.Test

class LoginFlowTests : BaseTest() {

    private val addAccountRobot = AddAccountRobot()
    private val loginRobot = LoginRobot()
    private val inboxRobot = InboxRobot(composeTestRule)

    @Before
    fun signIn() {
        addAccountRobot
            .signIn()
            .verify { loginElementsDisplayed() }
    }

    @Test
    fun loginUserHappyPath() {
        val user = users.getUser { it.name == "pro" }
        loginRobot
            .loginUser<LoginRobot>(user)

        inboxRobot
            .verify { mailboxScreenDisplayed() }
    }

    @Test
    fun loginUserWithSecondaryPasswordHappyPath() {
        val user = users.getUser(usernameAndOnePass = false) { it.name == "twopasswords" }
        loginRobot
            .loginUser<MailboxPasswordRobot>(user)
            .unlockMailbox<LoginRobot>(user)

        inboxRobot
            .verify { mailboxScreenDisplayed() }
    }
}
