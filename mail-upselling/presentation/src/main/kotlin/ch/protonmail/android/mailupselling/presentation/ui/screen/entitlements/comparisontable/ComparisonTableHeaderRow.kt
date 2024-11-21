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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun ComparisonTableHeaderRow(onPaidColumnPlaced: (Dp) -> Unit) {
    var paidColumnWidth by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = ProtonDimens.ExtraSmallSpacing / 2)
            .padding(bottom = ProtonDimens.DefaultSpacing)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(paidColumnWidth)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = stringResource(R.string.upselling_free_plan),
                style = ProtonTheme.typography.body1Medium,
                color = UpsellingLayoutValues.ComparisonTable.textColor,
                textAlign = TextAlign.Center,
                fontSize = UpsellingLayoutValues.ComparisonTable.titleColumnSize
            )
        }
        Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))

        Box(
            modifier = Modifier
                .wrapContentWidth()
                .onGloballyPositioned {
                    with(localDensity) {
                        val columnWidth = it.size.width.toDp()

                        onPaidColumnPlaced(columnWidth)
                        paidColumnWidth = columnWidth
                    }
                }
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        brush = UpsellingLayoutValues.ComparisonTable.plusBadgeGradient,
                        shape = UpsellingLayoutValues.ComparisonTable.plusBadgeShape
                    )
                    .padding(horizontal = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing)
                    .padding(vertical = ProtonDimens.ExtraSmallSpacing)
                    .wrapContentHeight(),
                text = stringResource(R.string.upselling_plus_plan),
                style = ProtonTheme.typography.body1Medium,
                color = UpsellingLayoutValues.ComparisonTable.textColor,
                textAlign = TextAlign.Center,
                fontSize = UpsellingLayoutValues.ComparisonTable.titleColumnSize
            )
        }
    }
}
