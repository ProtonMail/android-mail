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

package ch.protonmail.android.uitest.e2e.mailbox.detail.attachments

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
import ch.protonmail.android.networkmocks.mockwebserver.requests.withNetworkDelay
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.MessageDetailSnackbar
import ch.protonmail.android.uitest.robot.detail.section.attachmentsSection
import ch.protonmail.android.uitest.robot.detail.section.conversation.messagesCollapsedSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.messageHeaderSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Ignore
import org.junit.Test

@RegressionTest
@UninstallModules(ServerProofModule::class)
@HiltAndroidTest
internal class AttachmentConversationModeTests : MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @SdkSuppress(minSdkVersion = 29)
    @Ignore("To be addressed with MAILANDR-1276")
    @TestId("194318")
    fun testMultipleAttachmentDownloadingInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_194318.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_194318.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_194318.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_194318_2.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/attachments/*")
                    respondWith "/mail/v4/attachments/attachment_194318"
                    withStatusCode 200 matchWildcards true serveOnce true
                    withMimeType MimeType.OctetStream withNetworkDelay 150_000L
            )
        }

        val expectedSnackbar = MessageDetailSnackbar.MultipleDownloadsWarning

        navigator { navigateTo(Destination.MailDetail()) }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            attachmentsSection {
                tapItem()

                verify { hasLoaderDisplayedForItem() }
            }

            messageHeaderSection { collapseMessage() }
            messagesCollapsedSection { openMessageAtIndex(0) }

            messageBodySection { waitUntilMessageIsShown() }
            attachmentsSection { tapItem() }

            snackbarSection {
                verify { isDisplaying(expectedSnackbar) }
            }
        }
    }
}
