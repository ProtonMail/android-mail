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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun UpsellingAutoRenewGenericPolicyText(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = UpsellingLayoutValues.autoRenewTextSize,
    color: Color = UpsellingLayoutValues.autoRenewText,
    planUiModel: DynamicPlanInstanceUiModel? = null,
    isShort: Boolean = false
) {
    val text = planUiModel?.let { getRenewalNoticeForPromotion(it, isShort) }
        ?: stringResource(R.string.upselling_auto_renew_text)

    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = ProtonTheme.typography.captionRegular,
        fontSize = fontSize,
        color = color,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun getRenewalNoticeForPromotion(planUiModel: DynamicPlanInstanceUiModel, short: Boolean): String {
    val displayedPrice = planUiModel.primaryPrice
    val period = planUiModel.cycle.cycleStringValue()
    val (baseText, price) = when (planUiModel) {
        is DynamicPlanInstanceUiModel.Promotional ->
            Pair(
                if (short) R.string.upselling_auto_renew_text_promo_short else R.string.upselling_auto_renew_text_promo,
                displayedPrice.secondaryPrice ?: displayedPrice.highlightedPrice
            )

        is DynamicPlanInstanceUiModel.Standard ->
            Pair(R.string.upselling_auto_renew_text_standard, displayedPrice.highlightedPrice)
    }


    return stringResource(baseText, planUiModel.currency, price.string(), period.string())
}
