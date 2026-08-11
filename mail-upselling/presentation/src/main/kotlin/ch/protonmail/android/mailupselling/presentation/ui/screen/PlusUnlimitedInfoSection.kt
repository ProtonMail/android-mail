/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues

@Composable
internal fun PlusUnlimitedInfoSection() {
    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .height(1.dp)
            .background(UpsellingLayoutValues.PlusUnlimited.separatorBrush)
    )
    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
    Text(
        modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large),
        text = stringResource(R.string.upselling_pu_subtitle),
        style = ProtonTheme.typography.bodyLargeNorm,
        fontWeight = FontWeight.Normal,
        color = UpsellingLayoutValues.subtitleColor,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
    Image(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.Spacing.Large),
        painter = painterResource(id = R.drawable.illustration_pu_products),
        contentDescription = NO_CONTENT_DESCRIPTION,
        contentScale = ContentScale.FillWidth
    )
}

@Preview(widthDp = 412, showBackground = true, backgroundColor = 0xFF2E0F70)
@Composable
private fun PlusUnlimitedInfoSectionPreview() {
    ProtonTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlusUnlimitedInfoSection()
        }
    }
}
