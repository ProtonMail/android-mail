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
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.normalizedPrice
import ch.protonmail.android.mailupselling.presentation.extension.toDecimalString
import ch.protonmail.android.mailupselling.presentation.extension.totalDefaultPriceNullable
import ch.protonmail.android.mailupselling.presentation.extension.totalPrice
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper.Companion.OnboardingFreeOverriddenEntitlements
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModels
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPriceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PROTON_FREE
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import me.proton.core.plan.domain.entity.DynamicPlans
import javax.inject.Inject

class OnboardingUpsellPlanUiModelsMapper @Inject constructor(
    private val dynamicPlanEntitlementsUiMapper: DynamicPlanEntitlementsUiMapper,
    private val dynamicPlanInstanceUiMapper: DynamicPlanInstanceUiMapper
) {

    fun toUiModel(dynamicPlans: DynamicPlans, userId: UserId): OnboardingUpsellPlanUiModels {
        val freePlan = OnboardingUpsellPlanUiModel(
            title = PROTON_FREE,
            priceUiModel = OnboardingUpsellPriceUiModel.Free,
            entitlements = PlanEntitlementsUiModel.SimpleList(OnboardingFreeOverriddenEntitlements),
            payButtonPlanUiModel = null,
            premiumValueDrawables = emptyList()
        )

        val monthlyPlans = getMonthlyPlans(userId, dynamicPlans) + freePlan
        val annualPlans = getAnnualPlans(userId, dynamicPlans) + freePlan

        return OnboardingUpsellPlanUiModels(
            monthlyPlans = monthlyPlans,
            annualPlans = annualPlans
        )
    }

    private fun getMonthlyPlans(userId: UserId, dynamicPlans: DynamicPlans): List<OnboardingUpsellPlanUiModel> {
        return dynamicPlans.plans.map { plan ->
            val monthlyPlanInstance = requireNotNull(plan.instances[1])
            val monthlyPlanPrice = monthlyPlanInstance.price.values.first()

            val priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                currency = monthlyPlanPrice.currency,
                originalAmount = monthlyPlanPrice.totalDefaultPriceNullable()?.let {
                    TextUiModel.Text(it.toDecimalString())
                },
                amount = monthlyPlanPrice.normalizedPrice(monthlyPlanInstance.cycle),
                period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
            )

            OnboardingUpsellPlanUiModel(
                title = plan.title,
                priceUiModel = priceUiModel,
                entitlements = dynamicPlanEntitlementsUiMapper.toListUiModel(plan, UpsellingEntryPoint.PostOnboarding),
                payButtonPlanUiModel = dynamicPlanInstanceUiMapper.createPlanUiModel(
                    userId = userId,
                    planInstance = monthlyPlanInstance,
                    cycle = DynamicPlanCycle.Monthly,
                    dynamicPlan = plan
                ),
                premiumValueDrawables = planNameToPremiumValueDrawables(plan.name)
            )
        }
    }

    private fun getAnnualPlans(userId: UserId, dynamicPlans: DynamicPlans): List<OnboardingUpsellPlanUiModel> {
        return dynamicPlans.plans.map { plan ->
            val monthlyPlanInstance = requireNotNull(plan.instances[1])
            val annualPlanInstance = requireNotNull(plan.instances[12])

            val annualPrice = annualPlanInstance.price.values.first()
            val annualOriginalPrice = resolveAnnualPlanOriginalPrice(annualPrice, monthlyPlanInstance)

            val priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                currency = annualPrice.currency,
                originalAmount = TextUiModel.Text(annualOriginalPrice.toDecimalString()),
                amount = TextUiModel.Text(annualPrice.totalPrice().toDecimalString()),
                period = TextUiModel.TextRes(R.string.upselling_onboarding_year)
            )

            OnboardingUpsellPlanUiModel(
                title = plan.title,
                priceUiModel = priceUiModel,
                entitlements = dynamicPlanEntitlementsUiMapper.toListUiModel(plan, UpsellingEntryPoint.PostOnboarding),
                payButtonPlanUiModel = dynamicPlanInstanceUiMapper.createPlanUiModel(
                    userId = userId,
                    planInstance = annualPlanInstance,
                    cycle = DynamicPlanCycle.Yearly,
                    dynamicPlan = plan
                ),
                premiumValueDrawables = planNameToPremiumValueDrawables(plan.name)
            )
        }
    }

    private fun resolveAnnualPlanOriginalPrice(
        annualPrice: DynamicPlanPrice,
        monthlyInstance: DynamicPlanInstance
    ): Float {
        val annualRenewalPrice = annualPrice.totalDefaultPriceNullable()
        if (annualRenewalPrice != null) return annualRenewalPrice

        val monthlyPrice = monthlyInstance.price.values.first()
        val standardMonthlyPrice = monthlyPrice.totalDefaultPriceNullable() ?: monthlyPrice.totalPrice()

        @Suppress("MagicNumber")
        return standardMonthlyPrice * 12
    }

    private fun planNameToPremiumValueDrawables(planName: String?): List<Int> {
        val plusDrawables = listOf(
            R.drawable.ic_upselling_logo_mail,
            R.drawable.ic_upselling_logo_calendar
        )

        val unlimitedDrawables = listOf(
            R.drawable.ic_upselling_logo_mail,
            R.drawable.ic_upselling_logo_calendar,
            R.drawable.ic_upselling_logo_vpn,
            R.drawable.ic_upselling_logo_drive,
            R.drawable.ic_upselling_logo_pass
        )

        return when (planName) {
            DynamicPlansOneClickIds.PlusPlanId -> plusDrawables
            DynamicPlansOneClickIds.UnlimitedPlanId -> unlimitedDrawables
            else -> emptyList()
        }
    }
}
