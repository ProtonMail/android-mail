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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonSecondaryButton
import ch.protonmail.android.design.compose.component.protonSecondaryButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeHint
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodyLargeWeak
import ch.protonmail.android.mailcontentsearch.presentation.R

/**
 * The state shown when a search found nothing.
 *
 * With content search off and available, the message invites the user to set it up. Otherwise it
 * only suggests changing the query, as there is nothing left to turn on.
 */
@Composable
internal fun ContentSearchNoResultsState(
    query: String,
    canSetUpContentSearch: Boolean,
    actions: ContentSearchNoResultsActions,
    modifier: Modifier = Modifier
) {
    SearchMessageState(
        iconRes = R.drawable.ic_search_empty_result,
        title = stringResource(R.string.content_search_no_results_title_for_query, query),
        description = if (canSetUpContentSearch) {
            withLearnMoreLink(
                description = stringResource(R.string.content_search_no_results_set_up_description),
                onLearnMoreClick = actions.onLearnMore
            )
        } else {
            AnnotatedString(stringResource(R.string.content_search_no_results_description))
        },
        modifier = modifier,
        descriptionStyle = ProtonTheme.typography.bodyLargeHint,
        action = if (canSetUpContentSearch) {
            {
                SetUpContentSearchButton(onClick = actions.onSetUpContentSearch)
            }
        } else {
            null
        }
    )
}

internal data class ContentSearchNoResultsActions(
    val onLearnMore: () -> Unit,
    val onSetUpContentSearch: () -> Unit
)

@Composable
private fun SetUpContentSearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The button owns its shape rather than drawing a rounded background behind it, so the ripple
    // is clipped to the pill instead of spilling into a rectangle.
    ProtonSecondaryButton(
        modifier = modifier,
        onClick = onClick,
        shape = ProtonTheme.shapes.massive,
        colors = ButtonDefaults.protonSecondaryButtonColors(
            backgroundColor = ProtonTheme.colors.interactionBrandWeakNorm,
            contentColor = ProtonTheme.colors.textAccent
        )
    ) {
        Text(
            text = stringResource(R.string.content_search_no_results_set_up_action),
            style = ProtonTheme.typography.labelLarge
        )
    }
}

private val OpticalCentreLift = 64.dp

@Composable
internal fun SearchMessageState(
    @DrawableRes iconRes: Int,
    description: String,
    modifier: Modifier = Modifier,
    title: String? = null
) = SearchMessageState(
    iconRes = iconRes,
    description = AnnotatedString(description),
    modifier = modifier,
    title = title
)

@Composable
internal fun SearchMessageState(
    @DrawableRes iconRes: Int,
    description: AnnotatedString,
    modifier: Modifier = Modifier,
    title: String? = null,
    descriptionStyle: TextStyle = ProtonTheme.typography.bodyLargeWeak,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Centred in the body the group reads as low, as the body starts under the search bar
            // and its filter chip. This lifts it back to the screen's optical centre.
            .padding(bottom = OpticalCentreLift)
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
        if (title != null) {
            Text(
                text = title,
                style = ProtonTheme.typography.bodyLargeNorm,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
        }
        Text(
            text = description,
            style = descriptionStyle,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            action()
        }
    }
}

@Preview(name = "No results", showBackground = true)
@Preview(name = "No results \u00b7 dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NoResultsStatePreview() {
    ProtonTheme {
        ContentSearchNoResultsState(
            query = "weather",
            canSetUpContentSearch = false,
            actions = ContentSearchNoResultsActions(onLearnMore = {}, onSetUpContentSearch = {})
        )
    }
}

@Preview(name = "No results, content search off", showBackground = true)
@Preview(name = "No results, content search off \u00b7 dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NoResultsStateWithSetUpPreview() {
    ProtonTheme {
        ContentSearchNoResultsState(
            query = "weather",
            canSetUpContentSearch = true,
            actions = ContentSearchNoResultsActions(onLearnMore = {}, onSetUpContentSearch = {})
        )
    }
}
