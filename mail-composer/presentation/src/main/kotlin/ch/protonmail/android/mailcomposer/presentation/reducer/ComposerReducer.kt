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

    @Suppress("NotImplementedDeclaration", "ForbiddenComment")
    // TODO the from, subject and body are not considered here yet, we'll add it to the draft model later
    fun newStateFrom(currentState: ComposerDraftState, operation: ComposerOperation): ComposerDraftState =
        when (operation) {
            is ComposerAction.SenderChanged -> updateSenderTo(currentState, operation.sender)
            is ComposerAction.RecipientsBccChanged -> updateRecipientsBcc(currentState, operation.recipients)
            is ComposerAction.RecipientsCcChanged -> updateRecipientsCc(currentState, operation.recipients)
            is ComposerAction.RecipientsToChanged -> updateRecipientsTo(currentState, operation.recipients)
            is ComposerAction.SubjectChanged -> TODO()
            is ComposerEvent.DefaultSenderReceived -> updateSenderTo(currentState, operation.sender)
            is ComposerEvent.GetDefaultSenderError -> updateStateToSenderError(currentState)
            is ComposerEvent.ChangeSenderFailed -> updateStateForChangeSenderFailed(currentState)
            is ComposerEvent.ErrorGettingSubscriptionToChangeSender -> currentState.copy(
                error = Effect.of(TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription))
            )
            is ComposerEvent.UpgradeToChangeSender -> updateStateToPaidFeatureMessage(currentState)
            is ComposerEvent.SenderAddressesReceived -> currentState.copy(
                senderAddresses = operation.senders,
                changeSenderBottomSheetVisibility = Effect.of(true)
            )

            is ComposerAction.OnChangeSender,
            is ComposerAction.DraftBodyChanged -> currentState
        }

    private fun updateStateForChangeSenderFailed(currentState: ComposerDraftState) = currentState.copy(
        changeSenderBottomSheetVisibility = Effect.of(false),
        error = Effect.of(TextUiModel(R.string.composer_error_resolving_sender_address))
    )

    private fun updateStateToPaidFeatureMessage(currentState: ComposerDraftState) =
        currentState.copy(premiumFeatureMessage = Effect.of(TextUiModel(R.string.composer_change_sender_paid_feature)))

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
        bcc = currentState.fields.bcc,
        hasError = hasError(recipients, currentState.fields.to)
    )

    private fun updateRecipientsCc(
        currentState: ComposerDraftState,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentState = currentState,
        to = currentState.fields.to,
        cc = recipients,
        bcc = currentState.fields.bcc,
        hasError = hasError(recipients, currentState.fields.cc)
    )

    private fun updateRecipientsBcc(
        currentState: ComposerDraftState,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentState = currentState,
        to = currentState.fields.to,
        cc = currentState.fields.cc,
        bcc = recipients,
        hasError = hasError(recipients, currentState.fields.bcc)
    )

    private fun updateRecipients(
        currentState: ComposerDraftState,
        to: List<RecipientUiModel>,
        cc: List<RecipientUiModel>,
        bcc: List<RecipientUiModel>,
        hasError: Boolean
    ): ComposerDraftState {
        val allValid = (to + cc + bcc).all { it is RecipientUiModel.Valid }
        val error = if (hasError) Effect.of(TextUiModel(R.string.composer_error_invalid_email)) else Effect.empty()

        return currentState.copy(
            fields = currentState.fields.copy(to = to, cc = cc, bcc = bcc),
            error = error,
            isSubmittable = allValid
        )
    }

    // For now we consider an error state if the last recipient is invalid and we have not deleted a recipient
    private fun hasError(newRecipients: List<RecipientUiModel>, currentRecipients: List<RecipientUiModel>) =
        newRecipients.size > currentRecipients.size && newRecipients.lastOrNull() is RecipientUiModel.Invalid
}
