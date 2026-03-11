/*
 * Copyright (c) 2025 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later vers ion.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailupselling.presentation.mapper

import android.content.Context
import ch.protonmail.android.mailupselling.domain.extensions.normalizedPrice
import ch.protonmail.android.mailupselling.domain.extensions.normalizedPriceWithCurrency
import ch.protonmail.android.mailupselling.domain.extensions.totalPriceWithCurrency
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeSupportedTags
import ch.protonmail.android.mailupselling.domain.model.isTaggedWith
import ch.protonmail.android.mailupselling.domain.usecase.GetDiscountRate
import ch.protonmail.android.mailupselling.domain.usecase.GetYearlySaving
import ch.protonmail.android.mailupselling.presentation.extension.toUiModel
import ch.protonmail.android.mailupselling.presentation.mapper.RenewalCycle.BiYearly
import ch.protonmail.android.mailupselling.presentation.mapper.RenewalCycle.Monthly
import ch.protonmail.android.mailupselling.presentation.mapper.RenewalCycle.Yearly
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PromoKind
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.model.Product
import javax.inject.Inject

class PlanUpgradeInstanceUiModelMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDiscountRate: GetDiscountRate,
    private val getYearlySaving: GetYearlySaving
) {

    fun toUiModel(
        monthlyPlanInstance: ProductOfferDetail,
        yearlyPlanInstance: ProductOfferDetail
    ): Pair<PlanUpgradeInstanceUiModel, PlanUpgradeInstanceUiModel> {

        val monthlyUiModel = createPlanUiModel(
            cycle = PlanUpgradeCycle.Monthly,
            productDetail = monthlyPlanInstance
        )

        val yearlyUiModel = createPlanUiModel(
            cycle = PlanUpgradeCycle.Yearly,
            productDetail = yearlyPlanInstance,
            comparisonPriceInstance = monthlyPlanInstance
        )

        return Pair(monthlyUiModel, yearlyUiModel)
    }

    private fun createPlanUiModel(
        productDetail: ProductOfferDetail,
        cycle: PlanUpgradeCycle,
        comparisonPriceInstance: ProductOfferDetail? = null
    ): PlanUpgradeInstanceUiModel {
        val currentPrice = productDetail.offer.current
        val defaultPrice = productDetail.offer.renew

        val promoKind = when {
            productDetail.isTaggedWith(PlanUpgradeSupportedTags.BlackFriday) -> PromoKind.BlackFriday
            productDetail.isTaggedWith(PlanUpgradeSupportedTags.SpringOffer) -> PromoKind.SpringPromo
            productDetail.isTaggedWith(PlanUpgradeSupportedTags.IntroductoryPrice) -> PromoKind.IntroPrice
            productDetail.offer.isBaseOffer -> null
            else -> null
        }
        val isPromotional = promoKind != null
        val currency = productDetail.offer.current.currency

        return if (isPromotional) {
            val promotionalPrice = currentPrice.normalizedPrice(cycle.months)
            val renewalPrice = defaultPrice.normalizedPrice(cycle.months)

            val isSeasonalOffer = promoKind == PromoKind.SpringPromo || promoKind == PromoKind.BlackFriday

            // In case of seasonal offer, discount % is based on monthly pricing, not on original same-cycle pricing.
            val discountRate = if (isSeasonalOffer && cycle == PlanUpgradeCycle.Yearly) {
                comparisonPriceInstance?.let { getDiscountRate(it, productDetail) }
            } else {
                getDiscountRate(promotionalPrice, renewalPrice)
            }

            val params = PlanUpgradeInstanceUiModel.Promotional.Params(
                name = productDetail.header.title,
                pricePerCycle = currentPrice.normalizedPriceWithCurrency(currency, cycle.months).toUiModel(),
                promotionalPrice = currentPrice.totalPriceWithCurrency(currency).toUiModel(),
                renewalPrice = defaultPrice.totalPriceWithCurrency(currency).toUiModel(),
                yearlySaving = comparisonPriceInstance?.let { getYearlySaving(it, productDetail) },
                discountRate = discountRate,
                cycle = cycle,
                product = productDetail.toProduct(context)
            )

            PlanUpgradeInstanceUiModel.Promotional(promoKind, params)
        } else {
            PlanUpgradeInstanceUiModel.Standard(
                name = productDetail.header.title,
                pricePerCycle = currentPrice.normalizedPriceWithCurrency(currency, cycle.months).toUiModel(),
                totalPrice = currentPrice.totalPriceWithCurrency(currency).toUiModel(),
                yearlySaving = comparisonPriceInstance?.let { getYearlySaving(it, productDetail) },
                discountRate = comparisonPriceInstance?.let { getDiscountRate(it, productDetail) },
                cycle = cycle,
                product = productDetail.toProduct(context)
            )
        }
    }
}

internal fun ProductOfferDetail.toProduct(context: Context): Product {
    return Product(
        planName = metadata.planName,
        productId = metadata.productId,
        accountId = requireNotNull(offer.current.customerId),
        offerToken = offer.token,
        cycle = offer.current.cycle,
        header = header,
        entitlements = metadata.entitlements,
        renewalText = getRenewalText(context)
    )
}

private fun ProductOfferDetail.getRenewalText(context: Context): String? {
    val res = context.resources

    fun getBlackFridayRenewal(): String? {
        return when (offer.renew.cycle) {
            Monthly -> res.getString(R.string.payment_bf_renew_monthly, offer.renew.formatted)
            Yearly -> res.getString(R.string.payment_bf_renew_annually, offer.renew.formatted)
            BiYearly -> res.getString(R.string.payment_welcome_offer_renew_biennially, offer.renew.formatted)
            else -> null
        }
    }

    fun getIntroPricingRenewal(): String? {
        return when (offer.renew.cycle) {
            Monthly -> res.getString(R.string.payment_welcome_offer_renew_monthly, offer.renew.formatted)
            Yearly -> res.getString(R.string.payment_welcome_offer_renew_annually, offer.renew.formatted)
            BiYearly -> res.getString(R.string.payment_welcome_offer_renew_biennially, offer.renew.formatted)
            else -> null
        }
    }

    return when {
        offer.isBaseOffer -> null
        offer.tags.value.contains("bf-promo") -> getBlackFridayRenewal()
        offer.tags.value.contains("introductory-price") -> getIntroPricingRenewal()
        offer.tags.value.contains("spring26") -> getBlackFridayRenewal()
        else -> res.getQuantityString(
            R.plurals.payment_welcome_offer_renew_other,
            offer.current.cycle,
            offer.renew.formatted
        )
    }

}

private object RenewalCycle {

    const val Monthly = 1
    const val Yearly = 12
    const val BiYearly = 24
}
