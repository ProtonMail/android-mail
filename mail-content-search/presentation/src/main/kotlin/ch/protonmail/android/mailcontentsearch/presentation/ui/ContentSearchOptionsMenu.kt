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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailpagination.domain.model.IncludeFilter

/**
 * The app bar's overflow menu. Anchors itself to its own icon button, so it no longer needs the manual
 * offset the floating variant used to position a bare [Popup].
 */
@Composable
internal fun SearchOptionsMenu(
    includeFilter: IncludeFilter,
    onToggleIncludeSpam: () -> Unit,
    onToggleIncludeTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.Default),
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.content_search_options_content_description),
                tint = ProtonTheme.colors.iconNorm
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Max),
            shape = RoundedCornerShape(ProtonDimens.CornerRadius.ExtraLarge),
            containerColor = ProtonTheme.colors.backgroundNorm
        ) {
            // The scopes read as "Include messages in: Trash", so they are labeled by this header
            // rather than repeating "Include" on each row.
            Text(
                modifier = Modifier.padding(
                    start = MenuItemHorizontalPadding,
                    end = MenuItemHorizontalPadding,
                    top = ProtonDimens.Spacing.Medium,
                    bottom = ProtonDimens.Spacing.Small
                ),
                text = stringResource(R.string.content_search_include_messages_in),
                style = ProtonTheme.typography.bodyMediumWeak
            )
            SearchOptionToggleItem(
                textRes = R.string.content_search_include_trash,
                checked = includeFilter.includeTrash,
                onClick = onToggleIncludeTrash
            )
            SearchOptionToggleItem(
                textRes = R.string.content_search_include_spam,
                checked = includeFilter.includeSpam,
                onClick = onToggleIncludeSpam
            )
            HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.content_search_about_content_search),
                        style = ProtonTheme.typography.bodyMediumNorm
                    )
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(ProtonDimens.IconSize.Medium),
                        painter = painterResource(id = R.drawable.ic_proton_info_circle),
                        contentDescription = null,
                        tint = ProtonTheme.colors.iconNorm
                    )
                },
                contentPadding = MenuItemContentPadding,
                // Dismisses only, for now: the content-search explainer this opens is still to be
                // designed, so there is nowhere to send the user yet.
                onClick = { expanded = false }
            )
        }
    }
}

@Composable
private fun SearchOptionToggleItem(
    @StringRes textRes: Int,
    checked: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        // The checkmark is the only thing marking a scope as included, so state it for screen readers
        // too rather than leaving it to the icon.
        modifier = Modifier.semantics { toggleableState = ToggleableState(checked) },
        text = {
            Text(
                text = stringResource(textRes),
                style = ProtonTheme.typography.bodyMediumNorm
            )
        },
        // The checkmark leads the row. An excluded scope holds its place with an empty slot of the same
        // size, so both labels stay on one left edge whichever of them is included.
        leadingIcon = {
            if (checked) {
                Icon(
                    modifier = Modifier.size(ProtonDimens.IconSize.Medium),
                    painter = painterResource(id = R.drawable.ic_proton_checkmark),
                    contentDescription = null,
                    tint = ProtonTheme.colors.iconNorm
                )
            } else {
                Spacer(modifier = Modifier.size(ProtonDimens.IconSize.Medium))
            }
        },
        contentPadding = MenuItemContentPadding,
        onClick = onClick
    )
}

private val MenuItemHorizontalPadding = 20.dp
private val MenuItemContentPadding = PaddingValues(
    horizontal = MenuItemHorizontalPadding,
    vertical = ProtonDimens.Spacing.Medium
)
