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
import ch.protonmail.android.mailcomposer.presentation.usecase.GetChangeSenderAddresses
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import javax.inject.Inject

class ComposerReducer @Inject constructor(
    private val getChangeSenderAddresses: GetChangeSenderAddresses
) {

    @Suppress("NotImplementedDeclaration", "ForbiddenComment")
    // TODO the from, subject and body are not considered here yet, we'll add it to the draft model later
    suspend fun newStateFrom(currentState: ComposerDraftState, operation: ComposerOperation): ComposerDraftState =
        when (operation) {
            is ComposerAction.DraftBodyChanged -> currentState
            is ComposerAction.SenderChanged -> TODO()
            is ComposerAction.RecipientsBccChanged -> updateRecipientsBcc(currentState.fields, operation.recipients)
            is ComposerAction.RecipientsCcChanged -> updateRecipientsCc(currentState.fields, operation.recipients)
            is ComposerAction.RecipientsToChanged -> updateRecipientsTo(currentState.fields, operation.recipients)
            is ComposerAction.SubjectChanged -> TODO()
            is ComposerEvent.DefaultSenderReceived -> updateSenderTo(currentState, operation.address)
            is ComposerEvent.GetDefaultSenderError -> updateStateToSenderError(currentState)
            is ComposerAction.OnChangeSender -> updateStateForChangeSender(currentState)
        }

    private suspend fun updateStateForChangeSender(currentState: ComposerDraftState) = getChangeSenderAddresses().fold(
        ifLeft = {
            when (it) {
                GetChangeSenderAddresses.Error.UpgradeToChangeSender -> updateStateToPaidFeatureError(
                    currentState,
                    TextUiModel(R.string.composer_change_sender_paid_feature)
                )
                GetChangeSenderAddresses.Error.FailedDeterminingUserSubscription,
                GetChangeSenderAddresses.Error.FailedGettingPrimaryUser -> updateStateToError(
                    currentState,
                    TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription)
                )
            }
        },
        ifRight = { TODO() }
    )

    private fun updateStateToError(currentState: ComposerDraftState, message: TextUiModel) = when (currentState) {
        is ComposerDraftState.Submittable -> currentState.copy(error = Effect.of(message))
        is ComposerDraftState.NotSubmittable -> currentState.copy(error = Effect.of(message))
    }

    private fun updateStateToPaidFeatureError(currentState: ComposerDraftState, message: TextUiModel) =
        when (currentState) {
            is ComposerDraftState.Submittable -> currentState.copy(premiumFeatureMessage = Effect.of(message))
            is ComposerDraftState.NotSubmittable -> currentState.copy(premiumFeatureMessage = Effect.of(message))
        }

    private fun updateStateToSenderError(currentState: ComposerDraftState) = when (currentState) {
        is ComposerDraftState.Submittable,
        is ComposerDraftState.NotSubmittable -> ComposerDraftState.NotSubmittable(
            fields = currentState.fields.copy(from = ""),
            premiumFeatureMessage = currentState.premiumFeatureMessage,
            error = Effect.of(TextUiModel(R.string.composer_error_invalid_sender))
        )
    }

    private fun updateSenderTo(currentState: ComposerDraftState, address: String) = when (currentState) {
        is ComposerDraftState.Submittable -> currentState.copy(currentState.fields.copy(from = address))
        is ComposerDraftState.NotSubmittable -> currentState.copy(currentState.fields.copy(from = address))
    }

    private fun updateRecipientsTo(
        currentFields: ComposerFields,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentFields = currentFields,
        to = recipients,
        cc = currentFields.cc,
        bcc = currentFields.bcc,
        hasError = hasError(recipients, currentFields.to)
    )

    private fun updateRecipientsCc(
        currentFields: ComposerFields,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentFields = currentFields,
        to = currentFields.to,
        cc = recipients,
        bcc = currentFields.bcc,
        hasError = hasError(recipients, currentFields.cc)
    )

    private fun updateRecipientsBcc(
        currentFields: ComposerFields,
        recipients: List<RecipientUiModel>
    ): ComposerDraftState = updateRecipients(
        currentFields = currentFields,
        to = currentFields.to,
        cc = currentFields.cc,
        bcc = recipients,
        hasError = hasError(recipients, currentFields.bcc)
    )

    private fun updateRecipients(
        currentFields: ComposerFields,
        to: List<RecipientUiModel>,
        cc: List<RecipientUiModel>,
        bcc: List<RecipientUiModel>,
        hasError: Boolean
    ): ComposerDraftState {
        val allValid = (to + cc + bcc).all { it is RecipientUiModel.Valid }
        return if (allValid) {
            ComposerDraftState.Submittable(
                fields = currentFields.copy(to = to, cc = cc, bcc = bcc),
                premiumFeatureMessage = Effect.empty(),
                error = Effect.empty()
            )
        } else {
            ComposerDraftState.NotSubmittable(
                fields = currentFields.copy(to = to, cc = cc, bcc = bcc),
                premiumFeatureMessage = Effect.empty(),
                error = if (hasError) Effect.of(TextUiModel(R.string.composer_error_invalid_email)) else Effect.empty()
            )
        }
    }

    // For now we consider an error state if the last recipient is invalid and we have not deleted a recipient
    private fun hasError(newRecipients: List<RecipientUiModel>, currentRecipients: List<RecipientUiModel>) =
        newRecipients.size > currentRecipients.size && newRecipients.lastOrNull() is RecipientUiModel.Invalid
}
