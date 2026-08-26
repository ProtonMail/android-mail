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
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeUiModel
import me.proton.android.core.payment.domain.model.ProductOfferDetail

internal sealed interface UpsellingScreenContentState {

    data object Loading : UpsellingScreenContentState
    data class Data(val plans: PlanUpgradeUiModel) : UpsellingScreenContentState
    data class Error(val error: Effect<TextUiModel>) : UpsellingScreenContentState
}

sealed interface UpsellingScreenContentOperation {

    sealed interface UpsellingScreenContentEvent : UpsellingScreenContentOperation {

        data class DataLoaded(
            val plans: List<ProductOfferDetail>,
            val upsellingEntryPoint: UpsellingEntryPoint.Feature,
            val theme: UpsellContentTheme?
        ) : UpsellingScreenContentEvent

        sealed interface LoadingError : UpsellingScreenContentEvent {
            data object NoSubscriptions : LoadingError
        }
    }
}
