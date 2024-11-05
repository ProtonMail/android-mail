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

package ch.protonmail.android.mailupselling.presentation.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import javax.inject.Inject

class GetOnboardingUpsellingPlans @Inject constructor(
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices
) {

    suspend operator fun invoke(userId: UserId): Either<GetOnboardingPlansError, DynamicPlans> = either {
        val plans = runCatching { getDynamicPlansAdjustedPrices(userId) }
            .getOrElse { raise(GetOnboardingPlansError.GenericError) }

        if (plans.plans.isEmpty()) raise(GetOnboardingPlansError.NoPlans)
        if (!plans.containExpectedPlans()) raise(GetOnboardingPlansError.MismatchingPlans)
        if (!plans.containExpectedPlanCycles()) raise(GetOnboardingPlansError.MismatchingPlanCycles)

        return plans.right()
    }

    private fun DynamicPlans.containExpectedPlans(): Boolean {
        val containExpectedPlansSize = plans.size == 2
        val containExpectedEntries = plans.map { it.name }.containsAll(setOf(Unlimited, MailPlus))

        return containExpectedPlansSize && containExpectedEntries
    }

    private fun DynamicPlans.containExpectedPlanCycles(): Boolean {
        val containsMonthlyAndYearly = plans.all {
            it.instances.keys.containsAll(setOf(MonthlyDuration, YearlyDuration))
        }

        val containsInstancesWithPrice = plans.all { plan ->
            plan.instances.all { it.value.price.isNotEmpty() }
        }

        return containsMonthlyAndYearly && containsInstancesWithPrice
    }

    sealed interface GetOnboardingPlansError {
        data object GenericError : GetOnboardingPlansError
        data object NoPlans : GetOnboardingPlansError
        data object MismatchingPlans : GetOnboardingPlansError
        data object MismatchingPlanCycles : GetOnboardingPlansError
    }

    private companion object {

        const val MonthlyDuration = 1
        const val YearlyDuration = 12

        val MailPlus = DynamicPlansOneClickIds.PlusPlanId
        val Unlimited = DynamicPlansOneClickIds.UnlimitedPlanId
    }
}
