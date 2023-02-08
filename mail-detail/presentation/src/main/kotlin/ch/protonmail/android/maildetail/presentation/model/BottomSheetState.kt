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
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import me.proton.core.label.domain.entity.LabelId

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

sealed interface LabelAsBottomSheetState : BottomSheetContentState {

    data class Data(
        val labelUiModelsWithSelectedState: List<LabelUiModelWithSelectedState>
    ) : LabelAsBottomSheetState

    object Loading : LabelAsBottomSheetState

    sealed interface LabelAsBottomSheetOperation : BottomSheetOperation

    sealed interface LabelAsBottomSheetEvent : LabelAsBottomSheetOperation {
        data class ActionData(
            val customLabelList: List<MailLabelUiModel.Custom>,
            val selectedLabels: List<LabelId>
        ) : LabelAsBottomSheetEvent
    }

    sealed interface LabelAsBottomSheetAction : LabelAsBottomSheetOperation {
        data class LabelToggled(val labelId: LabelId) : LabelAsBottomSheetAction
    }
}
