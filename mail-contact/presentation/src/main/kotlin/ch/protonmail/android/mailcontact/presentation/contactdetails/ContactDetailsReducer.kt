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

package ch.protonmail.android.mailcontact.presentation.contactdetails

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import javax.inject.Inject

class ContactDetailsReducer @Inject constructor() {

    internal fun newStateFrom(currentState: ContactDetailsState, event: ContactDetailsEvent): ContactDetailsState {
        return when (event) {
            is ContactDetailsEvent.ContactLoaded -> reduceContactLoaded(currentState, event)
            is ContactDetailsEvent.LoadContactError -> reduceLoadContactError(currentState)
            is ContactDetailsEvent.CloseContactDetails -> reduceCloseContactDetails(currentState)
            is ContactDetailsEvent.DeleteRequested -> reduceContactDeleteRequested(currentState)
            is ContactDetailsEvent.DeleteConfirmed -> reduceContactDeleteConfirmed(currentState)
            is ContactDetailsEvent.CallPhoneNumber -> reduceCallPhoneNumber(currentState, event)
            is ContactDetailsEvent.ComposeEmail -> reduceComposeEmail(currentState, event)
            is ContactDetailsEvent.CopyToClipboard -> reduceCopyToClipboard(currentState, event)
        }
    }

    private fun reduceContactLoaded(
        currentState: ContactDetailsState,
        event: ContactDetailsEvent.ContactLoaded
    ): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(
                contact = event.contactDetailsUiModel
            )

            is ContactDetailsState.Loading -> ContactDetailsState.Data(contact = event.contactDetailsUiModel)
        }
    }

    private fun reduceLoadContactError(currentState: ContactDetailsState): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState
            is ContactDetailsState.Loading -> currentState.copy(
                errorLoading = Effect.of(TextUiModel(R.string.contact_details_loading_error))
            )
        }
    }

    private fun reduceCloseContactDetails(currentState: ContactDetailsState): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(close = Effect.of(Unit))
            is ContactDetailsState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceContactDeleteRequested(currentState: ContactDetailsState): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(
                showDeleteConfirmDialog = Effect.of(Unit)
            )

            is ContactDetailsState.Loading -> currentState
        }
    }

    private fun reduceContactDeleteConfirmed(currentState: ContactDetailsState): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_details_delete_success))
            )

            is ContactDetailsState.Loading -> currentState
        }
    }

    private fun reduceCallPhoneNumber(
        currentState: ContactDetailsState,
        event: ContactDetailsEvent.CallPhoneNumber
    ): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(
                callPhoneNumber = Effect.of(event.phoneNumber)
            )

            is ContactDetailsState.Loading -> currentState
        }
    }

    private fun reduceComposeEmail(
        currentState: ContactDetailsState,
        event: ContactDetailsEvent.ComposeEmail
    ): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(
                openComposer = Effect.of(event.email)
            )

            is ContactDetailsState.Loading -> currentState
        }
    }

    private fun reduceCopyToClipboard(
        currentState: ContactDetailsState,
        event: ContactDetailsEvent.CopyToClipboard
    ): ContactDetailsState {
        return when (currentState) {
            is ContactDetailsState.Data -> currentState.copy(
                copyToClipboard = Effect.of(event.value)
            )

            is ContactDetailsState.Loading -> currentState
        }
    }
}
