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

import ch.protonmail.android.maildetail.presentation.model.BottomSheetContentState
import ch.protonmail.android.maildetail.presentation.model.BottomSheetOperation
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState.Data
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState.Loading
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState.MoveToBottomSheetAction
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState.MoveToBottomSheetEvent
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import javax.inject.Inject

class BottomSheetReducer @Inject constructor() {

    fun newStateFrom(currentState: BottomSheetContentState?, event: BottomSheetOperation): BottomSheetContentState? {
        return when (event) {
            is MoveToBottomSheetEvent.ActionData -> Data(event.moveToDestinations, null)
            is MoveToBottomSheetAction.MoveToDestinationSelected -> when (currentState) {
                is MoveToBottomSheetState -> currentState.toNewSelectedState(event.mailLabelId)
                else -> currentState
            }
            MoveToBottomSheetAction.Requested -> currentState
            BottomSheetOperation.Dismiss -> null
        }
    }

    private fun MoveToBottomSheetState.toNewSelectedState(mailLabelId: MailLabelId): MoveToBottomSheetState {
        return when (this) {
            is Loading -> this
            is Data -> {
                val listWIthSelectedLabel = moveToDestinations.map { it.setSelectedIfLabelIdMatch(mailLabelId) }
                this.copy(
                    moveToDestinations = listWIthSelectedLabel,
                    selected = listWIthSelectedLabel.first { it.id == mailLabelId }
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
