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
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellBillingMessageUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellButtonsUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanSwitcherUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModels

internal object OnboardingUpsellPreviewData {

    val PlanSwitcherUiModel = OnboardingUpsellPlanSwitcherUiModel(
        discount = TextUiModel.Text("Save 20%")
    )

    val PlanUiModels = OnboardingUpsellPlanUiModels(
        monthlyPlans = listOf(
            OnboardingUpsellPlanUiModel(
                title = "Unlimited",
                currency = "CHF",
                monthlyPrice = null,
                monthlyPriceWithDiscount = TextUiModel.Text("12.99"),
                entitlements = listOf(
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 3"),
                        localResource = R.drawable.ic_upselling_gift
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 4"),
                        localResource = R.drawable.ic_upselling_tag
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 5"),
                        localResource = R.drawable.ic_upselling_storage
                    )
                )
            ),
            OnboardingUpsellPlanUiModel(
                title = "Mail Plus",
                currency = "CHF",
                monthlyPrice = null,
                monthlyPriceWithDiscount = TextUiModel.Text("4.99"),
                entitlements = listOf(
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 3"),
                        localResource = R.drawable.ic_upselling_gift
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 4"),
                        localResource = R.drawable.ic_upselling_tag
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 5"),
                        localResource = R.drawable.ic_upselling_storage
                    )
                )
            ),
            OnboardingUpsellPlanUiModel(
                title = "Proton Free",
                currency = null,
                monthlyPrice = null,
                monthlyPriceWithDiscount = null,
                entitlements = listOf(
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    )
                )
            )
        ),
        annualPlans = listOf(
            OnboardingUpsellPlanUiModel(
                title = "Unlimited",
                currency = "CHF",
                monthlyPrice = TextUiModel.Text("12.99"),
                monthlyPriceWithDiscount = TextUiModel.Text("9.99"),
                entitlements = listOf(
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 3"),
                        localResource = R.drawable.ic_upselling_gift
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 4"),
                        localResource = R.drawable.ic_upselling_tag
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 5"),
                        localResource = R.drawable.ic_upselling_storage
                    )
                )
            ),
            OnboardingUpsellPlanUiModel(
                title = "Mail Plus",
                currency = "CHF",
                monthlyPrice = TextUiModel.Text("4.99"),
                monthlyPriceWithDiscount = TextUiModel.Text("3.99"),
                entitlements = listOf(
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 3"),
                        localResource = R.drawable.ic_upselling_gift
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 4"),
                        localResource = R.drawable.ic_upselling_tag
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 5"),
                        localResource = R.drawable.ic_upselling_storage
                    )
                )
            ),
            OnboardingUpsellPlanUiModel(
                title = "Proton Free",
                currency = null,
                monthlyPrice = null,
                monthlyPriceWithDiscount = null,
                entitlements = listOf(
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    DynamicEntitlementUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    )
                )
            )
        )
    )

    val ButtonsUiModel = OnboardingUpsellButtonsUiModel(
        billingMessage = mapOf(
            "Proton Unlimited" to OnboardingUpsellBillingMessageUiModel(
                monthlyBillingMessage = TextUiModel.Text("Billed at CHF 9.99 every month"),
                annualBillingMessage = TextUiModel.Text("Billed at CHF 119.88 every 12 months")
            ),
            "Mail Plus" to OnboardingUpsellBillingMessageUiModel(
                monthlyBillingMessage = TextUiModel.Text("Billed at CHF 9.99 every month"),
                annualBillingMessage = TextUiModel.Text("Billed at CHF 119.88 every 12 months")
            )
        ),
        getButtonLabel = mapOf(
            "Proton Unlimited" to TextUiModel.Text("Get Proton Unlimited"),
            "Mail Plus" to TextUiModel.Text("Get Mail Plus")
        )
    )
}
