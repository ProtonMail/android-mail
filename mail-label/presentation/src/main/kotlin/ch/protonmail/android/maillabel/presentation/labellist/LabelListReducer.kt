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

package ch.protonmail.android.maillabel.presentation.labellist

import ch.protonmail.android.mailcommon.presentation.Effect
import javax.inject.Inject

class LabelListReducer @Inject constructor() {

    internal fun newStateFrom(currentState: LabelListState, operation: LabelListOperation): LabelListState {
        return when (operation) {
            is LabelListEvent.LabelListLoaded -> reduceLabelListLoaded(operation)
            is LabelListEvent.ErrorLoadingLabelList -> LabelListState.Loading(errorLoading = Effect.of(Unit))
            is LabelListEvent.OpenLabelForm,
            is LabelListViewAction.OnAddLabelClick -> {
                when (currentState) {
                    is LabelListState.Loading -> currentState
                    is LabelListState.ListLoaded.Data -> currentState.copy(openLabelForm = Effect.of(Unit))
                    is LabelListState.ListLoaded.Empty -> currentState.copy(openLabelForm = Effect.of(Unit))
                }
            }
        }
    }

    private fun reduceLabelListLoaded(operation: LabelListEvent.LabelListLoaded): LabelListState {
        return if (operation.labelList.isNotEmpty()) {
            LabelListState.ListLoaded.Data(labels = operation.labelList)
        } else {
            LabelListState.ListLoaded.Empty()
        }
    }
}
