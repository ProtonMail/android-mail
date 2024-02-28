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

package ch.protonmail.android.uitest.e2e.mailbox.selection

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.bottombar.BottomBarActionEntry
import ch.protonmail.android.uitest.robot.bottombar.bottomBarSection
import ch.protonmail.android.uitest.robot.bottombar.verify
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class SelectionModeBottomBarActionsTests : MockedNetworkTest(
    loginType = LoginTestUserTypes.Paid.FancyCapybara
) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("216624")
    fun testBottomBarInboxActionsOnUnreadMessage() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_216624.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        navigator { navigateTo(Destination.Inbox) }

        mailboxRobot {
            clickAndMatchSelectionModeBottomBarActions(BottomBarActionEntry.Defaults.actionsOnUnreadItem)
        }
    }

    @Test
    @TestId("216624/2")
    fun testBottomBarInboxActionsOnReadMessage() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_216624_2.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        navigator { navigateTo(Destination.Inbox) }

        mailboxRobot {
            clickAndMatchSelectionModeBottomBarActions(BottomBarActionEntry.Defaults.actionsOnReadItem)
        }
    }

    @Test
    @TestId("216625")
    fun testBottomBarInboxActionsOnUnreadTrashedMessage() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=3&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_216625.json"
                    withStatusCode 200
            )
        }

        navigator { navigateTo(Destination.Trash) }

        mailboxRobot {
            clickAndMatchSelectionModeBottomBarActions(BottomBarActionEntry.Defaults.actionsOnTrashedUnreadItem)
        }
    }

    @Test
    @TestId("216625/2")
    fun testBottomBarInboxActionsOnReadTrashedMessage() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=3&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_216625_2.json"
                    withStatusCode 200
            )
        }

        navigator { navigateTo(Destination.Trash) }

        mailboxRobot {
            clickAndMatchSelectionModeBottomBarActions(BottomBarActionEntry.Defaults.actionsOnTrashedReadItem)
        }
    }

    @Test
    @TestId("216625/3")
    fun testBottomBarInboxActionsOnUnreadSpamMessage() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=4&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_216625_3.json"
                    withStatusCode 200
            )
        }

        navigator { navigateTo(Destination.Spam) }

        mailboxRobot {
            clickAndMatchSelectionModeBottomBarActions(BottomBarActionEntry.Defaults.actionsOnSpamUnreadItem)
        }
    }

    @Test
    @TestId("216625/4")
    fun testBottomBarInboxActionsOnReadSpamMessage() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=4&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_216625_4.json"
                    withStatusCode 200
            )
        }

        navigator { navigateTo(Destination.Spam) }

        mailboxRobot {
            clickAndMatchSelectionModeBottomBarActions(BottomBarActionEntry.Defaults.actionsOnSpamReadItem)
        }
    }
}

private fun MailboxRobot.clickAndMatchSelectionModeBottomBarActions(expectedActions: Array<BottomBarActionEntry>) {
    listSection { selectItemsAt(0) }

    bottomBarSection {
        verify { hasActions(*expectedActions) }
    }
}
