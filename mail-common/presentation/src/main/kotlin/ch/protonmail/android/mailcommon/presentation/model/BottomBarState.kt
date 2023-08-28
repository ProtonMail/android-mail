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

package ch.protonmail.android.mailcommon.presentation.model

sealed interface BottomBarState {

    sealed interface Data : BottomBarState {

        val actions: List<ActionUiModel>

        data class Hidden(override val actions: List<ActionUiModel>) : Data
        data class Shown(override val actions: List<ActionUiModel>) : Data
    }

    object Loading : BottomBarState

    sealed interface Error : BottomBarState {
        object FailedLoadingActions : Error
    }
}

sealed interface BottomBarEvent {

    data class ActionsData(val actionUiModels: List<ActionUiModel>) : BottomBarEvent

    data class ShowAndUpdateActionsData(val actionUiModels: List<ActionUiModel>) : BottomBarEvent

    object ShowBottomSheet : BottomBarEvent
    object HideBottomSheet : BottomBarEvent

    object ErrorLoadingActions : BottomBarEvent
}
