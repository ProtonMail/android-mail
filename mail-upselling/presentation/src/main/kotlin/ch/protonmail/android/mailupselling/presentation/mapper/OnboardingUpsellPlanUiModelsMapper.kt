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
import ch.protonmail.android.mailupselling.presentation.extension.normalizedPrice
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper.Companion.OnboardingFreeOverriddenEntitlements
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModels
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PROTON_FREE
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlans
import javax.inject.Inject

class OnboardingUpsellPlanUiModelsMapper @Inject constructor(
    private val dynamicPlanEntitlementsUiMapper: DynamicPlanEntitlementsUiMapper,
    private val onboardingDynamicPlanInstanceUiMapper: OnboardingDynamicPlanInstanceUiMapper
) {

    fun toUiModel(dynamicPlans: DynamicPlans, userId: UserId): OnboardingUpsellPlanUiModels {
        val freePlan = OnboardingUpsellPlanUiModel(
            title = PROTON_FREE,
            currency = null,
            monthlyPrice = null,
            monthlyPriceWithDiscount = null,
            entitlements = OnboardingFreeOverriddenEntitlements,
            payButtonPlanUiModel = null
        )

        val monthlyPlans = dynamicPlans.plans.map { plan ->
            val monthlyPlanInstance = requireNotNull(plan.instances[1])
            val monthlyPlanPrice = monthlyPlanInstance.price.values.first()

            OnboardingUpsellPlanUiModel(
                title = plan.title,
                currency = monthlyPlanPrice.currency,
                monthlyPrice = null,
                monthlyPriceWithDiscount = monthlyPlanPrice.normalizedPrice(monthlyPlanInstance.cycle),
                entitlements = dynamicPlanEntitlementsUiMapper.toUiModel(plan, UpsellingEntryPoint.PostOnboarding),
                payButtonPlanUiModel = onboardingDynamicPlanInstanceUiMapper.toUiModel(
                    userId,
                    monthlyPlanInstance,
                    plan = plan
                )
            )
        }.toMutableList().apply {
            add(freePlan)
        }

        val annualPlans = dynamicPlans.plans.map { plan ->
            val monthlyPlanInstance = requireNotNull(plan.instances[1])
            val annualPlanInstance = requireNotNull(plan.instances[12])
            val monthlyPlanPrice = monthlyPlanInstance.price.values.first()
            val annualPlanPrice = annualPlanInstance.price.values.first()

            OnboardingUpsellPlanUiModel(
                title = plan.title,
                currency = annualPlanPrice.currency,
                monthlyPrice = monthlyPlanPrice.normalizedPrice(monthlyPlanInstance.cycle),
                monthlyPriceWithDiscount = annualPlanPrice.normalizedPrice(annualPlanInstance.cycle),
                entitlements = dynamicPlanEntitlementsUiMapper.toUiModel(plan, UpsellingEntryPoint.PostOnboarding),
                payButtonPlanUiModel = onboardingDynamicPlanInstanceUiMapper.toUiModel(
                    userId,
                    annualPlanInstance,
                    plan = plan
                )
            )
        }.toMutableList().apply {
            add(freePlan)
        }

        return OnboardingUpsellPlanUiModels(
            monthlyPlans = monthlyPlans,
            annualPlans = annualPlans
        )
    }
}
