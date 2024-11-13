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
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState.Data
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState.UpsellingScreenContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import javax.inject.Inject

internal class UpsellingContentReducer @Inject constructor(
    private val dynamicPlanUiMapper: DynamicPlanUiMapper
) {

    fun newStateFrom(operation: UpsellingScreenContentOperation): UpsellingScreenContentState {
        return when (operation) {
            is UpsellingScreenContentEvent.DataLoaded -> reducePlansListToNewState(operation)
            is UpsellingScreenContentEvent.LoadingError -> reduceErrorEvent(operation)
        }
    }

    private fun reducePlansListToNewState(
        operation: UpsellingScreenContentEvent.DataLoaded
    ): UpsellingScreenContentState = Data(
        dynamicPlanUiMapper.toUiModel(
            operation.userId,
            operation.plans,
            operation.upsellingEntryPoint
        )
    )

    private fun reduceErrorEvent(event: UpsellingScreenContentEvent.LoadingError) = when (event) {
        UpsellingScreenContentEvent.LoadingError.NoUserId -> UpsellingScreenContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
        )

        UpsellingScreenContentEvent.LoadingError.NoSubscriptions -> UpsellingScreenContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
        )
    }
}
