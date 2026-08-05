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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.design.compose.theme.titleSmallNorm
import ch.protonmail.android.mailcontentsearch.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * "Recent": the queries the user ran before, one per row. Tapping a row re-runs its search, the
 * trailing icon dismisses that one query, and the header action clears the whole term history.
 */
@Composable
internal fun RecentSearchTermsSection(
    terms: ImmutableList<String>,
    actions: RecentSearchesActions,
    modifier: Modifier = Modifier
) {
    if (terms.isEmpty()) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                text = stringResource(R.string.content_search_recent_searches_title),
                modifier = Modifier.weight(1f)
            )
            Text(
                modifier = Modifier
                    .clickable(onClick = actions.onClearTermsClicked)
                    .padding(
                        horizontal = ProtonDimens.Spacing.Large,
                        vertical = ProtonDimens.Spacing.Medium
                    ),
                text = stringResource(R.string.content_search_clear_recent_searches),
                style = ProtonTheme.typography.bodyMediumWeak,
                color = ProtonTheme.colors.textNorm
            )
        }
        terms.forEach { term ->
            RecentSearchTermRow(
                term = term,
                onClick = { actions.onTermClicked(term) },
                onDismiss = { actions.onTermDismissed(term) }
            )
        }
    }
}

@Composable
private fun RecentSearchTermRow(
    term: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = ProtonDimens.Spacing.Large,
                end = ProtonDimens.Spacing.MediumLight,
                top = ProtonDimens.Spacing.Medium,
                bottom = ProtonDimens.Spacing.Medium
            ),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(ProtonDimens.IconSize.Default),
            painter = painterResource(id = R.drawable.ic_material_schedule),
            contentDescription = null,
            tint = ProtonTheme.colors.iconWeak
        )
        Text(
            modifier = Modifier.weight(1f),
            text = term,
            style = ProtonTheme.typography.bodyLargeNorm,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // A nested clickable inside a clickable row: the row re-runs the search, the icon removes the
        // query, so the icon has to swallow the tap.
        Surface(
            onClick = onDismiss,
            // The icon stays small, but a miss on a 24dp target re-runs the search instead of removing
            // the term, so the tappable area is grown to the 48dp minimum.
            modifier = Modifier.minimumInteractiveComponentSize(),
            shape = CircleShape,
            color = ProtonTheme.colors.backgroundNorm
        ) {
            Icon(
                modifier = Modifier
                    .padding(ProtonDimens.Spacing.Small)
                    .size(DismissIconSize),
                painter = painterResource(id = R.drawable.ic_material_close),
                contentDescription = stringResource(
                    R.string.content_search_dismiss_recent_search_content_description,
                    term
                ),
                tint = ProtonTheme.colors.iconWeak
            )
        }
    }
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(
            horizontal = ProtonDimens.Spacing.Large,
            vertical = ProtonDimens.Spacing.Medium
        ),
        text = text,
        style = ProtonTheme.typography.titleSmallNorm,
        color = ProtonTheme.colors.textWeak
    )
}

private val DismissIconSize = 16.dp

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RecentSearchTermsSectionPreview() {
    ProtonTheme {
        RecentSearchTermsSection(
            terms = persistentListOf("invoice", "flight"),
            actions = RecentSearchesActions.Empty
        )
    }
}
