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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.model.decode
import ch.protonmail.android.mailcommon.domain.model.hasEmailData
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
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
import ch.protonmail.android.mailcomposer.domain.usecase.ClearMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAllAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploader
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses.Error
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
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithParentAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithRecipients
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSubject
import ch.protonmail.android.mailcomposer.domain.usecase.StoreExternalAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.domain.usecase.isInvalidDueToDisabledAddress
import ch.protonmail.android.mailcomposer.domain.usecase.isInvalidDueToPaidAddress
import ch.protonmail.android.mailcomposer.presentation.mapper.ParticipantMapper
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.DraftUiModel
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
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchContactGroups
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchDeviceContacts
import ch.protonmail.android.mailcontact.domain.usecase.featureflags.IsDeviceContactsSuggestionsEnabled
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.test.idlingresources.ComposerIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.network.domain.NetworkManager
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@Deprecated("Part of Composer V1, to be replaced with ComposerViewModel2")
@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val appInBackgroundState: AppInBackgroundState,
    private val storeAttachments: StoreAttachments,
    private val storeDraftWithBody: StoreDraftWithBody,
    private val storeDraftWithSubject: StoreDraftWithSubject,
    private val storeDraftWithAllFields: StoreDraftWithAllFields,
    private val storeDraftWithRecipients: StoreDraftWithRecipients,
    private val storeExternalAttachments: StoreExternalAttachments,
    private val getContacts: GetContacts,
    private val searchContacts: SearchContacts,
    private val searchDeviceContacts: SearchDeviceContacts,
    private val deviceContactsSuggestionsPrompt: DeviceContactsSuggestionsPrompt,
    private val searchContactGroups: SearchContactGroups,
    private val sortContactsForSuggestions: SortContactsForSuggestions,
    private val participantMapper: ParticipantMapper,
    private val reducer: ComposerReducer,
    private val isValidEmailAddress: IsValidEmailAddress,
    private val getPrimaryAddress: GetPrimaryAddress,
    private val getComposerSenderAddresses: GetComposerSenderAddresses,
    private val composerIdlingResource: ComposerIdlingResource,
    private val draftUploader: DraftUploader,
    private val observeMessageAttachments: ObserveMessageAttachments,
    private val observeMessageSendingError: ObserveMessageSendingError,
    private val clearMessageSendingError: ClearMessageSendingError,
    private val formatMessageSendingError: FormatMessageSendingError,
    private val sendMessage: SendMessage,
    private val networkManager: NetworkManager,
    private val getLocalMessageDecrypted: GetLocalMessageDecrypted,
    private val injectAddressSignature: InjectAddressSignature,
    private val parentMessageToDraftFields: ParentMessageToDraftFields,
    private val styleQuotedHtml: StyleQuotedHtml,
    private val storeDraftWithParentAttachments: StoreDraftWithParentAttachments,
    private val deleteAttachment: DeleteAttachment,
    private val deleteAllAttachments: DeleteAllAttachments,
    private val reEncryptAttachments: ReEncryptAttachments,
    private val observeMessagePassword: ObserveMessagePassword,
    private val validateSenderAddress: ValidateSenderAddress,
    private val saveMessageExpirationTime: SaveMessageExpirationTime,
    private val observeMessageExpirationTime: ObserveMessageExpirationTime,
    private val getExternalRecipients: GetExternalRecipients,
    private val convertHtmlToPlainText: ConvertHtmlToPlainText,
    isDeviceContactsSuggestionsEnabled: IsDeviceContactsSuggestionsEnabled,
    getDecryptedDraftFields: GetDecryptedDraftFields,
    savedStateHandle: SavedStateHandle,
    observePrimaryUserId: ObservePrimaryUserId,
    provideNewDraftId: ProvideNewDraftId,
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val searchContactsJobs = mutableMapOf<ContactSuggestionsField, Job>()
    private val mutableState = MutableStateFlow(
        ComposerDraftState.initial(
            MessageId(savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) ?: provideNewDraftId().id),
            // setting the passed recipient directly here in the initial value makes the UX a bit smoother
            to = savedStateHandle.extractRecipient() ?: emptyList()
        )
    )
    val state: StateFlow<ComposerDraftState> = mutableState
    val isBodyUpdating = MutableStateFlow(false)

    private val composerActionsChannel = Channel<ComposerAction>(Channel.BUFFERED)

    init {
        val inputDraftId = savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey)
        val draftAction = savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey)
            ?.deserialize<DraftAction>()
        val recipientAddress = savedStateHandle.extractRecipient()

        val draftActionForShare = savedStateHandle.get<String>(ComposerScreen.DraftActionForShareKey)
            ?.deserialize<DraftAction.PrefillForShare>()

        primaryUserId.onEach { userId ->
            getPrimaryAddress(userId)
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingDefaultSenderAddress) }
                .onRight {
                    emitNewStateFor(ComposerEvent.DefaultSenderReceived(SenderUiModel(it.email)))
                    if (isCreatingEmptyDraft(inputDraftId, draftAction)) {
                        injectAddressSignature(SenderEmail(it.email))
                    }
                    recipientAddress?.let { recipient ->
                        emitNewStateFor(onToChanged(ComposerAction.RecipientsToChanged(recipient)))
                    }
                }
        }.launchIn(viewModelScope)

        when {
            inputDraftId != null -> prefillWithExistingDraft(inputDraftId, getDecryptedDraftFields)
            draftAction != null -> prefillForDraftAction(draftAction)
            draftActionForShare != null -> prefillForShareDraftAction(draftActionForShare)
            else -> uploadDraftContinuouslyWhileInForeground(DraftAction.Compose)
        }

        observeMessageAttachments()
        observeSendingError()
        observeMessagePassword()
        observeMessageExpirationTime()
        observeDeviceContactsSuggestionsPromptEnabled()

        emitNewStateFor(ComposerEvent.OnIsDeviceContactsSuggestionsEnabled(isDeviceContactsSuggestionsEnabled()))

        viewModelScope.launch {
            processActions()
        }
    }

    private fun isCreatingEmptyDraft(inputDraftId: String?, draftAction: DraftAction?): Boolean =
        inputDraftId == null && (draftAction == null || draftAction is DraftAction.ComposeToAddresses)

    private fun prefillForShareDraftAction(shareDraftAction: DraftAction.PrefillForShare) {
        val fileShareInfo = shareDraftAction.intentShareInfo.decode()

        uploadDraftContinuouslyWhileInForeground(DraftAction.Compose)

        viewModelScope.launch {
            fileShareInfo.attachmentUris.takeIfNotEmpty()?.let { uris ->
                storeAttachments(
                    primaryUserId(),
                    currentMessageId(),
                    currentSenderEmail(),
                    uris.map { Uri.parse(it) }
                ).onLeft { error ->
                    if (error is StoreDraftWithAttachmentError.FileSizeExceedsLimit) {
                        emitNewStateFor(ComposerEvent.ErrorAttachmentsExceedSizeLimit)
                    }
                }
            }

            if (fileShareInfo.hasEmailData()) {
                emitNewStateFor(
                    ComposerEvent.PrefillDataReceivedViaShare(
                        prepareDraftFieldsFor(fileShareInfo).toDraftUiModel()
                    )
                )
            }
        }
    }

    private suspend fun prepareDraftFieldsFor(intentShareInfo: IntentShareInfo): DraftFields {
        val draftBody = DraftBody(intentShareInfo.emailBody ?: "")
        val subject = Subject(intentShareInfo.emailSubject ?: "")
        val recipientsTo = RecipientsTo(
            intentShareInfo.emailRecipientTo.takeIfNotEmpty()?.map {
                participantMapper.recipientUiModelToParticipant(RecipientUiModel.Valid(it), contactsOrEmpty())
            } ?: emptyList()
        )

        val recipientsCc = RecipientsCc(
            intentShareInfo.emailRecipientCc.takeIfNotEmpty()?.map {
                participantMapper.recipientUiModelToParticipant(RecipientUiModel.Valid(it), contactsOrEmpty())
            } ?: emptyList()
        )

        val recipientsBcc = RecipientsBcc(
            intentShareInfo.emailRecipientBcc.takeIfNotEmpty()?.map {
                participantMapper.recipientUiModelToParticipant(RecipientUiModel.Valid(it), contactsOrEmpty())
            } ?: emptyList()
        )

        return DraftFields(
            currentSenderEmail(),
            subject,
            draftBody,
            recipientsTo,
            recipientsCc,
            recipientsBcc,
            null
        )
    }

    private fun prefillForDraftAction(draftAction: DraftAction) {
        val parentMessageId = draftAction.getParentMessageId() ?: return
        Timber.d("Opening composer for draft action ${draftAction::class.java.simpleName} / ${currentMessageId()}")
        emitNewStateFor(ComposerEvent.OpenWithMessageAction(currentMessageId(), draftAction))

        viewModelScope.launch {
            val parentMessage = getLocalMessageDecrypted(primaryUserId(), parentMessageId)
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingParentMessageData) }
                .getOrNull()
                ?: return@launch

            val draftFields = parentMessageToDraftFields(primaryUserId(), parentMessage, draftAction)
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingParentMessageData) }
                .getOrNull()
                ?: return@launch

            val senderValidationResult = validateSenderAddress(primaryUserId(), draftFields.sender)
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingParentMessageData) }
                .getOrNull()
                ?: return@launch

            uploadDraftContinuouslyWhileInForeground(draftAction)
            val validatedSender = senderValidationResult.validAddress

            emitNewStateFor(
                ComposerEvent.PrefillDraftDataReceived(
                    draftUiModel = draftFields.copy(sender = validatedSender).toDraftUiModel(),
                    isDataRefreshed = true,
                    isBlockedSendingFromPmAddress = senderValidationResult.isInvalidDueToPaidAddress(),
                    isBlockedSendingFromDisabledAddress = senderValidationResult.isInvalidDueToDisabledAddress()
                )
            )
            storeDraftWithParentAttachments.invoke(
                primaryUserId(),
                currentMessageId(),
                parentMessage,
                validatedSender,
                draftAction
            )

            // User may skip editing Subject line, so we need to store it here.
            storeDraftWithSubject(
                primaryUserId(), currentMessageId(), validatedSender, draftFields.subject
            )

            if (senderValidationResult is ValidateSenderAddress.ValidationResult.Invalid) {
                reEncryptAttachments(
                    userId = primaryUserId(),
                    messageId = currentMessageId(),
                    previousSender = senderValidationResult.invalid,
                    newSenderEmail = validatedSender
                ).onLeft {
                    Timber.e("Failed to re-encrypt attachments: $it")
                    handleReEncryptionFailed()
                }
            }
        }
    }

    private fun prefillWithExistingDraft(inputDraftId: String, getDecryptedDraftFields: GetDecryptedDraftFields) {
        Timber.d("Opening composer with $inputDraftId / ${currentMessageId()}")
        emitNewStateFor(ComposerEvent.OpenExistingDraft(currentMessageId()))

        viewModelScope.launch {
            getDecryptedDraftFields(primaryUserId(), currentMessageId())
                .onRight { draftFields ->
                    uploadDraftContinuouslyWhileInForeground(DraftAction.Compose)
                    storeExternalAttachments(primaryUserId(), currentMessageId())
                    emitNewStateFor(
                        ComposerEvent.PrefillDraftDataReceived(
                            draftUiModel = draftFields.draftFields.toDraftUiModel(),
                            isDataRefreshed = draftFields is DecryptedDraftFields.Remote,
                            isBlockedSendingFromPmAddress = false,
                            isBlockedSendingFromDisabledAddress = false
                        )
                    )
                }
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingDraftData) }

        }
    }

    private fun DraftFields.toDraftUiModel(): DraftUiModel {
        val quotedHtml = this.originalHtmlQuote?.let {
            QuotedHtmlContent(it, styleQuotedHtml(it))
        }
        return DraftUiModel(this, quotedHtml)
    }

    override fun onCleared() {
        super.onCleared()
        composerIdlingResource.clear()
    }

    internal fun submit(action: ComposerAction) {
        viewModelScope.launch {
            logViewModelAction(action, "Enqueued.")
            composerActionsChannel.send(action)
        }
    }

    @Suppress("ComplexMethod")
    private suspend fun processActions() {
        composerActionsChannel.consumeEach { action ->
            logViewModelAction(action, "Executing.")
            composerIdlingResource.increment()
            when (action) {
                is ComposerAction.AttachmentsAdded -> onAttachmentsAdded(action)
                is ComposerAction.DraftBodyChanged -> onDraftBodyChanged(action)

                is ComposerAction.SenderChanged -> emitNewStateFor(onSenderChanged(action))
                is ComposerAction.SubjectChanged -> emitNewStateFor(onSubjectChanged(action))
                is ComposerAction.ChangeSenderRequested -> emitNewStateFor(onChangeSender())
                is ComposerAction.RecipientsToChanged -> emitNewStateFor(onToChanged(action))
                is ComposerAction.RecipientsCcChanged -> emitNewStateFor(onCcChanged(action))
                is ComposerAction.RecipientsBccChanged -> emitNewStateFor(onBccChanged(action))
                is ComposerAction.ContactSuggestionTermChanged -> onSearchTermChanged(
                    action.searchTerm,
                    action.suggestionsField
                )

                is ComposerAction.ContactSuggestionsDismissed -> emitNewStateFor(action)
                is ComposerAction.DeviceContactsPromptDenied -> onDeviceContactsPromptDenied()
                is ComposerAction.OnAddAttachments -> emitNewStateFor(action)
                is ComposerAction.OnCloseComposer -> emitNewStateFor(onCloseComposer(action))
                is ComposerAction.OnSendMessage -> emitNewStateFor(handleOnSendMessage(action))
                is ComposerAction.ConfirmSendingWithoutSubject -> emitNewStateFor(onSendMessage(action))
                is ComposerAction.RejectSendingWithoutSubject -> emitNewStateFor(action)
                is ComposerAction.RemoveAttachment -> onAttachmentsRemoved(action)
                is ComposerAction.OnSetExpirationTimeRequested -> emitNewStateFor(action)
                is ComposerAction.ExpirationTimeSet -> onExpirationTimeSet(action)
                is ComposerAction.SendExpiringMessageToExternalRecipientsConfirmed -> emitNewStateFor(
                    onSendMessage(action)
                )

                is ComposerAction.RespondInlineRequested -> onRespondInline()
            }
            composerIdlingResource.decrement()

            logViewModelAction(action, "Completed.")
        }
    }

    private fun uploadDraftContinuouslyWhileInForeground(draftAction: DraftAction) {
        appInBackgroundState.observe().onEach { isAppInBackground ->
            if (isAppInBackground) {
                Timber.d("App is in background, stop continuous upload")
                draftUploader.stopContinuousUpload()
            } else {
                Timber.d("App is in foreground, start continuous upload")
                draftUploader.startContinuousUpload(
                    primaryUserId(), currentMessageId(), draftAction, this.viewModelScope
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeMessageAttachments() {
        primaryUserId
            .flatMapLatest { userId -> observeMessageAttachments(userId, currentMessageId()) }
            .onEach { emitNewStateFor(ComposerEvent.OnAttachmentsUpdated(it)) }
            .launchIn(viewModelScope)
    }

    private fun observeSendingError() {
        primaryUserId
            .flatMapLatest { userId -> observeMessageSendingError(userId, currentMessageId()) }
            .onEach {
                formatMessageSendingError(it)?.run {
                    emitNewStateFor(ComposerEvent.OnSendingError(TextUiModel.Text(this)))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeMessagePassword() {
        primaryUserId
            .flatMapLatest { userId -> observeMessagePassword(userId, currentMessageId()) }
            .onEach { emitNewStateFor(ComposerEvent.OnMessagePasswordUpdated(it)) }
            .launchIn(viewModelScope)
    }

    private fun observeMessageExpirationTime() {
        primaryUserId
            .flatMapLatest { userId -> observeMessageExpirationTime(userId, currentMessageId()) }
            .onEach { emitNewStateFor(ComposerEvent.OnMessageExpirationTimeUpdated(it)) }
            .launchIn(viewModelScope)
    }

    @Suppress("FunctionMaxLength")
    private fun observeDeviceContactsSuggestionsPromptEnabled() {
        viewModelScope.launch {
            emitNewStateFor(
                ComposerEvent.OnIsDeviceContactsSuggestionsPromptEnabled(
                    deviceContactsSuggestionsPrompt.getPromptEnabled()
                )
            )
        }
    }

    fun validateEmailAddress(emailAddress: String): Boolean = isValidEmailAddress(emailAddress)

    fun clearSendingError() {
        viewModelScope.launch {
            clearMessageSendingError(primaryUserId(), currentMessageId()).onLeft {
                Timber.e("Failed to clear SendingError: $it")
            }
        }
    }

    private fun onAttachmentsAdded(action: ComposerAction.AttachmentsAdded) {
        viewModelScope.launch {
            storeAttachments(primaryUserId(), currentMessageId(), currentSenderEmail(), action.uriList).onLeft {
                if (it is StoreDraftWithAttachmentError.FileSizeExceedsLimit) {
                    emitNewStateFor(ComposerEvent.ErrorAttachmentsExceedSizeLimit)
                }
            }
        }
    }

    private fun onAttachmentsRemoved(action: ComposerAction.RemoveAttachment) {
        viewModelScope.launch {
            deleteAttachment(primaryUserId(), currentSenderEmail(), currentMessageId(), action.attachmentId)
                .onLeft { Timber.e("Failed to delete attachment: $it") }
        }
    }

    private fun onExpirationTimeSet(action: ComposerAction.ExpirationTimeSet) {
        viewModelScope.launch {
            saveMessageExpirationTime(primaryUserId(), currentMessageId(), currentSenderEmail(), action.duration).fold(
                ifLeft = { emitNewStateFor(ComposerEvent.ErrorSettingExpirationTime) },
                ifRight = { emitNewStateFor(action) }
            )
        }
    }

    private fun onDeviceContactsPromptDenied() {
        viewModelScope.launch {
            deviceContactsSuggestionsPrompt.setPromptDisabled()
        }
    }

    private suspend fun onCloseComposer(action: ComposerAction.OnCloseComposer): ComposerOperation {
        val draftFields = buildDraftFields()
        return when {
            draftFields.haveBlankRecipients() &&
                draftFields.haveBlankSubject() &&
                isBodyEmptyOrEqualsToSignatureAndFooter(currentDraftBody()) -> action

            else -> {
                draftUploader.stopContinuousUpload()
                storeDraftWithAllFields(primaryUserId(), currentMessageId(), draftFields)
                draftUploader.upload(primaryUserId(), currentMessageId())

                ComposerEvent.OnCloseWithDraftSaved
            }
        }
    }

    private suspend fun handleOnSendMessage(action: ComposerAction.OnSendMessage): ComposerOperation {
        val draftFields = buildDraftFields()
        return if (draftFields.haveBlankSubject()) {
            ComposerEvent.ConfirmEmptySubject
        } else if (state.value.messageExpiresIn != Duration.ZERO) {
            if (!state.value.isMessagePasswordSet) {
                val externalRecipients = draftFields.let {
                    getExternalRecipients(primaryUserId(), it.recipientsTo, it.recipientsCc, it.recipientsBcc)
                }
                if (externalRecipients.isNotEmpty()) {
                    ComposerEvent.ConfirmSendExpiringMessageToExternalRecipients(externalRecipients)
                } else {
                    onSendMessage(action)
                }
            } else {
                onSendMessage(action)
            }
        } else {
            onSendMessage(action)
        }
    }

    private fun onRespondInline() {
        state.value.fields.quotedBody?.let { quotedHtmlContent ->
            val plainTextQuotedContent = convertHtmlToPlainText(quotedHtmlContent.styled.value)
            emitNewStateFor(ComposerEvent.RespondInlineContent(plainTextQuotedContent))
        }
    }

    private suspend fun onSendMessage(action: ComposerOperation): ComposerOperation {
        val draftFields = buildDraftFields()
        return when {
            draftFields.areBlank() -> action
            else -> {
                draftUploader.stopContinuousUpload()
                sendMessage(primaryUserId(), currentMessageId(), draftFields)

                if (networkManager.isConnectedToNetwork()) {
                    ComposerAction.OnSendMessage
                } else {
                    ComposerEvent.OnSendMessageOffline
                }
            }
        }
    }

    private suspend fun buildDraftFields() = withContext(defaultDispatcher) {
        DraftFields(
            currentSenderEmail(),
            currentSubject(),
            currentDraftBody(),
            currentValidRecipientsTo(),
            currentValidRecipientsCc(),
            currentValidRecipientsBcc(),
            currentDraftQuotedHtmlBody()
        )
    }

    private suspend fun onSubjectChanged(action: ComposerAction.SubjectChanged): ComposerOperation =
        storeDraftWithSubject(primaryUserId.first(), currentMessageId(), currentSenderEmail(), action.subject).fold(
            ifLeft = {
                Timber.e("Store draft ${currentMessageId()} with new subject ${action.subject} failed")
                ComposerEvent.ErrorStoringDraftSubject
            },
            ifRight = { action }
        )

    private suspend fun onSenderChanged(action: ComposerAction.SenderChanged): ComposerOperation = storeDraftWithBody(
        primaryUserId(),
        currentMessageId(),
        currentDraftBody(),
        currentDraftQuotedHtmlBody(),
        SenderEmail(action.sender.email)
    )
        .onRight {
            reEncryptAttachments(
                userId = primaryUserId(),
                messageId = currentMessageId(),
                previousSender = SenderEmail(state.value.fields.sender.email),
                newSenderEmail = SenderEmail(action.sender.email)
            ).onLeft {
                Timber.e("Failed to re-encrypt attachments: $it")
                handleReEncryptionFailed()
            }
        }
        .fold(
            ifLeft = {
                Timber.e("Store draft ${currentMessageId()} with new sender ${action.sender.email} failed")
                ComposerEvent.ErrorStoringDraftSenderAddress
            },
            ifRight = {
                injectAddressSignature(
                    senderEmail = SenderEmail(action.sender.email),
                    previousSenderEmail = currentSenderEmail()
                )
                action
            }
        )

    private suspend fun onDraftBodyChanged(action: ComposerAction.DraftBodyChanged) {
        isBodyUpdating.value = true

        emitNewStateFor(ComposerAction.DraftBodyChanged(action.draftBody))

        // Do not store the draft if the body is exactly the same as signature + footer.
        if (isBodyEmptyOrEqualsToSignatureAndFooter(action.draftBody)) {
            isBodyUpdating.value = false
            return
        }

        storeDraftWithBody(
            primaryUserId(),
            currentMessageId(),
            action.draftBody,
            currentDraftQuotedHtmlBody(),
            currentSenderEmail()
        ).onLeft { emitNewStateFor(ComposerEvent.ErrorStoringDraftBody) }

        isBodyUpdating.value = false
    }

    private suspend fun injectAddressSignature(senderEmail: SenderEmail, previousSenderEmail: SenderEmail? = null) {
        injectAddressSignature(primaryUserId(), currentDraftBody(), senderEmail, previousSenderEmail).getOrNull()?.let {
            emitNewStateFor(ComposerEvent.ReplaceDraftBody(it))
        }
    }

    private suspend fun isBodyEmptyOrEqualsToSignatureAndFooter(draftBody: DraftBody): Boolean {
        // Consider the body empty even if it has white spaces or newlines.
        if (draftBody.value.trim().isEmpty()) return true

        val bodyWithSignature = injectAddressSignature(
            primaryUserId(),
            DraftBody(""),
            currentSenderEmail()
        )

        val isBodyEqualSignature = bodyWithSignature.getOrNull()?.value == draftBody.value
        return draftBody.value.isNotBlank() && isBodyEqualSignature
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun currentSubject() = Subject(state.value.fields.subject)

    private fun currentDraftBody() = DraftBody(state.value.fields.body)

    private fun currentDraftQuotedHtmlBody(): OriginalHtmlQuote? = state.value.fields.quotedBody?.original

    private fun currentSenderEmail() = SenderEmail(state.value.fields.sender.email)

    private fun currentMessageId() = state.value.fields.draftId

    private suspend fun currentValidRecipientsTo(): RecipientsTo {
        val contacts = contactsOrEmpty()
        return RecipientsTo(
            state.value.fields.to.filterIsInstance<RecipientUiModel.Valid>().map {
                participantMapper.recipientUiModelToParticipant(it, contacts)
            }
        )
    }

    private suspend fun currentValidRecipientsCc(): RecipientsCc {
        val contacts = contactsOrEmpty()
        return RecipientsCc(
            state.value.fields.cc.filterIsInstance<RecipientUiModel.Valid>().map {
                participantMapper.recipientUiModelToParticipant(it, contacts)
            }
        )
    }

    private suspend fun currentValidRecipientsBcc(): RecipientsBcc {
        val contacts = contactsOrEmpty()
        return RecipientsBcc(
            state.value.fields.bcc.filterIsInstance<RecipientUiModel.Valid>().map {
                participantMapper.recipientUiModelToParticipant(it, contacts)
            }
        )
    }

    private suspend fun contactsOrEmpty() = getContacts(primaryUserId()).getOrElse { emptyList() }

    private suspend fun onChangeSender() = getComposerSenderAddresses().fold(
        ifLeft = { changeSenderError ->
            when (changeSenderError) {
                Error.UpgradeToChangeSender -> ComposerEvent.ErrorFreeUserCannotChangeSender
                Error.FailedDeterminingUserSubscription,
                Error.FailedGettingPrimaryUser -> ComposerEvent.ErrorVerifyingPermissionsToChangeSender
            }
        },
        ifRight = { userAddresses ->
            ComposerEvent.SenderAddressesReceived(userAddresses.map { SenderUiModel(it.email) })
        }
    )

    private suspend fun onToChanged(action: ComposerAction.RecipientsToChanged): ComposerOperation {
        val contacts = contactsOrEmpty()
        return action.recipients.filterIsInstance<RecipientUiModel.Valid>().takeIfNotEmpty()?.let { validRecipients ->
            storeDraftWithRecipients(
                primaryUserId(),
                currentMessageId(),
                currentSenderEmail(),
                to = validRecipients.map { participantMapper.recipientUiModelToParticipant(it, contacts) }
            ).fold(
                ifLeft = { ComposerEvent.ErrorStoringDraftRecipients },
                ifRight = { action }
            )
        } ?: action
    }


    private suspend fun onCcChanged(action: ComposerAction.RecipientsCcChanged): ComposerOperation {
        val contacts = contactsOrEmpty()
        return action.recipients.filterIsInstance<RecipientUiModel.Valid>().takeIfNotEmpty()?.let { validRecipients ->
            storeDraftWithRecipients(
                primaryUserId(),
                currentMessageId(),
                currentSenderEmail(),
                cc = validRecipients.map { participantMapper.recipientUiModelToParticipant(it, contacts) }
            ).fold(
                ifLeft = { ComposerEvent.ErrorStoringDraftRecipients },
                ifRight = { action }
            )
        } ?: action
    }


    private suspend fun onBccChanged(action: ComposerAction.RecipientsBccChanged): ComposerOperation {
        val contacts = contactsOrEmpty()
        return action.recipients.filterIsInstance<RecipientUiModel.Valid>().takeIfNotEmpty()?.let { validRecipients ->
            storeDraftWithRecipients(
                primaryUserId(),
                currentMessageId(),
                currentSenderEmail(),
                bcc = validRecipients.map { participantMapper.recipientUiModelToParticipant(it, contacts) }
            ).fold(
                ifLeft = { ComposerEvent.ErrorStoringDraftRecipients },
                ifRight = { action }
            )
        } ?: action
    }

    private suspend fun onSearchTermChanged(searchTerm: String, suggestionsField: ContactSuggestionsField) {

        // cancel previous search Job for this [suggestionsField] type
        searchContactsJobs[suggestionsField]?.cancel()

        if (searchTerm.isNotBlank()) {
            searchContactsJobs[suggestionsField] = combine(
                searchContacts(primaryUserId(), searchTerm, onlyMatchingContactEmails = true),
                searchContactGroups(primaryUserId(), searchTerm)
            ) { contacts, contactGroups ->

                val deviceContacts = if (state.value.isDeviceContactsSuggestionsEnabled) {
                    searchDeviceContacts(searchTerm).getOrNull() ?: emptyList()
                } else emptyList()

                val suggestions = sortContactsForSuggestions(
                    contacts.getOrNull() ?: emptyList(),
                    deviceContacts,
                    contactGroups.getOrNull() ?: emptyList(),
                    maxContactAutocompletionCount
                )

                emitNewStateFor(
                    ComposerEvent.UpdateContactSuggestions(
                        suggestions,
                        suggestionsField
                    )
                )

            }.launchIn(viewModelScope)
        } else {
            emitNewStateFor(
                ComposerAction.ContactSuggestionsDismissed(
                    suggestionsField
                )
            )
        }

    }

    private suspend fun handleReEncryptionFailed() {
        deleteAllAttachments(primaryUserId(), currentSenderEmail(), currentMessageId())
        emitNewStateFor(ComposerEvent.ErrorAttachmentsReEncryption)
    }

    private fun emitNewStateFor(operation: ComposerOperation) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, operation)
    }

    private fun SavedStateHandle.extractRecipient(): List<RecipientUiModel>? {
        return get<String>(ComposerScreen.SerializedDraftActionKey)?.deserialize<DraftAction>()
            .let { it as? DraftAction.ComposeToAddresses }
            ?.let {
                it.recipients.map { recipient ->
                    when {
                        validateEmailAddress(recipient) -> RecipientUiModel.Valid(recipient)
                        else -> RecipientUiModel.Invalid(recipient)
                    }
                }
            }
    }

    // Function to log at the debug level within the Composer ViewModel scope.
    // This is introduced to identify potential issues with the current mutex implementation and determine
    // whether and how we can properly remove it.
    private fun logViewModelAction(action: ComposerAction, message: String) {
        // Ignore DraftBodyChanged for now, it causes too much noise.
        if (action is ComposerAction.DraftBodyChanged) return

        Timber
            .tag("ComposerViewModel")
            .d("Action ${action::class.java.simpleName} ${System.identityHashCode(action)} - $message")
    }

    companion object {

        internal const val maxContactAutocompletionCount = 100
    }
}
