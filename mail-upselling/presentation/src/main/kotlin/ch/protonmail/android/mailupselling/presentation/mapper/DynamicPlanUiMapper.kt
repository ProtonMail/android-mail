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
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceListUiModel
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
        upsellingEntryPoint: UpsellingEntryPoint.Feature
    ): DynamicPlansUiModel {
        val emptyUiModel = DynamicPlansUiModel(
            icon = iconUiMapper.toUiModel(upsellingEntryPoint),
            title = titleUiMapper.toUiModel(upsellingEntryPoint),
            description = descriptionUiMapper.toUiModel(plan, upsellingEntryPoint),
            entitlements = entitlementsUiMapper.toUiModel(plan, upsellingEntryPoint),
            list = DynamicPlanInstanceListUiModel.Empty
        )

        if (plan.instances.isEmpty()) return emptyUiModel

        val monthlyPlan = requireNotNull(plan.instances.minByOrNull { it.key }?.value)
        val yearlyPlan = requireNotNull(plan.instances.maxByOrNull { it.key }?.value)

        val shorterCycleUiModel = planInstanceUiMapper.toUiModel(
            userId,
            monthlyPlan,
            discount = null,
            highlighted = false,
            plan = plan
        )

        if (monthlyPlan == yearlyPlan) {
            return emptyUiModel.copy(list = DynamicPlanInstanceListUiModel.Invalid)
        }

        val discountRate = getDiscountRate(monthlyPlan, yearlyPlan)

        val longerCycleUiModel = planInstanceUiMapper.toUiModel(
            userId,
            yearlyPlan,
            discount = discountRate,
            highlighted = true,
            plan = plan
        )

        return emptyUiModel.copy(
            list = DynamicPlanInstanceListUiModel.Data(
                shorterCycleUiModel,
                longerCycleUiModel
            )
        )
    }
}
