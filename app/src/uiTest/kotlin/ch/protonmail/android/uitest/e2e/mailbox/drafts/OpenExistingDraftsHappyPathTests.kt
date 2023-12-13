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

package ch.protonmail.android.uitest.e2e.mailbox.drafts

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.common.section.fullscreenLoaderSection
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify
import ch.protonmail.android.uitest.robot.composer.section.senderSection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.composer.section.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class OpenExistingDraftsHappyPathTests :
    MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara),
    OpenExistingDraftsTest {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val expectedToChip = RecipientChipEntry(
        index = 0,
        text = "aa@bb.cc",
        hasDeleteIcon = true,
        state = RecipientChipValidationState.Valid
    )

    private val expectedCcChip = RecipientChipEntry(
        index = 0,
        text = "dd@ee.ff",
        state = RecipientChipValidationState.Valid
    )

    private val expectedBccChip = RecipientChipEntry(
        index = 0,
        text = "gg@hh.ii",
        state = RecipientChipValidationState.Valid
    )

    private val expectedSubject = "Test subject"
    private val expectedSubjectPlaceholder = "(No Subject)"
    private val expectedMessageBody = "Some text"
    private val expectedAliasAddress = "shortcapybara@pm.me.proton.black"

    @Test
    @TestId("212662", "212664")
    fun openDraftWithPrefilledToRecipientAndOtherFields() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212662.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212662.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            toRecipientSection { verify { hasRecipientChips(expectedToChip) } }
            ccRecipientSection { verify { isHidden() } }
            bccRecipientSection { verify { isHidden() } }
            toRecipientSection { expandCcAndBccFields() }
            ccRecipientSection { verify { isEmptyField() } }
            bccRecipientSection { verify { isEmptyField() } }
            subjectSection { verify { hasSubject(expectedSubject) } }
            messageBodySection { verify { hasText(expectedMessageBody) } }
        }
    }

    @Test
    @SmokeTest
    @TestId("212663")
    fun openDraftWithAllPrefilledFields() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212663.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212663.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            verifyPrefilledFields(
                toRecipientChip = expectedToChip,
                ccRecipientChip = expectedCcChip,
                bccRecipientChip = expectedBccChip,
                subject = expectedSubject,
                messageBody = expectedMessageBody
            )
        }
    }

    @Test
    @SmokeTest
    @TestId("212663/2")
    fun openDraftWithAllPrefilledFieldsInMessageMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=8&Sort=Time&Desc=1")
                    respondWith "/mail/v4/messages/messages_212663.json"
                    withStatusCode 200 withPriority MockPriority.Highest,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212663.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            verifyPrefilledFields(
                toRecipientChip = expectedToChip,
                ccRecipientChip = expectedCcChip,
                bccRecipientChip = expectedBccChip,
                subject = expectedSubject,
                messageBody = expectedMessageBody
            )
        }
    }

    @Test
    @TestId("212665")
    fun openDraftWithoutSubjectAndMessageBody() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212665.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212665.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            verifyPrefilledFields(toRecipientChip = expectedToChip, subject = expectedSubjectPlaceholder)
        }
    }

    @Test
    @TestId("212666")
    fun openingDraftsAlwaysFetchesRemoteContentFirst() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212666.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212666.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212666_2.json"
                    withStatusCode 200 matchWildcards true
            )
        }

        val expectedUpdatedToChip = expectedToChip.copy(text = "aa@bb2.cc")
        val expectedUpdatedSubject = "Test subject 2"
        val expectedUpdatedMessageBody = "Some text 2"

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            verifyPrefilledFields(
                toRecipientChip = expectedToChip,
                subject = expectedSubject,
                messageBody = expectedMessageBody
            )

            topAppBarSection { tapCloseButton() }
        }

        mailboxRobot {
            listSection { clickMessageByPosition(0) }
        }

        composerRobot {
            fullscreenLoaderSection { waitUntilGone() }

            verifyPrefilledFields(
                toRecipientChip = expectedUpdatedToChip,
                subject = expectedUpdatedSubject,
                messageBody = expectedUpdatedMessageBody
            )
        }
    }

    @Test
    @TestId("212675")
    fun openingDraftPreservesSenderAddress() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212675.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212675.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            senderSection { verify { hasValue(expectedAliasAddress) } }
            toRecipientSection { verify { hasRecipientChips(expectedToChip) } }
        }
    }
}
