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

package ch.protonmail.android.uitest.e2e.mailbox.detail.message

import arrow.core.Either
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.maildetail.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.mailbox.detail.DetailRemoteContentTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.detail.messageBodySection
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test


@SmokeTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class MessageDetailDetailRemoteContentTest :
    MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut),
    DetailRemoteContentTest {

    private val inboxRobot = InboxRobot(composeTestRule)
    private val messageDetailRobot = MessageDetailRobot(composeTestRule)
    private val expectedBodyText = "Various img elements"

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @JvmField
    @BindValue // GetDecryptedMessageBody needs to be mocked to make sure content is passed as expected to the WebView.
    val decryptedMessageBody: GetDecryptedMessageBody = mockk {
        coEvery {
            this@mockk.invoke(any(), any())
        } returns Either.Right(
            getFakeDecryptedMessageBodyWithRemoteContent(
                assetName = "html_remote_content_placeholder.html",
                mimeType = MimeType.Html
            )
        )
    }

    @Test
    @TestId("184207")
    fun checkRemoteContentNotBlockedWhenConversationModeIsDisabled() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_184207.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_184207.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_base_placeholder.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot.run {
            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyText)
                    hasRemoteImageLoaded(true)
                }
            }
        }
    }

    @Test
    @TestId("184210")
    fun checkRemoteContentBlockedWhenConversationModeIsDisabled() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_184210.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_184210.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_base_placeholder.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/messages/read"
                    respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.clickMessageByPosition(0)

        messageDetailRobot.run {
            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyText)
                    hasRemoteImageLoaded(false)
                }
            }
        }
    }
}
