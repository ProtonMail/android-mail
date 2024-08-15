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
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingEntryPoint
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan

internal sealed interface UpsellingBottomSheetContentState {

    data object Loading : UpsellingBottomSheetContentState
    data class Data(val plans: DynamicPlansUiModel) : UpsellingBottomSheetContentState
    data class Error(val error: Effect<TextUiModel>) : UpsellingBottomSheetContentState

    sealed interface UpsellingBottomSheetContentOperation {

        sealed interface UpsellingBottomSheetContentEvent : UpsellingBottomSheetContentOperation {

            data class DataLoaded(
                val userId: UserId,
                val plans: DynamicPlan,
                val upsellingEntryPoint: UpsellingEntryPoint
            ) : UpsellingBottomSheetContentEvent
            sealed interface LoadingError : UpsellingBottomSheetContentEvent {
                data object NoUserId : LoadingError
                data object NoSubscriptions : LoadingError
            }
        }
    }
}
