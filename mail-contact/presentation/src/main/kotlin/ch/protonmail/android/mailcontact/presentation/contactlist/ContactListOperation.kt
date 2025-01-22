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

package ch.protonmail.android.mailcontact.presentation.contactlist

import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel

sealed interface ContactListOperation

internal sealed interface ContactListViewAction : ContactListOperation {
    data object OnOpenBottomSheet : ContactListViewAction
    data object OnOpenContactSearch : ContactListViewAction
    data object OnDismissBottomSheet : ContactListViewAction
    data object OnNewContactClick : ContactListViewAction
    data object OnNewContactGroupClick : ContactListViewAction
    data object OnImportContactClick : ContactListViewAction
}

internal sealed interface ContactListEvent : ContactListOperation {
    data class ContactListLoaded(
        val contactList: List<ContactListItemUiModel>,
        val contactGroups: List<ContactGroupItemUiModel>,
        val isContactGroupsUpsellingVisible: Boolean
    ) : ContactListEvent
    data object ErrorLoadingContactList : ContactListEvent
    data object SubscriptionUpgradeRequiredError : ContactListEvent
    data object OpenUpsellingBottomSheet : ContactListEvent
    data object OpenContactForm : ContactListEvent
    data object OpenContactGroupForm : ContactListEvent
    data object OpenImportContact : ContactListEvent
    data object OpenBottomSheet : ContactListEvent
    data object OpenContactSearch : ContactListEvent
    data object DismissBottomSheet : ContactListEvent
    data object UpsellingInProgress : ContactListEvent
}
