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

import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.util.UiDeviceHolder.uiDevice
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@SmokeTest
@HiltAndroidTest
@SdkSuppress(minSdkVersion = 28)
@UninstallModules(ServerProofModule::class)
internal class MessageLoadingTests : MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut) {

    private val inboxRobot = InboxRobot(composeTestRule)
    private val messageDetailRobot = MessageDetailRobot(composeTestRule)

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("66392")
    fun checkMessageLoadedInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_66392.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_66392.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_66392.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200 serveOnce true withPriority MockPriority.Highest
            )
        }

        val expectedMessageBody = "Bye and hello"

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }

        uiDevice.pressBack()

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }
    }

    @Test
    @TestId("66393")
    fun checkMessageLoadedInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_66393.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_66393.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations/*"
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_66393.json"
                    withStatusCode 200 matchWildcards true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_66393.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200 serveOnce true withPriority MockPriority.Highest
            )
        }

        val expectedMessageBody = "Hello once again"

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }

        uiDevice.pressBack()

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }
    }

    @Test
    @TestId("66394")
    fun checkLongMessageLoadedInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_66394.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_66394.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_66394.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200 serveOnce true withPriority MockPriority.Highest
            )
        }

        val expectedMessageBody = "Lorem ipsum"

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }

        uiDevice.pressBack()

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }
    }

    @Test
    @TestId("66395")
    fun checkLongMessageLoadedInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_66395.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_66395.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations/*"
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_66395.json"
                    withStatusCode 200 matchWildcards true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_66395.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200 serveOnce true withPriority MockPriority.Highest
            )
        }

        val expectedMessageBody = "Lorem ipsum"

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }

        uiDevice.pressBack()

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }
    }

    @Test
    @TestId("78993")
    fun checkMostRecentUnreadMessageIsOpenedInConversation() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_78993.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_78993.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations/*"
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_78993.json"
                    withStatusCode 200 matchWildcards true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_78993.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200,
                "/mail/v4/conversations/read"
                    respondWith "/mail/v4/conversations/read/conversations_read_base_placeholder.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        val expectedMessageBody = "Third message"

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot
            .waitUntilMessageIsShown()
            .verify { messageBodyInWebViewContains(expectedMessageBody) }
    }
}
