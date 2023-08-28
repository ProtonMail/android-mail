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

package ch.protonmail.android.mailcommon.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import javax.inject.Inject

class BottomBarReducer @Inject constructor() {

    @SuppressWarnings("UnusedPrivateMember")
    fun newStateFrom(currentState: BottomBarState, event: BottomBarEvent): BottomBarState {
        return when (event) {
            is BottomBarEvent.ActionsData -> currentState.toNewStateForActionData(event)
            is BottomBarEvent.ShowAndUpdateActionsData -> BottomBarState.Data.Shown(event.actionUiModels)
            is BottomBarEvent.HideBottomSheet -> currentState.toNewStateForHiding()
            is BottomBarEvent.ShowBottomSheet -> currentState.toNewStateForShowing()
            is BottomBarEvent.ErrorLoadingActions -> currentState.toNewStateForErrorLoading()
        }
    }

    private fun BottomBarState.toNewStateForActionData(operation: BottomBarEvent.ActionsData) = when (this) {
        is BottomBarState.Data.Hidden -> BottomBarState.Data.Hidden(operation.actionUiModels)
        is BottomBarState.Data.Shown -> BottomBarState.Data.Shown(operation.actionUiModels)
        is BottomBarState.Error.FailedLoadingActions -> BottomBarState.Data.Hidden(operation.actionUiModels)
        is BottomBarState.Loading -> BottomBarState.Data.Hidden(operation.actionUiModels)
    }

    private fun BottomBarState.toNewStateForErrorLoading() = when (this) {
        is BottomBarState.Data -> this
        is BottomBarState.Error.FailedLoadingActions -> this
        is BottomBarState.Loading -> BottomBarState.Error.FailedLoadingActions
    }

    private fun BottomBarState.toNewStateForHiding() = when (this) {
        is BottomBarState.Data.Shown -> BottomBarState.Data.Hidden(this.actions)
        else -> this
    }

    private fun BottomBarState.toNewStateForShowing() = when (this) {
        is BottomBarState.Data.Hidden -> BottomBarState.Data.Shown(this.actions)
        else -> this
    }
}
