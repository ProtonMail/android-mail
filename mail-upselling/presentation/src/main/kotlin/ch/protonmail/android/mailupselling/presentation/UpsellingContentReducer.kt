/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.mapper.PlanUpgradeUiMapper
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.ProductInstances
import javax.inject.Inject

internal class UpsellingContentReducer @Inject constructor(
    private val planUpgradeUiMapper: PlanUpgradeUiMapper
) {

    suspend fun newStateFrom(operation: UpsellingScreenContentOperation): UpsellingScreenContentState {
        return when (operation) {
            is UpsellingScreenContentEvent.DataLoaded -> reducePlansListToNewState(operation)
            is UpsellingScreenContentEvent.LoadingError -> reduceErrorEvent(operation)
        }
    }

    private suspend fun reducePlansListToNewState(
        operation: UpsellingScreenContentEvent.DataLoaded
    ): UpsellingScreenContentState {
        val uiModel = planUpgradeUiMapper.toUiModel(
            ProductInstances(operation.plans),
            operation.upsellingEntryPoint,
            operation.theme
        )

        return uiModel.fold(
            ifLeft = {
                UpsellingScreenContentState.Error(
                    error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
                )
            },
            ifRight = { UpsellingScreenContentState.Data(it) }
        )
    }

    private fun reduceErrorEvent(event: UpsellingScreenContentEvent.LoadingError) = when (event) {
        UpsellingScreenContentEvent.LoadingError.NoSubscriptions -> UpsellingScreenContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
        )
    }
}
