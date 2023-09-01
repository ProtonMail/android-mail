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

import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.core.auth.test.MinimalSignInInternalTests
import me.proton.core.auth.test.rule.AcceptExternalRule
import me.proton.core.auth.test.usecase.WaitForPrimaryAccount
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import org.junit.Rule
import javax.inject.Inject

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(LocalhostApiModule::class)
internal class LoginFlowTests : BaseTest(), MinimalSignInInternalTests {

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    override val quark: Quark = BaseTest.quark
    override val users: User.Users = BaseTest.users

    @get:Rule(order = RuleOrder_21_Injected)
    val acceptExternalRule = AcceptExternalRule { extraHeaderProvider }

    @Inject
    lateinit var extraHeaderProvider: ExtraHeaderProvider

    @Inject
    lateinit var waitForPrimaryAccount: WaitForPrimaryAccount

    override fun verifyAfter() {
        waitForPrimaryAccount()

        mailboxRobot { verify { isShown() } }
    }
}
