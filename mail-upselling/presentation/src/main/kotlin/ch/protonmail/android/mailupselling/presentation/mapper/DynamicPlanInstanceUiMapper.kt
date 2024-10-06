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
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.normalizedPrice
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import javax.inject.Inject

class DynamicPlanInstanceUiMapper @Inject constructor() {

    fun toUiModel(
        userId: UserId,
        instance: DynamicPlanInstance,
        highlighted: Boolean,
        discount: Int?,
        plan: DynamicPlan
    ): DynamicPlanInstanceUiModel {
        val price = instance.price.values.first()
        return DynamicPlanInstanceUiModel(
            userId = UserIdUiModel(userId),
            name = plan.title,
            price = price.normalizedPrice(cycle = instance.cycle),
            cycle = instance.cycle,
            currency = price.currency,
            discount = discount?.let {
                TextUiModel.TextResWithArgs(R.string.upselling_discount_tag, listOf(it))
            },
            viewId = computeViewId(userId, instance),
            highlighted = highlighted,
            dynamicPlan = plan
        )
    }

    private fun computeViewId(userId: UserId, instance: DynamicPlanInstance) = "$userId$instance".hashCode()
}
