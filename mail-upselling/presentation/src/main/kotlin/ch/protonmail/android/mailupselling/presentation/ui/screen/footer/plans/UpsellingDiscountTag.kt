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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun UpsellingDiscountTag(modifier: Modifier = Modifier, discountRate: Int) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(colorStops = UpsellingLayoutValues.discountTagColorStops),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.upselling_discount_tag, discountRate.toString()),
            style = ProtonTheme.typography.overlineMedium,
            color = UpsellingLayoutValues.SquarePaymentButtons.discountTagTextColor
        )
    }
}

@Preview
@Composable
private fun UpsellingDiscountTagPreview() {
    ProtonTheme {
        UpsellingDiscountTag(discountRate = 20)
    }
}
