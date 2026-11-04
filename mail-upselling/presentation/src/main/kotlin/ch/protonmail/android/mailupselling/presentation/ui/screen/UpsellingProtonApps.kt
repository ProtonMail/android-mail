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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues

/** The Proton apps that come with a paid plan, under the upsell's description. */
@Composable
internal fun UpsellingProtonApps(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = ProtonDimens.Spacing.Large)) {
        HorizontalDivider(color = UpsellingLayoutValues.ProtonApps.dividerColor)
        Row(
            modifier = Modifier.padding(top = ProtonDimens.Spacing.ExtraLarge, bottom = ProtonDimens.Spacing.Large),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            protonApps.forEach { app -> ProtonApp(app, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ProtonApp(app: ProtonAppUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(LogoAreaHeight), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(app.logo),
                contentDescription = null,
                modifier = Modifier.size(app.logoWidth, app.logoHeight)
            )
        }
        Text(
            text = stringResource(app.name),
            style = ProtonTheme.typography.bodySmall.copy(lineHeight = NameLineHeight),
            color = UpsellingLayoutValues.subtitleColor,
            textAlign = TextAlign.Center
        )
    }
}

private data class ProtonAppUiModel(
    @DrawableRes val logo: Int,
    @StringRes val name: Int,
    val logoWidth: Dp,
    val logoHeight: Dp
)

// The logos are drawn at the design's 1.25 scale of their 32dp marks.
private val protonApps = listOf(
    ProtonAppUiModel(R.drawable.ic_upselling_product_mail, R.string.upselling_product_mail, 27.5.dp, 22.5.dp),
    ProtonAppUiModel(R.drawable.ic_upselling_product_calendar, R.string.upselling_product_calendar, 27.5.dp, 22.5.dp),
    ProtonAppUiModel(R.drawable.ic_upselling_product_drive, R.string.upselling_product_drive, 27.5.dp, 22.5.dp),
    ProtonAppUiModel(R.drawable.ic_upselling_product_vpn, R.string.upselling_product_vpn, 27.5.dp, 25.dp),
    ProtonAppUiModel(R.drawable.ic_upselling_product_pass, R.string.upselling_product_pass, 27.5.dp, 27.5.dp)
)

private val LogoAreaHeight = 32.dp
private val NameLineHeight = 16.sp

@Preview(backgroundColor = 0xFF2E1A7A, showBackground = true)
@Composable
private fun UpsellingProtonAppsPreview() {
    ProtonTheme {
        UpsellingProtonApps()
    }
}
