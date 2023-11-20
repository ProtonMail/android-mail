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
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.simulateNoNetwork
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.TestingNotes
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.common.section.fullscreenLoaderSection
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState
import ch.protonmail.android.uitest.robot.composer.model.snackbar.ComposerSnackbar
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
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
internal class OpenExistingDraftsErrorTests :
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

    private val expectedSubject = "Test subject"
    private val expectedMessageBody = "Some text"

    @Test
    @TestId("212667")
    fun openingDraftInOfflineModeWithNoLocalCacheShowsSnackbarWarning() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212667.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    simulateNoNetwork true matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            snackbarSection { verify { isDisplaying(ComposerSnackbar.DraftOutOfSync) } }
            verifyEmptyFields()
        }
    }

    @Test
    @SmokeTest
    @TestId("212668")
    fun openingDraftWhenBeReturnsErrorWithNoLocalCacheShowsSnackbarWarning() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212668.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            verifyEmptyFields()
            snackbarSection { verify { isDisplaying(ComposerSnackbar.DraftOutOfSync) } }
        }
    }

    @Test
    @TestId("212669")
    fun openingDraftWithDecryptionErrorShowsOutOfSyncWarning() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212669.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212669.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        composerRobot {
            verifyEmptyFields()
            snackbarSection { verify { isDisplaying(ComposerSnackbar.DraftOutOfSync) } }
        }
    }

    @Test
    @TestingNotes("Add snackbar check once it's implemented (see MAILANDR-862)")
    @TestId("212670")
    fun openingDraftInOfflineModeWithLocalCacheShowsCachedData() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212670.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212670.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212670.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    simulateNoNetwork true matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        verifyLoadedCachedData()
    }

    @Test
    @SmokeTest
    @TestingNotes("Add snackbar check once it's implemented (see MAILANDR-862)")
    @TestId("212673")
    fun openingDraftOnBeErrorWithLocalCacheShowsCachedData() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212673.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212673.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212673.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 matchWildcards true
            )
        }

        navigator {
            navigateTo(Destination.EditDraft())
        }

        verifyLoadedCachedData()
    }

    @Test
    @TestId("212674")
    fun openingDraftOnDecryptionErrorWithLocalCacheShowsEmptyFields() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_212674.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212674.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_212674_2.json"
                    withStatusCode 200 matchWildcards true
            )
        }

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

        mailboxRobot { listSection { clickMessageByPosition(0) } }

        composerRobot {
            fullscreenLoaderSection { waitUntilGone() }
            verifyEmptyFields()
            snackbarSection { verify { isDisplaying(ComposerSnackbar.DraftOutOfSync) } }
        }
    }

    private fun verifyLoadedCachedData() {
        composerRobot {
            verifyPrefilledFields(
                toRecipientChip = expectedToChip,
                subject = expectedSubject,
                messageBody = expectedMessageBody
            )

            topAppBarSection { tapCloseButton() }
        }

        mailboxRobot { listSection { clickMessageByPosition(0) } }

        composerRobot {
            fullscreenLoaderSection { waitUntilGone() }

            verifyPrefilledFields(
                toRecipientChip = expectedToChip,
                subject = expectedSubject,
                messageBody = expectedMessageBody
            )
        }
    }
}
