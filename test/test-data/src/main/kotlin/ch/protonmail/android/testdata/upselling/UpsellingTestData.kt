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

package ch.protonmail.android.testdata.upselling

import me.proton.android.core.payment.domain.model.ProductDetailHeader
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.model.ProductOfferPrice
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.core.payment.domain.model.ProductSelectionHeader

object UpsellingTestData {

    private val emptyOfferTags = ProductOfferTags(emptySet())
    private val offerToken = ProductOfferToken("token")

    object MailPlusProducts {

        private val mailPlusMetadata = ProductMetadata(
            productId = "productId",
            planName = "mail2022",
            customerId = "customerId",
            entitlements = emptyList()
        )

        val MonthlyProductOfferDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = true,
                tags = emptyOfferTags,
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyProductOfferList = ProductOfferList(
            metadata = ProductMetadata(
                productId = "productId",
                customerId = "customerId",
                planName = "mail2022",
                entitlements = emptyList()
            ),
            header = ProductSelectionHeader(
                title = "Mail Plus",
                description = "Description",
                cycleText = "/month",
                starred = false
            ),
            offers = listOf(MonthlyProductOfferDetail.offer)
        )

        val MonthlyPromoProductOfferDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = false,
                tags = ProductOfferTags(setOf("introductory-price")),
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 9 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 9.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyPromoAndBFProductDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = false,
                tags = ProductOfferTags(setOf("introductory-price", "bf-promo")),
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 9 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 9.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyPromoAndSummerProductDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = false,
                tags = ProductOfferTags(setOf("introductory-price", "summer26")),
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 9 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 9.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyBFProductDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = false,
                tags = ProductOfferTags(setOf("bf-promo")),
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 9 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 9.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyBFProductOfferList = ProductOfferList(
            metadata = ProductMetadata(
                productId = "productId",
                customerId = "customerId",
                planName = "mail2022",
                entitlements = emptyList()
            ),
            header = ProductSelectionHeader(
                title = "Mail Plus",
                description = "Description",
                cycleText = "/month",
                starred = false
            ),
            offers = listOf(MonthlyProductOfferDetail.offer, MonthlyBFProductDetail.offer)
        )

        val YearlyProductOfferDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "EUR 108.00",
                cycleText = "/year",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = true,
                tags = emptyOfferTags,
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 12,
                    amount = 108 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 108.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 12,
                    amount = 108 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 108.00"
                )
            )
        )

        val YearlyProductOfferList = ProductOfferList(
            metadata = ProductMetadata(
                productId = "productId",
                customerId = "customerId",
                planName = "mail2022",
                entitlements = emptyList()
            ),
            header = ProductSelectionHeader(
                title = "Mail Plus",
                description = "Description",
                cycleText = "/year",
                starred = false
            ),
            offers = listOf(YearlyProductOfferDetail.offer)
        )

        val YearlyPromoProductDetail = ProductOfferDetail(
            metadata = mailPlusMetadata,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Description",
                priceText = "108.00 EUR",
                cycleText = "/year",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = false,
                tags = ProductOfferTags(setOf("introductory-price")),
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 12,
                    amount = 54 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 108.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 12,
                    amount = 108 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 108.00"
                )
            )
        )
    }

    object UnlimitedMailProduct {

        private val bundleMetadata = ProductMetadata(
            productId = "productId",
            planName = "bundle2022",
            customerId = "customerId",
            entitlements = emptyList()
        )

        val MonthlyProductOfferDetail = ProductOfferDetail(
            metadata = bundleMetadata,
            header = ProductDetailHeader(
                title = "Proton Unlimited",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = true,
                tags = emptyOfferTags,
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyPromoProductDetail = ProductOfferDetail(
            metadata = bundleMetadata,
            header = ProductDetailHeader(
                title = "Proton Unlimited",
                description = "Description",
                priceText = "EUR 12.00",
                cycleText = "/month",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = false,
                tags = ProductOfferTags(setOf("introductory-price")),
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 9 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 9.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 1,
                    amount = 12 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 12.00"
                )
            )
        )

        val MonthlyProductOfferList = ProductOfferList(
            metadata = ProductMetadata(
                productId = "productId",
                customerId = "customerId",
                planName = "bundle2022",
                entitlements = emptyList()
            ),
            header = ProductSelectionHeader(
                title = "Proton Unlimited",
                description = "Description",
                cycleText = "/month",
                starred = false
            ),
            offers = listOf(MonthlyProductOfferDetail.offer)
        )

        val YearlyProductDetail = ProductOfferDetail(
            metadata = bundleMetadata,
            header = ProductDetailHeader(
                title = "Proton Unlimited",
                description = "Description",
                priceText = "EUR 108.00",
                cycleText = "/year",
                starred = false
            ),
            offer = ProductOffer(
                isBaseOffer = true,
                tags = emptyOfferTags,
                token = offerToken,
                current = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 12,
                    amount = 108 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 108.00"
                ),
                renew = ProductOfferPrice(
                    productId = "productId",
                    customerId = "customerId",
                    cycle = 12,
                    amount = 108 * 1000 * 1000,
                    currency = "EUR",
                    formatted = "EUR 108.00"
                )
            )
        )

        val YearlyProductOfferList = ProductOfferList(
            metadata = ProductMetadata(
                productId = "productId",
                customerId = "customerId",
                planName = "bundle2022",
                entitlements = emptyList()
            ),
            header = ProductSelectionHeader(
                title = "Proton Unlimited",
                description = "Description",
                cycleText = "/year",
                starred = false
            ),
            offers = listOf(YearlyProductDetail.offer)
        )
    }
}
