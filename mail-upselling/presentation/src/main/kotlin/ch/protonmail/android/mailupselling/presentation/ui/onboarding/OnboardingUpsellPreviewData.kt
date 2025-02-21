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

package ch.protonmail.android.mailupselling.presentation.ui.onboarding

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellButtonsUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanSwitcherUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModels
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPriceUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.plan.domain.entity.DynamicEntitlement
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlanType

internal object OnboardingUpsellPreviewData {

    val PlanSwitcherUiModel = OnboardingUpsellPlanSwitcherUiModel(
        discount = TextUiModel.Text("Save 20%")
    )

    private val dynPlan = DynamicPlan(
        name = "Proton Unlimited",
        order = 1,
        state = DynamicPlanState.Available,
        title = "Proton Unlimited",
        description = "For more storage and all premium features across all Proton services.",
        entitlements = listOf(
            DynamicEntitlement.Description(text = "Unlimited hide-my-email aliases", iconUrl = "nourl"),
            DynamicEntitlement.Description(text = "Unlimited hide-my-email aliases", iconUrl = "nourl")
        ),
        type = IntEnum(1, DynamicPlanType.Primary)
    )

    val OnboardingDynamicPlanInstanceUiModel = DynamicPlanInstanceUiModel.Standard(
        name = "Upgrade to Mail Plus",
        userId = UserIdUiModel(UserId("12")),
        currency = "EUR",
        cycle = DynamicPlanCycle.Monthly,
        viewId = 123,
        dynamicPlan = dynPlan,
        pricePerCycle = TextUiModel("12.99"),
        totalPrice = TextUiModel("144.99"),
        discountRate = null
    )

    val PremiumValuePlusDrawables = listOf(
        R.drawable.ic_upselling_logo_mail,
        R.drawable.ic_upselling_logo_calendar
    )

    val PremiumValueUnlimitedDrawables = listOf(
        R.drawable.ic_upselling_logo_mail,
        R.drawable.ic_upselling_logo_calendar,
        R.drawable.ic_upselling_logo_vpn,
        R.drawable.ic_upselling_logo_drive,
        R.drawable.ic_upselling_logo_pass
    )

    val PlanUiModels = OnboardingUpsellPlanUiModels(
        monthlyPlans = listOf(
            OnboardingUpsellPlanUiModel(
                title = "Unlimited",
                priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                    currency = "CHF",
                    originalAmount = null,
                    amount = TextUiModel.Text("12.99"),
                    period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
                ),
                entitlements = PlanEntitlementsUiModel.SimpleList(
                    listOf(
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 1"),
                            localResource = R.drawable.ic_upselling_pass
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 2"),
                            localResource = R.drawable.ic_upselling_mail
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 3"),
                            localResource = R.drawable.ic_upselling_gift
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 4"),
                            localResource = R.drawable.ic_upselling_tag
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 5"),
                            localResource = R.drawable.ic_upselling_storage
                        )
                    )
                ),
                payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                premiumValueDrawables = PremiumValueUnlimitedDrawables
            ),
            OnboardingUpsellPlanUiModel(
                title = "Mail Plus",
                priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                    currency = "CHF",
                    originalAmount = null,
                    amount = TextUiModel.Text("4.99"),
                    period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
                ),
                entitlements = PlanEntitlementsUiModel.SimpleList(
                    listOf(
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 1"),
                            localResource = R.drawable.ic_upselling_pass
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 2"),
                            localResource = R.drawable.ic_upselling_mail
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 3"),
                            localResource = R.drawable.ic_upselling_gift
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 4"),
                            localResource = R.drawable.ic_upselling_tag
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 5"),
                            localResource = R.drawable.ic_upselling_storage
                        )
                    )
                ),
                payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                premiumValueDrawables = PremiumValuePlusDrawables
            ),
            OnboardingUpsellPlanUiModel(
                title = "Proton Free",
                priceUiModel = OnboardingUpsellPriceUiModel.Free,
                entitlements = PlanEntitlementsUiModel.SimpleList(
                    listOf(
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 1"),
                            localResource = R.drawable.ic_upselling_pass
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 2"),
                            localResource = R.drawable.ic_upselling_mail
                        )
                    )
                ),
                payButtonPlanUiModel = null,
                premiumValueDrawables = emptyList()
            )
        ),
        annualPlans = listOf(
            OnboardingUpsellPlanUiModel(
                title = "Unlimited",
                priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                    currency = "CHF",
                    originalAmount = TextUiModel.Text("12.99"),
                    amount = TextUiModel.Text("9.99"),
                    period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
                ),
                entitlements = PlanEntitlementsUiModel.SimpleList(
                    listOf(
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 1"),
                            localResource = R.drawable.ic_upselling_pass
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 2"),
                            localResource = R.drawable.ic_upselling_mail
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 3"),
                            localResource = R.drawable.ic_upselling_gift
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 4"),
                            localResource = R.drawable.ic_upselling_tag
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 5"),
                            localResource = R.drawable.ic_upselling_storage
                        )
                    )
                ),
                payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                premiumValueDrawables = PremiumValueUnlimitedDrawables
            ),
            OnboardingUpsellPlanUiModel(
                title = "Mail Plus",
                priceUiModel = OnboardingUpsellPriceUiModel.Paid(
                    currency = "CHF",
                    originalAmount = TextUiModel.Text("4.99"),
                    amount = TextUiModel.Text("3.99"),
                    period = TextUiModel.TextRes(R.string.upselling_onboarding_month)
                ),
                entitlements = PlanEntitlementsUiModel.SimpleList(
                    listOf(
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 1"),
                            localResource = R.drawable.ic_upselling_pass
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 2"),
                            localResource = R.drawable.ic_upselling_mail
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 3"),
                            localResource = R.drawable.ic_upselling_gift
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 4"),
                            localResource = R.drawable.ic_upselling_tag
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 5"),
                            localResource = R.drawable.ic_upselling_storage
                        )
                    )
                ),
                payButtonPlanUiModel = OnboardingDynamicPlanInstanceUiModel,
                premiumValueDrawables = PremiumValuePlusDrawables
            ),
            OnboardingUpsellPlanUiModel(
                title = "Proton Free",
                priceUiModel = OnboardingUpsellPriceUiModel.Free,
                entitlements = PlanEntitlementsUiModel.SimpleList(
                    listOf(
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 1"),
                            localResource = R.drawable.ic_upselling_pass
                        ),
                        PlanEntitlementListUiModel.Overridden(
                            text = TextUiModel.Text("Entitlement 2"),
                            localResource = R.drawable.ic_upselling_mail
                        )
                    )
                ),
                payButtonPlanUiModel = null,
                premiumValueDrawables = emptyList()
            )
        )
    )

    val ButtonsUiModel = OnboardingUpsellButtonsUiModel(
        getButtonLabel = mapOf(
            "Proton Unlimited" to TextUiModel.Text("Get Proton Unlimited"),
            "Mail Plus" to TextUiModel.Text("Get Mail Plus")
        )
    )
}
