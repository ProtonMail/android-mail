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

package ch.protonmail.android.mailupselling.presentation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan

internal sealed interface UpsellingScreenContentState {

    data object Loading : UpsellingScreenContentState
    data class Data(val plans: DynamicPlansUiModel) : UpsellingScreenContentState
    data class Error(val error: Effect<TextUiModel>) : UpsellingScreenContentState

    sealed interface UpsellingScreenContentOperation {

        sealed interface UpsellingScreenContentEvent : UpsellingScreenContentOperation {

            data class DataLoaded(
                val userId: UserId,
                val plans: DynamicPlan,
                val upsellingEntryPoint: UpsellingEntryPoint.Feature
            ) : UpsellingScreenContentEvent

            sealed interface LoadingError : UpsellingScreenContentEvent {
                data object NoUserId : LoadingError
                data object NoSubscriptions : LoadingError
            }
        }
    }
}

sealed interface BottomSheetVisibilityEffect {
    data object Show : BottomSheetVisibilityEffect
    data object Hide : BottomSheetVisibilityEffect
}
