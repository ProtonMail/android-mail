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

package ch.protonmail.android.maildetail.presentation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel

data class BottomSheetState(
    val contentState: BottomSheetContentState?,
    val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty()
)

sealed interface BottomSheetVisibilityEffect {
    object Show : BottomSheetVisibilityEffect
    object Hide : BottomSheetVisibilityEffect
}

sealed interface BottomSheetContentState
sealed interface BottomSheetOperation {
    object Requested : BottomSheetOperation
    object Dismiss : BottomSheetOperation
}


sealed interface MoveToBottomSheetState : BottomSheetContentState {

    data class Data(
        val moveToDestinations: List<MailLabelUiModel>,
        val selected: MailLabelUiModel?
    ) : MoveToBottomSheetState

    object Loading : MoveToBottomSheetState

    sealed interface MoveToBottomSheetOperation : BottomSheetOperation

    sealed interface MoveToBottomSheetEvent : MoveToBottomSheetOperation {
        data class ActionData(val moveToDestinations: List<MailLabelUiModel>) : MoveToBottomSheetEvent
    }

    sealed interface MoveToBottomSheetAction : MoveToBottomSheetOperation {
        data class MoveToDestinationSelected(val mailLabelId: MailLabelId) : MoveToBottomSheetAction
    }
}
