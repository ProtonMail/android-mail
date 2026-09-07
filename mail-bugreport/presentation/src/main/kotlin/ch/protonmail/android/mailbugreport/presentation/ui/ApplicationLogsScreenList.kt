/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailbugreport.presentation.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews

/**
 * The list of log actions, laid out as a flat list with one leading icon per row.
 */
@Composable
internal fun ApplicationLogsScreenList(
    modifier: Modifier = Modifier,
    isStandalone: Boolean,
    actions: ApplicationLogsScreenList.Actions
) {
    LazyColumn(modifier = modifier) {
        if (isStandalone) viewLogsSection(actions)

        logsSection(R.string.application_events_header_export) {
            ApplicationLogsRow(
                icon = R.drawable.ic_proton_arrow_up_from_square,
                title = R.string.application_events_share,
                description = R.string.application_events_share_hint,
                onClick = actions.onShare
            )
            // Only offered from the logged in entry point.
            actions.onSendByEmail?.let { onSendByEmail ->
                ApplicationLogsRow(
                    icon = R.drawable.ic_proton_paper_plane,
                    title = R.string.application_events_send_by_email,
                    description = R.string.application_events_send_by_email_hint,
                    onClick = onSendByEmail
                )
            }
            ApplicationLogsRow(
                icon = R.drawable.ic_proton_arrow_down_to_square,
                title = R.string.application_events_save_to_disk,
                description = R.string.application_events_save_to_disk_hint,
                onClick = actions.onExport
            )
        }
    }
}

private fun LazyListScope.viewLogsSection(actions: ApplicationLogsScreenList.Actions) {
    logsSection(R.string.application_events_header_view) {
        ApplicationLogsRow(
            icon = R.drawable.ic_proton_window_terminal,
            title = R.string.application_events_view_logcat,
            description = R.string.application_events_view_logcat_hint,
            onClick = actions.onShowLogcat,
            showChevron = true
        )
        ApplicationLogsRow(
            icon = R.drawable.ic_proton_code,
            title = R.string.application_events_view_rust_events,
            description = R.string.application_events_view_rust_events_hint,
            onClick = actions.onShowRustEvents,
            showChevron = true
        )
        ApplicationLogsRow(
            icon = R.drawable.ic_proton_file_lines,
            title = R.string.application_events_view_events,
            description = R.string.application_events_view_events_hint,
            onClick = actions.onShowAppEvents,
            showChevron = true
        )
    }
}

private fun LazyListScope.logsSection(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
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
private fun ApplicationLogsRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false
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
                tint = ProtonTheme.colors.iconWeak
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
            ) {
                Text(
                    text = stringResource(title),
                    color = ProtonTheme.colors.textNorm,
                    style = ProtonTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(description),
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.bodyMedium
                )
            }
            if (showChevron) {
                Icon(
                    modifier = Modifier.size(ProtonDimens.IconSize.Medium),
                    painter = painterResource(R.drawable.ic_proton_chevron_right),
                    contentDescription = null,
                    tint = ProtonTheme.colors.iconHint
                )
            }
        }
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
    }
}

object ApplicationLogsScreenList {
    data class Actions(
        val onExport: () -> Unit,
        val onShare: () -> Unit,
        val onSendByEmail: (() -> Unit)?,
        val onShowAppEvents: () -> Unit,
        val onShowRustEvents: () -> Unit,
        val onShowLogcat: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExport = {},
                onShare = {},
                onSendByEmail = {},
                onShowAppEvents = {},
                onShowRustEvents = {},
                onShowLogcat = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun ApplicationLogsScreenPreview() {
    ProtonTheme {
        ApplicationLogsScreenList(
            actions = ApplicationLogsScreenList.Actions.Empty,
            isStandalone = true
        )
    }
}
