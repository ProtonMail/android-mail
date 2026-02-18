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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.cycleStringValue
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues

@Composable
internal fun UpsellingAutoRenewGenericPolicyText(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = UpsellingLayoutValues.autoRenewTextSize,
    color: Color = UpsellingLayoutValues.autoRenewText,
    planUiModel: PlanUpgradeInstanceUiModel? = null,
    isShort: Boolean = false
) {
    val text = planUiModel?.let { getRenewalNoticeForPromotion(it, isShort) }
        ?: stringResource(R.string.upselling_auto_renew_text)

    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = ProtonTheme.typography.labelSmall,
        fontWeight = FontWeight.Normal,
        fontSize = fontSize,
        color = color,
        textAlign = TextAlign.Center
    )
}

@Composable
@Suppress("DuplicateCaseInWhenExpression")
private fun getRenewalNoticeForPromotion(planUiModel: PlanUpgradeInstanceUiModel, short: Boolean): String {
    val displayedPrice = planUiModel.primaryPrice
    val period = planUiModel.cycle.cycleStringValue()
    val (baseText, price) = when (planUiModel) {
        is PlanUpgradeInstanceUiModel.Promotional.BlackFriday,
        is PlanUpgradeInstanceUiModel.Promotional.SpringPromo ->
            Pair(
                R.string.upselling_auto_renew_text_bfriday,
                displayedPrice.secondaryPrice?.getShorthandFormat()
                    ?: displayedPrice.highlightedPrice.getShorthandFormat()
            )

        is PlanUpgradeInstanceUiModel.Promotional.IntroductoryPrice ->
            Pair(
                if (short) R.string.upselling_auto_renew_text_promo_short else R.string.upselling_auto_renew_text_promo,
                displayedPrice.secondaryPrice?.getShorthandFormat()
                    ?: displayedPrice.highlightedPrice.getShorthandFormat()
            )

        is PlanUpgradeInstanceUiModel.Standard ->
            Pair(R.string.upselling_auto_renew_text_standard, displayedPrice.highlightedPrice.getShorthandFormat())
    }


    return stringResource(baseText, price, period.string())
}
