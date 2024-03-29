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

import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModel

sealed interface ContactDetailsOperation

internal sealed interface ContactDetailsViewAction : ContactDetailsOperation {
    object DeleteRequested : ContactDetailsViewAction
    object DeleteConfirmed : ContactDetailsViewAction
    object OnCloseClick : ContactDetailsViewAction
    data class OnCallClick(val phoneNumber: String) : ContactDetailsViewAction
    data class OnEmailClick(val email: String) : ContactDetailsViewAction
    data class OnLongClick(val value: String) : ContactDetailsViewAction
}

sealed interface ContactDetailsEvent : ContactDetailsOperation {
    data class ContactLoaded(
        val contactDetailsUiModel: ContactDetailsUiModel
    ) : ContactDetailsEvent

    object LoadContactError : ContactDetailsEvent
    object DeleteRequested : ContactDetailsEvent
    object DeleteConfirmed : ContactDetailsEvent
    object CloseContactDetails : ContactDetailsEvent
    data class CallPhoneNumber(val phoneNumber: String) : ContactDetailsEvent
    data class ComposeEmail(val email: String) : ContactDetailsEvent
    data class CopyToClipboard(val value: String) : ContactDetailsEvent
}
