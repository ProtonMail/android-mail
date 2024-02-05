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

package ch.protonmail.android.uitest.e2e.mailbox.detail.bodycontent

import arrow.core.right
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.networkmocks.assets.RawAssets
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.messageDetailRobot
import ch.protonmail.android.uitest.robot.detail.section.bannerSection
import ch.protonmail.android.uitest.robot.detail.section.conversation.messagesCollapsedSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.messageHeaderSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import me.proton.core.auth.domain.usecase.ValidateServerProof
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Before
import org.junit.Test

/**
 * Separate suite to mock decryption when UI testing as we can't possibly rely on links that might expire.
 * Will be improved once we intercept the remote content calls via MockWebServer (currently not possible).
 */
@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class MockedDetailRemoteContentTests : MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara) {

    private val expectedBodyText = "Various img elements"
    private val expectedBodyTextLastMessage = "Various img elements (2)"

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @JvmField
    @BindValue // GetDecryptedMessageBody needs to be mocked to make sure content is passed as expected to the WebView.
    val decryptedMessageBody: GetDecryptedMessageBody = mockk<GetDecryptedMessageBody>().apply { mockDecryptedBody() }

    @Before
    fun reset() {
        unmockkAll()
    }

    @Test
    @TestId("184207", "212679")
    fun checkRemoteContentNotBlockedInMessageMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_184207.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_184207.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184207.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        messageDetailRobot {
            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyText)
                    hasRemoteImageLoaded(true)
                }
            }

            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }
        }
    }

    @Test
    @SmokeTest
    @TestId("184206", "212680")
    fun checkRemoteContentNotBlockedInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_184206.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_184206.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_184206.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184206.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyText)
                    hasRemoteImageLoaded(true)
                }
            }

            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }
        }
    }

    @Test
    @TestId("184208", "184209")
    fun checkRemoteContentNotBlockedOnMultipleMessagesInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_184209.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_184209.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_184209.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184209.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184209_2.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        decryptedMessageBody.mockDecryptedBody(PlaceholderMockTwo)

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyTextLastMessage)
                    hasRemoteImageLoaded(true)
                }
            }

            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }

            messageHeaderSection {
                expanded { collapse() }
            }

            decryptedMessageBody.mockDecryptedBody(PlaceholderMockOne)

            messagesCollapsedSection {
                scrollToTop()
                openMessageAtIndex(0)
            }

            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyText)
                    hasRemoteImageLoaded(true)
                }
            }

            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }
        }
    }

    private fun GetDecryptedMessageBody.mockDecryptedBody(assetName: String = PlaceholderMockOne) {
        coEvery { this@mockDecryptedBody.invoke(any(), any()) } returns getHtmlMessageBodyContent(assetName).right()
    }

    private fun getHtmlMessageBodyContent(assetName: String): DecryptedMessageBody {
        val content = requireNotNull(RawAssets.getRawContentForPath(HtmlAssetsPath + assetName)) {
            "Unable to retrieve content for file '$assetName'."
        }

        return DecryptedMessageBody(
            MessageId("html-message-id"),
            String(content),
            MimeType.Html,
            emptyList(),
            UserAddressSample.PrimaryAddress
        )
    }

    companion object {

        private const val HtmlAssetsPath = "assets/network-mocks/html-assets/"
        const val PlaceholderMockOne = "html_remote_content_placeholder.html"
        const val PlaceholderMockTwo = "html_remote_content_placeholder_2.html"
    }
}
