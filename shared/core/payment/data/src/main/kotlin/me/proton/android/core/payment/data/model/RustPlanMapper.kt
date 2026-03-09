/*
 * Copyright (C) 2025 Proton AG
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

@file:Suppress("MagicNumber")

package me.proton.android.core.payment.data.model

import me.proton.android.core.payment.domain.model.ProductDetailHeader
import me.proton.android.core.payment.domain.model.ProductEntitlement
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.ProductOfferPrice
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.core.payment.domain.model.SubscriptionDetail
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import uniffi.mail_uniffi.Plan
import uniffi.mail_uniffi.PlanDecoration
import uniffi.mail_uniffi.PlanEntitlement
import uniffi.mail_uniffi.PlanInstance
import uniffi.mail_uniffi.PlanVendorName
import uniffi.mail_uniffi.Subscription

private fun getFormattedPrice(amount: ULong?, currency: String?) = when {
    amount == null -> ""
    currency == null -> ""
    else -> amount.toDouble().formatCentsPriceDefaultLocale(currency)
}

fun Subscription.toSubscriptionDetail() = SubscriptionDetail(
    name = name ?: "free",
    header = ProductDetailHeader(
        title = title,
        description = description,
        priceText = getFormattedPrice(amount, currency),
        cycleText = cycleDescription ?: "",
        starred = false
    ),
    entitlements = entitlements.map { it.toProductEntitlement() },
    price = amount?.let {
        ProductOfferPrice(
            productId = "unknown",
            customerId = "unknown",
            cycle = requireNotNull(cycle).toInt(),
            amount = requireNotNull(amount).toLong(),
            currency = requireNotNull(currency),
            formatted = getFormattedPrice(amount, currency)
        )
    },
    renew = renewAmount?.let {
        ProductOfferPrice(
            productId = "unknown",
            customerId = "unknown",
            cycle = requireNotNull(cycle).toInt(),
            amount = requireNotNull(renewAmount).toLong(),
            currency = requireNotNull(currency),
            formatted = getFormattedPrice(renewAmount, currency)
        )
    },
    periodEnd = periodEnd?.toLong(),
    managedBy = external?.toInt()
)

fun Plan.toProductOfferDetail(instance: PlanInstance) = ProductOfferDetail(
    metadata = toProductMetadata(instance),
    header = toProductDetailHeader(instance),
    offer = ProductOffer(
        isBaseOffer = false,
        tags = ProductOfferTags(emptySet()),
        token = ProductOfferToken(""),
        current = instance.toProductOfferPrice(),
        renew = instance.toProductOfferPrice()
    )
)

fun Plan.toProductMetadata(instance: PlanInstance) = ProductMetadata(
    productId = requireNotNull(instance.vendors[PlanVendorName.GOOGLE]?.productId),
    customerId = requireNotNull(instance.vendors[PlanVendorName.GOOGLE]?.customerId),
    planName = requireNotNull(name),
    entitlements = entitlements.map { it.toProductEntitlement() }
)

fun Plan.toProductDetailHeader(instance: PlanInstance) = ProductDetailHeader(
    title = title,
    description = description,
    priceText = instance.price.first().let { getFormattedPrice(it.current, it.currency) },
    cycleText = instance.description,
    starred = decorations.any { it as? PlanDecoration.Starred != null }
)

fun PlanInstance.toProductOfferPrice() = ProductOfferPrice(
    productId = requireNotNull(vendors[PlanVendorName.GOOGLE]?.productId),
    customerId = requireNotNull(vendors[PlanVendorName.GOOGLE]?.customerId),
    cycle = cycle.toInt(),
    amount = price.first().current.toLong(),
    currency = price.first().currency,
    formatted = price.first().let { getFormattedPrice(it.current, it.currency) }
)

fun PlanEntitlement.toProductEntitlement() = when (this) {
    is PlanEntitlement.Description -> toProductEntitlement()
    is PlanEntitlement.Progress -> toProductEntitlement()
}

fun PlanEntitlement.Description.toProductEntitlement() = ProductEntitlement.Description(
    iconName = iconName,
    text = text,
    hint = hint
)

fun PlanEntitlement.Progress.toProductEntitlement() = ProductEntitlement.Progress(
    startText = title ?: "",
    iconName = iconName,
    endText = text,
    min = min.toLong(),
    max = max.toLong(),
    current = current.toLong()
)
