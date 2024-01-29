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

package ch.protonmail.android.mailcontact.presentation.contactform

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import javax.inject.Inject

class ContactFormReducer @Inject constructor() {

    internal fun newStateFrom(currentState: ContactFormState, event: ContactFormEvent): ContactFormState {
        return when (event) {
            is ContactFormEvent.NewContact -> reduceNewContact(event)
            is ContactFormEvent.EditContact -> reduceEditContact(event)
            is ContactFormEvent.UpdateContactForm -> reduceUpdateContactForm(currentState, event)
            ContactFormEvent.LoadContactError -> reduceLoadContactError(currentState)
            ContactFormEvent.CloseContactForm -> reduceCloseContactForm(currentState)
            ContactFormEvent.SaveContactError -> reduceSaveContactError(currentState)
            ContactFormEvent.ContactCreated -> reduceContactCreated(currentState)
            ContactFormEvent.ContactUpdated -> reduceContactUpdated(currentState)
            ContactFormEvent.CreatingContact -> reduceCreatingContact(currentState)
        }
    }

    private fun reduceNewContact(event: ContactFormEvent.NewContact) =
        ContactFormState.Data.Create(contact = event.contactFormUiModel)

    private fun reduceEditContact(event: ContactFormEvent.EditContact) =
        ContactFormState.Data.Update(contact = event.contactFormUiModel)

    private fun reduceLoadContactError(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState
            is ContactFormState.Loading -> currentState.copy(
                errorLoading = Effect.of(TextUiModel(R.string.contact_form_loading_error))
            )
        }
    }

    private fun reduceCloseContactForm(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data.Create -> currentState.copy(close = Effect.of(Unit))
            is ContactFormState.Data.Update -> currentState.copy(close = Effect.of(Unit))
            is ContactFormState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceSaveContactError(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data.Create -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_form_save_error)),
                displayCreateLoader = false
            )
            is ContactFormState.Data.Update -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_form_save_error))
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceContactCreated(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data.Create -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_save_success))
            )
            is ContactFormState.Data.Update -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_save_success))
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceContactUpdated(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data.Create -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_save_success))
            )
            is ContactFormState.Data.Update -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_save_success))
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceCreatingContact(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data.Create -> currentState.copy(
                displayCreateLoader = true
            )
            is ContactFormState.Data.Update -> currentState
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateContactForm(
        currentState: ContactFormState,
        event: ContactFormEvent.UpdateContactForm
    ): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data.Create -> currentState.copy(contact = event.contact)
            is ContactFormState.Data.Update -> currentState.copy(contact = event.contact)
            is ContactFormState.Loading -> currentState
        }
    }
}
