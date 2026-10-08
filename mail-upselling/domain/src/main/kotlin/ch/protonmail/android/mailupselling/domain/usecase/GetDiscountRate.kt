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
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import javax.inject.Inject
import kotlin.math.roundToInt

class GetDiscountRate @Inject constructor() {

    operator fun invoke(shorterInstance: ProductOfferDetail, longerInstance: ProductOfferDetail): Int? =
        invoke(shorterInstance.offer, longerInstance.offer)

    operator fun invoke(shorterOffer: ProductOffer, longerOffer: ProductOffer): Int? {
        val longerInstancePrice = BigDecimal(longerOffer.currentPrice)
        val shorterInstancePrice = BigDecimal(shorterOffer.renewalPrice)
        val longerInstanceCycle = BigDecimal(longerOffer.cycle)
        val shorterInstanceCycle = BigDecimal(shorterOffer.cycle)

        val ratio = runCatching {
            val longerInstanceRate = longerInstancePrice.divide(longerInstanceCycle, 2, RoundingMode.HALF_UP)
            val shorterInstanceRate = shorterInstancePrice.divide(shorterInstanceCycle, 2, RoundingMode.HALF_UP)

            longerInstanceRate.divide(shorterInstanceRate, 2, RoundingMode.HALF_UP)
        }.getOrNull() ?: return null

        return calculateDiscountFromRatio(ratio.toFloat())
    }

    operator fun invoke(promotionalPrice: BigDecimal, renewalPrice: BigDecimal): Int? {
        if (promotionalPrice == renewalPrice) return null

        val ratio = runCatching { promotionalPrice.divide(renewalPrice, 2, RoundingMode.HALF_UP) }
            .getOrNull()
            ?: return null

        return calculateDiscountFromRatio(ratio.toFloat())
    }

    private fun calculateDiscountFromRatio(ratio: Float): Int? {
        if (ratio <= 0f || ratio > 1f) return null

        @Suppress("MagicNumber")
        return ((1 - ratio) * 100).takeIf { it > 0 }?.roundToInt()
    }
}

private val ProductOffer.currentPrice: Long
    get() = current.amount

private val ProductOffer.renewalPrice: Long
    get() = renew.amount

private val ProductOffer.cycle: Int
    get() = current.cycle
