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

package ch.protonmail.android.mailupselling.domain.usecase

import ch.protonmail.android.mailupselling.domain.model.MonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import javax.inject.Inject
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.model.getBaseOffer

class GetMonthlyToYearlyOffer @Inject constructor(
    private val getOwnedMonthlyOffer: GetOwnedMonthlyOffer,
    private val getYearlySaving: GetYearlySaving,
    private val getDiscountRate: GetDiscountRate
) {

    suspend operator fun invoke(upgrades: List<ProductOfferList>): MonthlyToYearlyOffer? =
        UpsellVariantPlan.entries.firstNotNullOfOrNull { plan -> offerFor(plan, upgrades) }

    private suspend fun offerFor(plan: UpsellVariantPlan, upgrades: List<ProductOfferList>): MonthlyToYearlyOffer? {
        val yearly = upgrades.yearlyBaseOffer(plan.planName) ?: return null
        val monthly = getOwnedMonthlyOffer(yearly) ?: return null
        val saving = getYearlySaving(monthly, yearly.offer) ?: return null
        return MonthlyToYearlyOffer(plan, monthly, yearly, saving, getDiscountRate(monthly, yearly.offer))
    }

    private fun List<ProductOfferList>.yearlyBaseOffer(planName: String): ProductOfferDetail? = this
        .filter { it.metadata.planName == planName }
        .mapNotNull { it.getBaseOffer() }
        .firstOrNull { it.offer.current.cycle == PlanUpgradeCycle.Yearly.months }
}
