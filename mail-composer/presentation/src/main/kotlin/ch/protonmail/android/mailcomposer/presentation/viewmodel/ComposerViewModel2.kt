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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.model.decode
import ch.protonmail.android.mailcommon.domain.model.hasEmailData
import ch.protonmail.android.mailcommon.domain.system.BuildVersionProvider
import ch.protonmail.android.mailcomposer.domain.model.DecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses.Error
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.presentation.facade.AddressesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.AttachmentsFacade
import ch.protonmail.android.mailcomposer.presentation.facade.DraftFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageAttributesFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageContentFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageParticipantsFacade
import ch.protonmail.android.mailcomposer.presentation.facade.MessageSendingFacade
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerStates
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.ObservedComposerDataChanges
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.AccessoriesEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.AttachmentsEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerAction2
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerStateEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.CompositeEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.EffectsEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.MainEvent
import ch.protonmail.android.mailcomposer.presentation.model.toParticipantFields
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerStateReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.ui.form.EmailValidator
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ShouldRestrictWebViewHeight
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions", "LargeClass")
@HiltViewModel(assistedFactory = ComposerViewModel2.Factory::class)
class ComposerViewModel2 @AssistedInject constructor(
    private val draftFacade: DraftFacade,
    private val attachmentsFacade: AttachmentsFacade,
    private val messageAttributesFacade: MessageAttributesFacade,
    private val messageContentFacade: MessageContentFacade,
    private val messageParticipantsFacade: MessageParticipantsFacade,
    private val messageSendingFacade: MessageSendingFacade,
    private val addressesFacade: AddressesFacade,
    private val appInBackgroundState: AppInBackgroundState,
    private val networkManager: NetworkManager,
    private val savedStateHandle: SavedStateHandle,
    private val composerStateReducer: ComposerStateReducer,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @Assisted private val recipientsStateManager: RecipientsStateManager,
    private val shouldRestrictWebViewHeight: ShouldRestrictWebViewHeight,
    private val buildVersionProvider: BuildVersionProvider
) : ViewModel() {

    internal val subjectTextField = TextFieldState()
    internal val bodyFieldText = TextFieldState()

    private val primaryUserId = messageParticipantsFacade.observePrimaryUserId()
    private var pendingStoreDraftJob: Job? = null

    private val composerActionsChannel = Channel<ComposerAction2>(Channel.BUFFERED)

    private val mutableComposerStates = MutableStateFlow(
        ComposerStates(
            main = ComposerState.Main.initial(draftId = MessageId(resolveDraftId())),
            attachments = ComposerState.Attachments.initial(),
            accessories = ComposerState.Accessories.initial(),
            effects = ComposerState.Effects.initial()
        )
    )

    internal val composerStates = mutableComposerStates.asStateFlow()

    private val draftAction = savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey)
        ?.deserialize<DraftAction>()

    init {
        viewModelScope.launch {
            if (!setupInitialState(draftAction)) return@launch

            uploadDraftContinuouslyWhileInForeground()

            observeComposerFields()
            observeAttachments()
            observeMessagePassword()
            observeMessageExpiration()
            observePendingSendingError()

            processActions()
        }
    }

    internal fun submitAction(action: ComposerAction2) {
        viewModelScope.launch {
            composerActionsChannel.send(action)
            logViewModelAction(action, "Enqueued")
        }
    }

    private suspend fun setupInitialState(draftAction: DraftAction?): Boolean {
        emitNewStateFromOperation(MainEvent.InitialLoadingToggled)
        val inputDraftId = savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey)

        val draftActionForShare = savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey)
            ?.deserialize<DraftAction.PrefillForShare>()
        val restoredHandle = savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) == true

        when {
            restoredHandle -> {
                emitNewStateFromOperation(onComposerRestored())
                return false
            }

            inputDraftId != null -> prefillWithExistingDraft(inputDraftId)
            draftAction != null -> prefillForDraftAction(draftAction)
            draftActionForShare != null -> prefillForShareDraftAction(draftActionForShare)
            else -> setupStandaloneDraft(inputDraftId, draftAction)
        }

        emitNewStateFromOperation(MainEvent.LoadingDismissed)

        return true
    }

    @OptIn(FlowPreview::class)
    private fun observeComposerFields() {
        val combinedFlow = combine(
            composerStates.map { it.main.senderUiModel }.distinctUntilChanged(),
            recipientsStateManager.recipients,
            snapshotFlow { subjectTextField.text },
            snapshotFlow { bodyFieldText.text }
        ) { sender, recipients, subject, body ->
            ObservedComposerDataChanges(
                SenderEmail(sender.email),
                recipients,
                Subject(subject.toString().stripNewLines()),
                DraftBody(body.toString())
            )
        }

        // Keep recipients validations separate from actual saving.
        combinedFlow.map { it.recipients }
            .distinctUntilChanged()
            .onEach {
                emitNewStateFromOperation(MainEvent.RecipientsChanged(recipientsStateManager.hasValidRecipients()))
            }
            .launchIn(viewModelScope)

        combinedFlow.debounce(timeout = 1.seconds).onEach { it ->
            pendingStoreDraftJob = viewModelScope.launch(defaultDispatcher) {
                Timber.tag("ComposerViewModel").d("Saving draft..")

                val (toParticipants, ccParticipants, bccParticipants) = it.recipients.toParticipantFields { recipient ->
                    messageParticipantsFacade.mapToParticipant(recipient)
                }

                val draftFields = DraftFields(
                    it.sender,
                    it.subject,
                    it.body,
                    RecipientsTo(toParticipants),
                    RecipientsCc(ccParticipants),
                    RecipientsBcc(bccParticipants),
                    composerStates.value.main.quotedHtmlContent?.original
                )

                if (shouldSkipSave(draftFields)) {
                    Timber.tag("ComposerViewModel").d("Not saving draft")
                    return@launch
                }

                draftFacade.storeDraft(
                    userId = primaryUserId.first(),
                    draftMessageId = currentMessageId(),
                    fields = draftFields,
                    action = currentDraftActionOrDefault()
                )

                savedStateHandle[ComposerScreen.HasSavedDraftKey] = true
                Timber.tag("ComposerViewModel").d("Draft saved.")
            }
        }.launchIn(viewModelScope)
    }

    private fun observeAttachments() {
        primaryUserId
            .flatMapLatest { userId -> attachmentsFacade.observeMessageAttachments(userId, currentMessageId()) }
            .onEach { emitNewStateFromOperation(AttachmentsEvent.OnListChanged(it)) }
            .launchIn(viewModelScope)
    }

    private fun observeMessagePassword() {
        primaryUserId
            .flatMapLatest { userId -> messageAttributesFacade.observeMessagePassword(userId, currentMessageId()) }
            .onEach { emitNewStateFromOperation(AccessoriesEvent.OnPasswordChanged(it)) }
            .launchIn(viewModelScope)
    }

    private fun observeMessageExpiration() {
        primaryUserId
            .flatMapLatest { userId -> messageAttributesFacade.observeMessageExpiration(userId, currentMessageId()) }
            .onEach { emitNewStateFromOperation(AccessoriesEvent.OnExpirationChanged(it)) }
            .launchIn(viewModelScope)
    }

    private fun observePendingSendingError() {
        primaryUserId
            .flatMapLatest { userId -> messageSendingFacade.observeAndFormatSendingErrors(userId, currentMessageId()) }
            .filterNotNull()
            .onEach { emitNewStateFromOperation(EffectsEvent.SendEvent.OnSendingError(it)) }
            .launchIn(viewModelScope)
    }

    private suspend fun setupStandaloneDraft(
        inputDraftId: String?,
        draftAction: DraftAction?,
        recipients: List<RecipientUiModel>? = null
    ) {
        addressesFacade.getPrimarySenderEmail(primaryUserId.first())
            .onLeft {
                return emitNewStateFromOperation(EffectsEvent.LoadingEvent.OnSenderAddressLoadingFailed)
            }
            .onRight { value ->
                emitNewStateFromOperation(MainEvent.SenderChanged(value))

                if (isCreatingEmptyDraft(inputDraftId, draftAction)) {
                    injectAddressSignature(value, previousSenderEmail = null)
                }

                recipients?.let { recipient ->
                    recipientsStateManager.updateRecipients(recipient, ContactSuggestionsField.TO)
                }
            }
    }

    private fun onComposerRestored(): EffectsEvent {
        // This is hit when process death occurs and the user could be in an inconsistent state:
        // Theoretically we can restore the draft from local storage, but it's not guaranteed that its content is
        // up to date and we don't know if it should overwrite the remote state.
        Timber.tag("ComposerViewModel").d("Restored Composer instance - navigating back.")
        return EffectsEvent.ComposerControlEvent.OnComposerRestored
    }

    private suspend fun prefillForDraftAction(draftAction: DraftAction) {
        val parentMessageId = draftAction.getParentMessageId()
            ?: return setupStandaloneDraft(null, draftAction, savedStateHandle.extractRecipient())

        val userId = primaryUserId.first()
        Timber.d("Opening composer for draft action ${draftAction::class.java.simpleName} / ${currentMessageId()}")

        val (parentMessage, draftFields) = draftFacade
            .parentMessageToDraftFields(userId, parentMessageId, draftAction)
            ?: return emitNewStateFromOperation(EffectsEvent.LoadingEvent.OnParentLoadingFailed)

        val senderValidationResult = addressesFacade.validateSenderAddress(userId, draftFields.sender).getOrNull()
            ?: return emitNewStateFromOperation(EffectsEvent.LoadingEvent.OnSenderAddressLoadingFailed)

        bodyFieldText.replaceText(draftFields.body.value, resetRange = true)
        subjectTextField.replaceText(draftFields.subject.value)

        recipientsStateManager.setFromParticipants(
            toRecipients = draftFields.recipientsTo.value,
            ccRecipients = draftFields.recipientsCc.value,
            bccRecipients = draftFields.recipientsBcc.value
        )
        val validatedSender = senderValidationResult.validAddress

        val quotedHtmlContent = draftFields.originalHtmlQuote.toQuotedContent()

        val shouldRestrictWebViewHeight = shouldRestrictWebViewHeight(null) &&
            buildVersionProvider.sdkInt() == Build.VERSION_CODES.P

        emitNewStateFromOperation(
            CompositeEvent.DraftContentReady(
                senderEmail = validatedSender.value,
                isDataRefreshed = true,
                senderValidationResult = senderValidationResult,
                quotedHtmlContent = quotedHtmlContent,
                shouldRestrictWebViewHeight = shouldRestrictWebViewHeight,
                forceBodyFocus = draftAction is DraftAction.Reply || draftAction is DraftAction.ReplyAll
            )
        )

        draftFacade.storeDraftWithParentAttachments(
            userId,
            currentMessageId(),
            parentMessage,
            validatedSender,
            draftAction
        )

        if (senderValidationResult is ValidateSenderAddress.ValidationResult.Invalid) {
            attachmentsFacade.reEncryptAttachments(
                userId = userId,
                messageId = currentMessageId(),
                previousSender = senderValidationResult.invalid,
                newSender = validatedSender
            ).onLeft {
                Timber.e("Failed to re-encrypt attachments: $it")
                handleReEncryptionFailed(userId, senderValidationResult.validAddress, currentMessageId())
            }
        }
    }

    private suspend fun handleReEncryptionFailed(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId
    ) {
        attachmentsFacade.deleteAllAttachments(userId, senderEmail, messageId)
        emitNewStateFromOperation(EffectsEvent.AttachmentEvent.ReEncryptError)
    }

    private suspend fun prefillWithExistingDraft(inputDraftId: String) {
        Timber.d("Opening composer with $inputDraftId / ${mutableComposerStates.value.main.draftId}")

        draftFacade.getDecryptedDraftFields(primaryUserId.first(), currentMessageId())
            .onRight { draftFields ->
                attachmentsFacade.storeExternalAttachments(primaryUserId.first(), currentMessageId())

                val fields = draftFields.draftFields
                bodyFieldText.replaceText(fields.body.value, resetRange = true)
                subjectTextField.replaceText(fields.subject.value)
                recipientsStateManager.setFromParticipants(
                    toRecipients = fields.recipientsTo.value,
                    ccRecipients = fields.recipientsCc.value,
                    bccRecipients = fields.recipientsBcc.value
                )

                val shouldRestrictWebViewHeight = shouldRestrictWebViewHeight(null) &&
                    buildVersionProvider.sdkInt() == Build.VERSION_CODES.P

                emitNewStateFromOperation(
                    CompositeEvent.DraftContentReady(
                        senderEmail = fields.sender.value,
                        isDataRefreshed = draftFields is DecryptedDraftFields.Remote,
                        senderValidationResult = ValidateSenderAddress.ValidationResult.Valid(fields.sender),
                        quotedHtmlContent = fields.originalHtmlQuote.toQuotedContent(),
                        shouldRestrictWebViewHeight = shouldRestrictWebViewHeight,
                        forceBodyFocus = false
                    )
                )
            }
            .onLeft {
                emitNewStateFromOperation(EffectsEvent.DraftEvent.OnDraftLoadingFailed)
            }
    }

    private suspend fun OriginalHtmlQuote?.toQuotedContent() = this?.let {
        QuotedHtmlContent(original = it, styled = messageContentFacade.styleQuotedHtml(it))
    }

    private suspend fun prefillForShareDraftAction(shareDraftAction: DraftAction.PrefillForShare) {
        val fileShareInfo = shareDraftAction.intentShareInfo.decode()
        val userId = primaryUserId.first()
        val senderEmail = addressesFacade.getPrimarySenderEmail(primaryUserId.first())
            .getOrNull()
            ?: return emitNewStateFromOperation(EffectsEvent.LoadingEvent.OnSenderAddressLoadingFailed)

        fileShareInfo.attachmentUris.takeIfNotEmpty()?.let { uris ->
            attachmentsFacade.storeAttachments(
                userId,
                currentMessageId(),
                senderEmail,
                uris.map { it.toUri() }
            ).onLeft { error ->
                Timber.e("Error storing attachment - Share Via flow - $error")
                emitNewStateFromOperation(EffectsEvent.AttachmentEvent.Error(error))
            }
        }

        if (fileShareInfo.hasEmailData()) {
            updateFieldsFromShareInfo(fileShareInfo)
        }

        val sharedDraftBody = DraftBody(fileShareInfo.emailBody ?: "")
        injectAddressSignature(
            senderEmail = senderEmail, draftBody = sharedDraftBody,
            previousSenderEmail = prevSenderEmail()
        )

        emitNewStateFromOperation(MainEvent.SenderChanged(senderEmail))
    }

    private fun updateFieldsFromShareInfo(intentShareInfo: IntentShareInfo) {
        recipientsStateManager.setFromRawRecipients(
            toRecipients = intentShareInfo.emailRecipientTo,
            ccRecipients = intentShareInfo.emailRecipientCc,
            bccRecipients = intentShareInfo.emailRecipientBcc
        )

        subjectTextField.replaceText(intentShareInfo.emailSubject.orEmpty())
    }

    private fun uploadDraftContinuouslyWhileInForeground() {
        appInBackgroundState.observe().onEach { isAppInBackground ->
            if (isAppInBackground) {
                Timber.d("App is in background, stop continuous upload")
                draftFacade.stopContinuousUpload()
            } else {
                Timber.d("App is in foreground, start continuous upload")
                draftFacade.startContinuousUpload(
                    primaryUserId.first(), currentMessageId(), currentDraftActionOrDefault(), this.viewModelScope
                )
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun injectAddressSignature(
        senderEmail: SenderEmail,
        previousSenderEmail: SenderEmail?,
        draftBody: DraftBody = DraftBody(bodyFieldText.text.toString())
    ) {
        draftFacade.injectAddressSignature(
            primaryUserId.first(),
            draftBody,
            senderEmail,
            previousSenderEmail
        ).getOrNull()
            ?.let { it: DraftBody -> bodyFieldText.replaceText(it.value, resetRange = false, keepSelection = true) }
    }

    private suspend fun processActions() {
        composerActionsChannel.consumeEach { action ->
            logViewModelAction(action, "Executing")
            when (action) {
                is ComposerAction2.ChangeSender -> onChangeSenderRequested()
                is ComposerAction2.SetSenderAddress -> onSetNewSender(action.sender)
                is ComposerAction2.RespondInline -> onRespondInline()

                is ComposerAction2.OpenExpirationSettings ->
                    emitNewStateFromOperation(EffectsEvent.SetExpirationReady)

                is ComposerAction2.SetMessageExpiration -> onExpirationSet(action.duration)

                is ComposerAction2.OpenFilePicker ->
                    emitNewStateFromOperation(EffectsEvent.AttachmentEvent.OnAddRequest)

                is ComposerAction2.StoreAttachments -> onStoreAttachments(action.uriList)
                is ComposerAction2.RemoveAttachment -> onAttachmentsRemoved(action.attachmentId)

                is ComposerAction2.CloseComposer -> onCloseComposer()
                is ComposerAction2.SendMessage -> handleOnSendMessage()

                is ComposerAction2.CancelSendWithNoSubject ->
                    emitNewStateFromOperation(EffectsEvent.SendEvent.OnCancelSendNoSubject)

                is ComposerAction2.ConfirmSendWithNoSubject -> onSendMessage(currentDraftFields())

                is ComposerAction2.CancelSendExpirationSetToExternal ->
                    emitNewStateFromOperation(MainEvent.LoadingDismissed)

                is ComposerAction2.ConfirmSendExpirationSetToExternal -> onSendMessage(currentDraftFields())

                is ComposerAction2.ClearSendingError -> onClearSendingError()
            }
            logViewModelAction(action, "Completed.")
        }
    }

    private suspend fun onRespondInline() {
        val quotedHtmlContent = composerStates.value.main.quotedHtmlContent ?: run {
            Timber.d("Expected quoteHtmlContent, got null")
            return
        }

        val plainTextQuotedContent = messageContentFacade.convertHtmlToPlainText(quotedHtmlContent.styled.value)

        bodyFieldText.edit {
            append(plainTextQuotedContent)
            this.selection = TextRange.Zero
        }

        emitNewStateFromOperation(MainEvent.OnQuotedHtmlRemoved)
    }

    private suspend fun onExpirationSet(expiration: Duration) {
        return messageAttributesFacade.saveMessageExpiration(
            userId = primaryUserId.first(),
            messageId = currentMessageId(),
            senderEmail = currentSenderEmail(),
            expiration = expiration
        ).fold(
            ifLeft = { emitNewStateFromOperation(EffectsEvent.ErrorEvent.OnSetExpirationError) },
            ifRight = { emitNewStateFromOperation(CompositeEvent.SetExpirationDismissed(expiration)) }
        )
    }

    private suspend fun onSetNewSender(sender: SenderUiModel) {
        val userId = primaryUserId.first()
        val newSender = SenderEmail(sender.email)

        attachmentsFacade.reEncryptAttachments(
            userId = userId,
            messageId = currentMessageId(),
            previousSender = currentSenderEmail(),
            newSender = newSender
        ).onLeft {
            Timber.e("Failed to re-encrypt attachments: $it")
            handleReEncryptionFailed(userId, newSender, currentMessageId())
        }

        emitNewStateFromOperation(CompositeEvent.UserChangedSender(newSender))

        val draftFields = currentDraftFields()
        injectAddressSignature(
            senderEmail = currentSenderEmail(),
            draftBody = DraftBody(draftFields.body.value),
            previousSenderEmail = prevSenderEmail()
        )
    }

    private suspend fun onChangeSenderRequested() {
        val addresses = addressesFacade.getSenderAddresses()
            .getOrElse {
                val event = when (it) {
                    Error.UpgradeToChangeSender -> EffectsEvent.ErrorEvent.OnSenderChangeFreeUserError
                    Error.FailedDeterminingUserSubscription,
                    Error.FailedGettingPrimaryUser -> EffectsEvent.ErrorEvent.OnSenderChangePermissionsError
                }

                return emitNewStateFromOperation(event)
            }
            .map { SenderUiModel(it.email) }

        return emitNewStateFromOperation(CompositeEvent.SenderAddressesListReady(addresses))
    }

    private suspend fun onClearSendingError() {
        messageSendingFacade.clearMessageSendingError(primaryUserId.first(), currentMessageId()).onLeft {
            Timber.e("Failed to clear SendingError: $it")
        }
    }

    private suspend fun onCloseComposer() {
        emitNewStateFromOperation(MainEvent.CoreLoadingToggled)
        pendingStoreDraftJob?.join()

        val draftFields = currentDraftFields()

        if (shouldSkipSave(draftFields)) {
            emitNewStateFromOperation(EffectsEvent.ComposerControlEvent.OnCloseRequest(hasDraftSaved = false))
        } else {
            val userId = primaryUserId.first()

            draftFacade.stopContinuousUpload()

            draftFacade.storeDraft(
                userId = primaryUserId.first(),
                draftMessageId = currentMessageId(),
                fields = draftFields,
                action = currentDraftActionOrDefault()
            )

            draftFacade.forceUpload(userId, currentMessageId())
            emitNewStateFromOperation(EffectsEvent.ComposerControlEvent.OnCloseRequest(hasDraftSaved = true))
        }
    }

    private suspend fun handleOnSendMessage() {
        emitNewStateFromOperation(MainEvent.CoreLoadingToggled)
        pendingStoreDraftJob?.join()

        val draftFields = currentDraftFields()
        if (draftFields.haveBlankSubject()) {
            return emitNewStateFromOperation(CompositeEvent.OnSendWithEmptySubject)
        }

        val accessoriesState = composerStates.value.accessories

        if (accessoriesState.messageExpiresIn != Duration.ZERO) {
            if (!accessoriesState.isMessagePasswordSet) {
                val externalRecipients = recipientsStateManager.recipients.value.let {
                    messageParticipantsFacade.getExternalRecipients(
                        userId = primaryUserId.first(),
                        recipientsTo = draftFields.recipientsTo,
                        recipientsCc = draftFields.recipientsCc,
                        recipientsBcc = draftFields.recipientsBcc
                    )
                }

                if (externalRecipients.isNotEmpty()) {
                    return emitNewStateFromOperation(
                        EffectsEvent.SendEvent.OnSendExpiringToExternalRecipients(externalRecipients)
                    )
                }
            }
        }

        onSendMessage(draftFields, toggleLoading = false)
    }

    private suspend fun onSendMessage(draftFields: DraftFields, toggleLoading: Boolean = true) {
        if (toggleLoading) emitNewStateFromOperation(MainEvent.CoreLoadingToggled)
        pendingStoreDraftJob?.join()

        draftFacade.stopContinuousUpload()
        messageSendingFacade.sendMessage(primaryUserId.first(), currentMessageId(), draftFields)

        if (networkManager.isConnectedToNetwork()) {
            emitNewStateFromOperation(EffectsEvent.SendEvent.OnSendMessage)
        } else {
            emitNewStateFromOperation(EffectsEvent.SendEvent.OnOfflineSendMessage)
        }
    }

    private suspend fun onStoreAttachments(uriList: List<Uri>) {
        attachmentsFacade.storeAttachments(primaryUserId.first(), currentMessageId(), currentSenderEmail(), uriList)
            .onLeft { error ->
                Timber.e("Error storing attachment - User flow - $error")
                emitNewStateFromOperation(EffectsEvent.AttachmentEvent.Error(error))
            }
    }

    private suspend fun onAttachmentsRemoved(attachmentId: AttachmentId) {
        attachmentsFacade.deleteAttachment(
            primaryUserId.first(),
            currentMessageId(),
            currentSenderEmail(),
            attachmentId
        ).onLeft { Timber.e("Failed to delete attachment: $it") }
    }

    @Suppress("ReturnCount")
    private suspend fun shouldSkipSave(draftFields: DraftFields): Boolean {
        val accessoriesState = composerStates.value.accessories
        val attachmentsState = composerStates.value.attachments

        if (accessoriesState != ComposerState.Accessories.initial() ||
            attachmentsState != ComposerState.Attachments.initial()
        ) {
            return false
        }

        if (!draftFields.haveBlankSubject()) return false

        if (draftFields.haveBlankRecipients() &&
            draftFields.haveBlankSubject() &&
            draftFields.body.value.isEmpty()
        ) {
            return true
        }

        if (draftFields.haveBlankRecipients()) {
            val signatureBody = draftFacade.injectAddressSignature(
                userId = primaryUserId.first(),
                draftBody = DraftBody(""),
                senderEmail = currentSenderEmail(),
                previousSenderEmail = null
            ).getOrNull()?.value

            return draftFields.body.value == signatureBody
        }

        return false
    }

    private fun currentMessageId() = mutableComposerStates.value.main.draftId
    private fun currentSenderEmail() = SenderEmail(mutableComposerStates.value.main.senderUiModel.email)
    private fun prevSenderEmail() = mutableComposerStates.value.main.prevSenderEmail
    private fun currentDraftActionOrDefault() = draftAction ?: DraftAction.Compose
    private fun resolveDraftId() = savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey)
        ?: draftFacade.provideNewDraftId().id

    private suspend fun currentDraftFields() = withContext(defaultDispatcher) {
        val (toParticipants, ccParticipants, bccParticipants) =
            recipientsStateManager.recipients.value.toParticipantFields { recipient ->
                messageParticipantsFacade.mapToParticipant(recipient)
            }

        DraftFields(
            currentSenderEmail(),
            Subject(subjectTextField.text.toString().stripNewLines()),
            DraftBody(bodyFieldText.text.toString()),
            RecipientsTo(toParticipants),
            RecipientsCc(ccParticipants),
            RecipientsBcc(bccParticipants),
            composerStates.value.main.quotedHtmlContent?.original
        )
    }

    private fun emitNewStateFromOperation(event: ComposerStateEvent) {
        mutableComposerStates.update { composerStateReducer.reduceNewState(it, event) }
    }

    private fun String.stripNewLines() = this.replace("[\n\r]".toRegex(), " ")

    private fun logViewModelAction(action: ComposerAction2, message: String) {
        Timber
            .tag("ComposerViewModel")
            .d("Action ${action::class.java.simpleName} ${System.identityHashCode(action)} - $message")
    }

    @AssistedFactory
    interface Factory {

        fun create(recipientsStateManager: RecipientsStateManager): ComposerViewModel2
    }
}

private fun TextFieldState.replaceText(
    text: String,
    resetRange: Boolean = false,
    keepSelection: Boolean = false
) {
    val oldSelection = selection
    clearText()
    edit {
        append(text)
        when {
            resetRange -> selection = TextRange.Zero
            keepSelection -> {
                val newStart = oldSelection.start.coerceIn(0, text.length)
                val newEnd = oldSelection.end.coerceIn(0, text.length)
                selection = TextRange(newStart, newEnd)
            }
            else -> Unit
        }
    }
}

private fun SavedStateHandle.extractRecipient(): List<RecipientUiModel>? {
    return get<String>(ComposerScreen.SerializedDraftActionKey)?.deserialize<DraftAction>()
        .let { it as? DraftAction.ComposeToAddresses }
        ?.let {
            it.recipients.map { recipient ->
                when {
                    EmailValidator.isValidEmail(recipient) -> RecipientUiModel.Valid(recipient)
                    else -> RecipientUiModel.Invalid(recipient)
                }
            }
        }
}

private fun isCreatingEmptyDraft(inputDraftId: String?, draftAction: DraftAction?): Boolean =
    inputDraftId == null && (draftAction == null || draftAction is DraftAction.ComposeToAddresses)
