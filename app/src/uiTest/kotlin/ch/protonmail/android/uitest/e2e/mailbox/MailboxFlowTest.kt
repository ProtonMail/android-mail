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

import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

@SmokeTest
@HiltAndroidTest
internal class MailboxFlowTest : MockedNetworkTest() {

    private val mailboxRobot = InboxRobot(composeTestRule)
    private val menuRobot = MenuRobot(composeTestRule)

    @Before
    fun setupDispatcher() {
        mockWebServer.dispatcher = mockNetworkDispatcher()
    }

    @Test
    fun openMailboxAndSwitchLocation() {
        mailboxRobot.verify { mailboxScreenDisplayed() }

        menuRobot
            .swipeOpenSidebarMenu()
            .openDrafts()
            .verify { draftsScreenDisplayed(composeTestRule) }

        menuRobot
            .swipeOpenSidebarMenu()
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
}
