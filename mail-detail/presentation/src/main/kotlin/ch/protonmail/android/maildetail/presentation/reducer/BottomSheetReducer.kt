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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.presentation.model.BottomSheetAction
import ch.protonmail.android.maildetail.presentation.model.BottomSheetEvent
import ch.protonmail.android.maildetail.presentation.model.BottomSheetOperation
import ch.protonmail.android.maildetail.presentation.model.BottomSheetState
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import javax.inject.Inject

class BottomSheetReducer @Inject constructor() {

    fun newStateFrom(currentState: BottomSheetState, event: BottomSheetOperation): BottomSheetState {
        return when (event) {
            is BottomSheetEvent.Data -> BottomSheetState.Data(event.moveToDestinations, null)
            is BottomSheetAction.MoveToDestinationSelected -> currentState.toNewSelectedState(event.mailLabelId)
        }
    }

    private fun BottomSheetState.toNewSelectedState(mailLabelId: MailLabelId): BottomSheetState {

        return when (this) {
            is BottomSheetState.Loading -> this
            is BottomSheetState.Data -> {
                val listWithSelectedLabel = moveToDestinations.map { it.setSelectedIfLabelIdMatch(mailLabelId) }
                this.copy(
                    moveToDestinations = listWithSelectedLabel,
                    selected = listWithSelectedLabel.first { it.id == mailLabelId }
                )
            }
        }
    }

    private fun MailLabelUiModel.setSelectedIfLabelIdMatch(mailLabelId: MailLabelId) =
        when (this) {
            is MailLabelUiModel.Custom -> copy(isSelected = id == mailLabelId)
            is MailLabelUiModel.System -> copy(isSelected = id == mailLabelId)
        }
}
