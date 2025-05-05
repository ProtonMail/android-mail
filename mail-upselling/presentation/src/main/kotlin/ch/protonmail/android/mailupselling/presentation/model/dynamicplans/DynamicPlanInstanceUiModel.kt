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

package ch.protonmail.android.mailupselling.presentation.model.dynamicplans

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import me.proton.core.plan.domain.entity.DynamicPlan

sealed class DynamicPlanInstanceUiModel(
    open val name: String,
    open val userId: UserIdUiModel,
    open val currency: String,
    open val discountRate: Int?,
    open val cycle: DynamicPlanCycle,
    open val viewId: Int,
    open val dynamicPlan: DynamicPlan
) {

    abstract val primaryPrice: DynamicPlanPriceDisplayUiModel

    data class Standard(
        override val name: String,
        override val userId: UserIdUiModel,
        private val pricePerCycle: TextUiModel,
        private val totalPrice: TextUiModel,
        override val discountRate: Int?,
        override val currency: String,
        override val cycle: DynamicPlanCycle,
        override val viewId: Int,
        override val dynamicPlan: DynamicPlan
    ) : DynamicPlanInstanceUiModel(
        name,
        userId,
        currency,
        discountRate,
        cycle,
        viewId,
        dynamicPlan
    ) {

        override val primaryPrice: DynamicPlanPriceDisplayUiModel
            get() {
                val (displayedPrice, standardPrice) = when (cycle) {
                    DynamicPlanCycle.Monthly -> Pair(pricePerCycle, null)
                    DynamicPlanCycle.Yearly -> Pair(totalPrice, pricePerCycle)
                }

                return DynamicPlanPriceDisplayUiModel(
                    pricePerCycle = pricePerCycle,
                    highlightedPrice = displayedPrice,
                    secondaryPrice = standardPrice
                )
            }
    }

    data class Promotional(
        override val name: String,
        override val userId: UserIdUiModel,
        private val pricePerCycle: TextUiModel,
        private val promotionalPrice: TextUiModel,
        private val renewalPrice: TextUiModel,
        override val discountRate: Int?,
        override val currency: String,
        override val cycle: DynamicPlanCycle,
        override val viewId: Int,
        override val dynamicPlan: DynamicPlan
    ) : DynamicPlanInstanceUiModel(
        name,
        userId,
        currency,
        discountRate,
        cycle,
        viewId,
        dynamicPlan
    ) {

        override val primaryPrice: DynamicPlanPriceDisplayUiModel
            get() = DynamicPlanPriceDisplayUiModel(
                pricePerCycle = pricePerCycle,
                highlightedPrice = promotionalPrice,
                secondaryPrice = renewalPrice
            )
    }
}

internal fun DynamicPlanInstanceUiModel.toTelemetryPayload(variant: DynamicPlansVariant = DynamicPlansVariant.Normal) =
    UpsellingTelemetryTargetPlanPayload(
        planName = dynamicPlan.name ?: "",
        planCycle = cycle.months,
        isPromotional = this is DynamicPlanInstanceUiModel.Promotional,
        isVariantB = variant == DynamicPlansVariant.PromoB,
        isSocialProofVariant = variant == DynamicPlansVariant.SocialProof
    )
