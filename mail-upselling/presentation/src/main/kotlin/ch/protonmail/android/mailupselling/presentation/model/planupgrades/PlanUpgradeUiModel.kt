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

internal data class PlanUpgradeUiModel(
    val icon: PlanUpgradeIconUiModel,
    val title: PlanUpgradeTitleUiModel,
    val description: PlanUpgradeDescriptionUiModel,
    val variant: PlanUpgradeVariant,
    val entitlements: PlanUpgradeEntitlementsListUiModel,
    val list: PlanUpgradeInstanceListUiModel
)

sealed interface PlanUpgradeVariant {
    data object Normal : PlanUpgradeVariant
    data object IntroductoryPrice : PlanUpgradeVariant
    data object SocialProof : PlanUpgradeVariant

    sealed interface BlackFriday : PlanUpgradeVariant {
        data object Wave1 : BlackFriday
        data object Wave2 : BlackFriday
    }

    sealed interface SpringPromo : PlanUpgradeVariant {
        data object Wave1 : SpringPromo
        data object Wave2 : SpringPromo
    }
}
