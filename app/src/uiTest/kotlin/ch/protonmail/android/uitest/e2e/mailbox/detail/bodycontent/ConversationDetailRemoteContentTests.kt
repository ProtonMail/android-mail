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

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.given
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
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ConversationDetailRemoteContentTests :
    MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara) {

    private val expectedBodyText = "Various img elements"
    private val expectedBodyTextLastMessage = "Various img elements (2)"

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @SmokeTest
    @TestId("184211", "212682")
    fun checkRemoteContentBlockedInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                given("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_184211.json"
                    withStatusCode 200,
                given("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_184211.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_184211.json"
                    withStatusCode 200 matchWildcards true,
                given("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184211.json"
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
                    hasRemoteImageLoaded(false)
                }
            }

            bannerSection { verify { hasBlockedRemoteImagesBannerDisplayed() } }
        }
    }

    @Test
    @TestId("184212")
    fun checkRemoteContentBlockedWithMultipleMessagesInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                given("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_184212.json"
                    withStatusCode 200,
                given("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_184212.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_184212.json"
                    withStatusCode 200 matchWildcards true,
                given("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184212.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                given("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_184212_2.json"
                    withStatusCode 200 matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyTextLastMessage)
                    hasRemoteImageLoaded(false)
                }
            }

            bannerSection { verify { hasBlockedRemoteImagesBannerDisplayed() } }

            messageHeaderSection {
                expanded { collapse() }
            }

            messagesCollapsedSection {
                scrollToTop()
                openMessageAtIndex(0)
            }

            messageBodySection {
                waitUntilMessageIsShown()

                verify {
                    messageInWebViewContains(expectedBodyText)
                    hasRemoteImageLoaded(false)
                }
            }

            bannerSection { verify { hasBlockedRemoteImagesBannerDisplayed() } }
        }
    }

    @Test
    @TestId("212683/2")
    fun checkRemoteContentBannerNotShownWhenNoRemoteContentIsDisplayedInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                given("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_212683_2.json"
                    withStatusCode 200,
                given("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_212683.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                given("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_212683.json"
                    withStatusCode 200 matchWildcards true,
                given("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212683.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }
        }
    }

    @Test
    @TestId("212684/2")
    fun checkCombinedBannerIsShownWhenBothRemoteContentAndEmbeddedImagesAreBlockedInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                given("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_212684_2.json"
                    withStatusCode 200,
                given("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_212684.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                given("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_212684.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                given("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212684.json"
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
                    hasRemoteImageLoaded(false)
                    hasEmbeddedImagesSuccessfullyLoaded(false)
                }
            }

            bannerSection { verify { hasBlockerEmbeddedAndRemoteImagesBannerDisplayed() } }
        }
    }

    @Test
    @TestId("212686/2", "212687")
    fun checkRemoteContentBlockedFromExternalAddressInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                given("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_212686_2.json"
                    withStatusCode 200,
                given("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_212686.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                given("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_212686.json"
                    withStatusCode 200 matchWildcards true,
                given("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212686.json"
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
                    hasRemoteImageLoaded(false)
                }
            }

            bannerSection { verify { hasBlockedRemoteImagesBannerDisplayed() } }
        }
    }
}
