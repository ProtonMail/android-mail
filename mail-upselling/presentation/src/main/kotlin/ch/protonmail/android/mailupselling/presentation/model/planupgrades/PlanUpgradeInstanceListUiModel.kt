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

package ch.protonmail.android.mailupselling.presentation.model.planupgrades

import androidx.compose.runtime.Stable

sealed interface PlanUpgradeInstanceListUiModel {

    @Stable
    sealed class Data(
        open val shorterCycle: PlanUpgradeInstanceUiModel,
        open val longerCycle: PlanUpgradeInstanceUiModel,
        val variant: PlanUpgradeVariant
    ) : PlanUpgradeInstanceListUiModel {

        data class Standard(
            override val shorterCycle: PlanUpgradeInstanceUiModel.Standard,
            override val longerCycle: PlanUpgradeInstanceUiModel.Standard
        ) : Data(shorterCycle, longerCycle, PlanUpgradeVariant.Normal)

        class IntroPrice(
            override val shorterCycle: PlanUpgradeInstanceUiModel,
            override val longerCycle: PlanUpgradeInstanceUiModel
        ) : Data(shorterCycle, longerCycle, PlanUpgradeVariant.IntroductoryPrice)

        class BlackFriday(
            blackFridayVariant: PlanUpgradeVariant.BlackFriday,
            override val shorterCycle: PlanUpgradeInstanceUiModel,
            override val longerCycle: PlanUpgradeInstanceUiModel
        ) : Data(shorterCycle, longerCycle, blackFridayVariant)

        class SpringPromo(
            springPromoVariant: PlanUpgradeVariant.SpringPromo,
            override val shorterCycle: PlanUpgradeInstanceUiModel,
            override val longerCycle: PlanUpgradeInstanceUiModel
        ) : Data(shorterCycle, longerCycle, springPromoVariant)

        data class SocialProof(
            override val shorterCycle: PlanUpgradeInstanceUiModel,
            override val longerCycle: PlanUpgradeInstanceUiModel
        ) : Data(shorterCycle, longerCycle, PlanUpgradeVariant.SocialProof)
    }
}
