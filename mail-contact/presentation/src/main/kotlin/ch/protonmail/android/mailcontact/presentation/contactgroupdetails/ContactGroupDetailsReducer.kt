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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.R
import javax.inject.Inject

class ContactGroupDetailsReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: ContactGroupDetailsState,
        event: ContactGroupDetailsEvent
    ): ContactGroupDetailsState {
        return when (event) {
            is ContactGroupDetailsEvent.ContactGroupLoaded -> reduceContactGroupLoaded(currentState, event)
            is ContactGroupDetailsEvent.LoadContactGroupError -> reduceLoadContactGroupError(currentState)
            is ContactGroupDetailsEvent.CloseContactGroupDetails -> reduceCloseContactGroupDetails(currentState)
            is ContactGroupDetailsEvent.ComposeEmail -> reduceComposeEmail(currentState, event)
            is ContactGroupDetailsEvent.ShowDeleteDialog -> reduceShowDeleteDialog(currentState)
            is ContactGroupDetailsEvent.DismissDeleteDialog -> reduceDismissDeleteDialog(currentState)
            ContactGroupDetailsEvent.DeletingSuccess -> reduceContactGroupDeleted(currentState)
            ContactGroupDetailsEvent.DeletingError -> reduceDeletingContactGroupError(currentState)
        }
    }

    private fun reduceContactGroupLoaded(
        currentState: ContactGroupDetailsState,
        event: ContactGroupDetailsEvent.ContactGroupLoaded
    ): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(
                isSendEnabled = event.contactGroupDetailsUiModel.memberCount > 0,
                contactGroup = event.contactGroupDetailsUiModel
            )

            is ContactGroupDetailsState.Loading -> ContactGroupDetailsState.Data(
                isSendEnabled = event.contactGroupDetailsUiModel.memberCount > 0,
                contactGroup = event.contactGroupDetailsUiModel,
                deleteDialogState = DeleteDialogState.Hidden
            )
        }
    }

    private fun reduceLoadContactGroupError(currentState: ContactGroupDetailsState): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState
            is ContactGroupDetailsState.Loading -> currentState.copy(
                errorLoading = Effect.of(TextUiModel(R.string.contact_group_details_loading_error))
            )
        }
    }

    private fun reduceCloseContactGroupDetails(currentState: ContactGroupDetailsState): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(close = Effect.of(Unit))
            is ContactGroupDetailsState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceComposeEmail(
        currentState: ContactGroupDetailsState,
        event: ContactGroupDetailsEvent.ComposeEmail
    ): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(openComposer = Effect.of(event.emails))
            is ContactGroupDetailsState.Loading -> currentState
        }
    }

    private fun reduceShowDeleteDialog(currentState: ContactGroupDetailsState): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(
                deleteDialogState = DeleteDialogState.Shown(
                    title = TextUiModel(R.string.contact_group_delete_dialog_title, currentState.contactGroup.name),
                    message = TextUiModel(R.string.contact_group_delete_dialog_message)
                )
            )

            is ContactGroupDetailsState.Loading -> currentState
        }
    }

    private fun reduceDismissDeleteDialog(currentState: ContactGroupDetailsState): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(
                deleteDialogState = DeleteDialogState.Hidden
            )

            is ContactGroupDetailsState.Loading -> currentState
        }
    }

    private fun reduceContactGroupDeleted(currentState: ContactGroupDetailsState): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(
                deleteDialogState = DeleteDialogState.Hidden,
                deletionSuccess = Effect.of(TextUiModel(R.string.contact_group_details_deletion_success))
            )

            is ContactGroupDetailsState.Loading -> currentState
        }
    }

    private fun reduceDeletingContactGroupError(currentState: ContactGroupDetailsState): ContactGroupDetailsState {
        return when (currentState) {
            is ContactGroupDetailsState.Data -> currentState.copy(
                deletionError = Effect.of(TextUiModel(R.string.contact_group_details_deletion_error)),
                deleteDialogState = DeleteDialogState.Hidden
            )

            is ContactGroupDetailsState.Loading -> currentState
        }
    }
}
