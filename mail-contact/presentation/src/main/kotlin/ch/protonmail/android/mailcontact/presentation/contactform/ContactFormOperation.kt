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

import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.Section

sealed interface ContactFormOperation

sealed interface ContactFormViewAction : ContactFormOperation {
    object OnCloseContactFormClick : ContactFormViewAction
    object OnSaveClick : ContactFormViewAction
    data class OnUpdateDisplayName(
        val displayName: String
    ) : ContactFormViewAction

    data class OnUpdateFirstName(
        val firstName: String
    ) : ContactFormViewAction

    data class OnUpdateLastName(
        val lastName: String
    ) : ContactFormViewAction

    data class OnAddItemClick(
        val section: Section
    ) : ContactFormViewAction

    data class OnRemoveItemClick(
        val section: Section,
        val fieldId: String
    ) : ContactFormViewAction

    data class OnUpdateItem(
        val section: Section,
        val fieldId: String,
        val newValue: InputField
    ) : ContactFormViewAction
}

sealed interface ContactFormEvent : ContactFormOperation {
    data class ContactLoaded(
        val contactFormUiModel: ContactFormUiModel
    ) : ContactFormEvent

    data class UpdateContactForm(
        val contact: ContactFormUiModel
    ) : ContactFormEvent

    data object LoadContactError : ContactFormEvent
    sealed interface SaveContactError : ContactFormEvent {
        data object Generic : SaveContactError
        data object ContactLimitReached : SaveContactError
    }

    object SavingContact : ContactFormEvent
    object ContactCreated : ContactFormEvent
    object ContactUpdated : ContactFormEvent
    object CloseContactForm : ContactFormEvent
    object InvalidEmailError : ContactFormEvent
}
