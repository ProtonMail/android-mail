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

package ch.protonmail.android.mailupselling.presentation.model.planupgrades

import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.YearlySaving
import me.proton.android.core.payment.presentation.model.Product

sealed class PlanUpgradeInstanceUiModel(
    open val name: String,
    open val discountRate: Int?,
    open val yearlySaving: YearlySaving?,
    open val cycle: PlanUpgradeCycle,
    open val product: Product
) {

    abstract val primaryPrice: PlanUpgradePriceDisplayUiModel

    data class Standard(
        override val name: String,
        private val pricePerCycle: PlanUpgradePriceUiModel,
        private val totalPrice: PlanUpgradePriceUiModel,
        override val yearlySaving: YearlySaving?,
        override val discountRate: Int?,
        override val cycle: PlanUpgradeCycle,
        override val product: Product
    ) : PlanUpgradeInstanceUiModel(
        name,
        discountRate,
        yearlySaving,
        cycle,
        product
    ) {

        override val primaryPrice: PlanUpgradePriceDisplayUiModel
            get() {
                val (displayedPrice, standardPrice) = when (cycle) {
                    PlanUpgradeCycle.Monthly -> Pair(pricePerCycle, null)
                    PlanUpgradeCycle.Yearly -> Pair(totalPrice, pricePerCycle)
                }

                return PlanUpgradePriceDisplayUiModel(
                    pricePerCycle = pricePerCycle,
                    highlightedPrice = displayedPrice,
                    secondaryPrice = standardPrice
                )
            }
    }

    sealed class Promotional(
        override val name: String,
        val pricePerCycle: PlanUpgradePriceUiModel,
        val promotionalPrice: PlanUpgradePriceUiModel,
        val renewalPrice: PlanUpgradePriceUiModel,
        override val yearlySaving: YearlySaving?,
        override val discountRate: Int?,
        override val cycle: PlanUpgradeCycle,
        override val product: Product
    ) : PlanUpgradeInstanceUiModel(
        name, discountRate, yearlySaving, cycle, product
    ) {

        override val primaryPrice: PlanUpgradePriceDisplayUiModel
            get() = PlanUpgradePriceDisplayUiModel(
                pricePerCycle = pricePerCycle,
                highlightedPrice = promotionalPrice,
                secondaryPrice = renewalPrice
            )

        data class IntroductoryPrice(
            private val params: Params
        ) : Promotional(
            params.name, params.pricePerCycle, params.promotionalPrice,
            params.renewalPrice, params.yearlySaving, params.discountRate,
            params.cycle, params.product
        )

        data class BlackFriday(
            private val params: Params
        ) : Promotional(
            params.name, params.pricePerCycle, params.promotionalPrice,
            params.renewalPrice, params.yearlySaving, params.discountRate,
            params.cycle, params.product
        )

        data class SpringPromo(
            private val params: Params
        ) : Promotional(
            params.name, params.pricePerCycle, params.promotionalPrice,
            params.renewalPrice, params.yearlySaving, params.discountRate,
            params.cycle, params.product
        )

        data class Params(
            val name: String,
            val pricePerCycle: PlanUpgradePriceUiModel,
            val promotionalPrice: PlanUpgradePriceUiModel,
            val renewalPrice: PlanUpgradePriceUiModel,
            val yearlySaving: YearlySaving?,
            val discountRate: Int?,
            val cycle: PlanUpgradeCycle,
            val product: Product
        )

        companion object {

            operator fun invoke(promoKind: PromoKind, params: Params): Promotional = when (promoKind) {
                PromoKind.IntroPrice -> IntroductoryPrice(params)
                PromoKind.BlackFriday -> BlackFriday(params)
                PromoKind.SpringPromo -> SpringPromo(params)
            }
        }
    }
}

enum class PromoKind {
    IntroPrice, BlackFriday, SpringPromo
}
