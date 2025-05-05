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

package ch.protonmail.android.mailupselling.presentation.model.dynamicplans

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

sealed interface DynamicPlanInstanceListUiModel {
    data object Empty : DynamicPlanInstanceListUiModel

    data object Invalid : DynamicPlanInstanceListUiModel

    sealed class Data(
        open val shorterCycle: DynamicPlanInstanceUiModel,
        open val longerCycle: DynamicPlanInstanceUiModel,
        val variant: DynamicPlansVariant
    ) : DynamicPlanInstanceListUiModel {

        data class Standard(
            override val shorterCycle: DynamicPlanInstanceUiModel.Standard,
            override val longerCycle: DynamicPlanInstanceUiModel.Standard
        ) : Data(shorterCycle, longerCycle, DynamicPlansVariant.Normal)

        class Promotional(
            override val shorterCycle: DynamicPlanInstanceUiModel,
            override val longerCycle: DynamicPlanInstanceUiModel
        ) : Data(shorterCycle, longerCycle, DynamicPlansVariant.PromoA)

        data class SocialProof(
            override val shorterCycle: DynamicPlanInstanceUiModel,
            override val longerCycle: DynamicPlanInstanceUiModel
        ) : Data(shorterCycle, longerCycle, DynamicPlansVariant.SocialProof)

        data class PromotionalVariantB(
            val main: DynamicPlanInstanceUiModel,
            val priceFormatted: TextUiModel,
            override val shorterCycle: DynamicPlanInstanceUiModel = main,
            override val longerCycle: DynamicPlanInstanceUiModel = main
        ) : Data(shorterCycle, longerCycle, DynamicPlansVariant.PromoB)
    }
}
