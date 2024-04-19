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

package ch.protonmail.android.mailupselling.domain.usecase

import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds.PlusPlanId
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds.UnlimitedPlanId
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlans
import javax.inject.Inject

class FilterDynamicPlansByUserSubscription @Inject constructor(
    private val isPaidUser: IsPaidUser,
    private val isPaidMailUser: IsPaidMailUser
) {

    suspend operator fun invoke(userId: UserId, dynamicPlans: DynamicPlans): List<DynamicPlan> {
        val isPaidUser = isPaidUser(userId).getOrElse { false }
        val isPaidMailUser = isPaidMailUser(userId).getOrElse { false }

        if (isPaidUser && isPaidMailUser) return emptyList()

        val filterPlanName = if (isPaidUser) UnlimitedPlanId else PlusPlanId
        val plans = dynamicPlans.plans.filter { it.name == filterPlanName && it.state == DynamicPlanState.Available }
        return dynamicPlans.copy(plans = plans).plans
    }
}
