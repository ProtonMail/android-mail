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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import javax.inject.Inject

class ComposerReducer @Inject constructor() {

    fun newStateFrom(currentState: ComposerDraftState, operation: ComposerOperation): ComposerDraftState =
        when (operation) {
            is ComposerAction -> operation.newStateForAction(currentState)
            is ComposerEvent -> operation.newStateForEvent(currentState)
        }

    @Suppress("NotImplementedDeclaration", "ForbiddenComment")
    // TODO the subject is not considered here yet, we'll add it to the draft model later
    private fun ComposerAction.newStateForAction(currentState: ComposerDraftState) = when (this) {
        is ComposerAction.SenderChanged -> updateSenderTo(currentState, this.sender)
        is ComposerAction.RecipientsBccChanged -> updateRecipientsBcc(currentState, this.recipients)
        is ComposerAction.RecipientsCcChanged -> updateRecipientsCc(currentState, this.recipients)
        is ComposerAction.RecipientsToChanged -> updateRecipientsTo(currentState, this.recipients)
        is ComposerAction.SubjectChanged -> TODO()
        is ComposerAction.OnChangeSender,
        is ComposerAction.DraftBodyChanged -> currentState
    }

    private fun ComposerEvent.newStateForEvent(currentState: ComposerDraftState) = when (this) {
        is ComposerEvent.DefaultSenderReceived -> updateSenderTo(currentState, this.sender)
        is ComposerEvent.GetDefaultSenderError -> updateStateToSenderError(currentState)
        is ComposerEvent.ChangeSenderFailed -> updateStateForChangeSenderFailed(
            currentState,
            TextUiModel(R.string.composer_error_resolving_sender_address)
        )
        is ComposerEvent.ErrorSavingDraftBodyUnresolvedSender -> updateStateToUnresolvedSender(currentState)
        is ComposerEvent.ErrorGettingSubscriptionToChangeSender -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription))
        )
        is ComposerEvent.UpgradeToChangeSender -> updateStateToPaidFeatureMessage(currentState)
        is ComposerEvent.ErrorSavingDraftSender -> updateStateForChangeSenderFailed(
            currentState,
            TextUiModel(R.string.composer_error_save_draft_with_new_sender)
        )
        is ComposerEvent.ErrorSavingDraftBodyDbFailure -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_store_draft_body_DB_failure))
        )
        is ComposerEvent.ErrorSavingDraftBodyEncryptionFailure -> currentState.copy(
            error = Effect.of(TextUiModel(R.string.composer_error_store_draft_body_encryption_failure))
        )
        is ComposerEvent.SenderAddressesReceived -> currentState.copy(
            senderAddresses = this.senders,
            changeSenderBottomSheetVisibility = Effect.of(true)
        )
    }


    private fun updateStateForChangeSenderFailed(currentState: ComposerDraftState, errorMessage: TextUiModel) =
        currentState.copy(changeSenderBottomSheetVisibility = Effect.of(false), error = Effect.of(errorMessage))

    private fun updateStateToPaidFeatureMessage(currentState: ComposerDraftState) =
        currentState.copy(premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature)))

    private fun updateStateToUnresolvedSender(currentState: ComposerDraftState) = currentState.copy(
        error = Effect.of(TextUiModel(R.string.composer_error_save_draft_could_not_resolve_sender)),
        changeSenderBottomSheetVisibility = Effect.of(true)
    )

    private fun updateStateToSenderError(currentState: ComposerDraftState) = currentState.copy(
        fields = currentState.fields.copy(sender = SenderUiModel("")),
        error = Effect.of(TextUiModel(R.string.composer_error_invalid_sender))
    )

    private fun updateSenderTo(currentState: ComposerDraftState, sender: SenderUiModel) = currentState.copy(
        fields = currentState.fields.copy(sender = sender),
        changeSenderBottomSheetVisibility = Effect.of(false)
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

    private fun updateRecipients(
        currentState: ComposerDraftState,
        to: List<RecipientUiModel>,
        cc: List<RecipientUiModel>,
        bcc: List<RecipientUiModel>
    ): ComposerDraftState {
        val allValid = (to + cc + bcc).all { it is RecipientUiModel.Valid }
        val hasInvalidRecipients = hasInvalidRecipients(to, cc, bcc, currentState)

        val capturedToDuplicates = captureDuplicateEmails(to)
        val capturedCcDuplicates = captureDuplicateEmails(cc)
        val capturedBccDuplicates = captureDuplicateEmails(bcc)
        val hasDuplicates = hasDuplicates(capturedToDuplicates, capturedCcDuplicates, capturedBccDuplicates)

        val error = when {
            hasDuplicates -> {
                Effect.of(
                    TextUiModel(
                        R.string.composer_error_duplicate_recipient,
                        getDuplicateEmailsError(capturedToDuplicates, capturedCcDuplicates, capturedBccDuplicates)
                    )
                )
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
            error = error,
            isSubmittable = allValid
        )
    }

    private fun hasDuplicates(
        capturedToDuplicates: CleanedRecipients,
        capturedCcDuplicates: CleanedRecipients,
        capturedBccDuplicates: CleanedRecipients
    ): Boolean = capturedToDuplicates.duplicatesFound.isNotEmpty() ||
        capturedCcDuplicates.duplicatesFound.isNotEmpty() ||
        capturedBccDuplicates.duplicatesFound.isNotEmpty()

    private fun getDuplicateEmailsError(
        capturedToDuplicates: CleanedRecipients,
        capturedCcDuplicates: CleanedRecipients,
        capturedBccDuplicates: CleanedRecipients
    ): String {
        val duplicates = capturedToDuplicates.duplicatesFound +
            capturedCcDuplicates.duplicatesFound +
            capturedBccDuplicates.duplicatesFound

        val validDuplicates = duplicates.filterIsInstance<RecipientUiModel.Valid>()
        val inValidDuplicates = duplicates.filterIsInstance<RecipientUiModel.Invalid>()

        return (validDuplicates.map { it.address } + inValidDuplicates.map { it.address })
            .distinct()
            .joinToString(", ")
    }

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
