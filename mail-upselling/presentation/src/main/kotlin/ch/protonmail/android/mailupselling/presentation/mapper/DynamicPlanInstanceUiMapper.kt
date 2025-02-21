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

package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.extension.normalized
import ch.protonmail.android.mailupselling.presentation.extension.normalizedPrice
import ch.protonmail.android.mailupselling.presentation.extension.toDecimalString
import ch.protonmail.android.mailupselling.presentation.extension.totalDefaultPrice
import ch.protonmail.android.mailupselling.presentation.extension.totalPrice
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.usecase.GetDiscountRate
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import javax.inject.Inject

class DynamicPlanInstanceUiMapper @Inject constructor(
    private val getDiscountRate: GetDiscountRate
) {

    fun toUiModel(
        userId: UserId,
        monthlyPlanInstance: DynamicPlanInstance,
        yearlyPlanInstance: DynamicPlanInstance,
        plan: DynamicPlan
    ): Pair<DynamicPlanInstanceUiModel, DynamicPlanInstanceUiModel> {

        val monthlyUiModel = createPlanUiModel(
            userId = userId,
            dynamicPlan = plan,
            cycle = DynamicPlanCycle.Monthly,
            planInstance = monthlyPlanInstance
        )

        val yearlyUiModel = createPlanUiModel(
            userId = userId,
            dynamicPlan = plan,
            cycle = DynamicPlanCycle.Yearly,
            planInstance = yearlyPlanInstance,
            comparisonPriceInstance = monthlyPlanInstance
        )

        return Pair(monthlyUiModel, yearlyUiModel)
    }

    fun createPlanUiModel(
        dynamicPlan: DynamicPlan,
        userId: UserId,
        planInstance: DynamicPlanInstance,
        cycle: DynamicPlanCycle,
        comparisonPriceInstance: DynamicPlanInstance? = null
    ): DynamicPlanInstanceUiModel {
        val price = planInstance.price.values.first()
        val currentPrice = price.current
        val defaultPrice = price.default

        val isPromotional = defaultPrice != null && currentPrice < defaultPrice

        return if (isPromotional) {
            val promotionalPrice = currentPrice.normalized(cycle.months)
            val renewalPrice = defaultPrice.normalized(cycle.months)
            DynamicPlanInstanceUiModel.Promotional(
                name = dynamicPlan.title,
                userId = UserIdUiModel(userId),
                pricePerCycle = price.normalizedPrice(cycle.months),
                promotionalPrice = TextUiModel.Text(price.totalPrice().toDecimalString()),
                renewalPrice = TextUiModel.Text(price.totalDefaultPrice().toDecimalString()),
                discountRate = getDiscountRate(promotionalPrice, renewalPrice),
                currency = price.currency,
                cycle = cycle,
                viewId = computeViewId(userId, planInstance),
                dynamicPlan = dynamicPlan
            )
        } else {
            DynamicPlanInstanceUiModel.Standard(
                name = dynamicPlan.title,
                userId = UserIdUiModel(userId),
                pricePerCycle = price.normalizedPrice(cycle.months),
                totalPrice = TextUiModel.Text(price.totalPrice().toDecimalString()),
                discountRate = comparisonPriceInstance?.let { getDiscountRate(it, planInstance) },
                currency = price.currency,
                cycle = cycle,
                viewId = computeViewId(userId, planInstance),
                dynamicPlan = dynamicPlan
            )
        }
    }

    private fun computeViewId(userId: UserId, instance: DynamicPlanInstance) = "$userId$instance".hashCode()
}
