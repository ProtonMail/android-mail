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
import ch.protonmail.android.uitest.robot.detail.messageDetailRobot
import ch.protonmail.android.uitest.robot.detail.section.attachmentsSection
import ch.protonmail.android.uitest.robot.detail.section.detailTopBarSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.verify
import ch.protonmail.android.uitest.robot.helpers.deviceRobot
import ch.protonmail.android.uitest.robot.helpers.section.intents
import ch.protonmail.android.uitest.robot.helpers.section.storage
import ch.protonmail.android.uitest.robot.helpers.section.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@UninstallModules(ServerProofModule::class)
@HiltAndroidTest
internal class AttachmentDetailsMainTests : MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("189705")
    fun testAttachmentLoaderIconShownOnFetching() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_189705.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_189705.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/attachments/*"
                    respondWith "/mail/v4/attachments/attachment_small_jpg"
                    withStatusCode 200 matchWildcards true withMimeType MimeType.OctetStream withNetworkDelay 5000
            )
        }

        navigator { navigateTo(Destination.MailDetail(messagePosition = 0)) }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            attachmentsSection {
                tapItem()
                verify { hasLoaderDisplayedForItem() }
            }
        }
    }

    @Test
    @TestId("194341")
    fun testAttachmentLoaderIsStillShownOnMessageReopening() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_194341.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_194341.json"
                    withStatusCode 200 matchWildcards true,
                "/mail/v4/attachments/*"
                    respondWith "/mail/v4/attachments/attachment_small_jpg"
                    withStatusCode 200 matchWildcards true withMimeType MimeType.OctetStream withNetworkDelay 5000
            )
        }

        navigator { navigateTo(Destination.MailDetail()) }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            attachmentsSection {
                tapItem()
            }

            detailTopBarSection { tapBackButton() }
        }

        mailboxRobot {
            listSection { clickMessageByPosition(0) }
        }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            attachmentsSection {
                verify { hasLoaderDisplayedForItem() }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    @TestId("194278")
    fun testFilesArePresentInTheDownloadFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_194278.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_194278.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/attachments/*"
                    respondWith "/mail/v4/attachments/attachment_small_jpg"
                    withStatusCode 200 matchWildcards true withMimeType MimeType.OctetStream withNetworkDelay 1000L
            )
        }

        val expectedImageFileName = "An attached image.jpeg"

        navigator { navigateTo(Destination.MailDetail(messagePosition = 0)) }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            attachmentsSection {
                tapItem()

                verify {
                    hasLoaderDisplayedForItem()
                    hasLoaderNotDisplayedForItem()
                }
            }
        }

        deviceRobot {
            storage {
                verify { containsFileInDownloadsWithName(expectedImageFileName) }
            }
        }
    }

    @Test
    @TestId("194303", "194344")
    fun testAttachmentIsNotRefetchedIfAlreadyDownloadedExists() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_194303.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages/*"
                    respondWith "/mail/v4/messages/message-id/message-id_194303.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                "/mail/v4/attachments/*"
                    respondWith "/mail/v4/attachments/attachment_small_jpg"
                    withStatusCode 200 matchWildcards true serveOnce true
                    withNetworkDelay 1000L withMimeType MimeType.OctetStream
            )
        }

        navigator { navigateTo(Destination.MailDetail()) }

        messageDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }

            attachmentsSection {
                tapItem()
                verify {
                    hasLoaderDisplayedForItem()
                    hasLoaderNotDisplayedForItem()
                }

                tapItem()
                verify { hasLoaderNotDisplayedForItem() }
            }
        }

        deviceRobot {
            intents { verify { actionViewIntentWasLaunched(times = 2) } }
        }
    }
}
