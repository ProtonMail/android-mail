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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadataWithState
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import ch.protonmail.android.mailattachments.domain.sample.AttachmentMetadataSamples
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.BodyFields
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftFieldsWithSyncStatus
import ch.protonmail.android.mailcomposer.domain.model.DraftHead
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipientValidity
import ch.protonmail.android.mailcomposer.domain.model.OpenDraftError
import ch.protonmail.android.mailcomposer.domain.model.PasteMimeType
import ch.protonmail.android.mailcomposer.domain.model.RecipientValidityError
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SendWithExpirationTimeResult
import ch.protonmail.android.mailcomposer.domain.model.SenderAddresses
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.CanSendWithExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ChangeSenderAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ConvertInlineImageToAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.CreateDraftForAction
import ch.protonmail.android.mailcomposer.domain.usecase.CreateEmptyDraft
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteInlineAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.GetDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.GetDraftSenderValidationError
import ch.protonmail.android.mailcomposer.domain.usecase.GetMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.GetSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.IsMessagePasswordSet
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.LoadMessageBodyImage
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePasswordChanged
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveRecipientsValidation
import ch.protonmail.android.mailcomposer.domain.usecase.OpenExistingDraft
import ch.protonmail.android.mailcomposer.domain.usecase.SanitizePastedContent
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ScheduleSendMessage
import ch.protonmail.android.mailcomposer.domain.usecase.SendMessage
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSubject
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateRecipients
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.DraftDisplayBodyUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerStateReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.usecase.ActiveComposerRegistry
import ch.protonmail.android.mailcomposer.presentation.usecase.AddAttachment
import ch.protonmail.android.mailcomposer.presentation.usecase.BuildDraftDisplayBody
import ch.protonmail.android.mailcomposer.presentation.usecase.GetFormattedScheduleSendOptions
import ch.protonmail.android.mailcontact.domain.usecase.PreloadContactSuggestions
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.attachment.NO_ATTACHMENT_LIMIT
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentMetadataUiModelSamples
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.composer.DraftFieldsTestData
import ch.protonmail.android.testdata.composer.DraftRecipientTestData
import io.mockk.Called
import io.mockk.Runs
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.serialize
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

