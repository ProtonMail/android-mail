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
 * Offers to turn content search on, from the top of the search screen.
 *
 * Shown to an account that has never answered the offer. The feature spotlight makes the same offer
 * to the primary account, so most of the time this banner belongs to the accounts added later.
 */
@Composable
internal fun ContentSearchSetUpBanner(actions: ContentSearchSetUpBannerActions, modifier: Modifier = Modifier) {
    ContentSearchBannerCard(modifier = modifier) {
        Row {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_star),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm,
                modifier = Modifier
                    .padding(top = IconTopPadding)
                    .size(IconSize)
            )

            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.ModeratelyLarge))

            Column {
                Text(
                    text = stringResource(R.string.content_search_set_up_banner_title),
                    style = ProtonTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))

                Text(
                    text = withLearnMoreLink(
                        description = stringResource(R.string.content_search_set_up_banner_description),
                        onLearnMoreClick = actions.onLearnMore
                    ),
                    style = ProtonTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ModeratelyLarge))

        Row {
            ProtonSecondaryButton(
                modifier = Modifier.weight(1f),
                onClick = actions.onTurnOn,
                shape = ProtonTheme.shapes.massive,
                colors = ButtonDefaults.protonSecondaryButtonColors(
                    backgroundColor = ProtonTheme.colors.interactionBrandWeakNorm,
                    contentColor = ProtonTheme.colors.textAccent
                )
            ) {
                Text(
                    text = stringResource(R.string.content_search_set_up_banner_turn_on),
                    style = ProtonTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Standard))

            ProtonSecondaryButton(
                modifier = Modifier.weight(1f),
                onClick = actions.onNotNow,
                shape = ProtonTheme.shapes.massive
            ) {
                Text(
                    text = stringResource(R.string.content_search_set_up_banner_not_now),
                    style = ProtonTheme.typography.labelLarge
                )
            }
        }
    }
}

internal data class ContentSearchSetUpBannerActions(
    val onTurnOn: () -> Unit,
    val onNotNow: () -> Unit,
    val onLearnMore: () -> Unit
)

private val IconSize = 20.dp

// Aligns the glyph with the middle of the title's first line rather than with its top.
private val IconTopPadding = 2.dp

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ContentSearchSetUpBannerPreview() {
    ProtonTheme {
        ContentSearchSetUpBanner(
            actions = ContentSearchSetUpBannerActions(onTurnOn = {}, onNotNow = {}, onLearnMore = {})
        )
    }
}
