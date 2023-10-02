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

package ch.protonmail.android.uitest.e2e.composer.sending

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
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
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.After
import org.junit.Before
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@TestingNotes("Scope to be expanded once MAILANDR-988 is addressed.")
@UninstallModules(ServerProofModule::class)
internal class ComposerSendMessageToMultipleExternalTests : MockedNetworkTest(
    loginType = LoginTestUserTypes.Paid.FancyCapybara
), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val externalRecipientTo = "test@example.com"
    private val externalRecipientCc = "test2@example.com"
    private val externalRecipientBcc = "test3@example.com"
    private val mergedRecipients = listOf(externalRecipientTo, externalRecipientCc, externalRecipientBcc)
    private val subject = "A subject"
    private val baseMessageBody = "A message body"

    @Before
    fun setupAndNavigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher(
            useDefaultDraftUploadResponse = true,
            useDefaultSendMessageResponse = true
        )

        navigator { navigateTo(Destination.Composer) }
    }

    @After
    fun verifyMessageSent() {
        mailboxRobot {
            snackbarSection {
                verify {
                    isDisplaying(ComposerSnackbar.SendingMessage)
                    isDisplaying(ComposerSnackbar.MessageSent)
                }
            }
        }
    }

    @Test
    @SmokeTest
    @TestId("216696")
    fun testMessageSendingToMultipleExternalUsers() {
        composerRobot {
            prepareDraft(mergedRecipients, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216729")
    fun testMessageSendingToMultipleExternalUsersWithNoBody() {
        composerRobot {
            prepareDraft(mergedRecipients, subject = subject)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216697")
    fun testMessageSendingCcMultipleExternalUsers() {
        composerRobot {
            prepareDraft(ccRecipients = mergedRecipients, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216698")
    fun testMessageSendingBccMultipleExternalUsers() {
        composerRobot {
            prepareDraft(bccRecipients = mergedRecipients, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216702")
    fun testMessageSendingToAndCcExternalUsers() {
        composerRobot {
            prepareDraft(
                externalRecipientTo,
                ccRecipient = externalRecipientCc,
                subject = subject,
                body = baseMessageBody
            )
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216705")
    fun testMessageSendingToAndBccExternalUsers() {
        composerRobot {
            prepareDraft(
                externalRecipientTo,
                bccRecipient = externalRecipientBcc,
                subject = subject,
                body = baseMessageBody
            )
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216707")
    fun testMessageSendingToAndCcAndBccExternalUsers() {
        composerRobot {
            prepareDraft(externalRecipientTo, externalRecipientCc, externalRecipientBcc, subject, baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216730")
    fun testMessageSendingToAndCcAndBccExternalUsersWithNoBody() {
        composerRobot {
            prepareDraft(externalRecipientTo, externalRecipientCc, externalRecipientBcc, subject)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216714")
    fun testMessageSendingToAndCcAndBccMultipleExternalUsers() {
        val toRecipients = listOf(externalRecipientTo, "test4@example.com")
        val ccRecipients = listOf(externalRecipientCc, "test5@example.com")
        val bccRecipients = listOf(externalRecipientBcc, "test6@example.com")

        composerRobot {
            prepareDraft(toRecipients, ccRecipients, bccRecipients, subject, baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }
}
