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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingVariantColors
import ch.protonmail.android.mailupselling.presentation.ui.planUpgradeVariantColors

@Composable
internal fun ComparisonTableHeaderRow(colors: UpsellingVariantColors, onPaidColumnPlaced: (Dp) -> Unit) {
    var paidColumnWidth by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = ProtonDimens.Spacing.Standard)
            .padding(bottom = ProtonDimens.Spacing.Large)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .widthIn(min = paidColumnWidth)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = stringResource(R.string.upselling_free_plan),
                style = ProtonTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.tableTextColor,
                textAlign = TextAlign.Center,
                fontSize = UpsellingLayoutValues.ComparisonTable.titleColumnSize
            )
        }
        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Large))

        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    with(localDensity) {
                        val columnWidth = it.size.width.toDp()

                        onPaidColumnPlaced(columnWidth)
                        paidColumnWidth = columnWidth
                    }
                }
        ) {
            Row {
                Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Small))
                PlusBadge(colors = colors)
                Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Small))
            }
        }
    }
}

@Composable
private fun PlusBadge(colors: UpsellingVariantColors) {
    Surface(
        modifier = Modifier
            .border(
                width = 2.dp,
                brush = colors.plusBadgeBorderBrush,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = colors.plusBadgeBackground
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.Spacing.Standard)
                .padding(vertical = ProtonDimens.Spacing.Compact),
            text = stringResource(R.string.upselling_plus_plan),
            style = ProtonTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.tableTextColor,
            textAlign = TextAlign.Center,
            fontSize = UpsellingLayoutValues.ComparisonTable.titleColumnSize
        )
    }
}

@Preview
@Composable
private fun ComparisonTableHeaderRowPreview() {
    ProtonTheme {
        ComparisonTableHeaderRow(
            colors = planUpgradeVariantColors(PlanUpgradeVariant.IntroductoryPrice)
        ) { }
    }
}
