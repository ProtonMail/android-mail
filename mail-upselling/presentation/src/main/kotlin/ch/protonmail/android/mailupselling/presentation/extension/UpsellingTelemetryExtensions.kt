/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation.extension

import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailupselling.presentation.model.UpsellingTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant

fun PlanUpgradeInstanceUiModel.toTelemetryPayload(
    modalVariant: UpsellModalVariant,
    upsellIsPromotional: Boolean,
    isIntroOffer: Boolean
) = UpsellingTelemetryPayload(
    selectedPlan = product.planName,
    selectedCycle = cycle.name,
    upsellIsPromotional = upsellIsPromotional,
    modalVariant = modalVariant,
    isIntroOffer = isIntroOffer
)

fun PlanUpgradeVariant.toUpsellModalVariant() = when (this) {
    is PlanUpgradeVariant.Normal.Unlimited -> UpsellModalVariant.COMPARISON_UNLIMITED
    else -> UpsellModalVariant.COMPARISON_PLUS
}

fun PlanUpgradeVariant.isIntroOffer() = this is PlanUpgradeVariant.IntroductoryPrice

fun PlanUpgradeVariant.isPromotional() = this !is PlanUpgradeVariant.Normal
