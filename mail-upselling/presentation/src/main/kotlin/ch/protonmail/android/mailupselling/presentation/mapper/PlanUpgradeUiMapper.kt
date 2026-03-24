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

package ch.protonmail.android.mailupselling.presentation.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.ProductInstances
import javax.inject.Inject

internal class PlanUpgradeUiMapper @Inject constructor(
    private val iconUiMapper: PlanUpgradeIconUiMapper,
    private val titleUiMapper: PlanUpgradeTitleUiMapper,
    private val descriptionUiMapper: PlanUpgradeDescriptionUiMapper,
    private val planInstanceUiMapper: PlanUpgradeInstanceUiModelMapper,
    private val entitlementsUiMapper: PlanUpgradeEntitlementsUiMapper,
    private val planUpgradeMapper: PlanUpgradeMapper
) {

    suspend fun toUiModel(
        products: ProductInstances,
        upsellingEntryPoint: UpsellingEntryPoint.Feature
    ): Either<PlanMappingError, PlanUpgradeUiModel> = either {
        if (products.instances.isEmpty()) raise(PlanMappingError.EmptyList)

        val monthlyPlan = products.instances.minBy { it.offer.current.cycle }
        val yearlyPlan = products.instances.maxBy { it.offer.current.cycle }

        if (monthlyPlan == yearlyPlan) raise(PlanMappingError.InvalidList)

        val variant = planUpgradeMapper.resolveVariant(
            monthlyPlan,
            yearlyPlan,
            upsellingEntryPoint
        )

        val (shorterCycleUiModel, longerCycleUiModel) = planInstanceUiMapper.toUiModel(
            monthlyPlan,
            yearlyPlan
        )

        return PlanUpgradeUiModel(
            icon = iconUiMapper.toUiModel(upsellingEntryPoint, variant),
            title = titleUiMapper.toUiModel(shorterCycleUiModel.primaryPrice, upsellingEntryPoint, variant),
            description = descriptionUiMapper.toUiModel(monthlyPlan, upsellingEntryPoint, variant),
            entitlements = entitlementsUiMapper.toTableUiModel(variant),
            variant = variant,
            list = planUpgradeMapper.resolveListUiModel(shorterCycleUiModel, longerCycleUiModel, variant)
        ).right()
    }
}

internal sealed interface PlanMappingError {
    data object EmptyList : PlanMappingError
    data object InvalidList : PlanMappingError
}
