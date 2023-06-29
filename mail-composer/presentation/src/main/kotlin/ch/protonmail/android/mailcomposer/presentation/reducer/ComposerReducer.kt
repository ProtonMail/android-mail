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
import ch.protonmail.android.mailcomposer.presentation.usecase.GetChangeSenderAddresses
import javax.inject.Inject

class ComposerReducer @Inject constructor(
    private val getChangeSenderAddresses: GetChangeSenderAddresses
) {

    @Suppress("NotImplementedDeclaration", "ForbiddenComment")
    // TODO the from, subject and body are not considered here yet, we'll add it to the draft model later
    suspend fun newStateFrom(currentState: ComposerDraftState, operation: ComposerOperation): ComposerDraftState =
        when (operation) {
            is ComposerAction.DraftBodyChanged -> currentState
            is ComposerAction.SenderChanged -> updateSenderTo(currentState, operation.sender)
            is ComposerAction.RecipientsBccChanged -> updateRecipientsBcc(currentState, operation.recipients)
            is ComposerAction.RecipientsCcChanged -> updateRecipientsCc(currentState, operation.recipients)
            is ComposerAction.RecipientsToChanged -> updateRecipientsTo(currentState, operation.recipients)
            is ComposerAction.SubjectChanged -> TODO()
            is ComposerEvent.DefaultSenderReceived -> updateSenderTo(currentState, operation.sender)
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
        ifRight = { userAddresses ->
            currentState.copy(
                senderAddresses = userAddresses.map { SenderUiModel(it.email) },
                changeSenderBottomSheetVisibility = Effect.of(true)
            )
        }
    )

    private fun updateStateToError(currentState: ComposerDraftState, message: TextUiModel) =
        currentState.copy(error = Effect.of(message))

    private fun updateStateToPaidFeatureError(currentState: ComposerDraftState, message: TextUiModel) =
        currentState.copy(premiumFeatureMessage = Effect.of(message))

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
