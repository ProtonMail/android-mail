/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.R

@Composable
internal fun WhatsNewPill(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .background(color = SpotlightColors.pillBackground, shape = ProtonTheme.shapes.massive)
            .padding(PillPadding),
        text = stringResource(R.string.spotlight_screen_whats_new_pill),
        style = ProtonTheme.typography.labelMedium,
        color = SpotlightColors.pillText,
        maxLines = 1
    )
}

private val PillPadding =
    PaddingValues(horizontal = ProtonDimens.Spacing.MediumLight, vertical = ProtonDimens.Spacing.Small)

@Preview(showBackground = true)
@Composable
private fun WhatsNewPillPreview() {
    ProtonTheme {
        WhatsNewPill()
    }
}
