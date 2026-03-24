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

package ch.protonmail.android.mailupselling.presentation.ui.onboarding

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradePlanType
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingPlanUpgradeUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData.NormalList

internal object OnboardingUpsellPreviewData {

    private val baseEntitlements =
        listOf(PlanUpgradeEntitlementListUiModel.Remote(TextUiModel("Entitlement"), "resource"))

    val mailPlusMonthly = OnboardingPlanUpgradeUiModel.Paid(
        PlanUpgradePlanType.MailPlus,
        variant = PlanUpgradeVariant.Normal.MailPlus,
        entitlements = baseEntitlements,
        cycle = PlanUpgradeCycle.Monthly,
        NormalList.shorterCycle
    )

    val mailPlusYearly = OnboardingPlanUpgradeUiModel.Paid(
        PlanUpgradePlanType.MailPlus,
        variant = PlanUpgradeVariant.Normal.MailPlus,
        entitlements = baseEntitlements,
        cycle = PlanUpgradeCycle.Yearly,
        NormalList.longerCycle
    )

    val unlimitedMonthly = OnboardingPlanUpgradeUiModel.Paid(
        PlanUpgradePlanType.Unlimited,
        variant = PlanUpgradeVariant.Normal.Unlimited,
        entitlements = baseEntitlements,
        cycle = PlanUpgradeCycle.Monthly,
        NormalList.shorterCycle
    )

    val unlimitedYearly = OnboardingPlanUpgradeUiModel.Paid(
        PlanUpgradePlanType.Unlimited,
        variant = PlanUpgradeVariant.Normal.Unlimited,
        entitlements = baseEntitlements,
        cycle = PlanUpgradeCycle.Yearly,
        NormalList.longerCycle
    )

    val freePlan = OnboardingPlanUpgradeUiModel.Free(
        planName = TextUiModel("Proton Free"),
        entitlements = baseEntitlements,
        currency = "CHF"
    )
}
