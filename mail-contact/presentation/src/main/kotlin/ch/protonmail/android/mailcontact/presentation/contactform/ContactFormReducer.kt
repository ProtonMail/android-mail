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
            is ContactFormEvent.ContactLoaded -> reduceContactLoaded(event)
            is ContactFormEvent.LoadContactError -> reduceLoadContactError(currentState)
            is ContactFormEvent.CloseContactForm -> reduceCloseContactForm(currentState)
        }
    }

    private fun reduceContactLoaded(event: ContactFormEvent.ContactLoaded): ContactFormState {
        return if (event.contactFormUiModel.id != null) {
            ContactFormState.Data.Update(contact = event.contactFormUiModel)
        } else {
            ContactFormState.Data.Create(contact = event.contactFormUiModel)
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
            is ContactFormState.Data.Create -> currentState.copy(close = Effect.of(Unit))
            is ContactFormState.Data.Update -> currentState.copy(close = Effect.of(Unit))
            is ContactFormState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }
}
