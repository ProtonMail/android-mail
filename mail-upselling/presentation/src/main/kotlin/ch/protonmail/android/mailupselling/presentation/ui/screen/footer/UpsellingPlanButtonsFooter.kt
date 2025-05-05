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
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import me.proton.core.compose.theme.ProtonTheme3

@Composable
internal fun UpsellingPlanButtonsFooter(
    modifier: Modifier = Modifier,
    plans: DynamicPlanInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    Column(
        modifier.background(UpsellingLayoutValues.UpsellingPlanButtonsFooter.backgroundColor)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(UpsellingLayoutValues.UpsellingPlanButtonsFooter.spacerHeight)
                .background(UpsellingLayoutValues.UpsellingPlanButtonsFooter.spacerColor)
        )

        when (plans) {
            is DynamicPlanInstanceListUiModel.Data.Promotional -> PaymentButtonsHorizontalLayout(plans, actions)
            is DynamicPlanInstanceListUiModel.Data.SocialProof -> PaymentButtonsHorizontalLayout(plans, actions)
            is DynamicPlanInstanceListUiModel.Data.PromotionalVariantB ->
                PaymentButtonsPromoLayout(plans.priceFormatted, plans.shorterCycle, actions)
            is DynamicPlanInstanceListUiModel.Data.Standard -> PaymentButtonsSideBySideLayout(plans, actions)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF522580)
@AdaptivePreviews
@Composable
private fun UpsellingStickyFooterPreview() {
    ProtonTheme3 {
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
    ProtonTheme3 {
        Box(modifier = Modifier.height(480.dp)) {
            UpsellingPlanButtonsFooter(
                plans = UpsellingContentPreviewData.PromoList,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
