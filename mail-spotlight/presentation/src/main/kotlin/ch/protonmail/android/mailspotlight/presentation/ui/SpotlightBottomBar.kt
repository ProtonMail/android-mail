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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonTextButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.R

@Composable
internal fun SpotlightBottomBar(onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(
        text = stringResource(R.string.spotlight_screen_got_it),
        onClick = onGotIt,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .padding(bottom = ProtonDimens.Spacing.Large)
    )
}

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonTextButton(
        modifier = modifier
            .background(
                color = ProtonTheme.colors.brandNorm,
                shape = ProtonTheme.shapes.massive
            ),
        onClick = onClick,
        contentPadding = ButtonContentPadding
    ) {
        Text(
            text = text,
            style = ProtonTheme.typography.titleMedium,
            color = ProtonTheme.colors.textInverted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private val ButtonContentPadding = PaddingValues(
    horizontal = ProtonDimens.Spacing.Standard,
    vertical = ProtonDimens.Spacing.Large
)

@Preview(showBackground = true)
@Composable
private fun SpotlightBottomBarPreview() {
    ProtonTheme {
        SpotlightBottomBar(onGotIt = {})
    }
}
