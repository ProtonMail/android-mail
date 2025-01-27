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

package ch.protonmail.android.mailmessage.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.Data
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class LabelAsBottomSheetReducer @Inject constructor() {

    fun newStateFrom(
        currentState: BottomSheetState?,
        operation: LabelAsBottomSheetState.LabelAsBottomSheetOperation
    ): BottomSheetState? {
        return when (operation) {
            is ActionData -> operation.toNewBottomSheetState(currentState)
            is LabelToggled -> operation.toNewBottomSheetState(currentState)
        }
    }

    private fun ActionData.toNewBottomSheetState(currentState: BottomSheetState?): BottomSheetState {
        return BottomSheetState(
            contentState = Data(
                customLabelList.map { label ->
                    LabelUiModelWithSelectedState(
                        labelUiModel = label,
                        selectedState = label.id.toLabelSelectedState(selectedLabels, partiallySelectedLabels)
                    )
                }.toImmutableList(),
                entryPoint = entryPoint
            ),
            bottomSheetVisibilityEffect = currentState?.bottomSheetVisibilityEffect ?: Effect.empty()
        )
    }

    private fun LabelToggled.toNewBottomSheetState(currentState: BottomSheetState?): BottomSheetState? {
        return when (val contentState = currentState?.contentState) {
            is Data -> {
                val newLabelList = contentState.labelUiModelsWithSelectedState.map { labelUiModelWithSelectedState ->
                    if (labelUiModelWithSelectedState.labelUiModel.id.labelId.id == labelId.id) {
                        labelUiModelWithSelectedState.copy(
                            selectedState = when (labelUiModelWithSelectedState.selectedState) {
                                is LabelSelectedState.NotSelected,
                                is LabelSelectedState.PartiallySelected -> LabelSelectedState.Selected
                                is LabelSelectedState.Selected -> LabelSelectedState.NotSelected
                            }
                        )
                    } else {
                        labelUiModelWithSelectedState
                    }
                }.toImmutableList()
                BottomSheetState(
                    contentState = Data(newLabelList, contentState.entryPoint),
                    bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect
                )
            }
            else -> currentState
        }
    }

    private fun MailLabelId.toLabelSelectedState(
        selectedLabels: List<LabelId>,
        partiallySelectedLabels: List<LabelId>
    ): LabelSelectedState {
        return if (selectedLabels.contains(this.labelId)) {
            LabelSelectedState.Selected
        } else if (partiallySelectedLabels.contains(this.labelId)) {
            LabelSelectedState.PartiallySelected
        } else {
            LabelSelectedState.NotSelected
        }
    }

}
