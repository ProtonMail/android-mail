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

package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanUiMapper
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.Data
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.UpsellingBottomSheetContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.UpsellingBottomSheetContentOperation.UpsellingBottomSheetContentEvent
import javax.inject.Inject

internal class UpsellingBottomSheetContentReducer @Inject constructor(
    private val dynamicPlanUiMapper: DynamicPlanUiMapper
) {

    fun newStateFrom(operation: UpsellingBottomSheetContentOperation): UpsellingBottomSheetContentState {
        return when (operation) {
            is UpsellingBottomSheetContentEvent.DataLoaded -> reducePlansListToNewState(operation)
            is UpsellingBottomSheetContentEvent.LoadingError -> reduceErrorEvent(operation)
        }
    }

    private fun reducePlansListToNewState(
        operation: UpsellingBottomSheetContentEvent.DataLoaded
    ): UpsellingBottomSheetContentState = Data(
        dynamicPlanUiMapper.toUiModel(
            operation.userId,
            operation.plans,
            operation.upsellingEntryPoint
        )
    )

    private fun reduceErrorEvent(event: UpsellingBottomSheetContentEvent.LoadingError) = when (event) {
        UpsellingBottomSheetContentEvent.LoadingError.NoUserId -> UpsellingBottomSheetContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
        )

        UpsellingBottomSheetContentEvent.LoadingError.NoSubscriptions -> UpsellingBottomSheetContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
        )
    }
}
