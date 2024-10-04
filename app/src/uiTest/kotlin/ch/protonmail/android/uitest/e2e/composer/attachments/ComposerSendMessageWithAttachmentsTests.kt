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

package ch.protonmail.android.uitest.e2e.composer.attachments

import java.time.Instant
import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.simulateNoNetwork
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.snackbar.ComposerSnackbar
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import kotlin.test.Test

@RegressionTest
@HiltAndroidTest
@SdkSuppress(minSdkVersion = 30, maxSdkVersion = 32)
@UninstallModules(ServerProofModule::class)
internal class ComposerSendMessageWithAttachmentsTests :
    MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara),
    ComposerAttachmentsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val protonRecipient = "sleepykoala@proton.black"
    private val subject = "Test subject"
    private val body = "Test body"

    @Before
    fun setupTests() {
        val attachmentName = Instant.now().epochSecond.toString()
        val uri = initFakeFileUri("placeholder_image.jpg", attachmentName, "image/jpg")
        stubPickerActivityResultWithUri(uri)
    }

    @Test
    @SmokeTest
    @TestId("226734")
    fun testAttachmentSendingToProtonMailAddress() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = true
        ) {
            addMockRequests(
                post("/mail/v4/attachments")
                    respondWith "/mail/v4/attachments/attachments_226734.json"
                    withStatusCode 200
            )
        }

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prefillMessageWithAttachment()
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.SendingMessage) } }
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSent) } }
        }
    }

    @Test
    @TestId("226735")
    fun testAttachmentSendingToProtonMailAddressWithError() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                post("/mail/v4/attachments")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503
            )
        }

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prefillMessageWithAttachment()
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.SendingMessage) } }
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSentError) } }
        }
    }

    @Test
    @TestId("226736")
    fun testAttachmentSendingToProtonMailAddressWhenOfflineError() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                post("/mail/v4/attachments")
                    simulateNoNetwork true
            )
        }

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prefillMessageWithAttachment()
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.SendingMessage) } }

            // Deferred as on FTL it might propagate too quickly.
            every { networkManager.isConnectedToNetwork() } returns false

            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSentError) } }
        }
    }

    private fun ComposerRobot.prefillMessageWithAttachment() {
        toRecipientSection { typeRecipient(protonRecipient, autoConfirm = true) }
        subjectSection { typeSubject(subject) }
        messageBodySection { typeMessageBody(body) }

        topAppBarSection { tapAttachmentsButton() }
    }
}
