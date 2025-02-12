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

package ch.protonmail.android.mailcomposer.presentation.reducer

import android.os.Build
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.DraftUiModel
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.usecase.ShouldRestrictWebViewHeight
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.NO_ATTACHMENT_LIMIT
import javax.inject.Inject
import kotlin.time.Duration

@Suppress("TooManyFunctions")
@Deprecated("Part of Composer V1, to be removed")
class ComposerReducer @Inject constructor(
    private val attachmentUiModelMapper: AttachmentUiModelMapper,
    private val shouldRestrictWebViewHeight: ShouldRestrictWebViewHeight
) {

    fun newStateFrom(currentState: ComposerDraftState, operation: ComposerOperation): ComposerDraftState =
        when (operation) {
            is ComposerAction -> operation.newStateForAction(currentState)
            is ComposerEvent -> operation.newStateForEvent(currentState)
        }

    @Suppress("ComplexMethod")
    private fun ComposerAction.newStateForAction(currentState: ComposerDraftState) = when (this) {
        is ComposerAction.AttachmentsAdded,
        is ComposerAction.RemoveAttachment -> currentState

        is ComposerAction.SenderChanged -> updateSenderTo(currentState, this.sender)
        is ComposerAction.RecipientsBccChanged -> updateRecipientsBcc(currentState, this.recipients)
        is ComposerAction.RecipientsCcChanged -> updateRecipientsCc(currentState, this.recipients)
        is ComposerAction.RecipientsToChanged -> updateRecipientsTo(currentState, this.recipients)
        is ComposerAction.ContactSuggestionTermChanged -> currentState
        is ComposerAction.DraftBodyChanged -> updateDraftBodyTo(currentState, this.draftBody)
        is ComposerAction.SubjectChanged -> updateSubjectTo(currentState, this.subject)
        is ComposerAction.OnAddAttachments -> updateForOnAddAttachments(currentState)
        is ComposerAction.OnCloseComposer -> updateCloseComposerState(currentState, false)
        is ComposerAction.ChangeSenderRequested -> currentState
        is ComposerAction.OnSendMessage -> updateStateForSendMessage(currentState)
        is ComposerAction.ContactSuggestionsDismissed -> updateStateForContactSuggestionsDismissed(
            currentState,
            this.suggestionsField
        )

        is ComposerAction.ConfirmSendingWithoutSubject -> updateForConfirmSendWithoutSubject(currentState)
        is ComposerAction.RejectSendingWithoutSubject -> updateForRejectSendWithoutSubject(currentState)
        is ComposerAction.OnSetExpirationTimeRequested -> updateStateForSetExpirationTimeRequested(currentState)
        is ComposerAction.ExpirationTimeSet -> updateStateForExpirationTimeSet(currentState)
        is ComposerAction.RespondInlineRequested,
        is ComposerAction.SendExpiringMessageToExternalRecipientsConfirmed -> currentState

        is ComposerAction.DeviceContactsPromptDenied -> updateStateForDeviceContactsPromptDenied(currentState, false)
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun ComposerEvent.newStateForEvent(currentState: ComposerDraftState) = when (this) {
        is ComposerEvent.DefaultSenderReceived -> updateSenderTo(currentState, this.sender)
        is ComposerEvent.ErrorLoadingDefaultSenderAddress -> updateStateToSenderError(currentState)
        is ComposerEvent.ErrorVerifyingPermissionsToChangeSender -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription))
        )

        is ComposerEvent.ErrorFreeUserCannotChangeSender -> updateStateToPaidFeatureMessage(currentState)
        is ComposerEvent.ErrorStoringDraftSenderAddress -> updateStateForChangeSenderFailed(
            currentState = currentState,
            errorMessage = TextUiModel(R.string.composer_error_store_draft_sender_address)
        )

        is ComposerEvent.ErrorStoringDraftBody -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_store_draft_body))
        )

        is ComposerEvent.ErrorStoringDraftSubject -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_store_draft_subject))
        )

        is ComposerEvent.ErrorStoringDraftRecipients -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_store_draft_recipients))
        )

        is ComposerEvent.SenderAddressesReceived -> currentState.copy(
            senderAddresses = this.senders,
            changeBottomSheetVisibility = Effect.of(true)
        )

        is ComposerEvent.OnCloseWithDraftSaved -> updateCloseComposerState(currentState, true)
        is ComposerEvent.OpenExistingDraft -> currentState.copy(isLoading = true)
        is ComposerEvent.OpenWithMessageAction -> updateStateForOpenWithMessageAction(currentState, draftAction)
        is ComposerEvent.PrefillDraftDataReceived -> updateComposerFieldsState(
            currentState,
            this.draftUiModel,
            this.isDataRefreshed,
            this.isBlockedSendingFromPmAddress,
            this.isBlockedSendingFromDisabledAddress
        )

        is ComposerEvent.PrefillDataReceivedViaShare -> updateComposerFieldsState(
            currentState,
            this.draftUiModel,
            isDataRefreshed = true,
            blockedSendingFromPmAddress = false,
            blockedSendingFromDisabledAddress = false
        )

        is ComposerEvent.ReplaceDraftBody -> {
            updateReplaceDraftBodyEffect(currentState, this.draftBody)
        }

        is ComposerEvent.ErrorLoadingDraftData -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_loading_draft)),
            isLoading = false
        )

        is ComposerEvent.OnSendMessageOffline -> updateStateForSendMessageOffline(currentState)
        is ComposerEvent.OnAttachmentsUpdated -> updateAttachmentsState(currentState, this.attachments)
        is ComposerEvent.ErrorLoadingParentMessageData -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_loading_parent_message)),
            isLoading = false
        )

        is ComposerEvent.ErrorAttachmentsExceedSizeLimit -> updateStateForAttachmentsExceedSizeLimit(currentState)
        is ComposerEvent.ErrorAttachmentsReEncryption -> updateStateForDeleteAllAttachment(currentState)
        is ComposerEvent.OnSendingError -> updateSendingErrorState(currentState, sendingError)
        is ComposerEvent.OnIsDeviceContactsSuggestionsEnabled -> updateIsAccessDeviceContactsEnabledState(
            currentState,
            this.enabled
        )

        is ComposerEvent.UpdateContactSuggestions -> updateStateForContactSuggestions(
            currentState,
            this.contactSuggestions,
            this.suggestionsField
        )

        is ComposerEvent.OnMessagePasswordUpdated -> updateStateForMessagePassword(currentState, this.messagePassword)
        is ComposerEvent.ConfirmEmptySubject -> currentState.copy(
            confirmSendingWithoutSubject = Effect.of(Unit)
        )

        is ComposerEvent.ErrorSettingExpirationTime -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_setting_expiration_time))
        )

        is ComposerEvent.OnMessageExpirationTimeUpdated -> updateStateForMessageExpirationTime(
            currentState,
            this.messageExpirationTime
        )

        is ComposerEvent.ConfirmSendExpiringMessageToExternalRecipients -> currentState.copy(
            confirmSendExpiringMessage = Effect.of(this.externalRecipients)
        )

        is ComposerEvent.RespondInlineContent -> updateStateForRespondInline(currentState, this.plainText)
        is ComposerEvent.OnIsDeviceContactsSuggestionsPromptEnabled -> currentState.copy(
            isDeviceContactsSuggestionsPromptEnabled = this.enabled
        )
    }

    private fun updateStateForRespondInline(
        currentState: ComposerDraftState,
        plainTextQuote: String
    ): ComposerDraftState {
        val bodyWithInlineQuote = currentState.fields.body.plus(plainTextQuote)
        return currentState.copy(
            fields = currentState.fields.copy(quotedBody = null),
            replaceDraftBody = Effect.of(TextUiModel(bodyWithInlineQuote))
        )
    }

    private fun updateComposerFieldsState(
        currentState: ComposerDraftState,
        draftUiModel: DraftUiModel,
        isDataRefreshed: Boolean,
        blockedSendingFromPmAddress: Boolean,
        blockedSendingFromDisabledAddress: Boolean
    ): ComposerDraftState {

        val validToRecipients = draftUiModel.draftFields.recipientsTo.value.map { RecipientUiModel.Valid(it.address) }
        val validCcRecipients = draftUiModel.draftFields.recipientsCc.value.map { RecipientUiModel.Valid(it.address) }
        val validBccRecipients = draftUiModel.draftFields.recipientsBcc.value.map { RecipientUiModel.Valid(it.address) }

        return currentState.copy(
            fields = currentState.fields.copy(
                sender = SenderUiModel(draftUiModel.draftFields.sender.value),
                subject = draftUiModel.draftFields.subject.value,
                body = draftUiModel.draftFields.body.value,
                quotedBody = draftUiModel.quotedHtmlContent,
                to = validToRecipients,
                cc = validCcRecipients,
                bcc = validBccRecipients
            ),
            isLoading = false,
            isSubmittable = (validToRecipients + validCcRecipients + validBccRecipients).isNotEmpty(),
            warning = if (!isDataRefreshed) {
                Effect.of(TextUiModel(R.string.composer_warning_local_data_shown))
            } else {
                Effect.empty()
            },
            senderChangedNotice = when {
                blockedSendingFromPmAddress ->
                    Effect.of(TextUiModel(R.string.composer_sender_changed_pm_address_is_a_paid_feature))

                blockedSendingFromDisabledAddress ->
                    Effect.of(TextUiModel(R.string.composer_sender_changed_original_address_disabled))

                else -> Effect.empty()
            },
            shouldRestrictWebViewHeight = shouldRestrictWebViewHeight(null) &&
                Build.VERSION.SDK_INT == Build.VERSION_CODES.P
        )
    }

    private fun updateAttachmentsState(currentState: ComposerDraftState, attachments: List<MessageAttachment>) =
        currentState.copy(
            attachments = AttachmentGroupUiModel(
                limit = NO_ATTACHMENT_LIMIT,
                attachments = attachments.map { attachmentUiModelMapper.toUiModel(it, true) }
            )
        )

    private fun updateSendingErrorState(currentState: ComposerDraftState, sendingError: TextUiModel) =
        currentState.copy(sendingErrorEffect = Effect.of(sendingError))

    private fun updateIsAccessDeviceContactsEnabledState(currentState: ComposerDraftState, enabled: Boolean) =
        currentState.copy(isDeviceContactsSuggestionsEnabled = enabled)

    private fun updateCloseComposerState(currentState: ComposerDraftState, isDraftSaved: Boolean) = if (isDraftSaved) {
        currentState.copy(closeComposerWithDraftSaved = Effect.of(Unit))
    } else {
        currentState.copy(closeComposer = Effect.of(Unit))
    }

    private fun updateDraftBodyTo(currentState: ComposerDraftState, draftBody: DraftBody): ComposerDraftState =
        currentState.copy(fields = currentState.fields.copy(body = draftBody.value))

    private fun updateSubjectTo(currentState: ComposerDraftState, subject: Subject): ComposerDraftState {
        // New line chars make the Subject invalid on BE side.
        val updatedSubject = subject.value.replace(Regex("[\\r\\n]+"), " ")
        return currentState.copy(fields = currentState.fields.copy(subject = updatedSubject))
    }

    private fun updateStateForOpenWithMessageAction(
        currentState: ComposerDraftState,
        draftAction: DraftAction
    ): ComposerDraftState {
        val bodyTextFieldEffect =
            if (draftAction is DraftAction.Reply || draftAction is DraftAction.ReplyAll) {
                Effect.of(Unit)
            } else {
                Effect.empty()
            }

        return currentState.copy(isLoading = true, focusTextBody = bodyTextFieldEffect)
    }

    private fun updateStateForChangeSenderFailed(currentState: ComposerDraftState, errorMessage: TextUiModel) =
        currentState.copy(changeBottomSheetVisibility = Effect.of(false), error = Effect.of(errorMessage))

    private fun updateStateForSendMessage(currentState: ComposerDraftState) =
        currentState.copy(closeComposerWithMessageSending = Effect.of(Unit))

    private fun updateForConfirmSendWithoutSubject(currentState: ComposerDraftState) = currentState.copy(
        closeComposerWithMessageSending = Effect.of(Unit),
        confirmSendingWithoutSubject = Effect.empty()
    )

    private fun updateForRejectSendWithoutSubject(currentState: ComposerDraftState) = currentState.copy(
        changeFocusToField = Effect.of(FocusedFieldType.SUBJECT),
        confirmSendingWithoutSubject = Effect.empty()
    )

    private fun updateStateForSendMessageOffline(currentState: ComposerDraftState) =
        currentState.copy(closeComposerWithMessageSendingOffline = Effect.of(Unit))

    private fun updateStateToPaidFeatureMessage(currentState: ComposerDraftState) =
        currentState.copy(premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature)))

    private fun updateStateToSenderError(currentState: ComposerDraftState) = currentState.copy(
        fields = currentState.fields.copy(sender = SenderUiModel("")),
        error = Effect.of(TextUiModel(R.string.composer_error_invalid_sender))
    )

    private fun updateStateForAttachmentsExceedSizeLimit(currentState: ComposerDraftState) =
        currentState.copy(attachmentsFileSizeExceeded = Effect.of(Unit))

    private fun updateStateForDeleteAllAttachment(currentState: ComposerDraftState) =
        currentState.copy(attachmentsReEncryptionFailed = Effect.of(Unit))

    private fun updateSenderTo(currentState: ComposerDraftState, sender: SenderUiModel) = currentState.copy(
        fields = currentState.fields.copy(sender = sender),
        changeBottomSheetVisibility = Effect.of(false)
    )

    private fun updateReplaceDraftBodyEffect(currentState: ComposerDraftState, draftBody: DraftBody) =
        currentState.copy(
            replaceDraftBody = Effect.of(TextUiModel(draftBody.value))
        )

    private fun updateStateForMessagePassword(currentState: ComposerDraftState, messagePassword: MessagePassword?) =
        currentState.copy(isMessagePasswordSet = messagePassword != null)

    private fun updateStateForSetExpirationTimeRequested(currentState: ComposerDraftState) =
        currentState.copy(changeBottomSheetVisibility = Effect.of(true))

    private fun updateStateForExpirationTimeSet(currentState: ComposerDraftState) =
        currentState.copy(changeBottomSheetVisibility = Effect.of(false))

    private fun updateStateForDeviceContactsPromptDenied(currentState: ComposerDraftState, enabled: Boolean) =
        currentState.copy(isDeviceContactsSuggestionsPromptEnabled = enabled)

    private fun updateStateForMessageExpirationTime(
        currentState: ComposerDraftState,
        messageExpirationTime: MessageExpirationTime?
    ) = currentState.copy(messageExpiresIn = messageExpirationTime?.expiresIn ?: Duration.ZERO)

    private fun updateForOnAddAttachments(currentState: ComposerDraftState) = currentState.copy(
        openImagePicker = Effect.of(Unit)
    )

    private fun updateRecipientsTo(
        currentState: ComposerDraftState,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentState = currentState,
        to = recipients,
        cc = currentState.fields.cc,
        bcc = currentState.fields.bcc
    )

    private fun updateRecipientsCc(
        currentState: ComposerDraftState,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentState = currentState,
        to = currentState.fields.to,
        cc = recipients,
        bcc = currentState.fields.bcc
    )

    private fun updateRecipientsBcc(
        currentState: ComposerDraftState,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentState = currentState,
        to = currentState.fields.to,
        cc = currentState.fields.cc,
        bcc = recipients
    )

    private fun updateStateForContactSuggestions(
        currentState: ComposerDraftState,
        contactSuggestions: List<ContactSuggestionUiModel>,
        suggestionsField: ContactSuggestionsField
    ) = currentState.copy(
        contactSuggestions = currentState.contactSuggestions.toMutableMap().apply {
            this[suggestionsField] = contactSuggestions
        },
        areContactSuggestionsExpanded = currentState.areContactSuggestionsExpanded.toMutableMap().apply {
            this[suggestionsField] = contactSuggestions.isNotEmpty()
        }
    )

    @Suppress("FunctionMaxLength")
    private fun updateStateForContactSuggestionsDismissed(
        currentState: ComposerDraftState,
        suggestionsField: ContactSuggestionsField
    ): ComposerDraftState = currentState.copy(
        areContactSuggestionsExpanded = currentState.areContactSuggestionsExpanded.toMutableMap().apply {
            this[suggestionsField] = false
        }
    )

    private fun updateRecipients(
        currentState: ComposerDraftState,
        to: List<RecipientUiModel>,
        cc: List<RecipientUiModel>,
        bcc: List<RecipientUiModel>
    ): ComposerDraftState {
        val allValid = (to + cc + bcc).all { it is RecipientUiModel.Valid }
        val notEmpty = (to + cc + bcc).isNotEmpty()
        val hasInvalidRecipients = hasInvalidRecipients(to, cc, bcc, currentState)

        val capturedToDuplicates = captureDuplicateEmails(to)
        val capturedCcDuplicates = captureDuplicateEmails(cc)
        val capturedBccDuplicates = captureDuplicateEmails(bcc)
        val hasDuplicates = hasDuplicates(capturedToDuplicates, capturedCcDuplicates, capturedBccDuplicates)

        val error = when {
            hasDuplicates -> {
                Effect.of(TextUiModel(R.string.composer_error_duplicate_recipient))
            }

            hasInvalidRecipients -> Effect.of(TextUiModel(R.string.composer_error_invalid_email))
            else -> Effect.empty()
        }

        return currentState.copy(
            fields = currentState.fields.copy(
                to = capturedToDuplicates.cleanedRecipients,
                cc = capturedCcDuplicates.cleanedRecipients,
                bcc = capturedBccDuplicates.cleanedRecipients
            ),
            recipientValidationError = error,
            isSubmittable = allValid && notEmpty
        )
    }

    private fun hasDuplicates(
        capturedToDuplicates: CleanedRecipients,
        capturedCcDuplicates: CleanedRecipients,
        capturedBccDuplicates: CleanedRecipients
    ): Boolean = capturedToDuplicates.duplicatesFound.isNotEmpty() ||
        capturedCcDuplicates.duplicatesFound.isNotEmpty() ||
        capturedBccDuplicates.duplicatesFound.isNotEmpty()

    private fun captureDuplicateEmails(recipients: List<RecipientUiModel>): CleanedRecipients {
        val itemsCounted = recipients.groupingBy { it }.eachCount()
        return CleanedRecipients(
            itemsCounted.map { it.key },
            itemsCounted.filter { it.value > 1 }.map { it.key }
        )
    }

    // For now we consider an error state if the last recipient is invalid and we have not deleted a recipient
    private fun hasInvalidRecipients(
        to: List<RecipientUiModel>,
        cc: List<RecipientUiModel>,
        bcc: List<RecipientUiModel>,
        currentState: ComposerDraftState
    ): Boolean {
        return hasError(to, currentState.fields.to) ||
            hasError(cc, currentState.fields.cc) ||
            hasError(bcc, currentState.fields.bcc)
    }

    private fun hasError(newRecipients: List<RecipientUiModel>, currentRecipients: List<RecipientUiModel>) =
        newRecipients.size > currentRecipients.size && newRecipients.lastOrNull() is RecipientUiModel.Invalid

    private data class CleanedRecipients(
        val cleanedRecipients: List<RecipientUiModel>,
        val duplicatesFound: List<RecipientUiModel>
    )
}
