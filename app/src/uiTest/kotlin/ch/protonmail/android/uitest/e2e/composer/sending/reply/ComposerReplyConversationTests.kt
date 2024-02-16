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

package ch.protonmail.android.uitest.e2e.composer.sending.reply

import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.put
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withMimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.e2e.mailbox.detail.attachments.inline.EmbeddedImagesTests
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.robot.common.section.fullscreenLoaderSection
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.snackbar.ComposerSnackbar
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.section.bannerSection
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
internal class ComposerReplyConversationTests :
    MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara),
    ComposerTests,
    EmbeddedImagesTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @SmokeTest
    @SdkSuppress(minSdkVersion = 29)
    @TestId("260089")
    fun testReplyToMessageWithEmbeddedImagesWithConversationModeEnabled() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultMessagesList = false,
            useDefaultDraftUploadResponse = false,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_260089.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_260089.json"
                    withStatusCode 200 matchWildcards true ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_260089.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_260089_2.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_260089.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/attachments/*")
                    respondWith "/mail/v4/attachments/attachment_260089"
                    withStatusCode 200 matchWildcards true serveOnce true
                    withMimeType MimeType.OctetStream,
                // Draft creation
                post("/mail/v4/messages")
                    respondWith "/mail/v4/messages/post/post_messages_260089.json"
                    withStatusCode 200 serveOnce true,
                // Draft upload (1st)
                put("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/put/put_messages_260089.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                // Final draft upload
                put("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/put/put_messages_260089_2.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                // Actual sending
                post("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/post/post_messages_260089_2.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection { verifyEmbeddedImageLoaded(expectedState = true) }
            bannerSection { verify { hasBlockedContentBannerNotDisplayed() } }

            messageHeaderSection { tapReplyButton() }
        }

        composerRobot {
            fullscreenLoaderSection { waitUntilGone() }

            messageBodySection { typeMessageBody("Reply") }
            topAppBarSection { tapSendButton() }

            snackbarSection {
                verify { isDisplaying(ComposerSnackbar.SendingMessage) }
                verify { isDisplaying(ComposerSnackbar.MessageSent) }
            }
        }
    }
}
