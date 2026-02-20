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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer

@Composable
internal fun UpsellingPlanButtonsFooter(
    modifier: Modifier = Modifier,
    plans: PlanUpgradeInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    val shouldShowIntroPriceFooter = plans is PlanUpgradeInstanceListUiModel.Data.IntroPrice &&
        plans.shorterCycle is PlanUpgradeInstanceUiModel.Promotional

    val shouldShowBlackFridayFooter = plans is PlanUpgradeInstanceListUiModel.Data.BlackFriday
    val shouldShowSpringPromoFooter = plans is PlanUpgradeInstanceListUiModel.Data.SpringPromo

    Column(
        modifier.background(UpsellingLayoutValues.UpsellingPlanButtonsFooter.backgroundColor)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(UpsellingLayoutValues.UpsellingPlanButtonsFooter.spacerHeight)
                .background(UpsellingLayoutValues.UpsellingPlanButtonsFooter.spacerColor)
        )

        when {
            shouldShowSpringPromoFooter -> when {
                plans.longerCycle is PlanUpgradeInstanceUiModel.Promotional.SpringPromo &&
                    plans.variant is PlanUpgradeVariant.SpringPromo ->
                    PaymentButtonsBlackFriday(plans.longerCycle, actions)
            }

            shouldShowBlackFridayFooter -> when {
                plans.longerCycle is PlanUpgradeInstanceUiModel.Promotional.BlackFriday &&
                    plans.variant == PlanUpgradeVariant.BlackFriday.Wave1 ->
                    PaymentButtonsBlackFriday(plans.longerCycle, actions)

                plans.shorterCycle is PlanUpgradeInstanceUiModel.Promotional.BlackFriday &&
                    plans.variant == PlanUpgradeVariant.BlackFriday.Wave2 ->
                    PaymentButtonsBlackFriday(plans.shorterCycle, actions)
            }

            shouldShowIntroPriceFooter -> PaymentButtonsIntroPricing(plans.shorterCycle, actions)
            else -> PaymentButtonsHorizontalLayout(plans, actions)
        }

        BottomNavigationBarSpacer()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF522580)
@AdaptivePreviews
@Composable
private fun UpsellingStickyFooterPreview() {
    ProtonTheme {
        Box(modifier = Modifier.height(480.dp)) {
            UpsellingPlanButtonsFooter(
                plans = UpsellingContentPreviewData.NormalList,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF522580)
@AdaptivePreviews
@Composable
private fun UpsellingStickyFooterPreview_PromoA() {
    ProtonTheme {
        Box(modifier = Modifier.height(480.dp)) {
            UpsellingPlanButtonsFooter(
                plans = UpsellingContentPreviewData.PromoList,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
