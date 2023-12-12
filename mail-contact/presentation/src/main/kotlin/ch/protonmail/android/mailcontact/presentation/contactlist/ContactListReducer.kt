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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import javax.inject.Inject

class ContactListReducer @Inject constructor() {

    internal fun newStateFrom(currentState: ContactListState, event: ContactListEvent): ContactListState {
        return when (event) {
            is ContactListEvent.ContactListLoaded -> reduceContactListLoaded(currentState, event)
            is ContactListEvent.ErrorLoadingContactList -> reduceErrorLoadingContactList()
            is ContactListEvent.OpenContactForm -> reduceOpenContactForm(currentState)
            is ContactListEvent.OpenContactGroupForm -> reduceOpenContactGroupForm(currentState)
            is ContactListEvent.OpenImportContact -> reduceOpenImportContact(currentState)
            is ContactListEvent.DismissBottomSheet -> reduceDismissBottomSheet(currentState)
            is ContactListEvent.OpenBottomSheet -> reduceOpenBottomSheet(currentState)
        }
    }

    private fun reduceContactListLoaded(
        currentState: ContactListState,
        event: ContactListEvent.ContactListLoaded
    ): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> {
                if (event.contactList.isNotEmpty()) {
                    ContactListState.ListLoaded.Data(
                        contacts = event.contactList
                    )
                } else ContactListState.ListLoaded.Empty()
            }
            is ContactListState.ListLoaded -> {
                if (event.contactList.isNotEmpty()) {
                    ContactListState.ListLoaded.Data(
                        bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect,
                        openContactForm = currentState.openContactForm,
                        contacts = event.contactList
                    )
                } else {
                    ContactListState.ListLoaded.Empty(
                        bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect,
                        openContactForm = currentState.openContactForm
                    )
                }
            }
        }
    }

    private fun reduceErrorLoadingContactList() =
        ContactListState.Loading(errorLoading = Effect.of(TextUiModel(R.string.contact_list_loading_error)))

    private fun reduceOpenContactForm(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.ListLoaded.Data -> currentState.copy(openContactForm = Effect.of(Unit))
            is ContactListState.ListLoaded.Empty -> currentState.copy(openContactForm = Effect.of(Unit))
        }
    }

    private fun reduceOpenContactGroupForm(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.ListLoaded.Data -> currentState.copy(openContactGroupForm = Effect.of(Unit))
            is ContactListState.ListLoaded.Empty -> currentState.copy(openContactGroupForm = Effect.of(Unit))
        }
    }

    private fun reduceOpenImportContact(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.ListLoaded.Data -> currentState.copy(openImportContact = Effect.of(Unit))
            is ContactListState.ListLoaded.Empty -> currentState.copy(openImportContact = Effect.of(Unit))
        }
    }

    private fun reduceOpenBottomSheet(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.ListLoaded.Data -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
            is ContactListState.ListLoaded.Empty -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
        }
    }

    private fun reduceDismissBottomSheet(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.ListLoaded.Data -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
            is ContactListState.ListLoaded.Empty -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
        }
    }
}
