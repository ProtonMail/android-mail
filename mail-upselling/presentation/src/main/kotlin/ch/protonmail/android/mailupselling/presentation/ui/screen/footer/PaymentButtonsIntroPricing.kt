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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen

@Composable
internal fun PaymentButtonsIntroPricing(
    instance: PlanUpgradeInstanceUiModel.Promotional,
    actions: UpsellingScreen.Actions
) {
    val initialPrice = instance.primaryPrice.highlightedPrice.getShorthandFormat()

    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = ProtonDimens.Spacing.Large),
                    text = promoAutoRenewalNotice(instance),
                    style = ProtonTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            MailPurchaseButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                ctaText = stringResource(R.string.upselling_mailbox_plus_promo_cta, initialPrice),
                product = instance.product,
                onPurchaseClicked = actions.onUpgradeAttempt,
                variant = MailPurchaseButtonVariant.Default,
                onSuccess = { _ -> actions.onSuccess() },
                onErrorMessage = actions.onError
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

            ProtonSolidButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                onClick = actions.onDismiss,
                colors = ButtonDefaults.buttonColors().copy(containerColor = Color.Transparent)
            ) {
                Text(
                    text = stringResource(R.string.upselling_mailbox_plus_promo_skip),
                    style = ProtonTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
        }
    }
}

@Composable
private fun promoAutoRenewalNotice(plan: PlanUpgradeInstanceUiModel.Promotional): String {
    val price = plan.primaryPrice
    val baseText = R.string.upselling_mailbox_plus_promo_renewal
    val secondaryPrice = price.secondaryPrice ?: price.highlightedPrice
    val firstMonthPrice = price.highlightedPrice.getShorthandFormat()
    val renewalPrice = secondaryPrice.getShorthandFormat()

    return stringResource(baseText, firstMonthPrice, renewalPrice)
}

@Preview(showBackground = true, backgroundColor = 0xFF522580)
@AdaptivePreviews
@Composable
private fun UpsellingStickyFooterPromoPreview() {
    ProtonTheme {
        Box(modifier = Modifier.height(480.dp)) {
            PaymentButtonsIntroPricing(
                instance = UpsellingContentPreviewData.PromoList.shorterCycle as PlanUpgradeInstanceUiModel.Promotional,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
