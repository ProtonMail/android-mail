/*
 * Copyright (c) 2026 Proton Technologies AG
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ch.protonmail.android.mailupselling.presentation.extension.toTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.extension.toUpsellModalVariant
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.cyclebuttons.CycleOptionCard

@Composable
internal fun PaymentButtonsPlusToUnlimited(
    plans: PlanUpgradeInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    val plan = plans.longerCycle
    val telemetryPayload = plan.toTelemetryPayload(
        modalVariant = plans.variant.toUpsellModalVariant(),
        upsellIsPromotional = false,
        isIntroOffer = false
    )

    Column {
        Text(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.Spacing.Large)
                .padding(top = ProtonDimens.Spacing.Large, bottom = ProtonDimens.Spacing.Small)
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.upselling_pu_choose_plan),
            style = ProtonTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = UpsellingLayoutValues.PaymentButtons.choosePlanColor,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.Large)
                .height(IntrinsicSize.Min)
        ) {
            CycleOptionCard(
                cycleOptionUiModel = plan,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                isSelected = true
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.MediumLight))

        UpsellingAutoRenewGenericPolicyText(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large),
            planUiModel = plan
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        MailPurchaseButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.Large),
            product = plan.product,
            variant = MailPurchaseButtonVariant.Default,
            onPurchaseClicked = { actions.onUpgradeAttempt(telemetryPayload) },
            onSuccess = { _ -> actions.onSuccess(telemetryPayload) },
            onErrorMessage = {
                actions.onUpgradeErrored(telemetryPayload)
                actions.onError(it)
            }
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        StayOnMailPlusButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.Large),
            onClick = { actions.onDoNotShowAgain() }
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
    }
}

@Composable
private fun StayOnMailPlusButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(ProtonDimens.Spacing.Massive)
            .background(
                color = UpsellingLayoutValues.UpsellingPlanButtonsFooter.backgroundColor,
                shape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.upselling_stay_on_mail_plus),
            style = ProtonTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = UpsellingLayoutValues.PaymentButtons.planNameColor
        )
    }
}

@AdaptivePreviews
@Composable
private fun PaymentButtonsPlusToUnlimitedPreview() {
    ProtonTheme {
        Box(modifier = Modifier.height(480.dp)) {
            PaymentButtonsPlusToUnlimited(
                plans = UpsellingContentPreviewData.PlusToUnlimitedList,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
