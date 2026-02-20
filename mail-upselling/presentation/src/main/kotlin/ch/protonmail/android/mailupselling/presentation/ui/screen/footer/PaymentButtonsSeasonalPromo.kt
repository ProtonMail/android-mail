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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingVariantColors
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData.BlackFridayList
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.cyclebuttons.CycleOptionCard

@Composable
internal fun PaymentButtonsSeasonalPromo(
    instance: PlanUpgradeInstanceUiModel,
    buttonVariant: MailPurchaseButtonVariant = MailPurchaseButtonVariant.Default,
    actions: UpsellingScreen.Actions,
    colors: UpsellingVariantColors? = null
) {
    Column {
        Box(
            Modifier
                .wrapContentHeight()
                .padding(horizontal = ProtonDimens.Spacing.Large)
        ) {
            CycleOptionCard(
                cycleOptionUiModel = instance,
                modifier = Modifier.fillMaxWidth(),
                isSelected = false,
                colors = colors
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        Box(modifier = Modifier.fillMaxWidth()) {
            MailPurchaseButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                product = instance.product,
                variant = buttonVariant,
                onSuccess = { _ -> actions.onSuccess() },
                onErrorMessage = actions.onError
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.MediumLight))

        UpsellingAutoRenewGenericPolicyText(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.Spacing.Large),
            planUiModel = instance,
            color = colors?.autoRenewalColor ?: UpsellingLayoutValues.autoRenewText
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
    }
}

@AdaptivePreviews
@Composable
private fun PaymentButtonsHorizontalLayoutPreview() {
    ProtonTheme {
        Box(modifier = Modifier.height(480.dp)) {
            PaymentButtonsSeasonalPromo(
                instance = BlackFridayList.shorterCycle,
                actions = UpsellingScreen.Actions.Empty,
                buttonVariant = MailPurchaseButtonVariant.BlackFriday
            )
        }
    }
}
