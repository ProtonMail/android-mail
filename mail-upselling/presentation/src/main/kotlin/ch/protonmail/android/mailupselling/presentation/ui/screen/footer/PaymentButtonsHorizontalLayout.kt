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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.cyclebuttons.CycleOptions

@Composable
internal fun PaymentButtonsHorizontalLayout(
    plans: PlanUpgradeInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    var selectedPlan by remember { mutableStateOf(plans.longerCycle) }

    Column {
        Text(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.Spacing.Large)
                .padding(
                    top = ProtonDimens.Spacing.Large,
                    bottom = ProtonDimens.Spacing.Small
                )
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.upselling_select_plan),
            style = ProtonTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = UpsellingLayoutValues.PaymentButtons.choosePlanColor,
            textAlign = TextAlign.Center
        )

        Box(
            Modifier
                .wrapContentHeight()
                .padding(horizontal = ProtonDimens.Spacing.Large)
        ) {
            CycleOptions(
                plans = plans,
                selectedPlan = selectedPlan,
                onPlanSelected = { selectedPlan = it }
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        Box(modifier = Modifier.fillMaxWidth()) {
            MailPurchaseButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                product = selectedPlan.product,
                variant = MailPurchaseButtonVariant.Default,
                onSuccess = { _ -> actions.onSuccess() },
                onErrorMessage = actions.onError
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.MediumLight))

        UpsellingAutoRenewGenericPolicyText(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.Spacing.Large),
            planUiModel = selectedPlan
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
    }
}

@AdaptivePreviews
@Composable
private fun PaymentButtonsHorizontalLayoutPreview() {
    ProtonTheme {
        Box(modifier = Modifier.height(480.dp)) {
            PaymentButtonsHorizontalLayout(
                plans = UpsellingContentPreviewData.NormalList,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
