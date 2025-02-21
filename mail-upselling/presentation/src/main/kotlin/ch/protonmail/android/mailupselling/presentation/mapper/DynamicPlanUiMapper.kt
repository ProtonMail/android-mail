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

import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import javax.inject.Inject

internal class DynamicPlanUiMapper @Inject constructor(
    private val iconUiMapper: DynamicPlanIconUiMapper,
    private val titleUiMapper: DynamicPlanTitleUiMapper,
    private val descriptionUiMapper: DynamicPlanDescriptionUiMapper,
    private val planInstanceUiMapper: DynamicPlanInstanceUiMapper,
    private val entitlementsUiMapper: DynamicPlanEntitlementsUiMapper
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

        if (monthlyPlan == yearlyPlan) {
            return emptyUiModel.copy(list = DynamicPlanInstanceListUiModel.Invalid)
        }

        val (shorterCycleUiModel, longerCycleUiModel) = planInstanceUiMapper.toUiModel(
            userId,
            monthlyPlan,
            yearlyPlan,
            plan
        )

        val listUiModel = resolveListUiModel(shorterCycleUiModel, longerCycleUiModel)
        return emptyUiModel.copy(list = listUiModel)
    }

    private fun resolveListUiModel(
        shorterCycleUiModel: DynamicPlanInstanceUiModel,
        longerCycleUiModel: DynamicPlanInstanceUiModel
    ): DynamicPlanInstanceListUiModel.Data {
        return when {
            shorterCycleUiModel is DynamicPlanInstanceUiModel.Promotional ||
                longerCycleUiModel is DynamicPlanInstanceUiModel.Promotional ->
                DynamicPlanInstanceListUiModel.Data.Promotional(shorterCycleUiModel, longerCycleUiModel)

            else -> DynamicPlanInstanceListUiModel.Data.Standard(
                shorterCycleUiModel as DynamicPlanInstanceUiModel.Standard,
                longerCycleUiModel as DynamicPlanInstanceUiModel.Standard
            )
        }
    }
}
