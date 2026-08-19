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

package ch.protonmail.android.mailupselling.domain.sdk

import javax.inject.Inject
import kotlinx.datetime.DateTimePeriod
import me.proton.android.core.payment.domain.model.ProductEntitlement
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.model.ProductOfferPrice
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.core.payment.domain.model.ProductSelectionHeader
import me.proton.android.payment.common.model.Entitlement as SdkEntitlement
import me.proton.android.payment.product.model.Offer as SdkOffer
import me.proton.android.payment.product.model.PricingPhase as SdkPricingPhase
import me.proton.android.payment.product.model.Product as SdkProduct

class SdkProductOfferMapper @Inject constructor() {

    fun mapToProductOfferList(
        sdkProducts: List<SdkProduct>,
        legacyByProductId: Map<String, ProductOfferList>
    ): List<ProductOfferList> = sdkProducts.mapNotNull { sdk ->
        val legacy = legacyByProductId[sdk.id] ?: return@mapNotNull null
        sdk.mapOne(legacy)
    }

    private fun SdkProduct.mapOne(legacy: ProductOfferList): ProductOfferList {
        val customerId = legacy.metadata.customerId
        // base (non-discounted) offer first
        val (baseOffers, promoOffers) = offers.partition { it is SdkOffer.NonDiscounted }
        val offers = (baseOffers + promoOffers).map { offer -> offer.toProductOffer(id, customerId) }
        return ProductOfferList(
            metadata = ProductMetadata(
                productId = id,
                customerId = customerId,
                planName = legacy.metadata.planName,
                entitlements = entitlements.map { it.toProductEntitlement() }
            ),
            header = ProductSelectionHeader(
                title = title,
                description = description,
                cycleText = legacy.header.cycleText,
                starred = legacy.header.starred
            ),
            offers = offers
        )
    }

    private fun SdkOffer.toProductOffer(productId: String, customerId: String) = ProductOffer(
        isBaseOffer = this is SdkOffer.NonDiscounted,
        tags = ProductOfferTags(tags.toSet()),
        token = ProductOfferToken(token),
        current = pricingPhases.first().toPrice(productId, customerId),
        renew = pricingPhases.last().toPrice(productId, customerId)
    )

    private fun SdkPricingPhase.toPrice(productId: String, customerId: String) = ProductOfferPrice(
        productId = productId,
        customerId = customerId,
        cycle = DateTimePeriod.parse(period).let { it.months + it.years * MONTHS_PER_YEAR },
        amount = price.amount,
        currency = price.currency,
        formatted = price.formattedAmount
    )

    private fun SdkEntitlement.toProductEntitlement(): ProductEntitlement = when (this) {
        is SdkEntitlement.Description -> ProductEntitlement.Description(
            iconName = iconName,
            text = text,
            hint = hint
        )
        is SdkEntitlement.Progress -> ProductEntitlement.Progress(
            startText = title,
            iconName = iconName,
            endText = text,
            min = min.toLong(),
            max = max.toLong(),
            current = current.toLong()
        )
    }

    private companion object {
        const val MONTHS_PER_YEAR = 12
    }
}
