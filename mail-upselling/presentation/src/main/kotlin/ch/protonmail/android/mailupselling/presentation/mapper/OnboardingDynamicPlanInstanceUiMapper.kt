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

import ch.protonmail.android.mailupselling.presentation.model.OnboardingDynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import javax.inject.Inject

class OnboardingDynamicPlanInstanceUiMapper @Inject constructor() {

    fun toUiModel(
        userId: UserId,
        instance: DynamicPlanInstance,
        plan: DynamicPlan
    ): OnboardingDynamicPlanInstanceUiModel {
        val price = instance.price.values.first()
        return OnboardingDynamicPlanInstanceUiModel(
            userId = UserIdUiModel(userId),
            name = plan.title,
            cycle = instance.cycle,
            currency = price.currency,
            viewId = computeViewId(userId, instance),
            dynamicPlan = plan
        )
    }

    private fun computeViewId(userId: UserId, instance: DynamicPlanInstance) = "$userId$instance".hashCode()
}
