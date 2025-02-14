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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitials
import ch.protonmail.android.mailcomposer.domain.model.DecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.StyledHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.AttachmentReEncryptionError
import ch.protonmail.android.mailcomposer.domain.usecase.ClearMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAllAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploader
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.GetDecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.GetExternalRecipients
import ch.protonmail.android.mailcomposer.domain.usecase.GetLocalMessageDecrypted
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePassword
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.ReEncryptAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.SendMessage
import ch.protonmail.android.mailcomposer.domain.usecase.StoreAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAllFields
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAttachmentError
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBodyError
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithParentAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithRecipients
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSubject
import ch.protonmail.android.mailcomposer.domain.usecase.StoreExternalAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.mapper.ParticipantMapper
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.usecase.ConvertHtmlToPlainText
import ch.protonmail.android.mailcomposer.presentation.usecase.FormatMessageSendingError
import ch.protonmail.android.mailcomposer.presentation.usecase.InjectAddressSignature
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields
import ch.protonmail.android.mailcomposer.presentation.usecase.SortContactsForSuggestions
import ch.protonmail.android.mailcomposer.presentation.usecase.StyleQuotedHtml
import ch.protonmail.android.mailcontact.domain.DeviceContactsSuggestionsPrompt
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchContactGroups
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchDeviceContacts
import ch.protonmail.android.mailcontact.domain.usecase.featureflags.IsDeviceContactsSuggestionsEnabled
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.mailmessage.domain.usecase.ShouldRestrictWebViewHeight
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.NO_ATTACHMENT_LIMIT
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import ch.protonmail.android.test.idlingresources.ComposerIdlingResource
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.util.kotlin.serialize
import org.junit.Assert.assertNull
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class ComposerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val storeAttachments = mockk<StoreAttachments>()
    private val storeDraftWithAllFields = mockk<StoreDraftWithAllFields>()
    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>()
    private val storeDraftWithSubjectMock = mockk<StoreDraftWithSubject> {
        coEvery { this@mockk.invoke(any(), any(), any(), any()) } returns Unit.right()
    }
    private val storeDraftWithRecipientsMock = mockk<StoreDraftWithRecipients>()
    private val storeExternalAttachmentStates = mockk<StoreExternalAttachments>()
    private val sendMessageMock = mockk<SendMessage>()
    private val networkManagerMock = mockk<NetworkManager>()
    private val getContactsMock = mockk<GetContacts>()
    private val searchContactsMock = mockk<SearchContacts>()
    private val searchDeviceContactsMock = mockk<SearchDeviceContacts>()
    private val deviceContactsSuggestionsPromptMock = mockk<DeviceContactsSuggestionsPrompt> {
        coEvery { this@mockk.getPromptEnabled() } returns true
        coEvery { this@mockk.setPromptDisabled() } just Runs
    }
    private val isDeviceContactsSuggestionsEnabledMock = mockk<IsDeviceContactsSuggestionsEnabled> {
        every { this@mockk.invoke() } returns false
    }
    private val searchContactGroupsMock = mockk<SearchContactGroups>()
    private val participantMapperMock = mockk<ParticipantMapper>()
    private val observePrimaryUserIdMock = mockk<ObservePrimaryUserId>()
    private val composerIdlingResource = spyk<ComposerIdlingResource>()
    private val isValidEmailAddressMock = mockk<IsValidEmailAddress>()
    private val getPrimaryAddressMock = mockk<GetPrimaryAddress>()
    private val provideNewDraftIdMock = mockk<ProvideNewDraftId>()
    private val draftUploaderMock = mockk<DraftUploader>()
    private val getComposerSenderAddresses = mockk<GetComposerSenderAddresses> {
        coEvery { this@mockk.invoke() } returns GetComposerSenderAddresses.Error.UpgradeToChangeSender.left()
    }
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val getDecryptedDraftFields = mockk<GetDecryptedDraftFields>()
    private val styleQuotedHtml = mockk<StyleQuotedHtml>()
    private val getLocalMessageDecrypted = mockk<GetLocalMessageDecrypted>()
    private val injectAddressSignature = mockk<InjectAddressSignature>()
    private val parentMessageToDraftFields = mockk<ParentMessageToDraftFields>()
    private val storeDraftWithParentAttachments = mockk<StoreDraftWithParentAttachments>()
    private val deleteAttachment = mockk<DeleteAttachment>()
    private val deleteAllAttachments = mockk<DeleteAllAttachments>()
    private val observeMessageAttachments = mockk<ObserveMessageAttachments>()
    private val observeMessageSendingError = mockk<ObserveMessageSendingError>()
    private val clearMessageSendingError = mockk<ClearMessageSendingError>()
    private val formatMessageSendingError = mockk<FormatMessageSendingError>()
    private val reEncryptAttachments = mockk<ReEncryptAttachments>()
    private val appInBackgroundStateFlow = MutableStateFlow(false)
    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { observe() } returns appInBackgroundStateFlow
    }
    private val observeMessagePassword = mockk<ObserveMessagePassword>()
    private val validateSenderAddress = mockk<ValidateSenderAddress>()
    private val saveMessageExpirationTime = mockk<SaveMessageExpirationTime>()
    private val observeMessageExpirationTime = mockk<ObserveMessageExpirationTime>()
    private val getExternalRecipients = mockk<GetExternalRecipients>()
    private val convertHtmlToPlainText = mockk<ConvertHtmlToPlainText>()

    private val getInitials = mockk<GetInitials> {
        every { this@mockk(any()) } returns BaseInitials
    }
    private val attachmentUiModelMapper = AttachmentUiModelMapper()
    private val sortContactsForSuggestions = SortContactsForSuggestions(getInitials, testDispatcher)
    private val shouldRestrictWebViewHeight = mockk<ShouldRestrictWebViewHeight> {
        every { this@mockk.invoke(null) } returns false
    }
    private val reducer = ComposerReducer(attachmentUiModelMapper, shouldRestrictWebViewHeight)

    private val viewModel by lazy {
        ComposerViewModel(
            appInBackgroundState,
            storeAttachments,
            storeDraftWithBodyMock,
            storeDraftWithSubjectMock,
            storeDraftWithAllFields,
            storeDraftWithRecipientsMock,
            storeExternalAttachmentStates,
            getContactsMock,
            searchContactsMock,
            searchDeviceContactsMock,
            deviceContactsSuggestionsPromptMock,
            searchContactGroupsMock,
            sortContactsForSuggestions,
            participantMapperMock,
            reducer,
            isValidEmailAddressMock,
            getPrimaryAddressMock,
            getComposerSenderAddresses,
            composerIdlingResource,
            draftUploaderMock,
            observeMessageAttachments,
            observeMessageSendingError,
            clearMessageSendingError,
            formatMessageSendingError,
            sendMessageMock,
            networkManagerMock,
            getLocalMessageDecrypted,
            injectAddressSignature,
            parentMessageToDraftFields,
            styleQuotedHtml,
            storeDraftWithParentAttachments,
            deleteAttachment,
            deleteAllAttachments,
            reEncryptAttachments,
            observeMessagePassword,
            validateSenderAddress,
            saveMessageExpirationTime,
            observeMessageExpirationTime,
            getExternalRecipients,
            convertHtmlToPlainText,
            isDeviceContactsSuggestionsEnabledMock,
            getDecryptedDraftFields,
            savedStateHandle,
            observePrimaryUserIdMock,
            provideNewDraftIdMock,
            testDispatcher
        )
    }

    @Test
    fun `should store attachments when attachments are added to the draft`() {
        // Given
        val uri = mockk<Uri>()
        val primaryAddress = UserAddressSample.PrimaryAddress
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val messageId = MessageIdSample.Invoice
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            expectedSubject,
            expectedDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc,
            null
        )
        val decryptedDraftFields = DecryptedDraftFields.Remote(expectedFields)
        expectedPrimaryAddress(expectedUserId) { primaryAddress }
        expectInputDraftMessageId { messageId }
        expectStoreAllDraftFieldsSucceeds(expectedUserId, messageId, expectedFields)
        expectStoreAttachmentsSucceeds(expectedUserId, messageId, expectedSenderEmail, listOf(uri))
        expectDecryptedDraftDataSuccess(expectedUserId, messageId) { decryptedDraftFields }
        expectStartDraftSync(expectedUserId, messageId)
        expectObservedMessageAttachments(expectedUserId, messageId)
        expectNoInputDraftAction()
        expectStoreParentAttachmentSucceeds(expectedUserId, messageId)
        expectObserveMessageSendingError(expectedUserId, messageId)
        expectMessagePassword(expectedUserId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, messageId)

        // When
        viewModel.submit(ComposerAction.AttachmentsAdded(listOf(uri)))

        // Then
        coVerify { storeAttachments(expectedUserId, messageId, expectedSenderEmail, listOf(uri)) }
    }

    @Test
    fun `should store the draft body when the body changes`() {
        // Given
        val primaryAddress = UserAddressSample.PrimaryAddress
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedQuotedDraftBody = null
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val action = ComposerAction.DraftBodyChanged(expectedDraftBody)
        expectedPrimaryAddress(expectedUserId) { primaryAddress }
        expectStoreDraftBodySucceeds(
            expectedMessageId,
            expectedDraftBody,
            expectedQuotedDraftBody,
            expectedSenderEmail,
            expectedUserId
        )
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithBodyMock(
                expectedUserId,
                expectedMessageId,
                expectedDraftBody,
                expectedQuotedDraftBody,
                expectedSenderEmail
            )
        }
    }

    @Test
    fun `should emit Effect for ReplaceDraftBody when sender changes`() = runTest {
        // Given
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedQuotedDraftBody = null
        val originalSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(
            expectedMessageId,
            expectedDraftBody,
            expectedQuotedDraftBody,
            expectedSenderEmail,
            expectedUserId
        )
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectReEncryptAttachmentSucceeds(expectedUserId, expectedMessageId, originalSenderEmail, expectedSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), originalSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // Change internal state of the View Model to simulate an existing draft body before changing sender
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            draftBody = expectedDraftBody,
            quotedBody = expectedQuotedDraftBody
        )

        val expectedReplaceDraftBodyTextUiModel = TextUiModel(expectDraftBodyWithSignature().value)

        // When
        viewModel.submit(action)

        // Then
        assertEquals(
            expectedReplaceDraftBodyTextUiModel,
            viewModel.state.value.replaceDraftBody.consume()
        )
    }

    @Test
    fun `should store draft with sender and current draft body when sender changes`() = runTest {
        // Given
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedQuotedDraftBody = null
        val previousSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(
            expectedMessageId,
            expectedDraftBody,
            expectedQuotedDraftBody,
            expectedSenderEmail,
            expectedUserId
        )
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectReEncryptAttachmentSucceeds(expectedUserId, expectedMessageId, previousSenderEmail, expectedSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), previousSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // Change internal state of the View Model to simulate an existing draft body before changing sender
        expectedViewModelInitialState(messageId = expectedMessageId, draftBody = expectedDraftBody)

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithBodyMock(
                expectedUserId,
                expectedMessageId,
                expectedDraftBody,
                expectedQuotedDraftBody,
                expectedSenderEmail
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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
    fun `should perform search when ContactSuggestionTermChanged`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSearchTerm = "proton"
        val suggestionField = ContactSuggestionsField.BCC
        val expectedContacts = listOf(ContactSample.Doe, ContactSample.John)
        val expectedDeviceContacts = emptyList<DeviceContact>()
        val expectedContactGroups = emptyList<ContactGroup>()
        val action = ComposerAction.ContactSuggestionTermChanged(expectedSearchTerm, suggestionField)

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectSearchContacts(expectedUserId, expectedSearchTerm, expectedContacts)
        expectSearchDeviceContacts(expectedSearchTerm, expectedDeviceContacts)
        expectSearchContactGroups(expectedUserId, expectedSearchTerm, expectedContactGroups)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        viewModel.submit(action)

        // Then
        verify {
            searchContactsMock(expectedUserId, expectedSearchTerm)
            searchContactGroupsMock(expectedUserId, expectedSearchTerm)
        }
    }

    @Test
    fun `should emit ContactSuggestionsDismissed when searchTerm is blank`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSearchTerm = ""
        val suggestionField = ContactSuggestionsField.BCC

        val expectedContacts = emptyList<Contact>()

        val expectedDeviceContacts = emptyList<DeviceContact>()

        val expectedContactGroups = emptyList<ContactGroup>()
        val action = ComposerAction.ContactSuggestionTermChanged(expectedSearchTerm, suggestionField)

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectSearchContacts(expectedUserId, expectedSearchTerm, expectedContacts)
        expectSearchDeviceContacts(expectedSearchTerm, expectedDeviceContacts)
        expectSearchContactGroups(expectedUserId, expectedSearchTerm, expectedContactGroups)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectIsDeviceContactsSuggestionsEnabled(true)

        // When
        viewModel.submit(action)
        val actual = viewModel.state.value

        // Then
        assertEquals(
            emptyMap(),
            actual.contactSuggestions
        )
        assertEquals(mapOf(ContactSuggestionsField.BCC to false), actual.areContactSuggestionsExpanded)
    }

    @Test
    fun `should call DeviceContactsSuggestionsPrompt when DeviceContactsPromptDenied is emitted`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSearchTerm = ""

        val expectedContacts = emptyList<Contact>()

        val expectedDeviceContacts = emptyList<DeviceContact>()

        val expectedContactGroups = emptyList<ContactGroup>()
        val action = ComposerAction.DeviceContactsPromptDenied

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectSearchContacts(expectedUserId, expectedSearchTerm, expectedContacts)
        expectSearchDeviceContacts(expectedSearchTerm, expectedDeviceContacts)
        expectSearchContactGroups(expectedUserId, expectedSearchTerm, expectedContactGroups)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectIsDeviceContactsSuggestionsEnabled(true)

        // When
        viewModel.submit(action)

        // Then
        viewModel.state.test {
            awaitItem()

            coVerify { deviceContactsSuggestionsPromptMock.setPromptDisabled() }
        }
    }

    @Test
    fun `should emit UpdateContactSuggestions when contact suggestions are found`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSearchTerm = "contact"
        val suggestionField = ContactSuggestionsField.BCC

        val expectedContacts = listOf(
            ContactSample.Doe.copy(
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "doe contact",
                        address = "address1@proton.ch"
                    )
                )
            ),
            ContactSample.John.copy(
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "john contact",
                        address = "address2@proton.ch"
                    )
                )
            )
        )

        val expectedDeviceContacts = emptyList<DeviceContact>()

        val expectedContactGroups = listOf(
            ContactGroup(
                UserIdSample.Primary,
                LabelIdSample.LabelCoworkers,
                "Coworkers contact group",
                "#AABBCC",
                listOf(ContactEmailSample.contactEmail1)
            )
        )
        val action = ComposerAction.ContactSuggestionTermChanged(expectedSearchTerm, suggestionField)

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectSearchContacts(expectedUserId, expectedSearchTerm, expectedContacts)
        expectSearchDeviceContacts(expectedSearchTerm, expectedDeviceContacts)
        expectSearchContactGroups(expectedUserId, expectedSearchTerm, expectedContactGroups)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectIsDeviceContactsSuggestionsEnabled(true)

        // When
        viewModel.submit(action)
        val actual = viewModel.state.value

        // Then
        assertEquals(
            mapOf(
                ContactSuggestionsField.BCC to listOf(
                    ContactSuggestionUiModel.Contact(
                        name = expectedContacts[0].contactEmails.first().name,
                        initial = BaseInitials,
                        email = expectedContacts[0].contactEmails.first().email
                    ),
                    ContactSuggestionUiModel.Contact(
                        name = expectedContacts[1].contactEmails.first().name,
                        initial = BaseInitials,
                        email = expectedContacts[1].contactEmails.first().email
                    ),
                    ContactSuggestionUiModel.ContactGroup(
                        expectedContactGroups[0].name,
                        expectedContactGroups[0].members.map { it.email },
                        expectedContactGroups[0].color
                    )
                )
            ),
            actual.contactSuggestions
        )
        assertEquals(mapOf(ContactSuggestionsField.BCC to true), actual.areContactSuggestionsExpanded)
    }

    @Test
    fun `should emit UpdateContactSuggestions when device contact suggestions are found`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSearchTerm = "contact"
        val suggestionField = ContactSuggestionsField.BCC

        val expectedContacts = emptyList<Contact>()
        val expectedContactGroups = emptyList<ContactGroup>()
        val expectedDeviceContacts = listOf(
            DeviceContact(
                "device contact 1 name",
                "device contact 1 email"
            ),
            DeviceContact(
                "device contact 2 name",
                "device contact 2 email"
            )
        )
        val action = ComposerAction.ContactSuggestionTermChanged(expectedSearchTerm, suggestionField)

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectSearchContacts(expectedUserId, expectedSearchTerm, expectedContacts)
        expectSearchDeviceContacts(expectedSearchTerm, expectedDeviceContacts)
        expectSearchContactGroups(expectedUserId, expectedSearchTerm, expectedContactGroups)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectIsDeviceContactsSuggestionsEnabled(true)

        // When
        viewModel.submit(action)
        val actual = viewModel.state.value

        // Then
        assertEquals(
            mapOf(
                ContactSuggestionsField.BCC to listOf(
                    ContactSuggestionUiModel.Contact(
                        name = expectedDeviceContacts[0].name,
                        initial = BaseInitials,
                        email = expectedDeviceContacts[0].email
                    ),
                    ContactSuggestionUiModel.Contact(
                        name = expectedDeviceContacts[1].name,
                        initial = BaseInitials,
                        email = expectedDeviceContacts[1].email
                    )
                )
            ),
            actual.contactSuggestions
        )
        assertEquals(mapOf(ContactSuggestionsField.BCC to true), actual.areContactSuggestionsExpanded)
    }

    @Test
    fun `should emit UpdateContactSuggestions limiting results according to constant max value`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSearchTerm = "contact"
        val suggestionField = ContactSuggestionsField.BCC

        val expectedContactsExceedingLimit = (1..ComposerViewModel.Companion.maxContactAutocompletionCount + 1).map {
            ContactSample.John.copy(
                contactEmails = listOf(
                    ContactTestData.buildContactEmailWith(
                        name = "contact $it",
                        address = "address$it@proton.ch"
                    )
                )
            )
        }
        val expectedDeviceContacts = emptyList<DeviceContact>()
        val expectedContactGroups = emptyList<ContactGroup>()
        val action = ComposerAction.ContactSuggestionTermChanged(expectedSearchTerm, suggestionField)

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectSearchContacts(expectedUserId, expectedSearchTerm, expectedContactsExceedingLimit)
        expectSearchDeviceContacts(expectedSearchTerm, expectedDeviceContacts)
        expectSearchContactGroups(expectedUserId, expectedSearchTerm, expectedContactGroups)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        viewModel.submit(action)
        advanceUntilIdle()
        val actual = viewModel.state.value

        // Then
        assertEquals(
            ComposerViewModel.Companion.maxContactAutocompletionCount,
            actual.contactSuggestions[ContactSuggestionsField.BCC]!!.size
        )
    }

    @Test
    fun `should dismiss contact suggestions when ContactSuggestionsDismissed is emitted`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val suggestionField = ContactSuggestionsField.BCC

        val action = ComposerAction.ContactSuggestionsDismissed(suggestionField)

        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        viewModel.submit(action)
        val actual = viewModel.state.value

        // Then
        assertEquals(mapOf(ContactSuggestionsField.BCC to false), actual.areContactSuggestionsExpanded)
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
            recipientsBcc,
            null
        )
        mockParticipantMapper()
        expectStoreAllDraftFieldsSucceeds(expectedUserId, expectedMessageId, expectedFields)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectUploadDraftSucceeds(expectedUserId, expectedMessageId)
        expectStopContinuousDraftUploadSucceeds()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // Change internal state of the View Model to simulate the existence of all fields before closing the composer
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            senderEmail = expectedSenderEmail,
            subject = expectedSubject,
            draftBody = expectedDraftBody,
            recipients = Triple(recipientsTo, recipientsCc, recipientsBcc)
        )

        // When
        viewModel.submit(ComposerAction.OnCloseComposer)

        // Then
        coVerifyOrder {
            draftUploaderMock.stopContinuousUpload()
            storeDraftWithAllFields(expectedUserId, expectedMessageId, expectedFields)
            draftUploaderMock.upload(expectedUserId, expectedMessageId)
        }
        assertEquals(Effect.of(Unit), viewModel.state.value.closeComposerWithDraftSaved)
    }

    @Test
    fun `should store all draft fields and send message when send button is clicked`() = runTest {
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
            recipientsBcc,
            null
        )
        mockParticipantMapper()
        expectNetworkManagerIsConnected()
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectSendMessageSucceds(expectedUserId, expectedMessageId, expectedFields)
        expectStopContinuousDraftUploadSucceeds()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // Change internal state of the View Model to simulate the existence of all fields before closing the composer
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            senderEmail = expectedSenderEmail,
            subject = expectedSubject,
            draftBody = expectedDraftBody,
            recipients = Triple(recipientsTo, recipientsCc, recipientsBcc)
        )

        // When
        viewModel.submit(ComposerAction.OnSendMessage)

        // Then
        coVerifyOrder {
            draftUploaderMock.stopContinuousUpload()
            sendMessageMock(expectedUserId, expectedMessageId, expectedFields)
        }
        assertEquals(Effect.of(Unit), viewModel.state.value.closeComposerWithMessageSending)
    }

    @Test
    fun `should store all draft fields and send message in offline when send button is clicked`() = runTest {
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
            recipientsBcc,
            null
        )
        mockParticipantMapper()
        expectNetworkManagerIsDisconnected()
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectSendMessageSucceds(expectedUserId, expectedMessageId, expectedFields)
        expectStopContinuousDraftUploadSucceeds()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // Change internal state of the View Model to simulate the existence of all fields before closing the composer
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            senderEmail = expectedSenderEmail,
            subject = expectedSubject,
            draftBody = expectedDraftBody,
            recipients = Triple(recipientsTo, recipientsCc, recipientsBcc)
        )

        // When
        viewModel.submit(ComposerAction.OnSendMessage)

        // Then
        coVerifyOrder {
            draftUploaderMock.stopContinuousUpload()
            sendMessageMock(expectedUserId, expectedMessageId, expectedFields)
        }
        assertEquals(Effect.of(Unit), viewModel.state.value.closeComposerWithMessageSendingOffline)
    }

    @Test
    fun `should not store draft when all fields are empty and composer is closed`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectContacts()

        // When
        viewModel.submit(ComposerAction.OnCloseComposer)

        // Then
        coVerify { storeDraftWithAllFields wasNot Called }
        coVerify(exactly = 0) { draftUploaderMock.upload(any(), any()) }
        assertEquals(Effect.of(Unit), viewModel.state.value.closeComposer)
    }

    @Test
    fun `should not store draft when body contains only signature and composer is closed`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        val expectedDraftBody = expectDraftBodyWithSignature()
        expectInjectAddressSignature(expectedUserId, expectedDraftBody, expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectContacts()

        // Change internal state of the View Model to simulate an existing draft body before closing composer
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            draftBody = expectedDraftBody
        )

        // When
        viewModel.submit(ComposerAction.OnCloseComposer)

        // Then
        coVerify { storeDraftWithAllFields wasNot Called }
        coVerify(exactly = 0) { draftUploaderMock.upload(any(), any()) }
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
                recipientsBcc,
                null
            )
            mockParticipantMapper()
            expectStoreAllDraftFieldsSucceeds(expectedUserId, expectedMessageId, expectedFields)
            expectNoInputDraftMessageId()
            expectNoInputDraftAction()
            expectStopContinuousDraftUploadSucceeds()
            expectUploadDraftSucceeds(expectedUserId, expectedMessageId)
            expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
            expectObservedMessageAttachments(expectedUserId, expectedMessageId)
            expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
            expectObserveMessageSendingError(expectedUserId, expectedMessageId)
            expectMessagePassword(expectedUserId, expectedMessageId)
            expectNoFileShareVia()
            expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
                draftUploaderMock.stopContinuousUpload()
                storeDraftWithAllFields(expectedUserId, expectedMessageId, expectedFields)
                draftUploaderMock.upload(expectedUserId, expectedMessageId)
            }
        }

    @Test
    fun `emits state with primary sender address when available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val primaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(SenderUiModel(primaryAddress.email), actual.fields.sender)
    }

    @Test
    fun `emits state with sender address error when not available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddressError(expectedUserId) { DataError.Local.NoDataCached }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedGetComposerSenderAddresses { addresses }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedGetComposerSenderAddressesError { GetComposerSenderAddresses.Error.UpgradeToChangeSender }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedGetComposerSenderAddressesError { GetComposerSenderAddresses.Error.FailedDeterminingUserSubscription }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        val originalSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(expectedMessageId, expectedDraftBody, null, expectedSenderEmail, expectedUserId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectReEncryptAttachmentSucceeds(expectedUserId, expectedMessageId, originalSenderEmail, expectedSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), originalSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(SenderUiModel(expectedSenderEmail.value), currentState.fields.sender)
        coVerify(exactly = 1) {
            reEncryptAttachments(expectedUserId, expectedMessageId, originalSenderEmail, expectedSenderEmail)
        }
    }

    @Test
    fun `emits all attachment deleted when re-encryption of attachment failed`() = runTest {
        // Given
        val expectedDraftBody = DraftBody("")
        val previousSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(expectedMessageId, expectedDraftBody, null, expectedSenderEmail, expectedUserId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectReEncryptAttachmentFails(expectedUserId, expectedMessageId, previousSenderEmail, expectedSenderEmail)
        expectDeleteAllAttachmentsSucceeds(expectedUserId, previousSenderEmail, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), previousSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(Effect.of(Unit), currentState.attachmentsReEncryptionFailed)
    }

    @Test
    fun `emits state with saving draft with new sender error when save draft with sender returns error`() = runTest {
        // Given
        val expectedDraftBody = DraftBody("")
        val originalSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail.value))
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodyFails(expectedMessageId, expectedDraftBody, null, expectedSenderEmail, expectedUserId) {
            StoreDraftWithBodyError.DraftSaveError
        }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), originalSenderEmail)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectStoreDraftBodyFails(expectedMessageId, expectedDraftBody, null, expectedSenderEmail, expectedUserId) {
            StoreDraftWithBodyError.DraftSaveError
        }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

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
        val decryptedDraftFields = DecryptedDraftFields.Remote(existingDraftFields)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        // Simulate a small delay in getDecryptedDraftFields to ensure the "loading" state was emitted
        expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId, 100) { decryptedDraftFields }
        expectStartDraftSync(UserIdSample.Primary, MessageIdSample.RemoteDraft)
        expectObservedMessageAttachments(expectedUserId, expectedDraftId)
        expectNoInputDraftAction()
        expectStoreParentAttachmentSucceeds(expectedUserId, expectedDraftId)
        expectObserveMessageSendingError(expectedUserId, expectedDraftId)
        expectMessagePassword(expectedUserId, expectedDraftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

        // When
        val actual = viewModel.state.value

        // Then
        assertTrue(actual.isLoading)
        coVerify { getDecryptedDraftFields(expectedUserId, expectedDraftId) }
    }

    @Test
    fun `emits state with remote draft fields to be prefilled when getting decrypted draft fields succeeds`() =
        runTest {
            // Given
            val expectedUserId = expectedUserId { UserIdSample.Primary }
            val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
            val expectedDraftFields = existingDraftFields
            val decryptedDraftFields = DecryptedDraftFields.Remote(existingDraftFields)
            expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
            expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId) { decryptedDraftFields }
            expectStartDraftSync(expectedUserId, expectedDraftId)
            expectObservedMessageAttachments(expectedUserId, expectedDraftId)
            expectStoreParentAttachmentSucceeds(expectedUserId, expectedDraftId)
            expectNoInputDraftAction()
            expectObserveMessageSendingError(expectedUserId, expectedDraftId)
            expectMessagePassword(expectedUserId, expectedDraftId)
            expectNoFileShareVia()
            expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

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
                expectedDraftFields.body.value,
                null
            )
            assertEquals(expectedComposerFields, actual.fields)
            coVerify { storeExternalAttachmentStates(expectedUserId, expectedDraftId) }
        }

    @Test
    fun `emits state with local draft fields to be prefilled when getting decrypted draft fields succeeds`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val expectedDraftFields = existingDraftFields
        val decryptedDraftFields = DecryptedDraftFields.Local(existingDraftFields)
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId) { decryptedDraftFields }
        expectStartDraftSync(expectedUserId, expectedDraftId)
        expectObservedMessageAttachments(expectedUserId, expectedDraftId)
        expectStoreParentAttachmentSucceeds(expectedUserId, expectedDraftId)
        expectNoInputDraftAction()
        expectObserveMessageSendingError(expectedUserId, expectedDraftId)
        expectMessagePassword(expectedUserId, expectedDraftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

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
            expectedDraftFields.body.value,
            null
        )
        assertEquals(expectedComposerFields, actual.fields)
        coVerify { storeExternalAttachmentStates(expectedUserId, expectedDraftId) }
        expectStoreDraftSubjectSucceeds(
            expectedDraftId, expectedSenderEmail,
            expectedUserId, expectedDraftFields.subject
        )
    }

    @Test
    fun `emits state with composer fields to be prefilled when getting parent message draft fields succeeds`() =
        runTest {
            // Given
            val expectedUserId = expectedUserId { UserIdSample.Primary }
            val expectedDraftId = expectedMessageId { MessageIdSample.EmptyDraft }
            val expectedParentId = MessageIdSample.Invoice
            val expectedAction = expectInputDraftAction { DraftAction.Reply(expectedParentId) }
            val expectedDecryptedParentBody = DecryptedMessageBodyTestData.htmlInvoice
            expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
            expectStartDraftSync(expectedUserId, expectedDraftId, expectedAction)
            expectNoInputDraftMessageId()
            val expectedMessageDecrypted = expectGetMessageWithDecryptedBodySuccess(expectedUserId, expectedParentId) {
                MessageWithDecryptedBody(MessageWithBodySample.Invoice, expectedDecryptedParentBody)
            }
            val expectedDraftFields = expectParentMessageToDraftFieldsSuccess(
                expectedUserId, expectedMessageDecrypted, expectedAction
            ) { draftFieldsWithQuotedBody }
            expectObservedMessageAttachments(expectedUserId, expectedDraftId)
            val expectedStyledQuote = expectStyleQuotedHtml(expectedDraftFields.originalHtmlQuote) {
                StyledHtmlQuote("<styled> ${expectedDraftFields.originalHtmlQuote?.value} </styled>")
            }
            expectStoreDraftWithParentAttachmentsSucceeds(
                expectedUserId,
                expectedDraftId,
                expectedMessageDecrypted,
                expectedDraftFields.sender,
                expectedAction
            )
            expectObserveMessageSendingError(expectedUserId, expectedDraftId)
            expectMessagePassword(expectedUserId, expectedDraftId)
            expectNoFileShareVia()
            expectValidSenderAddress(expectedUserId, expectedDraftFields.sender)
            expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

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
                expectedDraftFields.body.value,
                QuotedHtmlContent(expectedDraftFields.originalHtmlQuote!!, expectedStyledQuote)
            )
            assertEquals(expectedComposerFields, actual.fields)
        }

    @Test
    fun `emits state with valid sender and notice effect when parent draft sender is invalid`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedParentId = MessageIdSample.Invoice
        val expectedAction = expectInputDraftAction { DraftAction.Reply(expectedParentId) }
        val expectedDecryptedParentBody = DecryptedMessageBodyTestData.htmlInvoice
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(expectedUserId, expectedDraftId, expectedAction)
        expectNoInputDraftMessageId()
        val expectedMessageDecrypted = expectGetMessageWithDecryptedBodySuccess(expectedUserId, expectedParentId) {
            MessageWithDecryptedBody(MessageWithBodySample.Invoice, expectedDecryptedParentBody)
        }
        val expectedValidEmail = SenderEmail("valid-to-use-instead@proton.me")
        val expectedDraftFields = expectParentMessageToDraftFieldsSuccess(
            expectedUserId, expectedMessageDecrypted, expectedAction
        ) { draftFieldsWithQuotedBody }
        expectObservedMessageAttachments(expectedUserId, expectedDraftId)
        expectStyleQuotedHtml(expectedDraftFields.originalHtmlQuote) {
            StyledHtmlQuote("<styled> ${expectedDraftFields.originalHtmlQuote?.value} </styled>")
        }
        expectStoreDraftWithParentAttachmentsSucceeds(
            expectedUserId,
            expectedDraftId,
            expectedMessageDecrypted,
            expectedValidEmail,
            expectedAction
        )
        expectObserveMessageSendingError(expectedUserId, expectedDraftId)
        expectMessagePassword(expectedUserId, expectedDraftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)
        expectInvalidSenderAddress(
            expectedUserId,
            expectedDraftFields.sender,
            expectedValidEmail,
            ValidateSenderAddress.ValidationError.PaidAddress
        )
        expectReEncryptAttachmentSucceeds(
            expectedUserId,
            expectedDraftId,
            expectedDraftFields.sender,
            expectedValidEmail
        )

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(SenderUiModel(expectedValidEmail.value), actual.fields.sender)
        assertEquals(
            Effect.of(TextUiModel(R.string.composer_sender_changed_pm_address_is_a_paid_feature)),
            actual.senderChangedNotice
        )
        coVerify {
            reEncryptAttachments(expectedUserId, expectedDraftId, expectedDraftFields.sender, expectedValidEmail)
        }
    }

    @Test
    fun `emits state with error loading parent data when getting parent message draft fields fails`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedParentId = MessageIdSample.Invoice
        val expectedAction = expectInputDraftAction { DraftAction.Reply(expectedParentId) }
        val draftId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(expectedUserId, draftId, expectedAction)
        expectNoInputDraftMessageId()
        expectParentDraftDataError(expectedUserId, expectedParentId) { DataError.Local.DecryptionError }
        expectObservedMessageAttachments(expectedUserId, draftId)
        expectObserveMessageSendingError(expectedUserId, draftId)
        expectMessagePassword(expectedUserId, draftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, draftId)

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(TextUiModel(R.string.composer_error_loading_parent_message), actual.error.consume())
    }

    @Test
    fun `emits state with error loading existing draft when getting decrypted draft fields fails`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(expectedUserId, expectedDraftId, DraftAction.Compose)
        expectDecryptedDraftDataError(expectedUserId, expectedDraftId) { DataError.Local.NoDataCached }
        expectObservedMessageAttachments(expectedUserId, expectedDraftId)
        expectNoInputDraftAction()
        expectObserveMessageSendingError(expectedUserId, expectedDraftId)
        expectMessagePassword(expectedUserId, expectedDraftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

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
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(userId, messageId)

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(messageId, actual.fields.draftId)
        coVerify { draftUploaderMock.startContinuousUpload(userId, messageId, DraftAction.Compose, any()) }
    }

    @Test
    fun `emits state with an effect to open the file picker when add attachments action is submitted`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val decryptedDraftFields = DecryptedDraftFields.Remote(existingDraftFields)
        expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId) { decryptedDraftFields }
        expectStartDraftSync(expectedUserId, expectedDraftId)
        expectNoInputDraftAction()
        expectObservedMessageAttachments(expectedUserId, expectedDraftId)
        expectStoreParentAttachmentSucceeds(expectedUserId, expectedDraftId)
        expectObserveMessageSendingError(expectedUserId, expectedDraftId)
        expectMessagePassword(expectedUserId, expectedDraftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

        // When
        viewModel.submit(ComposerAction.OnAddAttachments)

        // Then
        val actual = viewModel.state.value
        assertEquals(Unit, actual.openImagePicker.consume())
    }

    @Test
    fun `emits state with updated attachments when the attachments change`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.Invoice }
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            expectedSubject,
            expectedDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc,
            null
        )
        val decryptedDraftFields = DecryptedDraftFields.Remote(expectedFields)
        expectNoInputDraftAction()
        expectStoreAllDraftFieldsSucceeds(expectedUserId, expectedDraftId, expectedFields)
        expectDecryptedDraftDataSuccess(expectedUserId, expectedDraftId) { decryptedDraftFields }
        expectStartDraftSync(expectedUserId, expectedDraftId)
        expectObservedMessageAttachments(expectedUserId, expectedDraftId)
        expectStoreParentAttachmentSucceeds(expectedUserId, expectedDraftId)
        expectObserveMessageSendingError(expectedUserId, expectedDraftId)
        expectMessagePassword(expectedUserId, expectedDraftId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedDraftId)

        // When
        viewModel.state.test {

            // Then
            val expected = AttachmentGroupUiModel(
                limit = NO_ATTACHMENT_LIMIT,
                attachments = listOf(AttachmentUiModelSample.deletableInvoice)
            )
            val actual = awaitItem().attachments
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `delete compose action triggers delete attachment use case`() = runTest {
        // Given
        val primaryAddress = UserAddressSample.PrimaryAddress
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val messageId = MessageIdSample.Invoice
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedAttachmentId = AttachmentId("attachment_id")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            expectedSubject,
            expectedDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc,
            null
        )
        val decryptedDraftFields = DecryptedDraftFields.Remote(expectedFields)
        expectedPrimaryAddress(expectedUserId) { primaryAddress }
        expectInputDraftMessageId { messageId }
        expectStoreAllDraftFieldsSucceeds(expectedUserId, messageId, expectedFields)
        expectDecryptedDraftDataSuccess(expectedUserId, messageId) { decryptedDraftFields }
        expectStartDraftSync(expectedUserId, messageId)
        expectObservedMessageAttachments(expectedUserId, messageId)
        expectNoInputDraftAction()
        expectAttachmentDeleteSucceeds(expectedUserId, expectedSenderEmail, messageId, expectedAttachmentId)
        expectStoreParentAttachmentSucceeds(expectedUserId, messageId)
        expectObserveMessageSendingError(expectedUserId, messageId)
        expectMessagePassword(expectedUserId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, messageId)

        // When
        viewModel.submit(ComposerAction.RemoveAttachment(expectedAttachmentId))

        // Then
        coVerify { deleteAttachment(expectedUserId, expectedSenderEmail, messageId, expectedAttachmentId) }
    }

    @Test
    fun `emit state with effect when attachment file size exceeded`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val primaryAddress = UserAddressSample.PrimaryAddress
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val messageId = MessageIdSample.Invoice
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftBody = DraftBody("I am plaintext")
        val recipientsTo = RecipientsTo(listOf(RecipientSample.John))
        val recipientsCc = RecipientsCc(listOf(RecipientSample.John))
        val recipientsBcc = RecipientsBcc(listOf(RecipientSample.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            expectedSubject,
            expectedDraftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc,
            null
        )
        val decryptedDraftFields = DecryptedDraftFields.Remote(expectedFields)
        expectedPrimaryAddress(expectedUserId) { primaryAddress }
        expectInputDraftMessageId { messageId }
        expectStoreAllDraftFieldsSucceeds(expectedUserId, messageId, expectedFields)
        expectStoreAttachmentsFailed(
            expectedUserId = expectedUserId,
            expectedMessageId = messageId,
            expectedSenderEmail = expectedSenderEmail,
            expectedUriList = listOf(uri),
            storeAttachmentError = StoreDraftWithAttachmentError.FileSizeExceedsLimit
        )
        expectDecryptedDraftDataSuccess(expectedUserId, messageId) { decryptedDraftFields }
        expectStartDraftSync(expectedUserId, messageId)
        expectObservedMessageAttachments(expectedUserId, messageId)
        expectNoInputDraftAction()
        expectStoreParentAttachmentSucceeds(expectedUserId, messageId)
        expectObserveMessageSendingError(expectedUserId, messageId)
        expectMessagePassword(expectedUserId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, messageId)

        // When
        viewModel.submit(ComposerAction.AttachmentsAdded(listOf(uri)))

        // Then
        viewModel.state.test {
            val expected = Effect.of(Unit)
            val actual = awaitItem().attachmentsFileSizeExceeded
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `stop syncing draft for current messageId when app is put in background`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectStopContinuousDraftUploadSucceeds()
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(userId, messageId)

        // When
        viewModel.state // app is in foreground
        appInBackgroundStateFlow.emit(true) // app is in background

        // Then
        coVerifyOrder {
            draftUploaderMock.startContinuousUpload(userId, messageId, DraftAction.Compose, any())
            draftUploaderMock.stopContinuousUpload()
        }
    }

    @Test
    fun `start syncing draft for current messageId when app is put back in foreground`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectStopContinuousDraftUploadSucceeds()
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(userId, messageId)

        // When
        viewModel.state // app is in foreground
        appInBackgroundStateFlow.emit(true) // app is in background
        appInBackgroundStateFlow.emit(false) // app is in foreground again

        // Then
        coVerifyOrder {
            draftUploaderMock.startContinuousUpload(userId, messageId, DraftAction.Compose, any())
            draftUploaderMock.stopContinuousUpload()
            draftUploaderMock.startContinuousUpload(userId, messageId, DraftAction.Compose, any())
        }
    }

    @Test
    fun `should update state with message password info when message password changes`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(userId, messageId)

        // When
        viewModel.state.test {
            // Then
            assertTrue(awaitItem().isMessagePasswordSet)
        }
    }

    @Test
    fun `should set recipient to state when recipient was given as an input`() = runTest {
        // Given
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedRecipient = RecipientSample.John
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)

        expectNoInputDraftAction()
        expectNoInputDraftMessageId()

        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectedMessageId { expectedMessageId }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectContacts()
        mockParticipantMapper()
        expectInputDraftAction { DraftAction.ComposeToAddresses(listOf(expectedRecipient.address)) }
        expectStoreDraftRecipientsSucceeds(
            expectedMessageId,
            expectedSenderEmail,
            expectedUserId,
            listOf(expectedRecipient)
        )
        expectStartDraftSync(expectedUserId, expectedMessageId)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectAddressValidation(expectedRecipient.address, true)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)

        assertEquals(viewModel.state.value.fields.to.first(), RecipientUiModel.Valid(expectedRecipient.address))
    }

    @Test
    fun `should emit state for showing bottom sheet when action for setting expiration time is submitted`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(userId, messageId)

        // When
        viewModel.submit(ComposerAction.OnSetExpirationTimeRequested)

        // Then
        viewModel.state.test {
            assertEquals(Effect.of(true), awaitItem().changeBottomSheetVisibility)
        }
    }

    @Test
    fun `should emit state for hiding bottom sheet when action for saving expiration time is submitted`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expirationTime = 1.days
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectSaveExpirationTimeForDraft(userId, messageId, expectedSenderEmail, expirationTime)
        expectObserveMessageExpirationTime(userId, messageId)

        // When
        viewModel.submit(ComposerAction.ExpirationTimeSet(duration = expirationTime))

        // Then
        viewModel.state.test {
            coVerify { saveMessageExpirationTime(userId, messageId, expectedSenderEmail, expirationTime) }
            assertEquals(Effect.of(false), awaitItem().changeBottomSheetVisibility)
        }
    }

    @Test
    fun `should emit state for showing an error when saving expiration time has failed`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expirationTime = 1.days
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(userId, messageId)
        coEvery {
            saveMessageExpirationTime(userId, messageId, expectedSenderEmail, 1.days)
        } returns DataError.Local.DbWriteFailed.left()

        // When
        viewModel.submit(ComposerAction.ExpirationTimeSet(duration = expirationTime))

        // Then
        viewModel.state.test {
            coVerify { saveMessageExpirationTime(userId, messageId, expectedSenderEmail, expirationTime) }
            assertEquals(Effect.of(TextUiModel(R.string.composer_error_setting_expiration_time)), awaitItem().error)
        }
    }

    @Test
    fun `should emit state with message expiration time when the expiration time has changed`() = runTest {
        // Given
        val userId = expectedUserId { UserIdSample.Primary }
        val messageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        expectedPrimaryAddress(userId) { UserAddressSample.PrimaryAddress }
        expectStartDraftSync(userId, messageId)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments(userId, messageId)
        expectInjectAddressSignature(userId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(userId, messageId)
        expectMessagePassword(userId, messageId)
        expectNoFileShareVia()
        val messageExpirationTime = expectObserveMessageExpirationTime(userId, messageId)

        // Then
        viewModel.state.test {
            assertEquals(messageExpirationTime.expiresIn, awaitItem().messageExpiresIn)
        }
    }

    @Test
    fun `should emit event to confirm sending expiring message when there are external recipients and no password`() =
        runTest {
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
                recipientsBcc,
                null
            )
            mockParticipantMapper()
            expectNetworkManagerIsDisconnected()
            expectNoInputDraftMessageId()
            expectNoInputDraftAction()
            expectSendMessageSucceds(expectedUserId, expectedMessageId, expectedFields)
            expectStopContinuousDraftUploadSucceeds()
            expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
            expectObservedMessageAttachments(expectedUserId, expectedMessageId)
            expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
            expectObserveMessageSendingError(expectedUserId, expectedMessageId)
            expectNoMessagePassword(expectedUserId, expectedMessageId)
            expectNoFileShareVia()
            expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
            val externalRecipients = expectExternalRecipients(expectedUserId, recipientsTo, recipientsCc, recipientsBcc)

            // Change internal state of the View Model to simulate the existence of all fields
            expectedViewModelInitialState(
                messageId = expectedMessageId,
                senderEmail = expectedSenderEmail,
                subject = expectedSubject,
                draftBody = expectedDraftBody,
                recipients = Triple(recipientsTo, recipientsCc, recipientsBcc)
            )

            // When
            viewModel.submit(ComposerAction.OnSendMessage)
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                assertEquals(Effect.of(externalRecipients), awaitItem().confirmSendExpiringMessage)
            }
        }

    @Test
    fun `should send message when sending an expiring message to external recipients was confirmed`() = runTest {
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
            recipientsBcc,
            null
        )
        mockParticipantMapper()
        expectNetworkManagerIsDisconnected()
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectSendMessageSucceds(expectedUserId, expectedMessageId, expectedFields)
        expectStopContinuousDraftUploadSucceeds()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), expectedSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectExternalRecipients(expectedUserId, recipientsTo, recipientsCc, recipientsBcc)

        // Change internal state of the View Model to simulate the existence of all fields before closing the composer
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            senderEmail = expectedSenderEmail,
            subject = expectedSubject,
            draftBody = expectedDraftBody,
            recipients = Triple(recipientsTo, recipientsCc, recipientsBcc)
        )

        // When
        viewModel.submit(ComposerAction.SendExpiringMessageToExternalRecipientsConfirmed)

        // Then
        coVerifyOrder {
            draftUploaderMock.stopContinuousUpload()
            sendMessageMock(expectedUserId, expectedMessageId, expectedFields)
        }
    }

    @Test
    fun `should emit Effect to ReplaceDraftBody when Respond Inline Action`() = runTest {
        // Given
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedQuotedHtmlContent = QuotedHtmlContent(
            OriginalHtmlQuote("<html>quoted body</html>"),
            StyledHtmlQuote("<html>STYLED quoted body</html>")
        )
        val expectedQuotedHtmlInPlainText = "quoted body"
        val originalSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.RespondInlineRequested
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftBodySucceeds(
            expectedMessageId,
            expectedDraftBody,
            expectedQuotedHtmlContent.original,
            originalSenderEmail,
            expectedUserId
        )
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStartDraftSync(expectedUserId, MessageIdSample.EmptyDraft)
        expectObservedMessageAttachments(expectedUserId, expectedMessageId)
        expectInjectAddressSignature(expectedUserId, expectDraftBodyWithSignature(), originalSenderEmail)
        expectObserveMessageSendingError(expectedUserId, expectedMessageId)
        expectMessagePassword(expectedUserId, expectedMessageId)
        expectNoFileShareVia()
        expectObserveMessageExpirationTime(expectedUserId, expectedMessageId)
        expectConvertHtmlToPlainTextSucceeds(expectedQuotedHtmlContent, expectedQuotedHtmlInPlainText)

        // Change internal state of the View Model to simulate an existing draft body before changing sender
        expectedViewModelInitialState(
            messageId = expectedMessageId,
            draftBody = expectedDraftBody,
            quotedBody = expectedQuotedHtmlContent
        )

        val expectedReplaceDraftBodyTextUiModel = TextUiModel(
            "${expectedDraftBody.value}$expectedQuotedHtmlInPlainText"
        )

        // When
        viewModel.submit(action)

        // Then
        assertEquals(expectedReplaceDraftBodyTextUiModel, viewModel.state.value.replaceDraftBody.consume())
        assertNull(viewModel.state.value.fields.quotedBody)
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(ComposerDraftState.Companion)
    }

    private fun expectConvertHtmlToPlainTextSucceeds(
        expectedQuotedHtmlContent: QuotedHtmlContent,
        expectedQuotedHtmlInPlainText: String
    ) {
        every { convertHtmlToPlainText(expectedQuotedHtmlContent.styled.value) } returns expectedQuotedHtmlInPlainText
    }

    private fun expectStyleQuotedHtml(originalHtmlQuote: OriginalHtmlQuote?, styledHtmlQuote: () -> StyledHtmlQuote) =
        styledHtmlQuote().also { coEvery { styleQuotedHtml(originalHtmlQuote!!) } returns it }

    private fun expectParentMessageToDraftFieldsSuccess(
        userId: UserId,
        messageWithDecryptedBody: MessageWithDecryptedBody,
        action: DraftAction,
        draftFields: () -> DraftFields
    ) = draftFields().also {
        coEvery { parentMessageToDraftFields(userId, messageWithDecryptedBody, action) } returns it.right()
    }

    private fun expectParentDraftDataError(
        userId: UserId,
        messageId: MessageId,
        error: () -> DataError.Local
    ) = error().also { coEvery { getLocalMessageDecrypted(userId, messageId) } returns it.left() }

    private fun expectGetMessageWithDecryptedBodySuccess(
        userId: UserId,
        messageId: MessageId,
        responseDelay: Long = 0L,
        result: () -> MessageWithDecryptedBody
    ) = result().also { messageWithDecryptedBody ->
        coEvery { getLocalMessageDecrypted(userId, messageId) } coAnswers {
            delay(responseDelay)
            messageWithDecryptedBody.right()
        }
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
        result: () -> DecryptedDraftFields
    ) = result().also { decryptedDraftFields ->
        coEvery { getDecryptedDraftFields(userId, draftId) } coAnswers {
            delay(responseDelay)
            decryptedDraftFields.right()
        }
    }

    private fun expectInputDraftAction(draftAction: () -> DraftAction) = draftAction().also {
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns it.serialize()
    }

    private fun expectNoInputDraftAction() {
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
    }

    private fun expectNoFileShareVia() {
        every { savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey) } returns null
    }

    private fun expectNoInputDraftMessageId() {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
    }

    private fun expectDraftBodyWithSignature() = DraftBody(
        """
            Email body


            Signature
        """.trimIndent()
    )

    private fun expectInjectAddressSignature(
        expectedUserId: UserId,
        expectedDraftBody: DraftBody,
        senderEmail: SenderEmail
    ) {
        coEvery { injectAddressSignature(expectedUserId, any(), senderEmail, any()) } returns expectedDraftBody.right()
    }

    private fun expectObserveMessageSendingError(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        sendingError: SendingError? = null
    ) {
        coEvery { observeMessageSendingError(expectedUserId, expectedMessageId) } returns if (sendingError != null) {
            flowOf(sendingError)
        } else {
            flowOf()
        }
    }

    private fun expectInputDraftMessageId(draftId: () -> MessageId) = draftId().also {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns it.id
    }

    private fun expectStopContinuousDraftUploadSucceeds() {
        coEvery { draftUploaderMock.stopContinuousUpload() } returns Unit
    }

    private fun expectUploadDraftSucceeds(expectedUserId: UserId, expectedMessageId: MessageId) {
        coEvery { draftUploaderMock.upload(expectedUserId, expectedMessageId) } returns Unit
    }

    private fun expectSendMessageSucceds(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        expectedFields: DraftFields
    ) {
        coEvery { sendMessageMock.invoke(expectedUserId, expectedMessageId, expectedFields) } returns Unit
    }

    private fun expectNetworkManagerIsConnected() {
        every { networkManagerMock.isConnectedToNetwork() } returns true
    }

    private fun expectNetworkManagerIsDisconnected() {
        every { networkManagerMock.isConnectedToNetwork() } returns false
    }

    private fun expectStartDraftSync(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction = DraftAction.Compose
    ) {
        coEvery { draftUploaderMock.startContinuousUpload(userId, messageId, action, any()) } returns Unit
    }

    private fun expectedViewModelInitialState(
        messageId: MessageId,
        senderEmail: SenderEmail = SenderEmail(""),
        subject: Subject = Subject(""),
        draftBody: DraftBody = DraftBody(""),
        quotedBody: QuotedHtmlContent? = null,
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
                draftBody.value,
                quotedBody
            ),
            attachments = AttachmentGroupUiModel(attachments = emptyList()),
            premiumFeatureMessage = Effect.empty(),
            recipientValidationError = Effect.empty(),
            error = Effect.empty(),
            isSubmittable = false,
            senderAddresses = emptyList(),
            changeBottomSheetVisibility = Effect.empty(),
            closeComposer = Effect.empty(),
            closeComposerWithDraftSaved = Effect.empty(),
            isLoading = false,
            closeComposerWithMessageSending = Effect.empty(),
            closeComposerWithMessageSendingOffline = Effect.empty(),
            confirmSendingWithoutSubject = Effect.empty(),
            changeFocusToField = Effect.empty(),
            attachmentsFileSizeExceeded = Effect.empty(),
            attachmentsReEncryptionFailed = Effect.empty(),
            warning = Effect.empty(),
            replaceDraftBody = Effect.empty(),
            isMessagePasswordSet = false,
            messageExpiresIn = Duration.ZERO,
            confirmSendExpiringMessage = Effect.empty(),
            isDeviceContactsSuggestionsEnabled = false,
            isDeviceContactsSuggestionsPromptEnabled = false,
            openImagePicker = Effect.empty(),
            shouldRestrictWebViewHeight = false
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

    private fun expectStoreDraftWithParentAttachmentsSucceeds(
        userId: UserId,
        messageId: MessageId,
        messageWithDecryptedBody: MessageWithDecryptedBody,
        senderEmail: SenderEmail,
        action: DraftAction
    ) {
        coEvery {
            storeDraftWithParentAttachments(userId, messageId, messageWithDecryptedBody, senderEmail, action)
        } returns Unit.right()
    }


    private fun expectStoreDraftBodySucceeds(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedQuotedBody: OriginalHtmlQuote?,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId
    ) {
        coEvery {
            storeDraftWithBodyMock(
                expectedUserId,
                expectedMessageId,
                expectedDraftBody,
                expectedQuotedBody,
                expectedSenderEmail
            )
        } returns Unit.right()
    }

    @SuppressWarnings("LongParameterList")
    private fun expectStoreDraftBodyFails(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedQuotedBody: OriginalHtmlQuote?,
        expectedSenderEmail: SenderEmail,
        expectedUserId: UserId,
        error: () -> StoreDraftWithBodyError
    ) = error().also {
        coEvery {
            storeDraftWithBodyMock(
                expectedUserId,
                expectedMessageId,
                expectedDraftBody,
                expectedQuotedBody,
                expectedSenderEmail
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
        } returns Unit.right()
    }

    private fun expectStoreAttachmentsSucceeds(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUriList: List<Uri>
    ) {
        coEvery {
            storeAttachments(expectedUserId, expectedMessageId, expectedSenderEmail, expectedUriList)
        } returns Unit.right()
    }

    private fun expectStoreAttachmentsFailed(
        expectedUserId: UserId,
        expectedMessageId: MessageId,
        expectedSenderEmail: SenderEmail,
        expectedUriList: List<Uri>,
        storeAttachmentError: StoreDraftWithAttachmentError
    ) {
        coEvery {
            storeAttachments(expectedUserId, expectedMessageId, expectedSenderEmail, expectedUriList)
        } returns storeAttachmentError.left()
    }

    private fun expectContacts(): List<Contact> {
        val expectedContacts = listOf(ContactSample.Doe, ContactSample.John)
        coEvery { getContactsMock.invoke(UserIdSample.Primary) } returns expectedContacts.right()
        return expectedContacts
    }

    private fun expectSearchContacts(
        expectedUserId: UserId,
        expectedSearchTerm: String,
        expectedContacts: List<Contact>
    ): List<Contact> {
        coEvery {
            searchContactsMock.invoke(expectedUserId, expectedSearchTerm)
        } returns flowOf(expectedContacts.right())
        return expectedContacts
    }

    private fun expectSearchDeviceContacts(
        expectedSearchTerm: String,
        expectedDeviceContacts: List<DeviceContact>
    ): List<DeviceContact> {
        coEvery {
            searchDeviceContactsMock.invoke(expectedSearchTerm)
        } returns expectedDeviceContacts.right()
        return expectedDeviceContacts
    }

    private fun expectSearchContactGroups(
        expectedUserId: UserId,
        expectedSearchTerm: String,
        expectedContactGroups: List<ContactGroup>
    ): List<ContactGroup> {
        coEvery {
            searchContactGroupsMock.invoke(expectedUserId, expectedSearchTerm)
        } returns flowOf(expectedContactGroups.right())
        return expectedContactGroups
    }

    private fun expectIsDeviceContactsSuggestionsEnabled(enabled: Boolean) {
        every { isDeviceContactsSuggestionsEnabledMock.invoke() } returns enabled
    }

    private fun expectObservedMessageAttachments(userId: UserId, messageId: MessageId) {
        every {
            observeMessageAttachments(userId, messageId)
        } returns flowOf(listOf(MessageAttachmentSample.invoice))
    }

    private fun expectAttachmentDeleteSucceeds(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) {
        coEvery { deleteAttachment(userId, senderEmail, messageId, attachmentId) } returns Unit.right()
    }

    private fun expectStoreParentAttachmentSucceeds(userId: UserId, messageId: MessageId) {
        coJustRun { storeExternalAttachmentStates(userId, messageId) }
    }

    private fun expectReEncryptAttachmentSucceeds(
        userId: UserId,
        messageId: MessageId,
        previousSenderEmail: SenderEmail,
        newSenderEmail: SenderEmail
    ) {
        coEvery { reEncryptAttachments(userId, messageId, previousSenderEmail, newSenderEmail) } returns Unit.right()
    }

    private fun expectReEncryptAttachmentFails(
        userId: UserId,
        messageId: MessageId,
        previousSenderEmail: SenderEmail,
        newSenderEmail: SenderEmail
    ) {
        coEvery {
            reEncryptAttachments(userId, messageId, previousSenderEmail, newSenderEmail)
        } returns AttachmentReEncryptionError.FailedToEncryptAttachmentKeyPackets.left()
    }

    private fun expectDeleteAllAttachmentsSucceeds(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId
    ) {
        coJustRun { deleteAllAttachments(userId, senderEmail, messageId) }
    }

    private fun expectMessagePassword(userId: UserId, messageId: MessageId) {
        val messagePassword = MessagePassword(userId, messageId, "password", null)
        coEvery { observeMessagePassword(userId, messageId) } returns flowOf(messagePassword)
    }

    private fun expectNoMessagePassword(userId: UserId, messageId: MessageId) {
        coEvery { observeMessagePassword(userId, messageId) } returns flowOf(null)
    }

    private fun expectAddressValidation(address: String, expectedResult: Boolean) {
        every { isValidEmailAddressMock(address) } returns expectedResult
    }

    private fun expectSaveExpirationTimeForDraft(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        expirationTime: Duration
    ) {
        coEvery { saveMessageExpirationTime(userId, messageId, senderEmail, expirationTime) } returns Unit.right()
    }

    private fun expectObserveMessageExpirationTime(userId: UserId, messageId: MessageId) =
        MessageExpirationTime(userId, messageId, 1.days).also {
            coEvery { observeMessageExpirationTime(userId, messageId) } returns flowOf(it)
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

    private fun expectValidSenderAddress(userId: UserId, senderEmail: SenderEmail) {
        coEvery {
            validateSenderAddress(userId, senderEmail)
        } returns ValidateSenderAddress.ValidationResult.Valid(senderEmail).right()
    }

    private fun expectInvalidSenderAddress(
        userId: UserId,
        invalid: SenderEmail,
        useInstead: SenderEmail,
        reason: ValidateSenderAddress.ValidationError
    ) {
        coEvery {
            validateSenderAddress(userId, invalid)
        } returns ValidateSenderAddress.ValidationResult.Invalid(useInstead, invalid, reason).right()
    }

    private fun expectExternalRecipients(
        userId: UserId,
        recipientsTo: RecipientsTo,
        recipientsCc: RecipientsCc,
        recipientsBcc: RecipientsBcc
    ) = listOf(RecipientSample.ExternalEncrypted).also {
        coEvery { getExternalRecipients(userId, recipientsTo, recipientsCc, recipientsBcc) } returns it
    }

    companion object TestData {

        const val RawDraftBody = "I'm a message body"

        val existingDraftFields = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            DraftBody("Decrypted body of this draft"),
            RecipientsTo(listOf(Recipient("you@proton.ch", "Name"))),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList()),
            null
        )

        val draftFieldsWithQuotedBody = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            DraftBody(""),
            RecipientsTo(listOf(Recipient("you@proton.ch", "Name"))),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList()),
            OriginalHtmlQuote("<blockquote> Quoted html of the parent message </blockquote>")
        )

        const val BaseInitials = "AB"
    }
}
