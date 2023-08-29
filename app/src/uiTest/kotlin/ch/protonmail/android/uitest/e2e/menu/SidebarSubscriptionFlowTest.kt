/*
 * Copyright (c) 2023 Proton AG
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

package ch.protonmail.android.uitest.e2e.menu

import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.util.extensions.waitUntilSignInScreenIsGone
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.plan.test.MinimalSubscriptionTests
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(LocalhostApiModule::class)
internal class SidebarSubscriptionFlowTest : BaseTest(), MinimalSubscriptionTests {

    private val loginRobot = LoginRobot()

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    override val quark: Quark = BaseTest.quark
    override val users: User.Users = BaseTest.users

    override fun startSubscription(user: User) {
        AddAccountRobot
            .clickSignIn()
            .login(user)

        loginRobot.waitUntilSignInScreenIsGone()
        mailboxRobot { verify { isShown() } }

        menuRobot {
            swipeOpenSidebarMenu()
            openSubscription()
        }
    }
}
