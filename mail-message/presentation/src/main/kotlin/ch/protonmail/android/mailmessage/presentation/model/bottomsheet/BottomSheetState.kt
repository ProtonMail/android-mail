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

package ch.protonmail.android.mailmessage.presentation.model.bottomsheet

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.label.domain.entity.LabelId

data class BottomSheetState(
    val contentState: BottomSheetContentState?,
    val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty()
) {

    fun isShowEffectWithoutContent() =
        bottomSheetVisibilityEffect == Effect.of(BottomSheetVisibilityEffect.Show) && contentState == null
}

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
        val moveToDestinations: ImmutableList<MailLabelUiModel>,
        val selected: MailLabelUiModel?
    ) : MoveToBottomSheetState

    object Loading : MoveToBottomSheetState

    sealed interface MoveToBottomSheetOperation : BottomSheetOperation

    sealed interface MoveToBottomSheetEvent : MoveToBottomSheetOperation {
        data class ActionData(val moveToDestinations: ImmutableList<MailLabelUiModel>) : MoveToBottomSheetEvent
    }

    sealed interface MoveToBottomSheetAction : MoveToBottomSheetOperation {
        data class MoveToDestinationSelected(val mailLabelId: MailLabelId) : MoveToBottomSheetAction
    }
}

sealed interface LabelAsBottomSheetState : BottomSheetContentState {

    data class Data(
        val labelUiModelsWithSelectedState: ImmutableList<LabelUiModelWithSelectedState>
    ) : LabelAsBottomSheetState

    object Loading : LabelAsBottomSheetState

    sealed interface LabelAsBottomSheetOperation : BottomSheetOperation

    sealed interface LabelAsBottomSheetEvent : LabelAsBottomSheetOperation {
        data class ActionData(
            val customLabelList: ImmutableList<MailLabelUiModel.Custom>,
            val selectedLabels: ImmutableList<LabelId>,
            val partiallySelectedLabels: ImmutableList<LabelId> = emptyList<LabelId>().toImmutableList()
        ) : LabelAsBottomSheetEvent
    }

    sealed interface LabelAsBottomSheetAction : LabelAsBottomSheetOperation {
        data class LabelToggled(val labelId: LabelId) : LabelAsBottomSheetAction
    }
}
