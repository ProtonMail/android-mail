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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.design.compose.theme.titleSmallNorm
import ch.protonmail.android.mailcontentsearch.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ch.protonmail.android.mailcommon.presentation.R as CommonR

/**
 * "Recent": the queries the user ran before, one per row. Tapping a row re-runs its search, the
 * trailing icon dismisses that one query, and the header action clears the whole term history.
 */
internal fun LazyListScope.recentSearchTermsSection(terms: ImmutableList<String>, actions: RecentSearchesActions) {
    if (terms.isEmpty()) return

    item(key = RECENT_TERMS_HEADER_KEY) {
        ClearableSectionHeader(
            text = stringResource(R.string.content_search_recent_searches_title),
            onClearClicked = actions.onClearTermsClicked
        )
    }
    items(items = terms, key = { term -> "$RECENT_TERM_KEY_PREFIX$term" }) { term ->
        RecentSearchTermRow(
            term = term,
            onClick = { actions.onTermClicked(term) },
            onDismiss = { actions.onTermDismissed(term) },
            modifier = Modifier.animateItem()
        )
    }
}

/** A section title with a trailing "Clear all" action. */
@Composable
internal fun ClearableSectionHeader(
    text: String,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ProtonDimens.Spacing.Standard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(
            text = text,
            modifier = Modifier.weight(1f)
        )
        Text(
            modifier = Modifier
                .padding(end = ProtonDimens.Spacing.Large)
                .clickable(onClick = onClearClicked)
                .padding(vertical = ProtonDimens.Spacing.Standard),
            text = stringResource(R.string.content_search_clear_recent_searches),
            style = ProtonTheme.typography.bodyMediumWeak,
            color = ProtonTheme.colors.textHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentSearchTermRow(
    term: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = ProtonDimens.Spacing.Large, end = RowEndPadding),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(ProtonDimens.IconSize.Default),
            painter = painterResource(id = R.drawable.ic_material_history),
            contentDescription = null,
            tint = ProtonTheme.colors.iconWeak
        )
        Text(
            modifier = Modifier.weight(1f),
            text = term,
            style = ProtonTheme.typography.bodyLargeNorm,
            color = ProtonTheme.colors.textWeak,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // A nested clickable inside a clickable row: the row re-runs the search, the icon removes the
        // query, so the icon has to swallow the tap.
        Surface(
            onClick = onDismiss,
            modifier = Modifier.size(DismissTouchTargetSize),
            color = Color.Unspecified
        ) {
            Icon(
                modifier = Modifier
                    .wrapContentSize()
                    .size(DismissIconSize),
                painter = painterResource(id = CommonR.drawable.ic_material_close),
                contentDescription = stringResource(
                    R.string.content_search_dismiss_recent_search_content_description,
                    term
                ),
                tint = ProtonTheme.colors.iconHint
            )
        }
    }
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(start = ProtonDimens.Spacing.Large)
            .padding(vertical = ProtonDimens.Spacing.Medium),
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = ProtonTheme.typography.titleSmallNorm,
        color = ProtonTheme.colors.textNorm
    )
}

private val DismissIconSize = ProtonDimens.IconSize.Medium
private val DismissTouchTargetSize = ProtonDimens.Spacing.Massive

private val RowEndPadding = ProtonDimens.Spacing.Large - (DismissTouchTargetSize - DismissIconSize) / 2

private const val RECENT_TERMS_HEADER_KEY = "recent-terms-header"
private const val RECENT_TERM_KEY_PREFIX = "recent-term-"

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RecentSearchTermsSectionPreview() {
    ProtonTheme {
        LazyColumn {
            recentSearchTermsSection(
                terms = persistentListOf("invoice", "flight"),
                actions = RecentSearchesActions.Empty
            )
        }
    }
}
