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

package ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.comparisontable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.ComparisonTable
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingVariantColors
import ch.protonmail.android.mailupselling.presentation.ui.planUpgradeVariantColors
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingCheckmark

@Composable
internal fun ComparisonTableEntitlement(
    uiModel: ComparisonTableEntitlementItemUiModel,
    colors: UpsellingVariantColors,
    plusCellWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = uiModel.title.string(),
            style = ProtonTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = colors.tableTextColor,
            textAlign = TextAlign.Start
        )

        val (freeText, freeTextColor) = when (val freeElement = uiModel.freeValue) {
            ComparisonTableEntitlement.Free.NotPresent ->
                Pair(
                    stringResource(R.string.upselling_comparison_table_not_present),
                    colors.tableTextColor.copy(alpha = 0.5f)
                )

            is ComparisonTableEntitlement.Free.Value -> Pair(freeElement.text.string(), colors.tableTextColor)
        }

        Text(
            modifier = Modifier
                .widthIn(min = plusCellWidth)
                .align(Alignment.CenterVertically),
            text = freeText,
            style = ProtonTheme.typography.labelMedium,
            fontSize = ComparisonTable.itemTextSize,
            color = freeTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Large))

        when (val paidValue = uiModel.paidValue) {
            ComparisonTableEntitlement.Plus.Present -> {
                Box(modifier = Modifier.widthIn(min = plusCellWidth)) {
                    UpsellingCheckmark(
                        tint = colors.checkmarkTint,
                        background = colors.checkmarkBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            is ComparisonTableEntitlement.Plus.Value -> {
                Text(
                    modifier = Modifier
                        .widthIn(min = plusCellWidth)
                        .align(Alignment.CenterVertically),
                    text = paidValue.text.string(),
                    style = ProtonTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.tableTextColor,
                    textAlign = TextAlign.Center,
                    fontSize = ComparisonTable.itemTextSize
                )
            }
        }
    }
}

@Preview
@Composable
private fun ComparisonTableEntitlementPreview() {
    ProtonTheme {
        ComparisonTableEntitlement(
            ComparisonTableEntitlements.Entitlements.last(),
            colors = planUpgradeVariantColors(PlanUpgradeVariant.BlackFriday.Wave1),
            plusCellWidth = 30.dp
        )
    }
}