internal class ComposerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>()
    private val storeDraftWithSubjectMock = mockk<StoreDraftWithSubject>()
    private val updateRecipients = mockk<UpdateRecipients>()
    private val sendMessageMock = mockk<SendMessage>()
    private val networkManagerMock = mockk<NetworkManager>()
    private val observePrimaryUserIdMock = mockk<ObservePrimaryUserId>()
    private val isValidEmailAddressMock = mockk<IsValidEmailAddress>()
    private val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
    private val deleteAttachment = mockk<DeleteAttachment>()
    private val deleteInlineAttachment = mockk<DeleteInlineAttachment>()
    private val addAttachment = mockk<AddAttachment>()
    private val observeMessageAttachments = mockk<ObserveMessageAttachments>()
    private val createEmptyDraft = mockk<CreateEmptyDraft>()
    private val createDraftForAction = mockk<CreateDraftForAction>()
    private val openExistingDraft = mockk<OpenExistingDraft>()
    private val recipientsStateManager = spyk<RecipientsStateManager>()
    private val discardDraft = mockk<DiscardDraft>()
    private val getDraftId = mockk<GetDraftId>()
    private val loadMessageBodyImage = mockk<LoadMessageBodyImage>()
    private val getFormattedScheduleSendOptions = mockk<GetFormattedScheduleSendOptions>()
    private val scheduleSendMessage = mockk<ScheduleSendMessage>()
    private val getSenderAddresses = mockk<GetSenderAddresses>()
    private val changeSenderAddress = mockk<ChangeSenderAddress>()
    private val composerRegistry = mockk<ActiveComposerRegistry> {
        every { this@mockk.register(any()) } just Runs
        every { isActive(any()) } returns true
    }
    private val observeMessagePasswordChanged = mockk<ObserveMessagePasswordChanged> {
        every { this@mockk.invoke() } returns flowOf()
    }
    private val isMessagePasswordSet = mockk<IsMessagePasswordSet>()
    private val observeRecipientsValidation = mockk<ObserveRecipientsValidation> {
        every { this@mockk.invoke() } returns flowOf()
    }
    private val showEncryptionInfoFeatureFlag = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns false
    }
    private val getDraftSenderValidationError = mockk<GetDraftSenderValidationError> {
        coEvery { this@mockk.invoke() } returns null
    }
    private val preloadContactSuggestions = mockk<PreloadContactSuggestions> {
        coEvery { this@mockk.invoke(UserIdSample.Primary) } returns Unit.right()
    }
    private val saveMessageExpirationTime = mockk<SaveMessageExpirationTime>()
    private val getMessageExpirationTime = mockk<GetMessageExpirationTime>()

    private val canSendWithExpirationTime = mockk<CanSendWithExpirationTime> {
        coEvery { this@mockk.invoke() } returns SendWithExpirationTimeResult.CanSend.right()
    }
    private val convertInlineImageToAttachment = mockk<ConvertInlineImageToAttachment>()

    private val buildDraftDisplayBody = mockk<BuildDraftDisplayBody> {
        val headSlot = slot<DraftHead>()
        val bodySlot = slot<DraftBody>()
        coEvery { this@mockk.invoke(capture(headSlot), capture(bodySlot)) } answers {
            DraftDisplayBodyUiModel(
                "<html> ${headSlot.captured.value} ${bodySlot.captured.value} </html>"
            )
        }
    }
    private val sanitizePastedContent = mockk<SanitizePastedContent>()
    private val appEventBroadcaster = mockk<AppEventBroadcaster>()
    private val reducer = ComposerStateReducer()

    @BeforeTest
    fun setUp() {
        mockkStatic(android.graphics.Color::parseColor)
        every { android.graphics.Color.parseColor(any()) } returns 0
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        unmockkStatic(android.graphics.Color::parseColor)
    }

    private fun viewModel() = ComposerViewModel(
        storeDraftWithBodyMock,
        storeDraftWithSubjectMock,
        updateRecipients,
        reducer,
        isValidEmailAddressMock,
        observeMessageAttachments,
        sendMessageMock,
        networkManagerMock,
        addAttachment,
        deleteAttachment,
        deleteInlineAttachment,
        openExistingDraft,
        createEmptyDraft,
        createDraftForAction,
        buildDraftDisplayBody,
        "test-composer-instance-id",
        recipientsStateManager,
        discardDraft,
        getDraftId,
        savedStateHandle,
        loadMessageBodyImage,
        getFormattedScheduleSendOptions,
        scheduleSendMessage,
        getSenderAddresses,
        changeSenderAddress,
        composerRegistry,
        testDispatcher,
        observeMessagePasswordChanged,
        isMessagePasswordSet,
        observeRecipientsValidation,
        showEncryptionInfoFeatureFlag,
        getDraftSenderValidationError,
        preloadContactSuggestions,
        saveMessageExpirationTime,
        getMessageExpirationTime,
        canSendWithExpirationTime,
        convertInlineImageToAttachment,
        sanitizePastedContent,
        appEventBroadcaster,
        observePrimaryUserIdMock
    )

    @Test
    fun `should close composer when restored from saved state handle (process death)`() = runTest {
        // Given
        expectedUserId { UserIdSample.Primary }
        expectRestoredState(savedStateHandle)
        expectNoInputDraftAction()
        expectInputDraftMessageId { MessageIdSample.EmptyDraft }

        viewModel().composerStates.test {
            assertEquals(Effect.of(Unit), awaitItem().effects.closeComposer)
        }
    }

    @Test
    fun `should store attachments when attachments are added to the draft`() {
        // Given
        val uri = mockk<Uri>()
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val messageId = MessageIdSample.Invoice
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.John))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.John))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            expectedSubject,
            expectedBodyFields,
            DraftMimeType.Html,
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
        expectInputDraftMessageId { messageId }
        expectInitComposerWithExistingDraftSuccess(expectedUserId, messageId) { expectedFields }
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectAddAttachmentsSucceeds(uri)
        ignoreRecipientsUpdates()

        // When
        viewModel().submit(ComposerAction.AddAttachments(listOf(uri)))

        // Then
        coVerify { addAttachment(uri, DraftMimeType.Html) }
    }

    @Test
    fun `should store the draft body when the body changes`() = runTest {
        // Given
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectNoObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        val viewModel = viewModel()

        // When
        performInput {
            viewModel.bodyTextField.edit { append(expectedDraftBody.value) }
        }

        // Then
        coVerify {
            storeDraftWithBodyMock(expectedDraftBody)
        }
    }

    @Test
    fun `should store draft subject when subject changes`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectNoObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        // When
        performInput {
            viewModel().subjectTextField.edit { append(expectedSubject.value) }
        }

        // Then
        coVerify { storeDraftWithSubjectMock(expectedSubject) }
    }

    @Test
    fun `should store draft recipients when they change`() = runTest {
        // Given
        val expectedTo = listOf(buildSingleRecipient("", "valid-to@email.com"))
        val expectedCc = listOf(buildSingleRecipient("", "valid-cc@email.com"))
        val expectedBcc = listOf(buildSingleRecipient("", "valid-bcc@email.com"))
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectUpdateRecipientsSucceeds(expectedTo, expectedCc, expectedBcc)
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)

        // When
        recipientsStateManager.setFromDraftRecipients(expectedTo, expectedCc, expectedBcc)

        // Then
        viewModel().composerStates.test {
            advanceDebounce()
            coVerify { updateRecipients(expectedTo, expectedCc, expectedBcc) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should close composer with draft save when composer is closed while draft was non empty`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(
            head = expectedDraftHead,
            body = expectedDraftBody
        )
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.John))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.John))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.John))
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        ignoreRecipientsUpdates()

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.CloseComposer)

        // Then
        assertEquals(expectedMessageId, viewModel.composerStates.value.effects.closeComposerWithDraftSaved.consume())
    }

    @Test
    fun `should send message when send button is clicked`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.MailToRecipient))
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectNetworkManagerIsConnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectSendMessageSucceeds()
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)
        expectSendEventEmission()

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.SendMessage)

        // Then
        coVerifyOrder {
            sendMessageMock()
            appEventBroadcaster.emit(AppEvent.MessageSent)
        }
        assertEquals(Effect.of(Unit), viewModel.composerStates.value.effects.closeComposerWithMessageSending)
    }

    @Test
    fun `should not send message when send button is clicked but draft fails to save (subject)`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.MailToRecipient))
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectNetworkManagerIsConnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectFails(expectedSubject) { SaveDraftError.SaveFailed }
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.SendMessage)

        // Then
        coVerify(exactly = 0) { sendMessageMock() }
        assertEquals(Effect.empty(), viewModel.composerStates.value.effects.closeComposerWithMessageSending)
        assertEquals(
            Effect.of(TextUiModel(R.string.composer_error_store_draft_generic)),
            viewModel.composerStates.value.effects.error
        )
    }

    @Test
    fun `should not send message when send button is clicked but draft fails to save (body)`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.MailToRecipient))
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectNetworkManagerIsConnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodyFails(expectedDraftBody) { SaveDraftError.SaveFailed }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.SendMessage)

        // Then
        coVerify(exactly = 0) { sendMessageMock() }
        assertEquals(Effect.empty(), viewModel.composerStates.value.effects.closeComposerWithMessageSending)
        assertEquals(
            Effect.of(TextUiModel(R.string.composer_error_store_draft_generic)),
            viewModel.composerStates.value.effects.error
        )
    }

    @Test
    fun `should send message in offline when send button is clicked while offline`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.MailToRecipient))
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectNetworkManagerIsDisconnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)
        expectSendMessageSucceeds()
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectSendEventEmission()
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.SendMessage)

        // Then
        coVerifyOrder {
            sendMessageMock()
            appEventBroadcaster.emit(AppEvent.MessageSent)
        }
        assertEquals(Effect.of(Unit), viewModel.composerStates.value.effects.closeComposerWithMessageSendingOffline)
    }

    @Test
    fun `should close composer without mention of saved draft when composer is closed with no draft saved`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectNoObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()
        expectedNoDraftSaved()

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.CloseComposer)

        // Then
        assertEquals(Effect.of(Unit), viewModel.composerStates.value.effects.closeComposer)
    }

    @Test
    fun `emits state with primary sender address when available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        val expectedDraftFields = expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        // When
        val actual = viewModel().composerStates.value

        // Then
        assertEquals(SenderUiModel(expectedDraftFields.sender.value), actual.main.sender)
    }

    @Test
    fun `stop init and emits state with initialization error when creating new empty draft fails`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        ignoreRecipientsUpdates()
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftFails(expectedUserId) { OpenDraftError.CouldNotFindAddress }

        // When
        val actual = viewModel().composerStates.value

        // Then
        assertEquals(TextUiModel(R.string.composer_error_create_draft), actual.effects.exitError.consume())
        verify { observeMessageAttachments wasNot Called }
        verify { updateRecipients wasNot Called }
    }

    @Test
    fun `emits state with saving draft body error when save draft body returns error`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftBody = DraftBody("updated-draft")
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodyFails(expectedDraftBody) {
            SaveDraftError.SaveFailed
        }
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        // When
        val viewModel = viewModel()
        performInput {
            viewModel.bodyTextField.edit { append(expectedDraftBody.value) }
        }

        // Then
        val currentState = viewModel.composerStates.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_generic), currentState.effects.error.consume())
    }

    @Test
    fun `emits state with saving draft subject error when save draft subject returns error`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectStoreDraftSubjectFails(expectedSubject) {
            SaveDraftError.SaveFailed
        }
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        // When
        val viewModel = viewModel()
        performInput {
            viewModel.subjectTextField.edit { append(expectedSubject.value) }
        }

        // Then
        val currentState = viewModel.composerStates.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_generic), currentState.effects.error.consume())
    }

    @Test
    fun `emits state with saving draft recipients error when save recipients returns error`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val toRecipients = listOf(
            buildSingleRecipient("", "valid@email.com"),
            buildSingleRecipient("", "invalid email", DraftRecipientValidity.Invalid(RecipientValidityError.Format))
        )
        val recipientsUiModels = listOf(
            RecipientUiModel.Validating("valid@email.com"),
            RecipientUiModel.Invalid("invalid email")
        )
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectUpdateRecipientsFails(toRecipients, emptyList(), emptyList()) {
            SaveDraftError.SaveFailed
        }
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        coEvery { storeDraftWithBodyMock(any()) } returns Unit.right()

        val viewModel = viewModel()
        viewModel.composerStates.test {
            // When
            recipientsStateManager.updateRecipients(recipientsUiModels, ContactSuggestionsField.TO)
            advanceDebounce()

            // Then
            val currentState = viewModel.composerStates.value
            assertEquals(
                TextUiModel(R.string.composer_error_store_draft_generic),
                currentState.effects.error.consume()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits state with loading draft content when draftId was given as input`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val draftFields = existingDraftFields
        // Simulate a small delay in getDecryptedDraftFields to ensure the "loading" state was emitted
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId, 100) { draftFields }
        expectStoreDraftSubjectSucceeds(existingDraftFields.subject)
        expectStoreDraftBodySucceeds(existingDraftFields.bodyFields.body)
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        ignoreRecipientsUpdates()

        // When
        val actual = viewModel().composerStates.value

        // Then
        assertEquals(actual.main.loadingType, ComposerState.LoadingType.Initial)
        coVerify { openExistingDraft(expectedUserId, expectedDraftId) }
    }

    @Test
    fun `emits state with remote draft fields to be prefilled when open draft succeeds`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val expectedDraftFields = existingDraftFields
        val expectedDisplayBody = DraftDisplayBodyUiModel(
            "<html> ${expectedDraftFields.bodyFields.head.value} ${expectedDraftFields.bodyFields.body.value} </html>"
        )
        expectUpdateRecipientsSucceeds(
            existingDraftFields.recipientsTo.value,
            existingDraftFields.recipientsCc.value,
            existingDraftFields.recipientsBcc.value
        )
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId) { existingDraftFields }
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(existingDraftFields.subject)
        expectStoreDraftBodySucceeds(existingDraftFields.bodyFields.body)
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)

        // When
        val viewModel = viewModel()
        val sender = viewModel.composerStates.value.main.sender

        // Then
        assertEquals(SenderUiModel(expectedDraftFields.sender.value), sender)
        assertEquals(existingDraftFields.bodyFields.body.value, viewModel.bodyTextField.text)
        assertEquals(existingDraftFields.subject.value, viewModel.subjectTextField.text)

        viewModel.displayBody.test {
            assertEquals(expectedDisplayBody, awaitItem())
        }
    }

    @Test
    fun `emits state with focusTextBody effect when draft has a recipient`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectUpdateRecipientsSucceeds(
            existingDraftFields.recipientsTo.value,
            existingDraftFields.recipientsCc.value,
            existingDraftFields.recipientsBcc.value
        )
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId) { existingDraftFields }
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(existingDraftFields.subject)
        expectStoreDraftBodySucceeds(existingDraftFields.bodyFields.body)
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)

        // When
        val actual = viewModel().composerStates.value

        assertEquals(Unit, actual.effects.focusTextBody.consume())
    }

    @Test
    fun `emits state with NO focusTextBody effect when draft has no recipients`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectUpdateRecipientsSucceeds(
            emptyList(),
            existingDraftFields.recipientsCc.value,
            existingDraftFields.recipientsBcc.value
        )
        val expectedExistingDraftFieldsNoRecipients = existingDraftFields.copy(recipientsTo = RecipientsTo(emptyList()))
        expectInitComposerWithExistingDraftSuccess(
            expectedUserId,
            expectedDraftId
        ) { expectedExistingDraftFieldsNoRecipients }
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(existingDraftFields.subject)
        expectStoreDraftBodySucceeds(existingDraftFields.bodyFields.body)
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)

        // When
        val actual = viewModel().composerStates.value

        assertEquals(null, actual.effects.focusTextBody.consume())
    }

    @Test
    fun `emits state with local draft fields to be prefilled when open existing draft succeeds`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val expectedDraftFields = existingDraftFields
        val expectedDisplayBody = DraftDisplayBodyUiModel(
            "<html> ${expectedDraftFields.bodyFields.head.value} ${expectedDraftFields.bodyFields.body.value} </html>"
        )
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId) { existingDraftFields }
        expectStoreDraftSubjectSucceeds(expectedDraftFields.subject)
        expectStoreDraftBodySucceeds(expectedDraftFields.bodyFields.body)
        expectObservedMessageAttachments()
        expectInputDraftAction { DraftAction.Compose }
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        ignoreRecipientsUpdates()

        // When
        val viewModel = viewModel()
        val sender = viewModel.composerStates.value.main.sender

        // Then
        assertEquals(SenderUiModel(expectedDraftFields.sender.value), sender)
        assertEquals(existingDraftFields.bodyFields.body.value, viewModel.bodyTextField.text)
        assertEquals(existingDraftFields.subject.value, viewModel.subjectTextField.text)

        viewModel.displayBody.test {
            assertEquals(expectedDisplayBody, awaitItem())
        }
    }

    @Test
    fun `emits state with draft not in sync warning when open existing draft from local cache`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val expectedDraftFields = existingDraftFields
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId, isDraftSynced = false) {
            existingDraftFields
        }
        expectStoreDraftSubjectSucceeds(expectedDraftFields.subject)
        expectStoreDraftBodySucceeds(expectedDraftFields.bodyFields.body)
        expectObservedMessageAttachments()
        expectInputDraftAction { DraftAction.Compose }
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        ignoreRecipientsUpdates()

        // When
        val actual = viewModel().composerStates.value

        // Then
        val expectedWarning = Effect.of(TextUiModel(R.string.composer_warning_local_data_shown))
        assertEquals(expectedWarning, actual.effects.warning)
    }

    @Test
    fun `stops init and emits state with unrecoverable exit error when open existing draft fails`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectInitComposerWithExistingDraftError(expectedUserId, expectedDraftId) { OpenDraftError.MessageIsNotADraft }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        ignoreRecipientsUpdates()

        // When
        val actual = viewModel().composerStates.value

        // Then
        assertEquals(TextUiModel(R.string.composer_error_loading_draft), actual.effects.exitError.consume())
        verify { observeMessageAttachments wasNot Called }
        verify { updateRecipients wasNot Called }
    }

    @Test
    fun `emits state with an effect to show the atts sources when add attachments action is submitted`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId) { existingDraftFields }
        expectNoInputDraftAction()
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectStoreDraftSubjectSucceeds(existingDraftFields.subject)
        expectStoreDraftBodySucceeds(existingDraftFields.bodyFields.body)
        ignoreRecipientsUpdates()

        // When'
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.AddAttachmentsRequested)

        // Then
        val actual = viewModel.composerStates.value
        assertEquals(true, actual.effects.changeBottomSheetVisibility.consume())
    }

    @Test
    fun `emits state with updated attachments when the attachments change`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftId = expectInputDraftMessageId { MessageIdSample.Invoice }
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.John))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.John))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.John))
        val expectedFields = DraftFields(
            expectedSenderEmail,
            subject = expectedSubject,
            bodyFields = expectedBodyFields,
            mimeType = DraftMimeType.Html,
            recipientsTo = recipientsTo,
            recipientsCc = recipientsCc,
            recipientsBcc = recipientsBcc
        )
        expectNoInputDraftAction()
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedDraftId) { expectedFields }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectStoreDraftSubjectSucceeds(expectedSubject)
        ignoreRecipientsUpdates()

        // When
        val viewModel = viewModel()
        viewModel.composerStates.test {
            advanceUntilIdle()

            // Then
            val expected = AttachmentGroupUiModel(
                limit = NO_ATTACHMENT_LIMIT,
                attachments = listOf(AttachmentMetadataUiModelSamples.DeletableInvoiceUploaded)
            )
            val actual = awaitItem().attachments.uiModel
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `delete compose action triggers delete attachment use case`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val messageId = MessageIdSample.Invoice
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val expectedAttachmentId = AttachmentId("attachment_id")
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.John))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.John))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.John))
        val expectedFields = DraftFields(
            sender = expectedSenderEmail,
            subject = expectedSubject,
            bodyFields = expectedBodyFields,
            mimeType = DraftMimeType.Html,
            recipientsTo = recipientsTo,
            recipientsCc = recipientsCc,
            recipientsBcc = recipientsBcc
        )
        expectInputDraftMessageId { messageId }
        expectInitComposerWithExistingDraftSuccess(expectedUserId, messageId) { expectedFields }
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectObservedMessageAttachments()
        expectNoInputDraftAction()
        expectAttachmentDeleteSucceeds(expectedAttachmentId)
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        ignoreRecipientsUpdates()

        // When
        viewModel().submit(ComposerAction.RemoveAttachment(expectedAttachmentId))

        // Then
        coVerify { deleteAttachment(expectedAttachmentId) }
    }

    @Test
    fun `should set recipient to state when recipient was given as an input`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedRecipient = DraftRecipientTestData.MailToRecipient.copy(validity = DraftRecipientValidity.Valid)
        val expectedAction = DraftAction.ComposeToAddresses(listOf(expectedRecipient.address))

        expectNoInputDraftMessageId()
        expectInputDraftAction { expectedAction }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectUpdateRecipientsSucceeds(listOf(expectedRecipient), emptyList(), emptyList())
        expectObservedMessageAttachments()
        expectAddressValidation(expectedRecipient.address, true)
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId) {
            DraftFieldsTestData.EmptyDraftWithPrimarySender
        }

        // When
        val viewModel = viewModel()
        viewModel.composerStates.test {
            // Then
            assertEquals(
                RecipientUiModel.Valid(expectedRecipient.address),
                recipientsStateManager.recipients.value.toRecipients.firstOrNull()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should show warning when send button is clicked with expiration set and external recipients`() = runTest {
        // Given
        val expectedSubject = Subject("Subject")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.ExternalRecipient))
        val recipientsCc = RecipientsCc(listOf())
        val recipientsBcc = RecipientsBcc(listOf())
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        val externalAddresses = listOf(DraftRecipientTestData.ExternalRecipient.address)
        expectNetworkManagerIsConnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectCanSendMessageWithExpiration(
            SendWithExpirationTimeResult.ExpirationUnsupportedForSome(externalAddresses)
        )
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.SendMessage)

        // Then
        coVerify { sendMessageMock wasNot Called }
        val expected: Effect<TextUiModel> = Effect.of(
            TextUiModel.TextResWithArgs(
                R.string.composer_send_expiring_message_to_external_will_fail,
                externalAddresses
            )
        )
        assertEquals(expected, viewModel.composerStates.value.effects.confirmSendExpiringMessage)
    }

    @Test
    fun `should send message when sending an expiring message to external recipients was confirmed`() = runTest {
        // Given
        val expectedSubject = Subject("Subject for the message")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.John))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.John))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.John))
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectNetworkManagerIsDisconnected()
        expectNoInputDraftMessageId()
        expectNoInputDraftAction()
        expectSendMessageSucceeds()
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        ignoreRecipientsUpdates()
        expectSendEventEmission()
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }

        // When
        viewModel().submit(ComposerAction.ConfirmSendExpirationSetToExternal)

        // Then
        coVerifyOrder {
            sendMessageMock()
            appEventBroadcaster.emit(AppEvent.MessageSent)
        }
    }

    @Test
    fun `should show confirmation dialog when discarding a draft`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.EmptyDraft
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectObservedMessageAttachments()
        ignoreRecipientsUpdates()
        coEvery { discardDraft(expectedUserId, expectedMessageId) } returns Unit.right()

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.DiscardDraftRequested)

        // Then
        viewModel.composerStates.test {
            assertEquals(Unit, awaitItem().effects.confirmDiscardDraft.consume())
        }
    }

    @Test
    fun `should call use case and close composer when discarding draft is confirmed`() = runTest {
        // Given
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectObservedMessageAttachments()
        ignoreRecipientsUpdates()
        coEvery { discardDraft(expectedUserId, expectedMessageId) } returns Unit.right()

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.DiscardDraftConfirmed)

        // Then
        viewModel.composerStates.test {
            assertEquals(Unit, awaitItem().effects.closeComposerWithDraftDiscarded.consume())
        }
        coVerify { discardDraft(expectedUserId, expectedMessageId) }
    }

    @Test
    fun `should close composer without calling use case when discard draft is confirmed while no draft was created`() =
        runTest {
            // Given
            val expectedUserId = expectedUserId { UserIdSample.Primary }
            expectNoInputDraftMessageId()
            expectInputDraftAction { DraftAction.Compose }
            expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
            expectNoObservedMessageAttachments()
            ignoreRecipientsUpdates()
            expectedNoDraftSaved()

            // When
            val viewModel = viewModel()
            viewModel.submit(ComposerAction.DiscardDraftConfirmed)

            // Then
            viewModel.composerStates.test {
                assertEquals(Unit, awaitItem().effects.closeComposerWithDraftDiscarded.consume())
            }
            coVerify { discardDraft wasNot Called }
        }

    @Test
    fun `should show warning when send button is clicked while subject is empty`() = runTest {
        // Given
        val expectedSubject = Subject("")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.MailToRecipient))
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectNetworkManagerIsConnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.SendMessage)

        // Then
        coVerify { sendMessageMock wasNot Called }
        assertEquals(Effect.of(Unit), viewModel.composerStates.value.effects.confirmSendingWithoutSubject)
    }

    @Test
    fun `displays available sender addresses when change sender is requested`() = runTest {
        // Given
        val expectedSubject = Subject("")
        val expectedSenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftHead = DraftHead("Draft head of this draft")
        val expectedDraftBody = DraftBody("I am plaintext")
        val expectedBodyFields = BodyFields(expectedDraftHead, expectedDraftBody)
        val recipientsTo = RecipientsTo(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsCc = RecipientsCc(listOf(DraftRecipientTestData.MailToRecipient))
        val recipientsBcc = RecipientsBcc(listOf(DraftRecipientTestData.MailToRecipient))
        val expectedMessageId = expectInputDraftMessageId { MessageIdSample.RemoteDraft }
        expectNetworkManagerIsConnected()
        expectNoInputDraftAction()
        expectStoreDraftSubjectSucceeds(expectedSubject)
        expectStoreDraftBodySucceeds(expectedDraftBody)
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithExistingDraftSuccess(expectedUserId, expectedMessageId) {
            DraftFields(
                sender = expectedSenderEmail,
                subject = expectedSubject,
                bodyFields = expectedBodyFields,
                mimeType = DraftMimeType.Html,
                recipientsTo = recipientsTo,
                recipientsCc = recipientsCc,
                recipientsBcc = recipientsBcc
            )
        }
        expectUpdateRecipientsSucceeds(recipientsTo.value, recipientsCc.value, recipientsBcc.value)
        val addresses = listOf(SenderEmail("test@pm.me"), SenderEmail("test1@pm.me"))
        val senderAddresses = SenderAddresses(
            addresses = addresses,
            selected = addresses[0]
        )
        coEvery { getSenderAddresses() } returns senderAddresses.right()
        val expected = addresses.map { SenderUiModel(it.value) }

        // When
        val viewModel = viewModel()
        viewModel.submit(ComposerAction.ChangeSender)

        // Then
        assertEquals(expected, viewModel.composerStates.value.main.senderAddresses)
    }

    @Test
    fun `should delete erroneous attachments when confirmed`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectStoreDraftSubjectSucceeds(Subject(""))
        expectStoreDraftBodySucceeds(DraftBody(""))
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        val id1 = AttachmentId("att-1")
        val id2 = AttachmentId("att-2")

        coEvery { deleteAttachment(id1) } returns Unit.right()
        coEvery { deleteAttachment(id2) } returns Unit.right()

        // When
        viewModel().submit(ComposerAction.AcknowledgeAttachmentErrors(listOf(id1, id2)))

        // Then
        coVerify { deleteAttachment(id1) }
        coVerify { deleteAttachment(id2) }
    }

    @Test
    fun `should stop initialization and not start observers when prefillForDraftAction fails`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val replyAction = DraftAction.Reply(MessageIdSample.Invoice)
        expectNoInputDraftMessageId()
        expectInputDraftAction { replyAction }
        expectNoRestoredState(savedStateHandle)
        coEvery { createDraftForAction(expectedUserId, replyAction) } returns OpenDraftError.MessageIsNotADraft.left()

        // When
        viewModel()

        // Then
        verify { observeMessageAttachments wasNot Called }
        verify { observeRecipientsValidation wasNot Called }
        verify { updateRecipients wasNot Called }
        coVerify { preloadContactSuggestions wasNot Called }
    }

    @Test
    fun `sanitizePastedText delegates and returns sanitized text`() = runTest {
        // Given
        expectedUserId { UserIdSample.Primary }
        expectRestoredState(savedStateHandle)
        expectNoInputDraftAction()
        expectInputDraftMessageId { MessageIdSample.EmptyDraft }
        val input = "Hello,\n\nThis is line 2.\nBest regards"
        val expected = "Hello,<br><br>This is line 2.<br>Best regards"
        every { sanitizePastedContent.invoke(input, PasteMimeType.Html) } returns expected

        // When
        val result = viewModel().sanitizePastedText("text/html", input)

        // Then
        assertEquals(expected, result)
        verify(exactly = 1) { sanitizePastedContent.invoke(input, PasteMimeType.Html) }
    }

    @Test
    fun `sanitizePastedText returns input as fallback when delegated use case raises exception`() = runTest {
        // Given
        expectedUserId { UserIdSample.Primary }
        expectRestoredState(savedStateHandle)
        expectNoInputDraftAction()
        expectInputDraftMessageId { MessageIdSample.EmptyDraft }
        val input = "<b>Hello</b>"
        every {
            sanitizePastedContent.invoke(input, PasteMimeType.PlainText)
        } throws RuntimeException("failure in sanitization")

        // When
        val result = viewModel().sanitizePastedText("text/plain", input)

        // Then
        assertEquals(input, result)
        verify(exactly = 1) { sanitizePastedContent.invoke(input, PasteMimeType.PlainText) }
    }


    @Test
    fun `should not save draft when composer is marked as inactive during debounce window`() = runTest {
        // Given
        val expectedDraftBody = DraftBody("I'm a message body")
        val expectedSubject = Subject("Test subject")
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectNoInputDraftMessageId()
        expectInputDraftAction { DraftAction.Compose }
        expectNoObservedMessageAttachments()
        expectNoFileShareVia()
        expectNoRestoredState(savedStateHandle)
        expectInitComposerWithNewEmptyDraftSucceeds(expectedUserId)
        ignoreRecipientsUpdates()

        // When
        val viewModel = viewModel()

        performInput(skipDebounce = false) {
            viewModel.bodyTextField.edit { append(expectedDraftBody.value) }
            viewModel.subjectTextField.edit { append(expectedSubject.value) }
        }

        // Simulate the composer being closed (onCleared unregisters the instance)
        every { composerRegistry.isActive(any()) } returns false

        advanceDebounce()

        // Then
        verify { updateRecipients wasNot called }
        verify { storeDraftWithBodyMock wasNot called }
        verify { storeDraftWithSubjectMock wasNot called }
    }

    // This is both used to mock the result of the "composer init" in the cases where we
    // create a new draft (eg. Compose, Reply, ComposeTo...)
    // and also as a hack to initialize the composer's state to an expected one to test
    private fun expectInitComposerWithNewEmptyDraftSucceeds(
        userId: UserId,
        result: () -> DraftFields = { DraftFieldsTestData.EmptyDraftWithPrimarySender }
    ) = result().also { draftFields ->
        coEvery { createEmptyDraft(userId) } coAnswers { draftFields.right() }
    }

    private fun expectInitComposerWithExistingDraftError(
        userId: UserId,
        draftId: MessageId,
        error: () -> OpenDraftError
    ) = error().also { coEvery { openExistingDraft(userId, draftId) } returns it.left() }

    private fun expectInitComposerWithExistingDraftSuccess(
        userId: UserId,
        draftId: MessageId,
        responseDelay: Long = 0L,
        isDraftSynced: Boolean = true,
        result: () -> DraftFields
    ) = result().also { draftFields ->
        coEvery { openExistingDraft(userId, draftId) } coAnswers {
            delay(responseDelay)
            if (isDraftSynced) {
                DraftFieldsWithSyncStatus.Remote(draftFields)
            } else {
                DraftFieldsWithSyncStatus.Local(draftFields)
            }.right()
        }
    }

    private fun expectInputDraftAction(draftAction: () -> DraftAction) = draftAction().also {
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns it.serialize()
    }

    private fun expectNoInputDraftAction() {
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
    }

    private fun expectNoFileShareVia() {
        every { savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey) } returns null
    }

    private fun expectNoInputDraftMessageId() {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns null
    }

    private fun expectInputDraftMessageId(draftId: () -> MessageId) = draftId().also {
        every { savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) } returns it.id
    }

    private fun expectSendMessageSucceeds() {
        coEvery { sendMessageMock.invoke() } returns Unit.right()
    }

    private fun expectNetworkManagerIsConnected() {
        every { networkManagerMock.isConnectedToNetwork() } returns true
    }

    private fun expectNetworkManagerIsDisconnected() {
        every { networkManagerMock.isConnectedToNetwork() } returns false
    }

    private fun expectedMessageId(messageId: () -> MessageId): MessageId = messageId().also {
        coEvery { getDraftId() } returns it.right()
    }

    private fun expectedNoDraftSaved() {
        coEvery { getDraftId() } returns DataError.Local.NotFound.left()
    }

    private fun expectedUserId(userId: () -> UserId): UserId = userId().also {
        coEvery { observePrimaryUserIdMock() } returns flowOf(it)
    }

    private fun expectInitComposerWithNewEmptyDraftFails(userId: UserId, dataError: () -> OpenDraftError) =
        dataError().also {
            coEvery { createEmptyDraft(userId) } returns it.left()
        }

    private fun expectStoreDraftBodySucceeds(expectedDraftBody: DraftBody) {
        coEvery {
            storeDraftWithBodyMock(expectedDraftBody)
        } returns Unit.right()
    }

    private fun expectStoreDraftBodyFails(expectedDraftBody: DraftBody, error: () -> SaveDraftError) = error().also {
        coEvery {
            storeDraftWithBodyMock(expectedDraftBody)
        } returns it.left()
    }

    private fun expectStoreDraftSubjectSucceeds(expectedSubject: Subject) {
        coEvery { storeDraftWithSubjectMock(expectedSubject) } returns Unit.right()
    }

    private fun expectStoreDraftSubjectFails(expectedSubject: Subject, error: () -> SaveDraftError) = error().also {
        coEvery {
            storeDraftWithSubjectMock(
                expectedSubject
            )
        } returns it.left()
    }

    private fun ignoreRecipientsUpdates() {
        coEvery { updateRecipients(any(), any(), any()) } returns Unit.right()
    }

    private fun expectUpdateRecipientsSucceeds(
        toRecipients: List<DraftRecipient>,
        ccRecipients: List<DraftRecipient>,
        bccRecipients: List<DraftRecipient>
    ) {
        coEvery { updateRecipients(toRecipients, ccRecipients, bccRecipients) } returns Unit.right()
    }

    private fun expectUpdateRecipientsFails(
        toRecipients: List<DraftRecipient>,
        ccRecipients: List<DraftRecipient>,
        bccRecipients: List<DraftRecipient>,
        error: () -> SaveDraftError
    ) = error().also {
        coEvery { updateRecipients(toRecipients, ccRecipients, bccRecipients) } returns it.left()
    }

    private fun expectNoObservedMessageAttachments() {
        coEvery {
            observeMessageAttachments()
        } returns flowOf(
            emptyList<AttachmentMetadataWithState>().right()
        )
    }

    private fun expectObservedMessageAttachments() {
        coEvery {
            observeMessageAttachments()
        } returns flowOf(
            listOf(
                AttachmentMetadataWithState(
                    AttachmentMetadataSamples.Invoice,
                    AttachmentState.Uploaded
                )
            ).right()
        )
    }

    private fun expectAttachmentDeleteSucceeds(attachmentId: AttachmentId) {
        coEvery { deleteAttachment(attachmentId) } returns Unit.right()
    }

    private fun expectAddressValidation(address: String, expectedResult: Boolean) {
        every { isValidEmailAddressMock(address) } returns expectedResult
    }

    private fun expectAddAttachmentsSucceeds(uri: Uri) {
        coEvery { addAttachment(uri, DraftMimeType.Html) } returns
            AddAttachment.AddAttachmentResult.StandardAttachmentAdded.right()
    }

    private fun expectRestoredState(savedStateHandle: SavedStateHandle) {
        every { savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) } returns true
    }

    private fun expectNoRestoredState(savedStateHandle: SavedStateHandle) {
        every { savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) } returns null
    }

    private fun expectCanSendMessageWithExpiration(result: SendWithExpirationTimeResult) {
        coEvery { canSendWithExpirationTime() } returns result.right()
    }

    private fun expectSendEventEmission() {
        coEvery { appEventBroadcaster.emit(AppEvent.MessageSent) } just runs
    }

    private fun TestScope.advanceDebounce() {
        this.advanceTimeBy(1.5.seconds)
    }

    private suspend fun TestScope.performInput(skipDebounce: Boolean = true, block: () -> Unit) {
        withContext(Dispatchers.Main) {
            block()
            Snapshot.sendApplyNotifications() // needed as we don't have Compose runtime in tests
        }

        if (skipDebounce) advanceDebounce()
    }

    companion object TestData {

        const val RawDraftBody = "I'm a message body"

        val existingDraftFields = DraftFields(
            SenderEmail("author@proton.me"),
            Subject("Here is the matter"),
            BodyFields(
                DraftHead("Draft head of this draft"),
                DraftBody("Decrypted body of this draft")
            ),
            DraftMimeType.Html,
            RecipientsTo(listOf(buildSingleRecipient("", "valid@email.com"))),
            RecipientsCc(emptyList()),
            RecipientsBcc(emptyList())
        )

        private fun buildSingleRecipient(
            name: String,
            address: String,
            validity: DraftRecipientValidity = DraftRecipientValidity.Validating,
            privacyLock: PrivacyLock = PrivacyLock.None
        ) = DraftRecipient.SingleRecipient(name, address, validity, privacyLock)
    }
}
