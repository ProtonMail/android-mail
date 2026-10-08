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

import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.ProductOfferPrice
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.payment.billing.model.StoreOffer
import me.proton.android.payment.billing.model.StorePricingDuration
import me.proton.android.payment.billing.model.StorePricingPeriod
import me.proton.android.payment.billing.usecase.GetActivePurchases
import me.proton.android.payment.billing.usecase.GetStoreProducts
import timber.log.Timber
import java.util.Optional
import javax.inject.Inject

class GetOwnedMonthlyOffer @Inject constructor(
    private val getActivePurchases: Optional<GetActivePurchases>,
    private val getStoreProducts: Optional<GetStoreProducts>
) {

    suspend operator fun invoke(yearly: ProductOfferDetail): ProductOffer? {
        val productId = yearly.monthlyProductId
        if (!isOwned(productId)) {
            Timber.d("upsell: $productId is not an active Play Store purchase on this device")
            return null
        }
        return getStoreProducts.orElse(null)
            ?.invoke(listOf(productId))
            ?.onFailure { Timber.d(it, "upsell: could not read the Play Store product $productId") }
            ?.getOrNull()
            ?.firstOrNull { it.id == productId }
            ?.offers
            ?.filterIsInstance<StoreOffer.NonDiscounted>()
            ?.mapNotNull { it.toProductOffer(productId) }
            ?.firstOrNull { it.current.cycle == PlanUpgradeCycle.Monthly.months }
    }

    private suspend fun isOwned(productId: String): Boolean = getActivePurchases.orElse(null)
        ?.invoke()
        ?.onFailure { Timber.d(it, "upsell: could not read the Play Store purchases") }
        ?.getOrNull()
        ?.any { it.productId == productId && it.acknowledged }
        ?: false
}

private const val YEARLY_CYCLE_SEGMENT = "_12_"
private const val MONTHLY_CYCLE_SEGMENT = "_1_"

private val ProductOfferDetail.monthlyProductId: String
    get() = metadata.productId.replace(YEARLY_CYCLE_SEGMENT, MONTHLY_CYCLE_SEGMENT)

private fun StoreOffer.NonDiscounted.toProductOffer(productId: String): ProductOffer? {
    val current = pricingPeriods.firstOrNull() ?: return null
    return ProductOffer(
        isBaseOffer = true,
        tags = ProductOfferTags(tags.toSet()),
        token = ProductOfferToken(token),
        current = current.toPrice(productId),
        renew = pricingPeriods.last().toPrice(productId)
    )
}

private fun StorePricingPeriod.toPrice(productId: String) = ProductOfferPrice(
    productId = productId,
    customerId = null,
    cycle = cycleMonths,
    amount = amount,
    currency = currency,
    formatted = displayPrice
)

private val StorePricingPeriod.cycleMonths: Int
    get() = when (cycleDurationUnit) {
        StorePricingDuration.MONTH -> cycleDurationCount
        StorePricingDuration.YEAR -> cycleDurationCount * PlanUpgradeCycle.Yearly.months
        StorePricingDuration.DAY,
        StorePricingDuration.WEEK -> 0
    }
