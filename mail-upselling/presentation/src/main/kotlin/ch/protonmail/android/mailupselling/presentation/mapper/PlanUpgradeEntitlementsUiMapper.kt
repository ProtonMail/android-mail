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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements.FallPromoEntitlements
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements.MailPlusEntitlements
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements.PlusToUnlimitedEntitlements
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements.UnlimitedEntitlements
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementsListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.UnlimitedPlanVariant
import me.proton.android.core.payment.domain.model.ProductEntitlement
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import javax.inject.Inject

class PlanUpgradeEntitlementsUiMapper @Inject constructor() {

    fun toOnboardingUiModel(plan: ProductOfferDetail) = mapToDefaults(plan.metadata.entitlements)

    fun toTableUiModel(variant: PlanUpgradeVariant, entryPoint: UpsellingEntryPoint.Feature) =
        mapToComparisonTable(variant, entryPoint)

    private fun mapToComparisonTable(variant: PlanUpgradeVariant, entryPoint: UpsellingEntryPoint.Feature) = when {
        entryPoint == UpsellingEntryPoint.Feature.PlusUnlimited ->
            PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
                items = PlusToUnlimitedEntitlements,
                baseColumnLabel = TextUiModel.TextRes(R.string.upselling_plus_plan)
            )

        variant is PlanUpgradeVariant.FallPromo ->
            PlanUpgradeEntitlementsListUiModel.ComparisonTableList(FallPromoEntitlements)

        variant is UnlimitedPlanVariant ->
            PlanUpgradeEntitlementsListUiModel.ComparisonTableList(UnlimitedEntitlements)

        else -> PlanUpgradeEntitlementsListUiModel.ComparisonTableList(MailPlusEntitlements)
    }

    private fun mapToDefaults(list: List<ProductEntitlement>): List<PlanUpgradeEntitlementListUiModel> {
        return list.asSequence()
            .filterIsInstance(ProductEntitlement.Description::class.java)
            .map {
                // Always remote since we don't override with local ones here.
                PlanUpgradeEntitlementListUiModel.Remote(
                    TextUiModel.Text(it.text),
                    it.iconName.toString()
                )
            }
            .toList()
    }
}
