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

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.system.BuildVersionProvider
import ch.protonmail.android.mailcommon.domain.util.toUrlSafeBase64String
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.DecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress.ValidationFailure.CouldNotValidate
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.facade.AddressesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.AttachmentsFacade
import ch.protonmail.android.mailcomposer.presentation.facade.DraftFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageAttributesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageContentFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageParticipantsFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageSendingFacade
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsState
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.CompositeEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.EffectsEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.MainEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.MainEvent.RecipientsChanged
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerStateReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.defaultDraftFields
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.defaultRecipientsState
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectComposeToAddressDraft
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectDraftAction
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectExistingDraft
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectParticipantsMapping
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectRestoredState
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectShareViaData
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.expectStandaloneDraft
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.messageId
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.parentMessageId
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.recipientsBcc
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.recipientsCc
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.recipientsTo
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.styledHtml
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.userId
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2SharedTestData.verifyStates
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.usecase.ShouldRestrictWebViewHeight
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verifySequence
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.proton.core.network.domain.NetworkManager
import me.proton.core.util.kotlin.serialize
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ComposerViewModel2Test {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val draftFacade = mockk<DraftFacade> {
        every { this@mockk.startContinuousUpload(userId, messageId, DraftAction.Compose, any()) } just runs
    }
    private val attachmentsFacade = mockk<AttachmentsFacade>()
    private val addressesFacade = mockk<AddressesFacade>()
    private val messageAttributesFacade = mockk<MessageAttributesFacade>()
    private val messageContentFacade = mockk<MessageContentFacade>()
    private val messageParticipantsFacade = mockk<MessageParticipantsFacade> {
        every { this@mockk.observePrimaryUserId() } returns flowOf(userId)
    }
    private val messageSendingFacade = mockk<MessageSendingFacade>()

    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { this@mockk.observe() } returns flowOf(false)
    }
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val networkManagerMock = mockk<NetworkManager>()
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
    fun `should close composer when restored from handle (process death)`() = runTest {
        // Given
        expectRestoredState(savedStateHandle)

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            loadingType = ComposerState.LoadingType.Initial
        )

        val expectedEffectsState = ComposerState.Effects.initial().copy(
            closeComposer = Effect.of(Unit)
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            verifyStates(main = expectedMainState, effects = expectedEffectsState, actualStates = awaitItem())
        }

        // Then
        verifySequence {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), EffectsEvent.ComposerControlEvent.OnComposerRestored)
        }
        confirmVerified(composerStateReducer)
    }

    @Test
    fun `should prefill composer with existing draft id (remote data)`() = runTest {
        // Given
        expectDraftLoaded(fromRemote = true, draftAction = DraftAction.Compose)

        val expectedEvent = CompositeEvent.DraftContentReady(
            senderEmail = defaultDraftFields.sender.value,
            isDataRefreshed = true,
            senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(defaultDraftFields.sender),
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml),
            shouldRestrictWebViewHeight = false,
            forceBodyFocus = false
        )

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value),
            isSubmittable = true,
            loadingType = ComposerState.LoadingType.None,
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml)
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(main = expectedMainState, actualStates = awaitItem())
        }

        advanceUntilIdle()

        assertEquals(defaultDraftFields.body.value, viewModel.bodyFieldText.text.toString())
        assertEquals(defaultDraftFields.subject.value, viewModel.subjectTextField.text.toString())

        coVerify(ordering = Ordering.SEQUENCE) {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            draftFacade.getDecryptedDraftFields(userId, messageId)
            messageContentFacade.styleQuotedHtml(defaultDraftFields.originalHtmlQuote!!)

            composerStateReducer.reduceNewState(any(), expectedEvent)
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            draftFacade.startContinuousUpload(userId, messageId, DraftAction.Compose, any())

            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = true))
            draftFacade.storeDraft(userId, messageId, defaultDraftFields, DraftAction.Compose)
        }

        confirmVerified(composerStateReducer, draftFacade)
    }

    @Test
    fun `should prefill composer with existing draft id and emit warning if from local data only`() = runTest {
        // Given
        expectDraftLoaded(fromRemote = false, draftAction = DraftAction.Compose)

        val expectedEvent = CompositeEvent.DraftContentReady(
            senderEmail = defaultDraftFields.sender.value,
            isDataRefreshed = false,
            senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(defaultDraftFields.sender),
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml),
            shouldRestrictWebViewHeight = false,
            forceBodyFocus = false
        )

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value),
            isSubmittable = true,
            loadingType = ComposerState.LoadingType.None,
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml)
        )

        val expectedEffectsState = ComposerState.Effects.initial().copy(
            warning = Effect.of(TextUiModel(R.string.composer_warning_local_data_shown))
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(main = expectedMainState, effects = expectedEffectsState, actualStates = awaitItem())
        }

        advanceUntilIdle()

        assertEquals(defaultDraftFields.body.value, viewModel.bodyFieldText.text.toString())
        assertEquals(defaultDraftFields.subject.value, viewModel.subjectTextField.text.toString())

        coVerify(ordering = Ordering.SEQUENCE) {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            draftFacade.getDecryptedDraftFields(userId, messageId)
            messageContentFacade.styleQuotedHtml(defaultDraftFields.originalHtmlQuote!!)

            composerStateReducer.reduceNewState(any(), expectedEvent)
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            draftFacade.startContinuousUpload(userId, messageId, DraftAction.Compose, any())

            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = true))
            draftFacade.storeDraft(userId, messageId, defaultDraftFields, DraftAction.Compose)
        }

        confirmVerified(composerStateReducer, draftFacade)
    }

    @Test
    fun `should prefill composer for a given draft action (reply)`() = runTest {
        // Given
        val draftAction = DraftAction.Reply(parentMessageId)
        val parentMessage = mockk<MessageWithDecryptedBody>()
        val draftFields = defaultDraftFields.copy(
            recipientsCc = RecipientsCc(emptyList()),
            recipientsBcc = RecipientsBcc(emptyList())
        )

        setupDraftAction(draftAction, parentMessage, draftFields)
        relaxMessageObservers()
        expectParticipantsMapping(messageParticipantsFacade)
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs

        val expectedEvent = CompositeEvent.DraftContentReady(
            senderEmail = draftFields.sender.value,
            isDataRefreshed = true,
            senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(draftFields.sender),
            quotedHtmlContent = QuotedHtmlContent(draftFields.originalHtmlQuote!!, styledHtml),
            shouldRestrictWebViewHeight = false,
            forceBodyFocus = true
        )

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = true,
            loadingType = ComposerState.LoadingType.None,
            quotedHtmlContent = QuotedHtmlContent(draftFields.originalHtmlQuote!!, styledHtml)
        )

        val expectedEffectState = ComposerState.Effects.initial().copy(
            focusTextBody = Effect.of(Unit)
        )

        val expectedRecipientTo = listOf(
            draftFields.recipientsTo.value.first().let { RecipientUiModel.Valid(it.address) }
        ).toImmutableList()

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            advanceUntilIdle()
            verifyStates(main = expectedMainState, effects = expectedEffectState, actualStates = awaitItem())
        }

        coVerify {
            draftFacade.provideNewDraftId()
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            draftFacade.parentMessageToDraftFields(userId, parentMessageId, draftAction)
            messageContentFacade.styleQuotedHtml(draftFields.originalHtmlQuote!!)

            composerStateReducer.reduceNewState(any(), expectedEvent)
            draftFacade.storeDraftWithParentAttachments(
                userId, messageId, parentMessage, draftFields.sender, draftAction
            )
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            draftFacade.startContinuousUpload(userId, messageId, draftAction, any())

            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = true))
            draftFacade.storeDraft(userId, messageId, draftFields, draftAction)
        }

        confirmVerified(draftFacade, composerStateReducer)

        assertEquals(draftFields.body.value, viewModel.bodyFieldText.text.toString())
        assertEquals(draftFields.subject.value, viewModel.subjectTextField.text.toString())
        assertEquals(
            recipientsStateManager.recipients.value,
            RecipientsState.Empty.copy(toRecipients = expectedRecipientTo)
        )
    }

    @Test
    fun `should prefill composer for a given draft action (reply all)`() = runTest {
        // Given
        val draftAction = DraftAction.ReplyAll(parentMessageId)
        val parentMessage = mockk<MessageWithDecryptedBody>()

        setupDraftAction(draftAction, parentMessage)
        relaxMessageObservers()
        expectParticipantsMapping(messageParticipantsFacade)
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs

        val expectedEvent = CompositeEvent.DraftContentReady(
            senderEmail = defaultDraftFields.sender.value,
            isDataRefreshed = true,
            senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(defaultDraftFields.sender),
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml),
            shouldRestrictWebViewHeight = false,
            forceBodyFocus = true
        )

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value),
            isSubmittable = true,
            loadingType = ComposerState.LoadingType.None,
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml)
        )

        val expectedEffectState = ComposerState.Effects.initial().copy(
            focusTextBody = Effect.of(Unit)
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            advanceUntilIdle()
            verifyStates(main = expectedMainState, effects = expectedEffectState, actualStates = awaitItem())
        }

        coVerify(ordering = Ordering.SEQUENCE) {
            draftFacade.provideNewDraftId()
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            draftFacade.parentMessageToDraftFields(userId, parentMessageId, draftAction)
            messageContentFacade.styleQuotedHtml(defaultDraftFields.originalHtmlQuote!!)

            composerStateReducer.reduceNewState(any(), expectedEvent)
            draftFacade.storeDraftWithParentAttachments(
                userId, messageId, parentMessage, defaultDraftFields.sender, draftAction
            )
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            draftFacade.startContinuousUpload(userId, messageId, draftAction, any())

            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = true))
            draftFacade.storeDraft(userId, messageId, defaultDraftFields, draftAction)
        }

        assertEquals(defaultDraftFields.body.value, viewModel.bodyFieldText.text.toString())
        assertEquals(defaultDraftFields.subject.value, viewModel.subjectTextField.text.toString())
        assertEquals(recipientsStateManager.recipients.value, defaultRecipientsState)
    }

    @Test
    fun `should prefill composer for a given draft action (forward)`() = runTest {
        // Given
        val draftAction = DraftAction.Forward(parentMessageId)
        val parentMessage = mockk<MessageWithDecryptedBody>()

        val draftFields = defaultDraftFields.copy(
            recipientsTo = RecipientsTo(emptyList()),
            recipientsCc = RecipientsCc(emptyList()),
            recipientsBcc = RecipientsBcc(emptyList())
        )

        setupDraftAction(draftAction, parentMessage, draftFields)
        relaxMessageObservers()
        expectParticipantsMapping(messageParticipantsFacade)
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = false,
            loadingType = ComposerState.LoadingType.None,
            quotedHtmlContent = QuotedHtmlContent(draftFields.originalHtmlQuote!!, styledHtml)
        )

        val expectedEffectState = ComposerState.Effects.initial().copy(
            focusTextBody = Effect.empty()
        )

        val expectedEvent = CompositeEvent.DraftContentReady(
            senderEmail = defaultDraftFields.sender.value,
            isDataRefreshed = true,
            senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(defaultDraftFields.sender),
            quotedHtmlContent = QuotedHtmlContent(defaultDraftFields.originalHtmlQuote!!, styledHtml),
            shouldRestrictWebViewHeight = false,
            forceBodyFocus = false
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            advanceUntilIdle()
            verifyStates(main = expectedMainState, effects = expectedEffectState, actualStates = awaitItem())
        }

        coVerify(ordering = Ordering.SEQUENCE) {
            draftFacade.provideNewDraftId()
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            draftFacade.parentMessageToDraftFields(userId, parentMessageId, draftAction)
            messageContentFacade.styleQuotedHtml(draftFields.originalHtmlQuote!!)

            composerStateReducer.reduceNewState(any(), expectedEvent)
            draftFacade.storeDraftWithParentAttachments(
                userId, messageId, parentMessage, draftFields.sender, draftAction
            )
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            draftFacade.startContinuousUpload(userId, messageId, draftAction, any())

            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = false))
            draftFacade.storeDraft(userId, messageId, draftFields, draftAction)
        }

        assertEquals(draftFields.body.value, viewModel.bodyFieldText.text.toString())
        assertEquals(draftFields.subject.value, viewModel.subjectTextField.text.toString())
        assertEquals(recipientsStateManager.recipients.value, RecipientsState.Empty)
    }

    @Test
    fun `should prefill composer and limit web view height when the FF is ON and the Android version is 9`() = runTest {
        // Given
        val draftAction = DraftAction.Forward(parentMessageId)
        val parentMessage = mockk<MessageWithDecryptedBody>()

        val draftFields = defaultDraftFields.copy(
            recipientsTo = RecipientsTo(emptyList()),
            recipientsCc = RecipientsCc(emptyList()),
            recipientsBcc = RecipientsBcc(emptyList())
        )

        setupDraftAction(draftAction, parentMessage, draftFields)
        relaxMessageObservers()
        expectParticipantsMapping(messageParticipantsFacade)
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs
        every { shouldRestrictWebViewHeight(null) } returns true
        every { buildVersionProvider.sdkInt() } returns Build.VERSION_CODES.P

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(draftFields.sender.value),
            isSubmittable = false,
            loadingType = ComposerState.LoadingType.None,
            quotedHtmlContent = QuotedHtmlContent(draftFields.originalHtmlQuote!!, styledHtml),
            shouldRestrictWebViewHeight = true
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            advanceUntilIdle()
            verifyStates(main = expectedMainState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should notify draft loading failure on existing draft action (parent message loading, exit)`() = runTest {
        // Given
        val draftAction = DraftAction.Forward(parentMessageId)
        setupDraftAction(draftAction, mockk(), defaultDraftFields)

        relaxMessageObservers()
        coEvery { draftFacade.parentMessageToDraftFields(any(), any(), any()) } returns null
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs

        val expectedEffectState = ComposerState.Effects.initial().copy(
            exitError = Effect.of(TextUiModel(R.string.composer_error_loading_parent_message))
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(effects = expectedEffectState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should notify and exit composer on sender validation unrecoverable failure`() = runTest {
        // Given
        val draftAction = DraftAction.Forward(parentMessageId)
        setupDraftAction(draftAction, mockk(), defaultDraftFields)
        relaxMessageObservers()

        coEvery {
            draftFacade.parentMessageToDraftFields(any(), any(), any())
        } returns Pair(mockk(), defaultDraftFields)

        coEvery { addressesFacade.validateSenderAddress(any(), any()) } returns CouldNotValidate.left()
        coEvery { draftFacade.startContinuousUpload(userId, messageId, draftAction, any()) } just runs

        val expectedEffectState = ComposerState.Effects.initial().copy(
            exitError = Effect.of(TextUiModel(R.string.composer_error_invalid_sender))
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(effects = expectedEffectState, actualStates = awaitItem())
        }
    }

    @Test
    fun `should notify draft loading failure`() = runTest {
        // Given
        expectExistingDraft(savedStateHandle, messageId.id)
        relaxMessageObservers()
        coEvery { draftFacade.getDecryptedDraftFields(userId, messageId) } returns DataError.Local.NoDataCached.left()

        val expectedEffectState = ComposerState.Effects.initial().copy(
            error = Effect.of(TextUiModel(R.string.composer_error_loading_draft))
        )

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(effects = expectedEffectState, actualStates = awaitItem())
        }

        assertTrue { viewModel.bodyFieldText.text.isEmpty() }
        assertTrue { viewModel.subjectTextField.text.isEmpty() }

        coVerify(ordering = Ordering.SEQUENCE) {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), EffectsEvent.DraftEvent.OnDraftLoadingFailed)
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = false))
        }

        confirmVerified(composerStateReducer)
    }

    @Test
    fun `should prefill with the share via data`() = runTest {
        // Given
        every { draftFacade.provideNewDraftId() } returns messageId
        relaxMessageObservers()
        expectParticipantsMapping(messageParticipantsFacade)

        val draftBody = DraftBody("Body message")
        val draftBodyWithSignature = DraftBody("Body message + signature")
        val intentShareData = IntentShareInfo.Empty.copy(
            attachmentUris = emptyList(),
            emailSubject = "Subject".toUrlSafeBase64String(),
            emailRecipientTo = recipientsTo.map { it.address.toUrlSafeBase64String() },
            emailRecipientCc = recipientsCc.map { it.address.toUrlSafeBase64String() },
            emailRecipientBcc = recipientsBcc.map { it.address.toUrlSafeBase64String() },
            emailBody = draftBody.value.toUrlSafeBase64String(),
            encoded = true
        )

        val expectedState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value),
            isSubmittable = true
        )

        expectShareViaData(savedStateHandle, DraftAction.PrefillForShare(intentShareData).serialize())
        every { draftFacade.provideNewDraftId() } returns messageId
        coEvery { addressesFacade.getPrimarySenderEmail(userId) } returns defaultDraftFields.sender.right()
        coEvery {
            draftFacade.injectAddressSignature(userId, draftBody, defaultDraftFields.sender, null)
        } returns draftBodyWithSignature.right()

        coEvery { draftFacade.storeDraft(userId, messageId, any(), any()) } returns Unit.right()
        every { savedStateHandle[ComposerScreen.HasSavedDraftKey] = true } just runs

        val viewModel = viewModel()

        // When + Then
        viewModel.composerStates.test {
            verifyStates(main = expectedState, actualStates = awaitItem())
        }

        assertEquals(draftBodyWithSignature.value, viewModel.bodyFieldText.text.toString())
        assertEquals(defaultDraftFields.subject.value, viewModel.subjectTextField.text.toString())
        assertEquals(recipientsStateManager.recipients.value, defaultRecipientsState)
    }

    @Test
    fun `should setup a standalone draft`() = runTest {
        // Given
        expectStandaloneDraft(savedStateHandle)
        every { draftFacade.provideNewDraftId() } returns messageId
        coEvery { addressesFacade.getPrimarySenderEmail(userId) } returns defaultDraftFields.sender.right()
        relaxMessageObservers()

        val expectedBodyWithSignature = DraftBody("Signature")
        coEvery {
            draftFacade.injectAddressSignature(userId, DraftBody(""), defaultDraftFields.sender, null)
        } returns expectedBodyWithSignature.right()

        coEvery {
            draftFacade.injectAddressSignature(userId, DraftBody("Signature"), defaultDraftFields.sender, null)
        } returns expectedBodyWithSignature.right()

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(defaultDraftFields.sender.value)
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            verifyStates(main = expectedMainState, actualStates = awaitItem())
        }

        // Then
        coVerify {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(defaultDraftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = false))
        }

        assertEquals(expectedBodyWithSignature.value, viewModel.bodyFieldText.text.toString())
        assertTrue(viewModel.subjectTextField.text.isEmpty())
    }

    @Test
    fun `should setup a standalone draft when composing to address`() = runTest {
        // Given
        val action = DraftAction.ComposeToAddresses(listOf("123@321.1"))
        val initialDraftFields = DraftFields(
            sender = SenderEmail("sender@email.com"),
            subject = Subject(""),
            body = DraftBody("Signature"),
            recipientsTo = RecipientsTo(recipientsTo),
            recipientsCc = RecipientsCc(emptyList()),
            recipientsBcc = RecipientsBcc(emptyList()),
            originalHtmlQuote = null
        )
        expectParticipantsMapping(messageParticipantsFacade)
        expectComposeToAddressDraft(
            savedStateHandle,
            Json
                .encodeToString(DraftAction.serializer(), action)
        )
        every { savedStateHandle[ComposerScreen.HasSavedDraftKey] = true } just runs
        coEvery { draftFacade.storeDraft(userId, messageId, initialDraftFields, action) } returns Unit.right()
        every { draftFacade.provideNewDraftId() } returns messageId
        coEvery { addressesFacade.getPrimarySenderEmail(userId) } returns initialDraftFields.sender.right()
        every { draftFacade.startContinuousUpload(userId, messageId, action, any()) } just runs
        relaxMessageObservers()

        val expectedBodyWithSignature = DraftBody("Signature")
        coEvery {
            draftFacade.injectAddressSignature(userId, DraftBody(""), initialDraftFields.sender, null)
        } returns expectedBodyWithSignature.right()

        val expectedMainState = ComposerState.Main.initial(messageId).copy(
            senderUiModel = SenderUiModel(initialDraftFields.sender.value),
            isSubmittable = true
        )

        val viewModel = viewModel()

        // When
        viewModel.composerStates.test {
            verifyStates(main = expectedMainState, actualStates = awaitItem())
        }

        // Then
        coVerify {
            composerStateReducer.reduceNewState(any(), MainEvent.InitialLoadingToggled)
            composerStateReducer.reduceNewState(any(), MainEvent.SenderChanged(initialDraftFields.sender))
            composerStateReducer.reduceNewState(any(), MainEvent.LoadingDismissed)
            composerStateReducer.reduceNewState(any(), RecipientsChanged(areSubmittable = true))
        }

        assertEquals(expectedBodyWithSignature.value, viewModel.bodyFieldText.text.toString())
        assertTrue(viewModel.subjectTextField.text.isEmpty())
    }

    private fun setupDraftAction(
        draftAction: DraftAction,
        parentMessage: MessageWithDecryptedBody,
        draftFields: DraftFields = defaultDraftFields
    ) {
        expectDraftAction(
            draftFacade,
            addressesFacade,
            messageContentFacade,
            savedStateHandle,
            draftAction,
            parentMessage,
            draftFields
        )
    }

    private fun expectDraftLoaded(fromRemote: Boolean, draftAction: DraftAction) {
        expectExistingDraft(savedStateHandle, messageId.id)
        relaxMessageObservers()
        expectParticipantsMapping(messageParticipantsFacade)

        val expectedDraftFields = if (fromRemote) {
            DecryptedDraftFields.Remote(defaultDraftFields)
        } else {
            DecryptedDraftFields.Local(defaultDraftFields)
        }

        coEvery { draftFacade.getDecryptedDraftFields(userId, messageId) } returns expectedDraftFields.right()
        coEvery { attachmentsFacade.storeExternalAttachments(userId, messageId) } just runs
        coEvery {
            messageContentFacade.styleQuotedHtml(defaultDraftFields.originalHtmlQuote!!)
        } returns styledHtml
        coEvery { draftFacade.storeDraft(userId, messageId, defaultDraftFields, draftAction) } returns Unit.right()
        every { savedStateHandle[ComposerScreen.HasSavedDraftKey] = true } just runs
    }

    private fun relaxMessageObservers() {
        every { attachmentsFacade.observeMessageAttachments(userId, messageId) } returns flowOf()
        coEvery { messageAttributesFacade.observeMessagePassword(userId, messageId) } returns flowOf()
        coEvery { messageAttributesFacade.observeMessageExpiration(userId, messageId) } returns flowOf()
        coEvery { messageSendingFacade.observeAndFormatSendingErrors(userId, messageId) } returns flowOf()
        every { attachmentsFacade.observeMessageAttachments(userId, messageId) } returns flowOf()
    }
}
