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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3

@Composable
internal fun ComparisonTableEntitlement(uiModel: ComparisonTableEntitlementItemUiModel, plusCellWidth: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ProtonDimens.ExtraSmallSpacing)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = uiModel.title.string(),
            style = ProtonTheme.typography.body2Regular,
            color = UpsellingLayoutValues.ComparisonTable.textColor,
            textAlign = TextAlign.Start
        )

        val freeText = when (val freeElement = uiModel.freeValue) {
            ComparisonTableEntitlement.Free.NotPresent ->
                stringResource(R.string.upselling_comparison_table_not_present)

            is ComparisonTableEntitlement.Free.Value -> freeElement.text.string()
        }

        Text(
            modifier = Modifier
                .width(plusCellWidth)
                .align(Alignment.CenterVertically),
            text = freeText,
            style = ProtonTheme.typography.body1Medium,
            color = UpsellingLayoutValues.ComparisonTable.textColor,
            textAlign = TextAlign.Center,
            fontSize = UpsellingLayoutValues.ComparisonTable.itemTextSize
        )

        Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))

        when (val paidValue = uiModel.paidValue) {
            ComparisonTableEntitlement.Plus.Present -> {
                Icon(
                    modifier = Modifier
                        .width(plusCellWidth)
                        .align(Alignment.CenterVertically),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_proton_checkmark_circle_filled),
                    tint = UpsellingLayoutValues.ComparisonTable.iconColor,
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
            }

            is ComparisonTableEntitlement.Plus.Value -> {
                Text(
                    modifier = Modifier
                        .width(plusCellWidth)
                        .align(Alignment.CenterVertically),
                    text = paidValue.text.string(),
                    style = ProtonTheme.typography.body1Medium,
                    color = UpsellingLayoutValues.ComparisonTable.textColor,
                    textAlign = TextAlign.Center,
                    fontSize = UpsellingLayoutValues.ComparisonTable.itemTextSize
                )
            }
        }
    }
}

@Preview
@Composable
private fun ComparisonTableEntitlementPreview() {
    ProtonTheme3 {
        ComparisonTableEntitlement(ComparisonTableElementPreviewData.Entitlements.last(), plusCellWidth = 30.dp)
    }
}
