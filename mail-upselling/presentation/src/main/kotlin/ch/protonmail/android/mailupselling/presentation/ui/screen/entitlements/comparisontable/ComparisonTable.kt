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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementsListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.planUpgradeVariantColors

@Composable
internal fun ComparisonTable(
    entitlementsUiModel: PlanUpgradeEntitlementsListUiModel.ComparisonTableList,
    variant: PlanUpgradeVariant,
    modifier: Modifier = Modifier
) {
    val colors = planUpgradeVariantColors(variant)
    var highlightHeight by remember { mutableStateOf(0.dp) }
    var plusCellHeaderWidth by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current

    Box(modifier = modifier.padding(horizontal = ProtonDimens.Spacing.Small)) {
        Box(
            modifier = Modifier
                .padding(top = ProtonDimens.Spacing.Small)
                .padding(end = ProtonDimens.Spacing.Standard)
                .background(
                    color = UpsellingLayoutValues.ComparisonTable.highlightBarColor,
                    shape = UpsellingLayoutValues.ComparisonTable.highlightBarShape
                )
                .width(plusCellHeaderWidth)
                .height(highlightHeight)
                .align(Alignment.BottomEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.Standard)
                .onGloballyPositioned { coordinates ->
                    with(localDensity) { highlightHeight = coordinates.size.height.toDp() }
                }
        ) {
            ComparisonTableHeaderRow(colors = colors, onPaidColumnPlaced = { plusCellHeaderWidth = it })

            entitlementsUiModel.items.forEachIndexed { index, item ->
                ComparisonTableEntitlement(
                    modifier = Modifier.padding(vertical = ProtonDimens.Spacing.Compact),
                    colors = colors,
                    uiModel = item,
                    plusCellWidth = plusCellHeaderWidth
                )

                if (index < entitlementsUiModel.items.size - 1) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ProtonDimens.Spacing.Small)
                            .height(UpsellingLayoutValues.ComparisonTable.spacerHeight)
                            .background(colors.tableDividerColor)
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ProtonDimens.Spacing.Small / 2)
                            .height(UpsellingLayoutValues.ComparisonTable.spacerHeight)
                    )
                }
            }
        }
    }
}

@Preview
@AdaptivePreviews
@Composable
private fun ComparisonTablePreview() {
    ProtonTheme {
        ComparisonTable(
            PlanUpgradeEntitlementsListUiModel.ComparisonTableList(ComparisonTableEntitlements.Entitlements),
            variant = PlanUpgradeVariant.IntroductoryPrice
        )
    }
}

@Preview
@AdaptivePreviews
@Composable
private fun ComparisonTablePreviewBlackFriday() {
    ProtonTheme {
        ComparisonTable(
            PlanUpgradeEntitlementsListUiModel.ComparisonTableList(ComparisonTableEntitlements.Entitlements),
            variant = PlanUpgradeVariant.BlackFriday.Wave1
        )
    }
}
