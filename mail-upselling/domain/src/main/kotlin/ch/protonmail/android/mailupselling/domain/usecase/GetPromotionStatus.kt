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

package ch.protonmail.android.mailupselling.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import javax.inject.Inject

class GetPromotionStatus @Inject constructor(
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val filterDynamicPlansByUserSubscription: FilterDynamicPlansByUserSubscription
) {

    suspend operator fun invoke(userId: UserId): PromoStatus {
        val plans = runCatching { getDynamicPlansAdjustedPrices(userId) }.getOrNull() ?: return PromoStatus.NO_PLANS
        val filteredPlans = filterDynamicPlansByUserSubscription(userId, plans)
        if (filteredPlans.isEmpty()) return PromoStatus.NO_PLANS
        val hasPromo = filteredPlans.any { it.hasPromotion() }
        return if (hasPromo) {
            PromoStatus.PROMO
        } else {
            PromoStatus.NORMAL
        }
    }
}

private fun DynamicPlan.hasPromotion(): Boolean {
    val planInstance = instances.values.first()

    val price = planInstance.price.values.first()
    val currentPrice = price.current
    val defaultPrice = price.default

    return defaultPrice != null && currentPrice < defaultPrice
}

enum class PromoStatus {
    NO_PLANS, NORMAL, PROMO
}
