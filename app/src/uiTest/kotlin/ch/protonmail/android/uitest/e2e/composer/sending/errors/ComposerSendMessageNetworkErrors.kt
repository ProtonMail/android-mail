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

package ch.protonmail.android.uitest.e2e.composer.sending.errors

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.put
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.simulateNoNetwork
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.TestingNotes
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.snackbar.ComposerSnackbar
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Ignore
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@TestingNotes("Scope to be expanded once MAILANDR-988 is addressed.")
@UninstallModules(ServerProofModule::class)
internal class ComposerSendMessageNetworkErrors : MockedNetworkTest(
    loginType = LoginTestUserTypes.Paid.FancyCapybara
), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val subject = "A subject"
    private val messageBody = "A message body"

    @Test
    @TestId("219650")
    fun testMessageSendingErrorOnKeyFetching() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultRecipientKeys = false
        ) {
            addMockRequests(
                get("/core/v4/keys")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 serveOnce true ignoreQueryParams true
            )
        }

        val recipients = listOf("example@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.AddressDisabled) } }
        }
    }

    @Test
    @TestId("219651")
    fun testMessageSendingErrorOnMultipleKeysFetching() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = true
        ) {
            addMockRequests(
                get("/core/v4/keys?Email=royalcat%40proton.black")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 serveOnce true withPriority MockPriority.Highest
            )
        }

        val recipients = listOf("royaldog@proton.black", "royalcat@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.AddressDisabled) } }
        }
    }

    @Test
    @TestId("219652")
    @Ignore("To be enabled again when MAILANDR-989 is addressed.")
    fun testMessageSendingErrorOnInvalidKeyFetched() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = false,
            useDefaultRecipientKeys = false
        ) {
            addMockRequests(
                get("/core/v4/keys?Email=royalcat%40proton.black")
                    respondWith "/core/v4/keys/keys_219652.json"
                    withStatusCode 200 serveOnce true
            )
        }

        val recipients = listOf("royalcat@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSentError) } }
        }
    }

    @Test
    @TestId("219653")
    @Ignore("To be enabled again when MAILANDR-989 is addressed.")
    fun testMessageSendingErrorOnMultipleInvalidKeysFetched() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                get("/core/v4/keys?Email=royalcat%40proton.black")
                    respondWith "/core/v4/keys/keys_219653.json"
                    withStatusCode 200 serveOnce true withPriority MockPriority.Highest
            )
        }

        val recipients = listOf("royalcat@proton.black", "royaldog@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSentError) } }
        }
    }

    @Test
    @TestId("222470")
    fun testMessageSendingWhenOffline() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = false,
            useDefaultSendMessageResponse = false,
            useDefaultRecipientKeys = false
        ) {
            addMockRequests(
                post("/mail/v4/messages")
                    simulateNoNetwork true ignoreQueryParams true serveOnce true,
                put("/mail/v4/messages")
                    simulateNoNetwork true ignoreQueryParams true,
                post("/mail/v4/messages/*")
                    simulateNoNetwork true matchWildcards true serveOnce true,
                get("/core/v4/keys")
                    simulateNoNetwork true ignoreQueryParams true
            )
        }

        val recipients = listOf("royalcat@proton.black")

        navigator { navigateTo(Destination.Composer) }

        every { networkManager.isConnectedToNetwork() } returns false

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageQueued) } }
        }
    }

    @Test
    @SmokeTest
    @TestId("219655")
    fun testMessageSendingWithServerErrorOnLastPost() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                post("/mail/v4/messages/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true
            )
        }

        val recipients = listOf("royalcat@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.SendingMessage) } }
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSentError) } }
        }
    }

    @Test
    @Ignore("To be enabled again when MAILANDR-1244 is addressed.")
    @TestId("219656")
    fun testMessageSendingWithServerErrorOnDraftUpload() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = false,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                post("/mail/v4/messages")
                    respondWith "/mail/v4/messages/post/post_messages_base_create_placeholder.json"
                    withStatusCode 200 serveOnce true,
                put("/mail/v4/messages/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true
            )
        }

        val recipients = listOf("royalcat@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.MessageSentError) } }
        }
    }

    @Test
    @Ignore("To be enabled again when MAILANDR-1244 is addressed.")
    @TestId("219657")
    fun testMessageSendingWithServerErrorOnDraftCreation() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = false,
            useDefaultSendMessageResponse = false
        ) {
            addMockRequests(
                post("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503,
                put("/mail/v4/messages/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true
            )
        }

        val recipients = listOf("royalcat@proton.black")

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            prepareDraft(toRecipients = recipients, subject = subject, body = messageBody)
            topAppBarSection { tapSendButton() }
        }

        mailboxRobot {
            snackbarSection {
                verify {
                    isDisplaying(ComposerSnackbar.SendingMessage)
                    isDisplaying(ComposerSnackbar.MessageSentError)
                }
            }
        }
    }
}
