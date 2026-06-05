/*
 * Copyright (c) 2025 Proton Technologies AG
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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonSettingsHeader
import ch.protonmail.android.design.compose.component.ProtonSettingsItem
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews

@Composable
internal fun ApplicationDebugScreenList(modifier: Modifier = Modifier, actions: ApplicationDebugScreen.Actions) {

    LazyColumn(modifier = modifier) {
        item {
            ProtonSettingsHeader(title = R.string.app_debug_screen_logs_title)
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.app_debug_screen_logs_subtitle),
                onClick = actions.onLogsNavigation
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
        item {
            ProtonSettingsHeader(title = R.string.app_debug_screen_database_export_section_title)
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.app_debug_screen_database_export_subtitle),
                onClick = actions.onDatabaseExportNavigation
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
        item {
            ProtonSettingsHeader(title = R.string.app_debug_screen_danger_title)
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.app_debug_screen_danger_subtitle),
                onClick = actions.onDangerZoneNavigation
            )
        }
    }
}

object ApplicationDebugScreen {
    data class Actions(
        val onBackClick: () -> Unit,
        val onLogsNavigation: () -> Unit,
        val onDangerZoneNavigation: () -> Unit,
        val onDatabaseExportNavigation: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onLogsNavigation = {},
                onDangerZoneNavigation = {},
                onDatabaseExportNavigation = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun ApplicationLogsScreenPreview() {
    ProtonTheme {
        ApplicationDebugScreen(
            actions = ApplicationDebugScreen.Actions.Empty
        )
    }
}
