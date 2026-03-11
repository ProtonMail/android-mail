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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailupselling.domain.model.BlackFridayPhase
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeSupportedTags
import ch.protonmail.android.mailupselling.domain.model.SpringPromoPhase
import ch.protonmail.android.mailupselling.domain.model.isTaggedWith
import ch.protonmail.android.mailupselling.domain.usecase.GetCurrentBlackFridayPhase
import ch.protonmail.android.mailupselling.domain.usecase.GetCurrentSpringPromoPhase
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import javax.inject.Inject

class ResolveUpsellingVisibilityForPlans @Inject constructor(
    private val getCurrentBlackFridayPhase: GetCurrentBlackFridayPhase,
    private val getCurrentSpringPromoPhase: GetCurrentSpringPromoPhase
) {

    suspend operator fun invoke(plans: List<ProductOfferDetail>): UpsellingVisibility {
        val instances = plans.takeIf { it.size == 2 } // We always expect 2 instances (monthly + yearly)
            ?: return UpsellingVisibility.Hidden

        // Only check Spring Promo phase if offers are tagged for it
        if (instances.any { it.isTaggedWith(PlanUpgradeSupportedTags.SpringOffer) }) {
            val phase = getCurrentSpringPromoPhase()
            if (phase is SpringPromoPhase.Active) {
                return phase.toUpsellingVisibility()
            }
        }

        // Only check BF phase if offers are tagged for it
        if (instances.any { it.isTaggedWith(PlanUpgradeSupportedTags.BlackFriday) }) {
            val phase = getCurrentBlackFridayPhase()
            if (phase is BlackFridayPhase.Active) {
                return phase.toUpsellingVisibility()
            }
        }

        // Fallback to intro pricing or normal
        return when {
            instances.any { it.isTaggedWith(PlanUpgradeSupportedTags.IntroductoryPrice) } ->
                UpsellingVisibility.Promotional.IntroductoryPrice

            else -> UpsellingVisibility.Normal
        }
    }

    private fun BlackFridayPhase.Active.toUpsellingVisibility() = when (this) {
        BlackFridayPhase.Active.Wave1 -> UpsellingVisibility.Promotional.BlackFriday.Wave1
        BlackFridayPhase.Active.Wave2 -> UpsellingVisibility.Promotional.BlackFriday.Wave2
    }

    private fun SpringPromoPhase.Active.toUpsellingVisibility() = when (this) {
        SpringPromoPhase.Active.Wave1 -> UpsellingVisibility.Promotional.SpringPromo.Wave1
        SpringPromoPhase.Active.Wave2 -> UpsellingVisibility.Promotional.SpringPromo.Wave2
    }
}
