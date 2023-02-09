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

package ch.protonmail.android.uitest.e2e.mailbox

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.protonmail.android.MainActivity
import ch.protonmail.android.di.NetworkConfigModule
import ch.protonmail.android.initializer.MainInitializer
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import ch.protonmail.android.uitest.rule.createMockLoginTestRule
import ch.protonmail.android.uitest.rule.createMockWebServerRuleChain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.network.data.di.BaseProtonApiUrl
import me.proton.core.user.domain.UserManager
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.test.BeforeTest

@HiltAndroidTest
@UninstallModules(NetworkConfigModule::class)
class MailboxFlowTest {

    @Inject lateinit var accountManager: AccountManager

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var mockWebServer: MockWebServer

    private val composeTestRule = createAndroidComposeRule<MainActivity>()
    private val loginTestRule = createMockLoginTestRule(::accountManager, ::userManager)

    @get:Rule
    val ruleChain = createMockWebServerRuleChain(
        mockWebServer = ::mockWebServer,
        composeTestRule = composeTestRule,
        loginTestRule = loginTestRule
    )

    private val mailboxRobot = InboxRobot(composeTestRule)
    private val menuRobot = MenuRobot(composeTestRule)

    @BeforeTest
    fun setup() {
        MainInitializer.init(composeTestRule.activity)
    }

    @Test
    fun openMailboxAndSwitchLocation() {
        mailboxRobot.verify { mailboxScreenDisplayed() }

        menuRobot
            .openDrafts()
            .verify { draftsScreenDisplayed(composeTestRule) }

        menuRobot
            .openAllMail()
            .verify { allMailScreenDisplayed(composeTestRule) }
    }

    /*
     * This could be improved by injecting an account with some known messages and  performing
     * verifications on the messages list to ensure filtering actually works as expected
     */
    @Test
    fun filterUnreadMessages() {
        mailboxRobot.verify { mailboxScreenDisplayed() }
        mailboxRobot.verify { unreadFilterIsDisplayed() }

        mailboxRobot
            .filterUnreadMessages()
            .verify { unreadFilterIsSelected() }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {

        @Provides
        @BaseProtonApiUrl
        fun baseProtonApiUrl(mockWebServer: MockWebServer) = mockWebServer.url("/")
    }
}
