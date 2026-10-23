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

package ch.protonmail.android.mailupselling.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import ch.protonmail.android.mailupselling.domain.extensions.totalPrice
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.YearlySaving
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import javax.inject.Inject

class GetYearlySaving @Inject constructor() {

    operator fun invoke(monthlyPlan: ProductOfferDetail, yearlyPlan: ProductOfferDetail): YearlySaving? =
        invoke(monthlyPlan.offer, yearlyPlan.offer)

    operator fun invoke(monthlyOffer: ProductOffer, yearlyOffer: ProductOffer): YearlySaving? {
        if (monthlyOffer.renew.cycle != PlanUpgradeCycle.Monthly.months) return null
        if (yearlyOffer.renew.cycle != PlanUpgradeCycle.Yearly.months) return null

        val currency = monthlyOffer.current.currency

        // Check on monthly renewal price since it's unaffected by promo/offers
        val rawValue = monthlyOffer.renew.totalPrice()
            .multiply(BigDecimal(PlanUpgradeCycle.Yearly.months))
            .minus(yearlyOffer.current.totalPrice())
            .setScale(2, RoundingMode.HALF_UP)

        return YearlySaving(currency, rawValue).takeIf { rawValue.signum() > 0 }
    }
}
