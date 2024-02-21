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

package ch.protonmail.android.mailcontact.presentation.managemembers

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import javax.inject.Inject

class ManageMembersReducer @Inject constructor() {

    internal fun newStateFrom(currentState: ManageMembersState, event: ManageMembersEvent): ManageMembersState {
        return when (event) {
            is ManageMembersEvent.MembersLoaded -> reduceMembersLoaded(currentState, event)
            is ManageMembersEvent.OnDone -> reduceOnDone(currentState, event)
            ManageMembersEvent.LoadMembersError -> reduceLoadMembersError(currentState)
            ManageMembersEvent.Close -> reduceClose(currentState)
            ManageMembersEvent.ErrorUpdatingMember -> reduceErrorUpdatingMember(currentState)
        }
    }

    private fun reduceMembersLoaded(
        currentState: ManageMembersState,
        event: ManageMembersEvent.MembersLoaded
    ): ManageMembersState {
        return when (currentState) {
            is ManageMembersState.Data -> currentState.copy(members = event.members)
            is ManageMembersState.Loading -> ManageMembersState.Data(members = event.members)
        }
    }

    private fun reduceOnDone(currentState: ManageMembersState, event: ManageMembersEvent.OnDone): ManageMembersState {
        return when (currentState) {
            is ManageMembersState.Data -> currentState.copy(onDone = Effect.of(event.selectedContactEmailIds))
            is ManageMembersState.Loading -> currentState
        }
    }

    private fun reduceLoadMembersError(currentState: ManageMembersState): ManageMembersState {
        return when (currentState) {
            is ManageMembersState.Data -> currentState
            is ManageMembersState.Loading -> currentState.copy(
                errorLoading = Effect.of(TextUiModel(R.string.members_loading_error))
            )
        }
    }

    private fun reduceErrorUpdatingMember(currentState: ManageMembersState): ManageMembersState {
        return when (currentState) {
            is ManageMembersState.Data -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.member_update_error))
            )
            is ManageMembersState.Loading -> currentState
        }
    }

    private fun reduceClose(currentState: ManageMembersState): ManageMembersState {
        return when (currentState) {
            is ManageMembersState.Data -> currentState.copy(close = Effect.of(Unit))
            is ManageMembersState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }
}
