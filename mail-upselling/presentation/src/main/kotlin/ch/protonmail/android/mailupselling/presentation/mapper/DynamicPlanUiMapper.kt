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

import ch.protonmail.android.mailupselling.domain.usecase.GetDiscountRate
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import javax.inject.Inject

internal class DynamicPlanUiMapper @Inject constructor(
    private val iconUiMapper: DynamicPlanIconUiMapper,
    private val titleUiMapper: DynamicPlanTitleUiMapper,
    private val descriptionUiMapper: DynamicPlanDescriptionUiMapper,
    private val planInstanceUiMapper: DynamicPlanInstanceUiMapper,
    private val entitlementsUiMapper: DynamicPlanEntitlementsUiMapper,
    private val getDiscountRate: GetDiscountRate
) {

    fun toUiModel(
        userId: UserId,
        plan: DynamicPlan,
        upsellingEntryPoint: UpsellingEntryPoint
    ): DynamicPlansUiModel {
        val emptyUiModel = DynamicPlansUiModel(
            icon = iconUiMapper.toUiModel(upsellingEntryPoint),
            title = titleUiMapper.toUiModel(upsellingEntryPoint),
            description = descriptionUiMapper.toUiModel(plan, upsellingEntryPoint),
            entitlements = entitlementsUiMapper.toUiModel(plan, upsellingEntryPoint),
            plans = listOf()
        )

        if (plan.instances.isEmpty()) return emptyUiModel

        val shorterCyclePlan = requireNotNull(plan.instances.minByOrNull { it.key }?.value)
        val longerCyclePlan = requireNotNull(plan.instances.maxByOrNull { it.key }?.value)

        val shorterCycleUiModel = planInstanceUiMapper.toUiModel(
            userId,
            shorterCyclePlan,
            highlighted = false,
            discount = null,
            plan = plan
        )

        if (shorterCyclePlan == longerCyclePlan) {
            return emptyUiModel.copy(plans = listOf(shorterCycleUiModel))
        }

        val discountRate = getDiscountRate(shorterCyclePlan, longerCyclePlan)

        val longerCycleUiModel = planInstanceUiMapper.toUiModel(
            userId,
            longerCyclePlan,
            highlighted = true,
            discount = discountRate,
            plan = plan
        )

        return emptyUiModel.copy(plans = listOf(shorterCycleUiModel, longerCycleUiModel))
    }
}
