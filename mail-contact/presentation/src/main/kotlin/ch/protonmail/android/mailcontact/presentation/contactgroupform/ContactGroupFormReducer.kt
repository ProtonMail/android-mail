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

package ch.protonmail.android.mailcontact.presentation.contactgroupform

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.R
import javax.inject.Inject

class ContactGroupFormReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: ContactGroupFormState,
        event: ContactGroupFormEvent
    ): ContactGroupFormState {
        return when (event) {
            is ContactGroupFormEvent.ContactGroupLoaded -> reduceContactGroupLoaded(currentState, event)
            is ContactGroupFormEvent.UpdateContactGroupFormUiModel -> {
                reduceUpdateContactGroupFormUiModel(currentState, event)
            }

            ContactGroupFormEvent.Close -> reduceClose(currentState)
            ContactGroupFormEvent.LoadError -> reduceLoadError(currentState)
            ContactGroupFormEvent.ContactGroupCreated -> reduceContactGroupCreated(currentState)
            ContactGroupFormEvent.ContactGroupUpdated -> reduceContactGroupUpdated(currentState)
            ContactGroupFormEvent.SaveContactGroupError -> reduceSaveContactGroupError(currentState)
            ContactGroupFormEvent.DuplicatedContactGroupName -> reduceDuplicatedContactGroupName(currentState)
            ContactGroupFormEvent.SavingContactGroup -> reduceSavingContactGroup(currentState)
            ContactGroupFormEvent.UpdateMembersError -> reduceUpdateMembersError(currentState)
            ContactGroupFormEvent.ShowDeleteDialog -> reduceShowDeleteDialog(currentState)
            ContactGroupFormEvent.DismissDeleteDialog -> reduceDismissDeleteDialog(currentState)
            ContactGroupFormEvent.DeletingSuccess -> reduceContactGroupDeleted(currentState)
            ContactGroupFormEvent.DeletingError -> reduceDeletingContactGroupError(currentState)
            ContactGroupFormEvent.SubscriptionNeededError -> reduceSubscriptionNeededError(currentState)
        }
    }

    private fun reduceContactGroupLoaded(
        currentState: ContactGroupFormState,
        event: ContactGroupFormEvent.ContactGroupLoaded
    ): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                contactGroup = event.contactGroupFormUiModel,
                colors = event.colors,
                isSaveEnabled = event.contactGroupFormUiModel.name.isNotBlank()
            )

            is ContactGroupFormState.Loading -> ContactGroupFormState.Data(
                contactGroup = event.contactGroupFormUiModel,
                colors = event.colors,
                isSaveEnabled = event.contactGroupFormUiModel.name.isNotBlank()
            )
        }
    }

    private fun reduceUpdateContactGroupFormUiModel(
        currentState: ContactGroupFormState,
        event: ContactGroupFormEvent.UpdateContactGroupFormUiModel
    ): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                contactGroup = event.contactGroupFormUiModel,
                isSaveEnabled = event.contactGroupFormUiModel.name.isNotBlank()
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceLoadError(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState
            is ContactGroupFormState.Loading -> currentState.copy(
                errorLoading = Effect.of(TextUiModel(R.string.contact_group_form_loading_error))
            )
        }
    }

    private fun reduceClose(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(close = Effect.of(Unit))
            is ContactGroupFormState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceContactGroupCreated(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_create_success))
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceContactGroupUpdated(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_update_success))
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceSaveContactGroupError(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_group_form_save_error)),
                displaySaveLoader = false
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceDuplicatedContactGroupName(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_group_form_save_error_already_exists)),
                displaySaveLoader = false
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceSavingContactGroup(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                displaySaveLoader = true
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateMembersError(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.add_members_error))
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceShowDeleteDialog(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                deleteDialogState = DeleteDialogState.Shown(
                    title = TextUiModel(R.string.contact_group_delete_dialog_title, currentState.contactGroup.name),
                    message = TextUiModel(R.string.contact_group_delete_dialog_message)
                )
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceDismissDeleteDialog(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                deleteDialogState = DeleteDialogState.Hidden
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceContactGroupDeleted(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                deleteDialogState = DeleteDialogState.Hidden,
                deletionSuccess = Effect.of(TextUiModel(R.string.contact_group_details_deletion_success))
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceDeletingContactGroupError(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                deletionError = Effect.of(TextUiModel(R.string.contact_group_details_deletion_error)),
                deleteDialogState = DeleteDialogState.Hidden
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }

    private fun reduceSubscriptionNeededError(currentState: ContactGroupFormState): ContactGroupFormState {
        return when (currentState) {
            is ContactGroupFormState.Data -> currentState.copy(
                subscriptionNeededError = Effect.of(TextUiModel(R.string.contact_group_form_subscription_error)),
                displaySaveLoader = false
            )

            is ContactGroupFormState.Loading -> currentState
        }
    }
}
