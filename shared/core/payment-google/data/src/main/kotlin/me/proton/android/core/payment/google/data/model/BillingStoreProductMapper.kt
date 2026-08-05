/*
 * Copyright (C) 2026 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.google.data.model

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import me.proton.android.payment.billing.model.StorePricingPhase
import me.proton.android.payment.billing.model.StoreProduct
import me.proton.android.payment.billing.model.StoreProductOffer

internal fun ProductDetails.toStoreProduct(): StoreProduct? {
    return subscriptionOfferDetails
        ?.map { it.toStoreProductOffer() }
        ?.takeIf { offers -> offers.any { it is StoreProductOffer.NonDiscounted } }
        ?.let { offers ->
            StoreProduct(
                id = productId,
                title = name,
                description = description,
                offers = offers
            )
        }
}

private fun SubscriptionOfferDetails.toStoreProductOffer(): StoreProductOffer {
    val phases = pricingPhases.pricingPhaseList.map { it.toStorePricingPhase() }
    // Base plan (no offerId) = non-discounted; any offer (with offerId) = discounted
    return if (offerId == null) {
        StoreProductOffer.NonDiscounted(token = offerToken, tags = offerTags, pricingPhases = phases)
    } else {
        StoreProductOffer.Discounted(token = offerToken, tags = offerTags, pricingPhases = phases)
    }
}

private fun PricingPhase.toStorePricingPhase(): StorePricingPhase = StorePricingPhase(
    priceAmount = priceAmountMicros,
    priceFormattedAmount = formattedPrice,
    currency = priceCurrencyCode,
    cycle = billingCycleCount,
    period = billingPeriod
)
