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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailbugreport.presentation.R
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun ApplicationLogsScreenList(modifier: Modifier = Modifier, actions: ApplicationLogsScreenList.Actions) {
    LazyColumn(modifier = modifier) {
        item {
            ProtonSettingsHeader(
                title = stringResource(R.string.application_events_header_view),
                modifier = Modifier.padding(bottom = ProtonDimens.SmallSpacing)
            )
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.application_events_view_logcat),
                hint = stringResource(R.string.application_events_view_logcat_hint),
                onClick = actions.onShowLogcat
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.application_events_view_events),
                hint = stringResource(R.string.application_events_view_events_hint),
                onClick = actions.onShowEvents
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }

        item { ProtonSettingsHeader(title = "Export", modifier = Modifier.padding(bottom = ProtonDimens.SmallSpacing)) }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.application_events_share),
                onClick = actions.onShare
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.application_events_save_to_disk),
                onClick = actions.onExport
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
    }
}

object ApplicationLogsScreenList {
    data class Actions(
        val onExport: () -> Unit,
        val onShare: () -> Unit,
        val onShowEvents: () -> Unit,
        val onShowLogcat: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExport = {},
                onShare = {},
                onShowEvents = {},
                onShowLogcat = {}
            )
        }
    }
}

@Preview
@Composable
private fun ApplicationLogsScreenPreview() {
    ProtonTheme {
        ApplicationLogsScreenList(actions = ApplicationLogsScreenList.Actions.Empty)
    }
}
