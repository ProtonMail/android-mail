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
import ch.protonmail.android.mailupselling.presentation.extension.totalPrice
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellBillingMessageUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellButtonsUiModel
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlans
import javax.inject.Inject

class OnboardingUpsellButtonsUiModelMapper @Inject constructor() {

    fun toUiModel(dynamicPlans: DynamicPlans): OnboardingUpsellButtonsUiModel {
        return OnboardingUpsellButtonsUiModel(
            billingMessage = dynamicPlans.plans.associate { plan ->
                plan.title to getBillingMessagesForPlan(plan)
            },
            getButtonLabel = dynamicPlans.plans.associate { plan ->
                plan.title to getButtonLabelForPlan(plan)
            }
        )
    }

    private fun getBillingMessagesForPlan(plan: DynamicPlan): OnboardingUpsellBillingMessageUiModel {
        val monthlyPlanInstance = requireNotNull(plan.instances[1])
        val monthlyPlanPrice = monthlyPlanInstance.price.values.first()
        val monthlyBillingMessage = TextUiModel.TextResWithArgs(
            R.string.upselling_onboarding_billing_monthly,
            listOf(monthlyPlanPrice.currency, monthlyPlanPrice.totalPrice())
        )

        val annualPlanInstance = requireNotNull(plan.instances[12])
        val annualPlanPrice = annualPlanInstance.price.values.first()
        val annualBillingMessage = TextUiModel.TextResWithArgs(
            R.string.upselling_onboarding_billing_annual,
            listOf(annualPlanPrice.currency, annualPlanPrice.totalPrice())
        )

        return OnboardingUpsellBillingMessageUiModel(
            monthlyBillingMessage = monthlyBillingMessage,
            annualBillingMessage = annualBillingMessage
        )
    }

    private fun getButtonLabelForPlan(plan: DynamicPlan): TextUiModel =
        TextUiModel.TextResWithArgs(R.string.upselling_onboarding_get_plan, listOf(plan.title))
}
