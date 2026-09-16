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

package ch.protonmail.android.debug

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.appbar.ProtonMediumTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme

/**
 * Building blocks shared by the debug menu screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugMenuScaffold(
    @StringRes title: Int,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonMediumTopAppBar(
                title = { Text(text = stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.presentation_back)
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(bottom = ProtonDimens.Spacing.ExtraLarge),
                content = content
            )
        }
    )
}

internal fun LazyListScope.debugMenuSection(
    @StringRes title: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    item(key = title) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(
                    start = ProtonDimens.Spacing.Large,
                    end = ProtonDimens.Spacing.Large,
                    top = ProtonDimens.Spacing.Large,
                    bottom = ProtonDimens.Spacing.Standard
                ),
                text = stringResource(title),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.bodyMedium
            )
            content()
        }
    }
}

@Composable
internal fun DebugMenuRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes description: Int? = null,
    tint: Color = ProtonTheme.colors.iconWeak,
    trailing: @Composable () -> Unit = {}
) = DebugMenuRow(
    icon = icon,
    title = stringResource(title),
    onClick = onClick,
    modifier = modifier,
    description = description?.let { stringResource(it) },
    tint = tint,
    trailing = trailing
)

/** The same row with runtime text, for entries that are not known at build time. */
@Composable
internal fun DebugMenuRow(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    tint: Color = ProtonTheme.colors.iconWeak,
    trailing: @Composable () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    horizontal = ProtonDimens.Spacing.Large,
                    vertical = ProtonDimens.Spacing.Medium
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.Default),
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
            ) {
                Text(
                    text = title,
                    color = ProtonTheme.colors.textNorm,
                    style = ProtonTheme.typography.titleMedium
                )
                description?.let {
                    Text(
                        text = it,
                        color = ProtonTheme.colors.textWeak,
                        style = ProtonTheme.typography.bodyMedium
                    )
                }
            }

            trailing()
        }
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
    }
}

@Composable
internal fun DebugMenuChevron() {
    Icon(
        modifier = Modifier.size(ProtonDimens.IconSize.Medium),
        painter = painterResource(R.drawable.ic_proton_chevron_right),
        contentDescription = null,
        tint = ProtonTheme.colors.iconHint
    )
}
