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
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import javax.inject.Inject

class ContactFormReducer @Inject constructor() {

    internal fun newStateFrom(currentState: ContactFormState, event: ContactFormEvent): ContactFormState {
        return when (event) {
            is ContactFormEvent.ContactLoaded -> reduceContactLoaded(currentState, event)
            is ContactFormEvent.UpdateContactForm -> reduceUpdateContactForm(currentState, event)
            ContactFormEvent.LoadContactError -> reduceLoadContactError(currentState)
            ContactFormEvent.CloseContactForm -> reduceCloseContactForm(currentState)
            is ContactFormEvent.SaveContactError -> reduceSaveContactError(currentState, event)
            ContactFormEvent.ContactCreated -> reduceContactCreated(currentState)
            ContactFormEvent.ContactUpdated -> reduceContactUpdated(currentState)
            ContactFormEvent.SavingContact -> reduceSavingContact(currentState)
            ContactFormEvent.InvalidEmailError -> reduceInvalidEmailError(currentState)
        }
    }

    private fun reduceContactLoaded(
        currentState: ContactFormState,
        event: ContactFormEvent.ContactLoaded
    ): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(contact = event.contactFormUiModel)
            is ContactFormState.Loading -> ContactFormState.Data(
                contact = event.contactFormUiModel,
                isSaveEnabled = isSaveEnabled(event.contactFormUiModel)
            )
        }
    }

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
            is ContactFormState.Data -> currentState.copy(close = Effect.of(Unit))
            is ContactFormState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceSaveContactError(currentState: ContactFormState, event: ContactFormEvent): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(
                showErrorSnackbar = if (event is ContactFormEvent.SaveContactError.ContactLimitReached) {
                    Effect.of(TextUiModel(R.string.contact_form_save_error_limit_reached))
                } else {
                    Effect.of(TextUiModel(R.string.contact_form_save_error))
                },
                displaySaveLoader = false
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceContactCreated(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_create_success))
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceContactUpdated(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_update_success))
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceSavingContact(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(
                displaySaveLoader = true
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceInvalidEmailError(currentState: ContactFormState): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_form_invalid_email_error))
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateContactForm(
        currentState: ContactFormState,
        event: ContactFormEvent.UpdateContactForm
    ): ContactFormState {
        return when (currentState) {
            is ContactFormState.Data -> currentState.copy(
                contact = event.contact,
                isSaveEnabled = isSaveEnabled(event.contact)
            )
            is ContactFormState.Loading -> currentState
        }
    }

    private fun isSaveEnabled(contact: ContactFormUiModel) =
        contact.displayName.isNotBlank() || contact.firstName.isNotBlank() || contact.lastName.isNotBlank()
}
