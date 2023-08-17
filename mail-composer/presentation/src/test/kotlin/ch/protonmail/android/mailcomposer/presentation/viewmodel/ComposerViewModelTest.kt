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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploader
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.GetDecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAllFields
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBodyError
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithRecipients
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSubject
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.mapper.ParticipantMapper
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.test.idlingresources.ComposerIdlingResource
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val storeDraftWithAllFields = mockk<StoreDraftWithAllFields>()
    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>()
    private val storeDraftWithSubjectMock = mockk<StoreDraftWithSubject>()
    private val storeDraftWithRecipientsMock = mockk<StoreDraftWithRecipients>()
    private val getContactsMock = mockk<GetContacts>()
    private val participantMapperMock = mockk<ParticipantMapper>()
    private val observePrimaryUserIdMock = mockk<ObservePrimaryUserId>()
    private val composerIdlingResource = spyk<ComposerIdlingResource>()
    private val isValidEmailAddressMock = mockk<IsValidEmailAddress>()
    private val getPrimaryAddressMock = mockk<GetPrimaryAddress>()
    private val provideNewDraftIdMock = mockk<ProvideNewDraftId>()
    private val draftUploader = mockk<DraftUploader>()
    private val getComposerSenderAddresses = mockk<GetComposerSenderAddresses> {
        coEvery { this@mockk.invoke() } returns GetComposerSenderAddresses.Error.UpgradeToChangeSender.left()
    }
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDecryptedDraftFields = mockk<GetDecryptedDraftFields>()
    private val reducer = ComposerReducer()

    private val viewModel by lazy {
        ComposerViewModel(
            storeDraftWithBodyMock,
            storeDraftWithSubjectMock,
            storeDraftWithAllFields,
            storeDraftWithRecipientsMock,
            getContactsMock,
            participantMapperMock,
            reducer,
            isValidEmailAddressMock,
            getPrimaryAddressMock,
            getComposerSenderAddresses,
            composerIdlingResource,
            draftUploader,
            getDecryptedDraftFields,
            savedStateHandle,
            observePrimaryUserIdMock,
            provideNewDraftIdMock
        )
    }

    @Test
    fun `should store the draft body when the body changes`() {
        // Given
        val primaryAddress = UserAddressSample.PrimaryAddress
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val action = ComposerAction.DraftBodyChanged(expectedDraftBody)
        expectedPrimaryAddress(expectedUserId) { primaryAddress }
        expectStoreDraftBodySucceeds(
            expectedMessageId,
            expectedDraftBody,
            expectedSenderEmail,
            expectedUserId
        )
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderEmail,
                expectedUserId
            )
        }
    }

    @Test
    fun `should store draft with sender and current draft body when sender changes`() = runTest {
        // Given
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(expectedMessageId, expectedDraftBody, expectedSenderEmail, expectedUserId)
        expectNoInputDraftMessageId()

        // Change internal state of the View Model to simulate an existing draft body before changing sender
        expectedViewModelInitialState(messageId = expectedMessageId, draftBody = expectedDraftBody)

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderEmail,
                expectedUserId
            )
        }
    }

    @Test
    fun `should store draft subject when subject changes`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SubjectChanged(expectedSubject)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftSubjectSucceeds(expectedMessageId, expectedSenderEmail, expectedUserId, expectedSubject)
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithSubjectMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                expectedSubject
            )
        }
    }

    @Test
    fun `should store draft recipients TO when they change`() = runTest {
        // Given
        val expectedRecipients = listOf(
            Recipient("valid@email.com", "Valid Email", false)
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Valid("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.RecipientsToChanged(recipientsUiModels)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftRecipientsSucceeds(
            expectedMessageId,
            expectedSenderEmail,
            expectedUserId,
            expectedTo = expectedRecipients
        )
        mockParticipantMapper()
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                to = expectedRecipients
            )
        }
    }

    @Test
    fun `should store draft recipients CC when they change`() = runTest {
        // Given
        val expectedRecipients = listOf(
            Recipient("valid@email.com", "Valid Email", false)
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Valid("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.RecipientsCcChanged(recipientsUiModels)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftRecipientsSucceeds(
            expectedMessageId,
            expectedSenderEmail,
            expectedUserId,
            expectedCc = expectedRecipients
        )
        mockParticipantMapper()
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                cc = expectedRecipients
            )
        }
    }

    @Test
    fun `should store draft recipients BCC when they change`() = runTest {
        // Given
        val expectedRecipients = listOf(
            Recipient("valid@email.com", "Valid Email", false)
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Valid("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.RecipientsBccChanged(recipientsUiModels)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftRecipientsSucceeds(
            expectedMessageId,
            expectedSenderEmail,
            expectedUserId,
            expectedBcc = expectedRecipients
        )
        mockParticipantMapper()
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                bcc = expectedRecipients
            )
        }
    }

    @Test
    fun `should store all draft fields and upload the draft when composer is closed`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftBody = DraftBody("I am plaintext")
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            expectedSubject,
            expectedDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        mockParticipantMapper()
        expectStoreAllDraftFieldsSucceeds(expectedUserId, expectedMessageId, expectedFields)
        expectNoInputDraftMessageId()
        expectUploadDraftSucceeds(expectedUserId, expectedMessageId)
        expectStopContinuousDraftUploadSucceeds()

        // Change internal state of the View Model to simulate the existence of all fields before closing the composer
        expectedViewModelInitialState(
            expectedMessageId,
            expectedSenderEmail,
            expectedSubject,
            expectedDraftBody,
            Triple(recipientsTo, recipientsCc, recipientsBcc)
        )

        // When
        viewModel.submit(ComposerAction.OnCloseComposer)

        // Then
        coVerifyOrder {
            draftUploader.stopContinuousUpload()
            storeDraftWithAllFields(expectedUserId, expectedMessageId, expectedFields)
            draftUploader.upload(expectedUserId, expectedMessageId)
        }
        assertEquals(Effect.of(Unit), viewModel.state.value.closeComposerWithDraftSaved)
    }

    @Test
    fun `should not store draft when all fields are empty and composer is closed`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(ComposerAction.OnCloseComposer)

        // Then
        coVerify { storeDraftWithAllFields wasNot Called }
        coVerify(exactly = 0) { draftUploader.upload(any(), any()) }
        assertEquals(Effect.of(Unit), viewModel.state.value.closeComposer)
    }

    @Test
    fun `should store and upload draft when any field which requires user input is not empty and composer is closed`() =
        runTest {
            // Given
            val expectedSubject = Subject("Added subject")
            val expectedDraftBody = DraftBody("")
            val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
            val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
            val expectedUserId = expectedUserId { UserIdSample.Primary }
            expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
            val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
            val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
            val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
            val expectedFields = DraftFields(
                expectedSenderEmail,
                expectedSubject,
                expectedDraftBody,
                recipientsTo,
                recipientsCc,
                recipientsBcc
            )
            mockParticipantMapper()
            expectStoreAllDraftFieldsSucceeds(expectedUserId, expectedMessageId, expectedFields)
            expectNoInputDraftMessageId()
            expectStopContinuousDraftUploadSucceeds()
            expectUploadDraftSucceeds(expectedUserId, expectedMessageId)

            // Change internal state of the View Model to simulate the
            // existence of all fields before closing the composer
            expectedViewModelInitialState(
                expectedMessageId,
                expectedSenderEmail,
                expectedSubject,
                recipients = Triple(recipientsTo, recipientsCc, recipientsBcc)
            )

            // When
            viewModel.submit(ComposerAction.OnCloseComposer)

            // Then
            coVerifyOrder {
                draftUploader.stopContinuousUpload()
                storeDraftWithAllFields(expectedUserId, expectedMessageId, expectedFields)
                draftUploader.upload(expectedUserId, expectedMessageId)
            }
        }

    @Test
    fun `emits state with primary sender address when available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedMessageId { MessageIdSample.EmptyDraft }
        val primaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(SenderUiModel(primaryAddress.email), actual.fields.sender)
    }

    @Test
    fun `emits state with sender address error when not available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddressError(expectedUserId) { DataError.Local.NoDataCached }
        expectNoInputDraftMessageId()

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(TextUiModel(R.string.composer_error_invalid_sender), actual.error.consume())
    }

    @Test
    fun `emits state with user addresses when sender can be changed`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val addresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedGetComposerSenderAddresses { addresses }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(ComposerAction.ChangeSenderRequested)

        // Then
        val currentState = viewModel.state.value
        val expected = addresses.map { SenderUiModel(it.email) }
        assertEquals(expected, currentState.senderAddresses)
    }

    @Test
    fun `emits state with upgrade plan to change sender when user cannot change sender`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedGetComposerSenderAddressesError { GetComposerSenderAddresses.Error.UpgradeToChangeSender }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(ComposerAction.ChangeSenderRequested)

        // Then
        val currentState = viewModel.state.value
        val expected = TextUiModel(R.string.composer_change_sender_paid_feature)
        assertEquals(expected, currentState.premiumFeatureMessage.consume())
    }

    @Test
    fun `emits state with error when cannot determine if user can change sender`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedGetComposerSenderAddressesError { GetComposerSenderAddresses.Error.FailedDeterminingUserSubscription }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(ComposerAction.ChangeSenderRequested)

        // Then
        val currentState = viewModel.state.value
        val expected = TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription)
        assertEquals(expected, currentState.error.consume())
    }

    @Test
    fun `emits state with new sender address when sender changed`() = runTest {
        // Given
        val expectedDraftBody = DraftBody("")
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(expectedMessageId, expectedDraftBody, expectedSenderEmail, expectedUserId)
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(SenderUiModel(expectedSenderEmail.value), currentState.fields.sender)
    }

    @Test
    fun `emits state with saving draft with new sender error when save draft with sender returns error`() = runTest {
        // Given
        val expectedDraftBody = DraftBody("")
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodyFails(expectedMessageId, expectedDraftBody, expectedSenderEmail, expectedUserId) {
            StoreDraftWithBodyError.DraftSaveError
        }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_sender_address), currentState.error.consume())
        loggingTestRule.assertErrorLogged(
            "Store draft $expectedMessageId with new sender ${expectedSenderEmail.value} failed"
        )
    }

    @Test
    fun `emits state with saving draft body error when save draft body returns error`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftBody = DraftBody("updated-draft")
        val action = ComposerAction.DraftBodyChanged(expectedDraftBody)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodyFails(expectedMessageId, expectedDraftBody, expectedSenderEmail, expectedUserId) {
            StoreDraftWithBodyError.DraftSaveError
        }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_body), currentState.error.consume())
    }

    @Test
    fun `emits state with saving draft subject error when save draft subject returns error`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SubjectChanged(expectedSubject)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftSubjectFails(expectedMessageId, expectedSenderEmail, expectedUserId, expectedSubject) {
            StoreDraftWithSubject.Error.DraftReadError
        }
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_subject), currentState.error.consume())
        loggingTestRule.assertErrorLogged(
            "Store draft $expectedMessageId with new subject $expectedSubject failed"
        )
    }

    @Test
    fun `emits state with saving draft subject error when save draft TO returns error`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedRecipients = listOf(
            Recipient("valid@email.com", "Valid Email", false)
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Valid("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        val action = ComposerAction.RecipientsToChanged(recipientsUiModels)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftRecipientsFails(
            expectedMessageId, expectedSenderEmail, expectedUserId,
            expectedTo = expectedRecipients, expectedCc = null, expectedBcc = null
        ) {
            StoreDraftWithRecipients.Error.DraftSaveError
        }
        mockParticipantMapper()
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_recipients), currentState.error.consume())
    }

    @Test
    fun `emits state with saving draft subject error when save draft CC returns error`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedRecipients = listOf(
            Recipient("valid@email.com", "Valid Email", false)
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Valid("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        val action = ComposerAction.RecipientsCcChanged(recipientsUiModels)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftRecipientsFails(
            expectedMessageId, expectedSenderEmail, expectedUserId,
            expectedTo = null, expectedCc = expectedRecipients, expectedBcc = null
        ) {
            StoreDraftWithRecipients.Error.DraftSaveError
        }
        mockParticipantMapper()
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_recipients), currentState.error.consume())
    }

    @Test
    fun `emits state with saving draft subject error when save draft BCC returns error`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedRecipients = listOf(
            Recipient("valid@email.com", "Valid Email", false)
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Valid("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        val action = ComposerAction.RecipientsBccChanged(recipientsUiModels)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftRecipientsFails(
            expectedMessageId, expectedSenderEmail, expectedUserId,
            expectedTo = null, expectedCc = null, expectedBcc = expectedRecipients
        ) {
            StoreDraftWithRecipients.Error.DraftSaveError
        }
        mockParticipantMapper()
        expectNoInputDraftMessageId()

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_recipients), currentState.error.consume())
    }

    @Test
    fun `emits state with loading draft content when draftId was given as input`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        // Simulate a small delay in getDecryptedDraftFields to ensure the "loading" state was emitted
        expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId, 100) { existingDraftFields }

        // When
        val actual = viewModel.state.value

        // Then
        assertTrue(actual.isLoading)
        coVerify { getDecryptedDraftFields(expectedUserId, expectedDraftId) }
    }

    @Test
    fun `emits state with draft fields to be prefilled when getting decrypted draft fields succeeds`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val expectedDraftFields = existingDraftFields
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId) { expectedDraftFields }

        // When
        val actual = viewModel.state.value

        // Then
        val expectedComposerFields = ComposerFields(
            expectedDraftId,
            SenderUiModel(expectedDraftFields.sender.value),
            expectedDraftFields.recipientsTo.value.map { RecipientUiModel.Valid(it.address) },
            emptyList(),
            emptyList(),
            expectedDraftFields.subject.value,
            expectedDraftFields.body.value
        )
        assertEquals(expectedComposerFields, actual.fields)
    }

    @Test
    fun `emits state with error loading existing draft when getting decrypted draft fields fails`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectDecryptedDraftDataError(expectedUserId, expectedDraftId) { DataError.Local.NoDataCached }

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(TextUiModel(R.string.composer_error_loading_draft), actual.error.consume())
    }

    @Test
    fun `starts syncing draft for current messageId when composer is opened`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(messageId, actual.fields.draftId)
        coVerify { draftUploader.startContinuousUpload(userId, messageId, DraftAction.Compose, any()) }
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(ComposerDraftState.Companion)
    }

    private fun expectDecryptedDraftDataError(
        userId: UserId,
        draftId: MessageId,
        error: () -> DataError
    ) = error().also { coEvery { getDecryptedDraftFields(userId, draftId) } returns it.left() }

    private fun expectDecryptedDraftDataSuccess(
        userId: UserId,
        draftId: MessageId,
        responseDelay: Long = 0L,
        result: () -> DraftFields
    ) = result().also { draftFields ->
        coEvery { getDecryptedDraftFields(userId, draftId) } coAnswers {
            delay(responseDelay)
            draftFields.right()
        }
    }

    private fun expectNoInputDraftMessageId() {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
    }

    private fun expectInputDraftMessageId(draftId: () -> MessageId) = draftId().also {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns it.id
    }

    private fun expectStopContinuousDraftUploadSucceeds() {
        coEvery { draftUploader.stopContinuousUpload() } returns Unit
    }

    private fun expectUploadDraftSucceeds(expectedUserId: UserId, expectedMessageId: MessageId) {
        coEvery { draftUploader.upload(expectedUserId, expectedMessageId) } returns Unit
    }

    private fun expectStartDraftSync(userId: UserId, messageId: MessageId) {
        coEvery { draftUploader.startContinuousUpload(userId, messageId, DraftAction.Compose, any()) } returns Unit
    }

    private fun expectedViewModelInitialState(
        messageId: MessageId,
        senderEmail: SenderEmail = SenderEmail(""),
        subject: Subject = Subject(""),
        draftBody: DraftBody = DraftBody(""),
        recipients: Triple<RecipientsTo, RecipientsCc, RecipientsBcc> = Triple(
            RecipientsTo(emptyList()),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList())
        )
    ) {
        val expected = ComposerDraftState(
            fields = ComposerFields(
                messageId,
                SenderUiModel(senderEmail.value),
                recipients.first.value.map { RecipientUiModel.Valid(it.address) },
                recipients.second.value.map { RecipientUiModel.Valid(it.address) },
                recipients.third.value.map { RecipientUiModel.Valid(it.address) },
                subject.value,
                draftBody.value
            ),
            premiumFeatureMessage = Effect.empty(),
            error = Effect.empty(),
            isSubmittable = false,
            senderAddresses = emptyList(),
            changeSenderBottomSheetVisibility = Effect.empty(),
            closeComposer = Effect.empty(),
            closeComposerWithDraftSaved = Effect.empty(),
            isLoading = false
        )

        mockkObject(ComposerDraftState.Companion)
        every { ComposerDraftState.initial(messageId) } returns expected
    }

    private fun expectedMessageId(messageId: () -> MessageId): MessageId = messageId().also {
        every { provideNewDraftIdMock() } returns it
    }

    private fun expectedUserId(userId: () -> UserId): UserId = userId().also {
        coEvery { observePrimaryUserIdMock() } returns flowOf(it)
    }

    private fun expectedPrimaryAddress(userId: UserId, userAddress: () -> UserAddress) = userAddress().also {
        coEvery { getPrimaryAddressMock(userId) } returns it.right()
    }

    private fun expectedPrimaryAddressError(userId: UserId, dataError: () -> DataError) = dataError().also {
        coEvery { getPrimaryAddressMock(userId) } returns it.left()
    }

    private fun expectedGetComposerSenderAddresses(addresses: () -> List<UserAddress>): List<UserAddress> =
        addresses().also { coEvery { getComposerSenderAddresses() } returns it.right() }

    private fun expectedGetComposerSenderAddressesError(
        error: () -> GetComposerSenderAddresses.Error
    ): GetComposerSenderAddresses.Error = error().also { coEvery { getComposerSenderAddresses() } returns it.left() }

    private fun expectStoreDraftBodySucceeds(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId
    ) {
        coEvery {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderEmail,
                expectedUserId
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftBodyFails(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        error: () -> StoreDraftWithBodyError
    ) = error().also {
        coEvery {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderEmail,
                expectedUserId
            )
        } returns it.left()
    }

    private fun expectStoreDraftSubjectSucceeds(
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        expectedSubject: Subject
    ) {
        coEvery {
            storeDraftWithSubjectMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                expectedSubject
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftSubjectFails(
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        expectedSubject: Subject,
        error: () -> StoreDraftWithSubject.Error
    ) = error().also {
        coEvery {
            storeDraftWithSubjectMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                expectedSubject
            )
        } returns it.left()
    }

    private fun expectStoreDraftRecipientsSucceeds(
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        expectedTo: List<Recipient>? = null,
        expectedCc: List<Recipient>? = null,
        expectedBcc: List<Recipient>? = null
    ) {
        coEvery {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                to = expectedTo,
                cc = expectedCc,
                bcc = expectedBcc
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftRecipientsFails(
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        expectedTo: List<Recipient>? = emptyList(),
        expectedCc: List<Recipient>? = emptyList(),
        expectedBcc: List<Recipient>? = emptyList(),
        error: () -> StoreDraftWithRecipients.Error
    ) = error().also {
        coEvery {
            storeDraftWithRecipientsMock(
                expectedUserId,
                expectedMessageId,
                expectedSenderEmail,
                to = expectedTo,
                cc = expectedCc,
                bcc = expectedBcc
            )
        } returns it.left()
    }

    private fun expectStoreAllDraftFieldsSucceeds(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        expectedFields: DraftFields
    ) {
        coEvery {
            storeDraftWithAllFields(
                expectedUserId,
                expectedMessageId,
                expectedFields
            )
        } returns Unit
    }

    private fun expectContacts(): List<Contact> {
        val expectedContacts = listOf(ContactSample.Doe, ContactSample.John)
        coEvery { getContactsMock.invoke(UserIdSample.Primary) } returns expectedContacts.right()
        return expectedContacts
    }

    private fun mockParticipantMapper() {
        val expectedContacts = expectContacts()
        every {
            participantMapperMock.recipientUiModelToParticipant(
                RecipientUiModel.Valid("valid@email.com"),
                expectedContacts
            )
        } returns Recipient("valid@email.com", "Valid Email", false)
        every {
            participantMapperMock.recipientUiModelToParticipant(
                RecipientUiModel.Valid(RecipientSample.John.address),
                any()
            )
        } returns Recipient(RecipientSample.John.address, RecipientSample.John.name, false)
    }

    companion object TestData {

        const val RawDraftBody = "I'm a message body"

        val existingDraftFields = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            DraftBody("Decrypted body of this draft"),
            RecipientsTo(listOf(Recipient("you@proton.ch", "Name"))),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList())
        )

    }
}
