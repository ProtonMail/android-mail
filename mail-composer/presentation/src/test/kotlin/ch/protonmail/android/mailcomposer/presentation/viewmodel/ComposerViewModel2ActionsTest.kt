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
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.system.BuildVersionProvider
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
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
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAttachmentError
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.facade.AddressesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.AttachmentsFacade
import ch.protonmail.android.mailcomposer.presentation.facade.DraftFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageAttributesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageContentFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageParticipantsFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageSendingFacade
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.AccessoriesEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.AttachmentsEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerAction2
import ch.protonmail.android.mailcomposer.presentation.model.operations.CompositeEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.EffectsEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.EffectsEvent.ComposerControlEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.MainEvent
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerStateReducer
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.defaultDraftFields
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectDraftAction
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectParticipantsMapping
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectStandaloneDraft
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.messageId
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.parentMessageId
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.recipientsTo
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.userId
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.verifyStates
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.usecase.ShouldRestrictWebViewHeight
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.AttachmentUiModel
import ch.protonmail.android.mailmessage.presentation.model.NO_ATTACHMENT_LIMIT
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verifySequence
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.network.domain.NetworkManager
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

internal class ComposerViewModel2ActionsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val draftFacade = mockk<DraftFacade> {
        every { this@mockk.startContinuousUpload(userId, messageId, DraftAction.Compose, any()) } just runs
    }
    private val attachmentsFacade = mockk<AttachmentsFacade>(relaxed = true)
    private val addressesFacade = mockk<AddressesFacade>(relaxed = true)
    private val messageAttributesFacade = mockk<MessageAttributesFacade>(relaxed = true)
    private val messageContentFacade = mockk<MessageContentFacade>(relaxed = true)
    private val messageParticipantsFacade = mockk<MessageParticipantsFacade> {
        every { this@mockk.observePrimaryUserId() } returns flowOf(userId)
    }
    private val messageSendingFacade = mockk<MessageSendingFacade>(relaxed = true)

    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { this@mockk.observe() } returns flowOf(false)
    }
    private val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
    private val networkManagerMock = mockk<NetworkManager>(relaxed = true)
    private val composerStateReducer = spyk<ComposerStateReducer>()
    private val recipientsStateManager = spyk<RecipientsStateManager>()
    private val shouldRestrictWebViewHeight = mockk<ShouldRestrictWebViewHeight> {
        every { this@mockk.invoke(null) } returns false
    }
    private val buildVersionProvider = mockk<BuildVersionProvider> {
        every { sdkInt() } returns Build.VERSION_CODES.S
    }

    private fun viewModel(): ComposerViewModel2 = ComposerViewModel2(
        draftFacade,
        attachmentsFacade,
        messageAttributesFacade,
        messageContentFacade,
        messageParticipantsFacade,
        messageSendingFacade,
        addressesFacade,
        appInBackgroundState,
        networkManagerMock,
        savedStateHandle,
        composerStateReducer,
        testDispatcher,
        recipientsStateManager,
        shouldRestrictWebViewHeight,
        buildVersionProvider
    )

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit state to show bottomsheet and show addresses list on change sender request`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val expectedAddresses = listOf(
            UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress
        )
        coEvery { addressesFacade.getSenderAddresses() } returns expectedAddresses.right()

        val initialMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalMainState = initialMainState.copy(
            senderAddresses = expectedAddresses.map { SenderUiModel(it.email) }.toImmutableList()
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            changeBottomSheetVisibility = Effect.of(true)
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            verifyStates(main = initialMainState, actualStates = awaitItem())

            viewModel.submitAction(ComposerAction2.ChangeSender)
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should emit state to show error on change sender request with no addresses (unknown permissions)`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        coEvery {
            addressesFacade.getSenderAddresses()
        } returns GetComposerSenderAddresses.Error.FailedGettingPrimaryUser.left()

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            error = Effect.of(TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription))
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.ChangeSender)
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should emit state to show warning on change sender request with free user`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        coEvery {
            addressesFacade.getSenderAddresses()
        } returns GetComposerSenderAddresses.Error.UpgradeToChangeSender.left()

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature))
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.ChangeSender)
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should emit state to set new sender address`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val newSender = SenderEmail("new-sender@proton.me")
        coEvery {
            attachmentsFacade.reEncryptAttachments(userId, messageId, defaultDraftFields.sender, newSender)
        } returns Unit.right()

        coEvery {
            draftFacade.injectAddressSignature(
                userId, DraftBody(""),
                senderEmail = newSender,
                previousSenderEmail = SenderEmail("sender@email.com")
            )
        } returns DraftBody("").right()

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(newSender.value),
            prevSenderEmail = SenderEmail("sender@email.com")
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            changeBottomSheetVisibility = Effect.of(false)
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.SetSenderAddress(SenderUiModel(newSender.value)))
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should update the state to show the bottomsheet when requesting the expiration screen`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            changeBottomSheetVisibility = Effect.of(true)
        )

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.OpenExpirationSettings)
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should update the state to hide the bottomsheet when the expiration is being set`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val draftFields = emptyDraftFields()
        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value)
        )

        val finalAccessoriesState = ComposerState.Accessories.initial().copy(
            messageExpiresIn = 1.hours
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            changeBottomSheetVisibility = Effect.of(false)
        )

        coEvery {
            messageAttributesFacade.saveMessageExpiration(userId, messageId, draftFields.sender, 1.hours)
        } returns Unit.right()

        coEvery {
            draftFacade.storeDraft(userId, messageId, draftFields, any())
        } returns Unit.right()

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.SetMessageExpiration(1.hours))
            verifyStates(
                main = finalMainState,
                accessories = finalAccessoriesState,
                effects = finalEffectsState,
                actualStates = awaitItem()
            )
        }
    }

    @Test
    fun `should update the state to show an error when the expiration can't be set`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            error = Effect.of(TextUiModel(R.string.composer_error_setting_expiration_time)),
            changeBottomSheetVisibility = Effect.of(false)
        )

        coEvery {
            messageAttributesFacade.saveMessageExpiration(userId, messageId, defaultDraftFields.sender, 1.hours)
        } returns DataError.Local.Unknown.left()


        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.SetMessageExpiration(1.hours))
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should update the state to open the file picker on attachments icon tap`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            openImagePicker = Effect.of(Unit)
        )

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            skipItems(1)

            viewModel.submitAction(ComposerAction2.OpenFilePicker)
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should proxy the call to store attachments when receiving the store attachments action`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val attachmentsList: List<Uri> = listOf(mockk())

        coEvery {
            attachmentsFacade.storeAttachments(userId, messageId, defaultDraftFields.sender, attachmentsList)
        } returns Unit.right()

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            verifyStates(main = finalMainState, actualStates = awaitItem())
            viewModel.submitAction(ComposerAction2.StoreAttachments(attachmentsList))
        }

        coVerify {
            attachmentsFacade.storeAttachments(userId, messageId, defaultDraftFields.sender, attachmentsList)
        }
    }

    @Test
    fun `should emit an error when attachments can't be stored`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            error = Effect.of(TextUiModel.TextRes(R.string.composer_attachment_error_saving_attachment))
        )

        coEvery {
            attachmentsFacade.storeAttachments(userId, messageId, defaultDraftFields.sender, any())
        } returns StoreDraftWithAttachmentError.FailedToStoreAttachments.left()

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            skipItems(1)
            viewModel.submitAction(ComposerAction2.StoreAttachments(listOf(mockk())))

            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should update the state when cancelling sending with not subject`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            changeFocusToField = Effect.of(FocusedFieldType.SUBJECT),
            confirmSendingWithoutSubject = Effect.empty()
        )

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            skipItems(1)
            viewModel.submitAction(ComposerAction2.CancelSendWithNoSubject)

            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should keep the state clear when clearing sending error upon request`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        coEvery { messageSendingFacade.clearMessageSendingError(userId, messageId) } returns Unit.right()

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            viewModel.submitAction(ComposerAction2.ClearSendingError)
            verifyStates(main = finalMainState, actualStates = awaitItem())
        }

        coVerify { messageSendingFacade.clearMessageSendingError(userId, messageId) }
    }

    @Test
    fun `should proxy the call to remove an attachment when receiving the remove attachment action`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val attachment = mockk<AttachmentId>()

        coEvery {
            attachmentsFacade.deleteAttachment(userId, messageId, defaultDraftFields.sender, attachment)
        } returns Unit.right()

        // When + Then
        val viewModel = viewModel()
        viewModel.composerStates.test {
            verifyStates(main = finalMainState, actualStates = awaitItem())
            viewModel.submitAction(ComposerAction2.RemoveAttachment(attachment))
        }

        coVerify {
            attachmentsFacade.deleteAttachment(userId, messageId, defaultDraftFields.sender, attachment)
        }
    }

    @Test
    fun `should stop draft uploader when app goes to the background`() = runTest {
        // Given
        expectStandaloneDraft(savedStateHandle)
        every { draftFacade.provideNewDraftId() } returns messageId
        val appInBackground = MutableSharedFlow<Boolean>()

        every { appInBackgroundState.observe() } returns appInBackground
        every { draftFacade.stopContinuousUpload() } just runs

        @Suppress("Unused") val viewModel = viewModel()

        // When
        appInBackground.emit(true)

        // Then
        coVerifySequence {
            draftFacade.provideNewDraftId()
            draftFacade.stopContinuousUpload()
        }

        confirmVerified(draftFacade)
    }

    @Test
    fun `should start draft uploader when app is back to the background`() = runTest {
        // Given
        expectStandaloneDraft(savedStateHandle)
        every { draftFacade.provideNewDraftId() } returns messageId
        val appInBackground = MutableSharedFlow<Boolean>()

        every { appInBackgroundState.observe() } returns appInBackground
        every { draftFacade.stopContinuousUpload() } just runs

        @Suppress("Unused") val viewModel = viewModel()

        // When
        appInBackground.emit(true)
        appInBackground.emit(false)
        appInBackground.emit(true)

        // Then
        verifySequence {
            draftFacade.provideNewDraftId()
            draftFacade.stopContinuousUpload()
            draftFacade.startContinuousUpload(userId, messageId, DraftAction.Compose, any())
            draftFacade.stopContinuousUpload()
        }
    }

    @Test
    fun `should show empty subject warning when sending without a subject`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)
        expectParticipantsMapping(messageParticipantsFacade)

        val recipients = listOf(RecipientUiModel.Valid(recipientsTo.first().address))
        recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.TO)

        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()

        val viewModel = viewModel()
        viewModel.submitAction(ComposerAction2.SendMessage)

        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(defaultDraftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = true))
            composerStateReducer.reduceNewState(any(), MainEvent.CoreLoadingToggled)
            composerStateReducer.reduceNewState(any(), CompositeEvent.OnSendWithEmptySubject)
        }

        coVerify(exactly = 0) { messageSendingFacade.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `should close composer and skip saving draft when fields are unchanged`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        // When + Then
        verifyDraftSave(viewModel(), defaultDraftFields, shouldSaveDraft = false, draftAction = DraftAction.Compose) {
            // no-op
        }
    }

    @Test
    fun `should close composer, save draft and force upload when body has changed`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val draftBody = DraftBody("draft-body")
        val expectedDraftFields = emptyDraftFields().copy(body = draftBody)

        coEvery { draftFacade.stopContinuousUpload() } just runs
        coEvery { draftFacade.storeDraft(userId, messageId, expectedDraftFields, any()) } returns Unit.right()
        coEvery { draftFacade.forceUpload(userId, messageId) } just runs
        coEvery {
            draftFacade.injectAddressSignature(
                userId, DraftBody("draft-body"),
                senderEmail = defaultDraftFields.sender, previousSenderEmail = null
            )
        } returns DraftBody("").right()

        val viewModel = viewModel()

        // When + Then
        verifyDraftSave(viewModel, expectedDraftFields, shouldSaveDraft = true, draftAction = DraftAction.Compose) {
            viewModel.bodyFieldText.edit { append(draftBody.value) }
        }
    }

    @Test
    fun `should close composer and save draft when subject has changed`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val subject = Subject("subject")
        val expectedDraftFields = emptyDraftFields().copy(subject = subject)

        coEvery { draftFacade.stopContinuousUpload() } just runs
        coEvery { draftFacade.storeDraft(userId, messageId, expectedDraftFields, any()) } returns Unit.right()
        coEvery { draftFacade.forceUpload(userId, messageId) } just runs

        val viewModel = viewModel()

        // When + Then
        verifyDraftSave(viewModel, expectedDraftFields, shouldSaveDraft = true, draftAction = DraftAction.Compose) {
            viewModel.subjectTextField.edit { append(subject.value) }
        }
    }

    @Test
    fun `should replace body when respond inline is requested`() = runTest {
        // Given
        val draftAction = DraftAction.Reply(parentMessageId)
        val parentMessage = mockk<MessageWithDecryptedBody>()
        val draftFields = defaultDraftFields.copy(
            recipientsCc = RecipientsCc(emptyList()),
            recipientsBcc = RecipientsBcc(emptyList())
        )

        expectDraftAction(
            draftFacade,
            addressesFacade,
            messageContentFacade,
            savedStateHandle,
            draftAction,
            parentMessage,
            draftFields
        )

        expectParticipantsMapping(messageParticipantsFacade)
        val expectedPlainText = "\nPlain Text"
        coEvery { messageContentFacade.convertHtmlToPlainText(any()) } returns expectedPlainText
        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs

        val firstMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value),
            isSubmittable = true,
            quotedHtmlContent = QuotedHtmlContent(
                original = OriginalHtmlQuote("original-html"),
                styled = StyledHtmlQuote("quoted-html")
            )
        )

        val finalMainState = firstMainState.copy(
            quotedHtmlContent = null
        )

        val expectedEffectsState = ComposerState.Effects.initial().copy(
            focusTextBody = Effect.of(Unit)
        )

        val expectedBody = "${defaultDraftFields.body.value}$expectedPlainText"

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(main = firstMainState, effects = expectedEffectsState, actualStates = awaitItem())

            viewModel.submitAction(ComposerAction2.RespondInline)
            advanceUntilIdle()

            verifyStates(main = finalMainState, effects = expectedEffectsState, actualStates = awaitItem())
        }

        assertEquals(expectedBody, viewModel.bodyFieldText.text.toString())
    }

    @Test
    fun `should send message directly when no password or expiration is set`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)
        expectParticipantsMapping(messageParticipantsFacade)

        val recipients = listOf(RecipientUiModel.Valid(recipientsTo.first().address))
        val recipientsTo = recipientsTo.first().let { Recipient(it.address, it.name) }
        recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.TO)

        val draftFields = emptyDraftFields().copy(
            sender = defaultDraftFields.sender,
            recipientsTo = RecipientsTo(listOf(recipientsTo)),
            subject = Subject("Subject"),
            body = DraftBody("Body")
        )

        every { draftFacade.stopContinuousUpload() } just runs
        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()
        every { networkManagerMock.isConnectedToNetwork() } returns true

        val firstMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = true
        )

        val finalMainState = firstMainState.copy(
            loadingType = ComposerState.LoadingType.Save
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            closeComposerWithMessageSending = Effect.of(Unit)
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            viewModel.subjectTextField.edit { append(draftFields.subject.value) }
            viewModel.bodyFieldText.edit { append(draftFields.body.value) }

            viewModel.submitAction(ComposerAction2.SendMessage)
            advanceUntilIdle()

            verifyStates(main = firstMainState, actualStates = awaitItem())
            verifyStates(main = finalMainState, actualStates = awaitItem())
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }

        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(draftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = true))
            composerStateReducer.reduceNewState(any(), MainEvent.CoreLoadingToggled)
            composerStateReducer.reduceNewState(any(), EffectsEvent.SendEvent.OnSendMessage)
        }

        coVerify(exactly = 1) {
            draftFacade.stopContinuousUpload()
            messageSendingFacade.sendMessage(userId, messageId, draftFields)
        }
    }

    @Test
    fun `should schedule send when no password or expiration is set and user is offline`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)
        expectParticipantsMapping(messageParticipantsFacade)

        val recipients = listOf(RecipientUiModel.Valid(recipientsTo.first().address))
        val recipientsTo = recipientsTo.first().let { Recipient(it.address, it.name) }
        recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.TO)

        val draftFields = emptyDraftFields().copy(
            sender = defaultDraftFields.sender,
            recipientsTo = RecipientsTo(listOf(recipientsTo)),
            subject = Subject("Subject"),
            body = DraftBody("Body")
        )

        every { draftFacade.stopContinuousUpload() } just runs
        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()
        every { networkManagerMock.isConnectedToNetwork() } returns false

        val firstMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = true
        )

        val finalMainState = firstMainState.copy(
            loadingType = ComposerState.LoadingType.Save
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            closeComposerWithMessageSendingOffline = Effect.of(Unit)
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            viewModel.subjectTextField.edit { append(draftFields.subject.value) }
            viewModel.bodyFieldText.edit { append(draftFields.body.value) }

            viewModel.submitAction(ComposerAction2.SendMessage)
            advanceUntilIdle()

            verifyStates(main = firstMainState, actualStates = awaitItem())
            verifyStates(main = finalMainState, actualStates = awaitItem())
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }

        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(draftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = true))
            composerStateReducer.reduceNewState(any(), MainEvent.CoreLoadingToggled)
            composerStateReducer.reduceNewState(any(), EffectsEvent.SendEvent.OnOfflineSendMessage)
        }

        coVerify(exactly = 1) {
            draftFacade.stopContinuousUpload()
            messageSendingFacade.sendMessage(userId, messageId, draftFields)
        }
    }

    @Test
    fun `should notify sending to external recipients when expiration is set`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)
        expectParticipantsMapping(messageParticipantsFacade)

        val recipients = listOf(RecipientUiModel.Valid(recipientsTo.first().address))
        val mappedRecipients = recipientsTo.first().let { Recipient(it.address, it.name) }

        val sharedFlowExpiration = MutableSharedFlow<MessageExpirationTime>()
        coEvery { messageAttributesFacade.observeMessageExpiration(userId, messageId) } returns sharedFlowExpiration

        recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.TO)

        val mockList: List<Participant> = listOf(mockk())
        coEvery {
            messageParticipantsFacade.getExternalRecipients(userId, RecipientsTo(recipientsTo), any(), any())
        } returns mockList

        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()

        val draftFields = emptyDraftFields().copy(
            sender = defaultDraftFields.sender,
            recipientsTo = RecipientsTo(listOf(mappedRecipients)),
            subject = Subject("Subject"),
            body = DraftBody("Body")
        )

        val firstMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = true
        )

        val accessoriesState = ComposerState.Accessories.initial().copy(
            messageExpiresIn = 1.hours
        )

        val finalMainState = firstMainState.copy(
            loadingType = ComposerState.LoadingType.Save
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            confirmSendExpiringMessage = Effect.of(mockList)
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            sharedFlowExpiration.emit(MessageExpirationTime(userId, messageId, 1.hours))
            viewModel.subjectTextField.edit { append(draftFields.subject.value) }
            viewModel.bodyFieldText.edit { append(draftFields.body.value) }

            viewModel.submitAction(ComposerAction2.SendMessage)
            advanceUntilIdle()

            verifyStates(main = firstMainState, actualStates = awaitItem())
            verifyStates(main = firstMainState, accessories = accessoriesState, actualStates = awaitItem())
            verifyStates(main = finalMainState, accessories = accessoriesState, actualStates = awaitItem())
            verifyStates(
                main = finalMainState,
                accessories = accessoriesState,
                effects = finalEffectsState,
                actualStates = awaitItem()
            )
        }

        coVerify(exactly = 0) { messageSendingFacade.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `should send normally sending to external recipients when expiration and password are set`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)
        expectParticipantsMapping(messageParticipantsFacade)

        val recipients = listOf(RecipientUiModel.Valid(recipientsTo.first().address))
        val mappedRecipients = recipientsTo.first().let { Recipient(it.address, it.name) }

        val sharedFlowExpiration = MutableSharedFlow<MessageExpirationTime>()
        val expectedMessagePassword = MessagePassword(userId, messageId, "password", null)
        coEvery { messageAttributesFacade.observeMessageExpiration(userId, messageId) } returns sharedFlowExpiration

        val sharedFlowPassword = MutableSharedFlow<MessagePassword>()
        val expectedMessageExpiration = MessageExpirationTime(userId, messageId, 1.hours)
        coEvery { messageAttributesFacade.observeMessagePassword(userId, messageId) } returns sharedFlowPassword

        val mockList: List<Participant> = listOf(mockk())
        coEvery {
            messageParticipantsFacade.getExternalRecipients(userId, RecipientsTo(recipientsTo), any(), any())
        } returns mockList

        every { draftFacade.stopContinuousUpload() } just runs
        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()
        every { networkManagerMock.isConnectedToNetwork() } returns true

        val draftFields = emptyDraftFields().copy(
            sender = defaultDraftFields.sender,
            recipientsTo = RecipientsTo(listOf(mappedRecipients)),
            subject = Subject("Subject"),
            body = DraftBody("Body")
        )

        val firstMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = true
        )

        val firstAccessoriesState = ComposerState.Accessories.initial().copy(
            messageExpiresIn = 1.hours
        )

        val finalAccessoriesState = firstAccessoriesState.copy(
            isMessagePasswordSet = true
        )

        val finalMainState = firstMainState.copy(
            loadingType = ComposerState.LoadingType.Save
        )

        val finalEffectsState = ComposerState.Effects.initial().copy(
            closeComposerWithMessageSending = Effect.of(Unit)
        )

        val viewModel = viewModel()

        // When + Then
        recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.TO)

        viewModel.composerStates.test {
            sharedFlowExpiration.emit(expectedMessageExpiration)
            sharedFlowPassword.emit(expectedMessagePassword)
            viewModel.subjectTextField.edit { append(draftFields.subject.value) }
            viewModel.bodyFieldText.edit { append(draftFields.body.value) }

            viewModel.submitAction(ComposerAction2.SendMessage)
            advanceUntilIdle()

            verifyStates(main = firstMainState, actualStates = awaitItem())
            verifyStates(main = firstMainState, accessories = firstAccessoriesState, actualStates = awaitItem())
            verifyStates(main = firstMainState, accessories = finalAccessoriesState, actualStates = awaitItem())
            verifyStates(main = finalMainState, accessories = finalAccessoriesState, actualStates = awaitItem())
            verifyStates(
                main = finalMainState,
                accessories = finalAccessoriesState,
                effects = finalEffectsState,
                actualStates = awaitItem()
            )
        }

        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(draftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = false))
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = true))
            composerStateReducer.reduceNewState(any(), AccessoriesEvent.OnExpirationChanged(expectedMessageExpiration))
            composerStateReducer.reduceNewState(any(), AccessoriesEvent.OnPasswordChanged(expectedMessagePassword))
            composerStateReducer.reduceNewState(any(), MainEvent.CoreLoadingToggled)
            composerStateReducer.reduceNewState(any(), EffectsEvent.SendEvent.OnSendMessage)
        }

        coVerify(exactly = 1) {
            draftFacade.stopContinuousUpload()
            messageSendingFacade.sendMessage(userId, messageId, draftFields)
        }
    }

    @Test
    fun `should emit event when attachments are observed and trigger a draft save`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val sharedFlow = MutableSharedFlow<List<MessageAttachment>>()
        val expectedAttachment = MessageAttachmentSample.invoice
        every { attachmentsFacade.observeMessageAttachments(userId, messageId) } returns sharedFlow
        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val attachmentsState = ComposerState.Attachments.initial().copy(
            AttachmentGroupUiModel(
                limit = NO_ATTACHMENT_LIMIT,
                attachments = listOf(
                    AttachmentUiModel(
                        attachmentId = expectedAttachment.attachmentId.id,
                        fileName = "invoice",
                        extension = "pdf",
                        size = expectedAttachment.size,
                        mimeType = expectedAttachment.mimeType,
                        deletable = true
                    )
                )
            )
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            skipItems(1)

            sharedFlow.emit(listOf(expectedAttachment))
            advanceUntilIdle()

            verifyStates(main = expectedMainState, attachments = attachmentsState, actualStates = awaitItem())
        }

        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(defaultDraftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = false))
            composerStateReducer.reduceNewState(any(), AttachmentsEvent.OnListChanged(listOf(expectedAttachment)))
        }

        coVerify(exactly = 1) {
            draftFacade.storeDraft(userId, messageId, emptyDraftFields(), DraftAction.Compose)
        }
    }

    @Test
    fun `should emit event when sending errors are observed`() = runTest {
        // Given
        expectNewDraftReady(savedStateHandle)

        val sharedFlow = MutableSharedFlow<String>()
        val expectedError = "Some error"
        every { messageSendingFacade.observeAndFormatSendingErrors(userId, messageId) } returns sharedFlow

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val expectedEffectsState = ComposerState.Effects.initial().copy(
            sendingErrorEffect = Effect.of(TextUiModel(expectedError))
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            skipItems(1)

            sharedFlow.emit(expectedError)
            advanceUntilIdle()

            verifyStates(main = expectedMainState, effects = expectedEffectsState, actualStates = awaitItem())
        }

        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(defaultDraftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = false))
            composerStateReducer.reduceNewState(any(), EffectsEvent.SendEvent.OnSendingError(expectedError))
        }
    }

    private suspend fun TestScope.verifyDraftSave(
        viewModel: ComposerViewModel2,
        draftFields: DraftFields,
        shouldSaveDraft: Boolean,
        hasValidRecipients: Boolean = false,
        draftAction: DraftAction,
        withAction: () -> Unit
    ) {

        val finalMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value),
            loadingType = ComposerState.LoadingType.Save
        )

        val finalEffectsState = if (shouldSaveDraft) {
            ComposerState.Effects.initial().copy(closeComposerWithDraftSaved = Effect.of(Unit))
        } else {
            ComposerState.Effects.initial().copy(closeComposer = Effect.of(Unit))
        }

        viewModel.composerStates.test {
            skipItems(1)
            withAction()

            viewModel.submitAction(ComposerAction2.CloseComposer)
            advanceUntilIdle()

            verifyStates(main = finalMainState, actualStates = awaitItem())
            verifyStates(main = finalMainState, effects = finalEffectsState, actualStates = awaitItem())
        }

        // Then
        coVerifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(defaultDraftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), MainEvent.RecipientsChanged(areSubmittable = hasValidRecipients))
            composerStateReducer.reduceNewState(any(), MainEvent.CoreLoadingToggled)
            composerStateReducer.reduceNewState(
                any(), ComposerControlEvent.OnCloseRequest(hasDraftSaved = shouldSaveDraft)
            )
        }

        if (shouldSaveDraft) {
            coVerify(exactly = 1) {
                draftFacade.storeDraft(userId, messageId, draftFields, draftAction)
                draftFacade.forceUpload(userId, messageId)
            }
        } else {
            coVerify(exactly = 0) { draftFacade.storeDraft(any(), any(), any(), any()) }
            coVerify(exactly = 0) { draftFacade.forceUpload(any(), any()) }
        }
    }

    private fun emptyDraftFields() = DraftFields(
        sender = defaultDraftFields.sender,
        subject = Subject(""),
        body = DraftBody(""),
        recipientsTo = RecipientsTo(emptyList()),
        recipientsCc = RecipientsCc(emptyList()),
        recipientsBcc = RecipientsBcc(emptyList()),
        originalHtmlQuote = null
    )

    private fun expectNewDraftReady(savedStateHandle: SavedStateHandle) {
        expectStandaloneDraft(savedStateHandle)
        every { draftFacade.provideNewDraftId() } returns messageId
        coEvery { addressesFacade.getPrimarySenderEmail(userId) } returns defaultDraftFields.sender.right()

        coEvery {
            draftFacade.injectAddressSignature(
                userId, DraftBody(""),
                senderEmail = defaultDraftFields.sender, previousSenderEmail = null
            )
        } returns DraftBody("").right()
    }
}
