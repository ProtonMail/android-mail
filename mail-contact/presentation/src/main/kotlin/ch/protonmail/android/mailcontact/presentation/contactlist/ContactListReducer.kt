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
            is ContactListEvent.OpenContactSearch -> reduceOpenContactSearch(currentState)
            is ContactListEvent.SubscriptionUpgradeRequiredError -> reduceErrorSubscriptionUpgradeRequired(currentState)
        }
    }

    private fun reduceContactListLoaded(
        currentState: ContactListState,
        event: ContactListEvent.ContactListLoaded
    ): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> {
                if (event.contactList.isNotEmpty()) {
                    ContactListState.Loaded.Data(
                        contacts = event.contactList,
                        contactGroups = event.contactGroups,
                        isContactGroupsCrudEnabled = event.isContactGroupsCrudEnabled,
                        isContactSearchEnabled = event.isContactSearchEnabled
                    )
                } else ContactListState.Loaded.Empty()
            }

            is ContactListState.Loaded -> {
                if (event.contactList.isNotEmpty()) {
                    ContactListState.Loaded.Data(
                        bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect,
                        contacts = event.contactList,
                        contactGroups = event.contactGroups,
                        isContactGroupsCrudEnabled = event.isContactGroupsCrudEnabled,
                        isContactSearchEnabled = event.isContactSearchEnabled
                    )
                } else {
                    ContactListState.Loaded.Empty(
                        bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect,
                        isContactGroupsCrudEnabled = event.isContactGroupsCrudEnabled,
                        isContactSearchEnabled = event.isContactSearchEnabled
                    )
                }
            }
        }
    }

    private fun reduceErrorLoadingContactList() =
        ContactListState.Loading(errorLoading = Effect.of(TextUiModel(R.string.contact_list_loading_error)))

    private fun reduceErrorSubscriptionUpgradeRequired(currentState: ContactListState) = when (currentState) {
        is ContactListState.Loaded.Empty -> currentState.copy(
            subscriptionError = Effect.of(TextUiModel.TextRes(R.string.contact_group_form_subscription_error)),
            bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
        )

        is ContactListState.Loaded.Data -> currentState.copy(
            subscriptionError = Effect.of(TextUiModel.TextRes(R.string.contact_group_form_subscription_error)),
            bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
        )

        is ContactListState.Loading -> currentState
    }

    private fun reduceOpenContactForm(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.Loaded.Data -> currentState.copy(
                openContactForm = Effect.of(Unit),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            is ContactListState.Loaded.Empty -> currentState.copy(
                openContactForm = Effect.of(Unit),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
        }
    }

    private fun reduceOpenContactGroupForm(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.Loaded.Data -> currentState.copy(
                openContactGroupForm = Effect.of(Unit),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            is ContactListState.Loaded.Empty -> currentState.copy(
                openContactGroupForm = Effect.of(Unit),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
        }
    }

    private fun reduceOpenImportContact(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.Loaded.Data -> currentState.copy(
                openImportContact = Effect.of(Unit),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            is ContactListState.Loaded.Empty -> currentState.copy(
                openImportContact = Effect.of(Unit),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
        }
    }

    private fun reduceOpenBottomSheet(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.Loaded.Data -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )

            is ContactListState.Loaded.Empty -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
        }
    }

    private fun reduceOpenContactSearch(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.Loaded.Data -> currentState.copy(
                openContactSearch = Effect.of(true)
            )

            is ContactListState.Loaded.Empty -> currentState.copy(
                openContactSearch = Effect.of(true)
            )
        }
    }

    private fun reduceDismissBottomSheet(currentState: ContactListState): ContactListState {
        return when (currentState) {
            is ContactListState.Loading -> currentState
            is ContactListState.Loaded.Data -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            is ContactListState.Loaded.Empty -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
        }
    }
}
