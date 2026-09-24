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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
    modifier: Modifier = Modifier,
    bannerHeight: Dp = 0.dp
) {
    SearchMessageState(
        bannerHeight = bannerHeight,
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
                PillButton(
                    text = stringResource(R.string.content_search_no_results_set_up_action),
                    onClick = actions.onSetUpContentSearch
                )
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

/** The state shown when a search could not run, with a way to run it again. */
@Composable
internal fun ContentSearchFailedState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    bannerHeight: Dp = 0.dp
) {
    SearchMessageState(
        bannerHeight = bannerHeight,
        iconRes = R.drawable.ic_search_empty_result,
        title = stringResource(R.string.content_search_failed_title),
        description = AnnotatedString(stringResource(R.string.content_search_failed_description)),
        modifier = modifier,
        descriptionStyle = ProtonTheme.typography.bodyLargeHint,
        action = {
            PillButton(text = stringResource(R.string.content_search_failed_retry), onClick = onRetry)
        }
    )
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = text,
            style = ProtonTheme.typography.labelLarge
        )
    }
}

// Centred in the body the group reads as low, as the body starts under the search bar and its filter
// chip. This lifts it back to the screen's optical centre.
private val OpticalCentreLift = 64.dp

@Composable
internal fun SearchMessageState(
    @DrawableRes iconRes: Int,
    description: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    bannerHeight: Dp = 0.dp
) = SearchMessageState(
    iconRes = iconRes,
    description = AnnotatedString(description),
    modifier = modifier,
    title = title,
    bannerHeight = bannerHeight
)

/**
 * A centred icon, title and message, with an optional action under them.
 *
 * [bannerHeight] is the room the indexing banner takes above this state. It is added to the lift, so
 * the group stays on the screen's centre instead of being pushed below it.
 */
@Composable
internal fun SearchMessageState(
    @DrawableRes iconRes: Int,
    description: AnnotatedString,
    modifier: Modifier = Modifier,
    title: String? = null,
    descriptionStyle: TextStyle = ProtonTheme.typography.bodyLargeWeak,
    bannerHeight: Dp = 0.dp,
    action: (@Composable () -> Unit)? = null
) {
    CentredMessage(
        lift = OpticalCentreLift + bannerHeight,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge),
        artwork = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
}

/**
 * Lays [message] on the vertical centre, with [artwork] above it and [lift] worth of room kept free
 * below it, as long as there is room for all three.
 *
 * A short page - a phone in landscape, with the indexing banner up - gives them up in order: first
 * the lift, then the artwork, and the words last. The words are what the user came to read.
 *
 * A message too tall even on its own starts at the top edge and runs off the bottom. Running off the
 * top would put it under the search bar.
 */
@Composable
private fun CentredMessage(
    artwork: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    lift: Dp = 0.dp,
    message: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            artwork()
            message()
        },
        measurePolicy = object : MeasurePolicy {

            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val loose = constraints.copy(minHeight = 0)
                val artwork = measurables[ARTWORK].measure(loose)
                val message = measurables[MESSAGE].measure(loose)

                // An unbounded box can't run short, and has no centre to lay the group on either.
                val room = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
                val showArtwork = artwork.height + message.height <= room

                val groupHeight = message.height + if (showArtwork) artwork.height else 0
                val liftHeight = lift.roundToPx().takeIf { groupHeight + it <= room } ?: 0

                val height = if (constraints.hasBoundedHeight) constraints.maxHeight else groupHeight
                val top = ((height - liftHeight - groupHeight) / 2).coerceAtLeast(0)

                return layout(constraints.maxWidth, height) {
                    if (showArtwork) artwork.place(0, top)
                    message.place(0, top + if (showArtwork) artwork.height else 0)
                }
            }

            // Only the message is reported, since the lift and the artwork are both given up before
            // the group is allowed to outgrow its box.
            override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
                measurables[MESSAGE].minIntrinsicHeight(width)
        }
    )
}

private const val ARTWORK = 0
private const val MESSAGE = 1

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

// The room a landscape page has left under the indexing banner. Neither the lift nor the artwork
// fits, so the message is all that is laid out.
@Preview(name = "No results, short page", showBackground = true, widthDp = 720, heightDp = 190)
@Composable
private fun NoResultsStateOnShortPagePreview() {
    ProtonTheme {
        ContentSearchNoResultsState(
            query = "weather",
            canSetUpContentSearch = false,
            actions = ContentSearchNoResultsActions(onLearnMore = {}, onSetUpContentSearch = {}),
            bannerHeight = 130.dp
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

@Preview(name = "Search failed", showBackground = true)
@Preview(name = "Search failed \u00b7 dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun FailedStatePreview() {
    ProtonTheme {
        ContentSearchFailedState(onRetry = {})
    }
}
