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

package me.proton.android.core.payment.domain.model

data class ProductOfferList(
    val metadata: ProductMetadata,
    val header: ProductSelectionHeader,
    val offers: List<ProductOffer>
)

data class PlayStoreProductOfferList(
    val metadata: PlayStoreProductMetadata,
    val offers: List<ProductOffer>
)

data class ProductSelectionHeader(
    val title: String,
    val description: String,
    val cycleText: String,
    val starred: Boolean
)

data class ProductOffer(
    val isBaseOffer: Boolean,
    val tags: ProductOfferTags,
    val token: ProductOfferToken,
    val current: ProductOfferPrice,
    val renew: ProductOfferPrice
)

@JvmInline
value class ProductOfferToken(val value: String)

data class ProductOfferPrice(
    val productId: String,
    val customerId: String?,
    val cycle: Int,
    val amount: Long,
    val currency: String,
    val formatted: String
)

fun ProductOfferList.getBaseOffer(): ProductOfferDetail? {
    val offer = this.offers.firstOrNull { it.isBaseOffer } ?: return null
    val header = ProductDetailHeader(
        title = header.title,
        description = header.description,
        priceText = offer.current.priceText(),
        cycleText = header.cycleText,
        starred = header.starred
    )
    return ProductOfferDetail(metadata, header, offer)
}

internal fun ProductOfferList.getProductForOffer(forOffer: ProductOffer): ProductOfferDetail? {
    val offer = this.offers.firstOrNull { it == forOffer } ?: return null
    val header = ProductDetailHeader(
        title = header.title,
        description = header.description,
        priceText = offer.current.priceText(),
        cycleText = header.cycleText,
        starred = header.starred
    )
    return ProductOfferDetail(metadata, header, offer)
}

private fun ProductOfferPrice.priceText() = replaceBareDollarSign(formatted, currency)
