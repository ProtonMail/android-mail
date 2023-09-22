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
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.models.snackbar.SnackbarTextEntry
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.composerRobot
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
@UninstallModules(ServerProofModule::class)
internal class ComposerSendMessageToMultipleProtonTests : MockedNetworkTest(
    loginType = LoginTestUserTypes.Paid.FancyCapybara
), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val protonRecipientTo = listOf(ParticipantEntry.WithParticipant("royalcat@proton.black"))
    private val protonRecipientCc = listOf(ParticipantEntry.WithParticipant("royaldog@proton.black"))
    private val protonRecipientBcc = listOf(ParticipantEntry.WithParticipant("specialfox@proton.black"))
    private val mergedRecipients = protonRecipientTo + protonRecipientCc + protonRecipientBcc
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
                    hasNormalMessage(SnackbarTextEntry.SendingMessage)
                    hasSuccessMessage(SnackbarTextEntry.MessageSent)
                }
            }
        }
    }

    @Test
    @SmokeTest
    @TestId("216699")
    fun testMessageSendingToMultipleProtonUsers() {
        composerRobot {
            prepareDraft(mergedRecipients, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216636")
    fun testMessageSendingToMultipleProtonUsersWithNoBody() {
        composerRobot {
            prepareDraft(mergedRecipients, subject = subject)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216723")
    fun testMessageSendingToMultipleProtonUsersWithNoSubjectOrBody() {
        composerRobot {
            prepareDraft(toRecipients = mergedRecipients)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216700")
    fun testMessageSendingCcMultipleProtonUsers() {
        composerRobot {
            prepareDraft(ccRecipients = mergedRecipients, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216701")
    fun testMessageSendingBccMultipleProtonUsers() {
        composerRobot {
            prepareDraft(bccRecipients = mergedRecipients, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216703")
    fun testMessageSendingToAndCcProtonUsers() {
        composerRobot {
            prepareDraft(protonRecipientTo, ccRecipients = protonRecipientCc, subject = subject, body = baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216704")
    fun testMessageSendingToAndBccProtonUsers() {
        composerRobot {
            prepareDraft(
                protonRecipientTo,
                bccRecipients = protonRecipientBcc,
                subject = subject,
                body = baseMessageBody
            )
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216706")
    fun testMessageSendingToAndCcAndBccProtonUsers() {
        composerRobot {
            prepareDraft(protonRecipientTo, protonRecipientCc, protonRecipientBcc, subject, baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216724")
    fun testMessageSendingToAndCcAndBccProtonUsersWithNoBody() {
        composerRobot {
            prepareDraft(protonRecipientTo, protonRecipientCc, protonRecipientBcc, subject)
            topAppBarSection { tapSendButton() }
        }
    }

    @Test
    @TestId("216714")
    fun testMessageSendingToAndCcAndBccMultipleProtonUsers() {
        val toRecipients = protonRecipientTo + ParticipantEntry.WithParticipant("sleepykoala@proton.black")
        val ccRecipients = protonRecipientCc + ParticipantEntry.WithParticipant("happyllama@proton.black")
        val bccRecipients = protonRecipientBcc + ParticipantEntry.WithParticipant("strangewalrus@proton.black")

        composerRobot {
            prepareDraft(toRecipients, ccRecipients, bccRecipients, subject, baseMessageBody)
            topAppBarSection { tapSendButton() }
        }
    }
}
