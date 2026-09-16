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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonSecondaryButton
import ch.protonmail.android.design.compose.component.protonSecondaryButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcontentsearch.presentation.R

/**
 * Tells the account that its mail is fully indexed, from the top of the search screen.
 *
 * Shown on the first visit after the indexing finishes. "Got it" is what records it as told, so an
 * account that leaves the screen without tapping it is told again on the next visit.
 */
@Composable
internal fun ContentSearchIndexingCompleteBanner(onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    ContentSearchBannerCard(modifier = modifier) {
        Row {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_sparkle),
                contentDescription = null,
                tint = ProtonTheme.colors.brandMinus20,
                modifier = Modifier
                    .padding(top = IconTopPadding)
                    .size(IconSize)
            )

            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.ModeratelyLarge))

            Column {
                Text(
                    text = stringResource(R.string.content_search_indexing_complete_banner_title),
                    style = ProtonTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Tiny))

                Text(
                    text = stringResource(R.string.content_search_indexing_complete_banner_description),
                    style = ProtonTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ModeratelyLarge))

        ProtonSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onGotIt,
            shape = ProtonTheme.shapes.massive,
            colors = ButtonDefaults.protonSecondaryButtonColors(
                backgroundColor = ProtonTheme.colors.interactionBrandWeakNorm,
                contentColor = ProtonTheme.colors.textNorm
            )
        ) {
            Text(
                text = stringResource(R.string.content_search_indexing_complete_banner_got_it),
                style = ProtonTheme.typography.labelLarge
            )
        }
    }
}

private val IconSize = 16.dp

// Aligns the glyph with the middle of the title's first line rather than with its top.
private val IconTopPadding = 4.dp

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun IndexingCompleteBannerPreview() {
    ProtonTheme {
        ContentSearchIndexingCompleteBanner(onGotIt = {})
    }
}
