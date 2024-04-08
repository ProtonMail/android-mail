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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import ch.protonmail.android.mailcontact.presentation.model.ContactGroupDetailsUiModel

sealed interface ContactGroupDetailsOperation

internal sealed interface ContactGroupDetailsViewAction : ContactGroupDetailsOperation {
    object OnCloseClick : ContactGroupDetailsViewAction
    object OnEmailClick : ContactGroupDetailsViewAction
    object OnDeleteClick : ContactGroupDetailsViewAction
    object OnDeleteConfirmedClick : ContactGroupDetailsViewAction
    object OnDeleteDismissedClick : ContactGroupDetailsViewAction
}

sealed interface ContactGroupDetailsEvent : ContactGroupDetailsOperation {
    data class ContactGroupLoaded(
        val contactGroupDetailsUiModel: ContactGroupDetailsUiModel
    ) : ContactGroupDetailsEvent
    object LoadContactGroupError : ContactGroupDetailsEvent
    data class ShowDeleteDialog(
        val groupName: String
    ) : ContactGroupDetailsEvent
    object DismissDeleteDialog : ContactGroupDetailsEvent
    object DeletingError : ContactGroupDetailsEvent
    object DeletingSuccess : ContactGroupDetailsEvent
    object CloseContactGroupDetails : ContactGroupDetailsEvent
    data class ComposeEmail(
        val emails: List<String>
    ) : ContactGroupDetailsEvent
}
