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

package ch.protonmail.android.uitest.e2e.mailbox.detail.attachments.inline

import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withMimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.messageDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.MessageDetailSnackbar
import ch.protonmail.android.uitest.robot.detail.model.attachments.AttachmentDetailItemEntry
import ch.protonmail.android.uitest.robot.detail.model.attachments.AttachmentDetailSummaryEntry
import ch.protonmail.android.uitest.robot.detail.section.attachmentsSection
import ch.protonmail.android.uitest.robot.detail.section.bannerSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
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
internal class ConversationDetailEmbeddedImagesTests :
    MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara),
    EmbeddedImagesTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @SmokeTest
    @TestId("203101", "203695")
    fun testConversationDetailEmbeddedImagesNotLoadedWithSettingOff() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203101.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203101.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203101.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203101.json"
                    withStatusCode 200 matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection { verifyEmbeddedImageLoaded(expectedState = false) }

            bannerSection { verify { hasBlockedEmbeddedImagesBannerDisplayed() } }
        }
    }

    @Test
    @TestId("203102", "203108")
    fun testConversationDetailEmbeddedImagesBodyLoadingError() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203102.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203102.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203104.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            snackbarSection {
                verify { isDisplaying(MessageDetailSnackbar.FailedToLoadMessage) }
            }
        }
    }

    @Test
    @TestId("203104", "203110")
    fun testConversationDetailEmbeddedImagesBodyDecryptionError() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203104.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203104.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203104.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203104.json"
                    withStatusCode 200 matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            snackbarSection { verify { isDisplaying(MessageDetailSnackbar.FailedToDecryptMessage) } }
        }
    }

    @Test
    @SmokeTest
    @SdkSuppress(minSdkVersion = 29)
    @TestId("203105", "203692")
    fun testConversationDetailEmbeddedImagesAreLoaded() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203105.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203105.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203105.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203105.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/attachments/*")
                    respondWith "/mail/v4/attachments/attachment_203105"
                    withStatusCode 200 matchWildcards true serveOnce true
                    withMimeType MimeType.OctetStream
            )
        }

        val expectedSummary = AttachmentDetailSummaryEntry(summary = "1 file", size = "1.5 kB")
        val expectedEntry = AttachmentDetailItemEntry(index = 0, fileName = "image.png", fileSize = "1.5 kB")

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection { verifyEmbeddedImageLoaded(expectedState = true) }

            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }

            attachmentsSection {
                verify {
                    hasSummaryDetails(expectedSummary)
                    hasAttachments(expectedEntry)
                }
            }
        }
    }

    @Test
    @TestId("203106")
    fun testConversationDetailEmbeddedImagesErrorsUponDownload() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203106.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203106.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203106.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203106.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/attachments/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection { verifyEmbeddedImageLoaded(expectedState = false) }
        }
    }

    @Test
    @TestId("203107")
    fun testConversationDetailEmbeddedImagesErrorsUponDecryption() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203107.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203107.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203107.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203107.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/attachments/*")
                    respondWith "/mail/v4/attachments/attachment_203107"
                    withStatusCode 200 matchWildcards true serveOnce true
                    withMimeType MimeType.OctetStream
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection { verifyEmbeddedImageLoaded(expectedState = false) }
        }
    }

    @Test
    @TestId("203696")
    fun testConversationDetailEmbeddedImagesBlockedBannerIsNotDisplayedWhenNoEmbeddedImagesArePresent() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203696.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203696.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203696.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203696.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }
        }
    }

    @Test
    @TestId("203699", "203700/2")
    fun testConversationDetailEmbeddedImagesBlockedBannerIsDisplayedOnExternalEmails() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_203700_2.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_203700.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_203700.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_203700.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            bannerSection { verify { hasBlockedEmbeddedImagesBannerDisplayed() } }
        }
    }
}
