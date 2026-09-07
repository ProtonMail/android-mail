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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews

@Composable
internal fun ApplicationDebugScreen(modifier: Modifier = Modifier, actions: ApplicationDebugScreen.Actions) {
    DebugMenuScaffold(
        modifier = modifier,
        title = R.string.app_debug_screen,
        onCloseClick = actions.onBackClick
    ) {
        debugMenuSection(title = R.string.app_debug_screen_logs_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_window_terminal,
                title = R.string.app_debug_screen_logs_subtitle,
                description = R.string.app_debug_screen_logs_description,
                showChevron = true,
                onClick = actions.onLogsNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_database_export_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_storage,
                title = R.string.app_debug_screen_database_export_subtitle,
                description = R.string.app_debug_screen_database_export_description,
                showChevron = true,
                onClick = actions.onDatabaseExportNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_telemetry_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_chart_line,
                title = R.string.app_debug_screen_telemetry_subtitle,
                description = R.string.app_debug_screen_telemetry_description,
                showChevron = true,
                onClick = actions.onTelemetryNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_app_state_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_arrows_rotate,
                title = R.string.app_debug_screen_app_state_subtitle,
                description = R.string.app_debug_screen_app_state_description,
                showChevron = true,
                onClick = actions.onAppStateNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_danger_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_exclamation_triangle_filled,
                title = R.string.app_debug_screen_danger_subtitle,
                description = R.string.app_debug_screen_danger_description,
                tint = ProtonTheme.colors.notificationError,
                showChevron = true,
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
        val onDatabaseExportNavigation: () -> Unit,
        val onTelemetryNavigation: () -> Unit,
        val onAppStateNavigation: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onLogsNavigation = {},
                onDangerZoneNavigation = {},
                onDatabaseExportNavigation = {},
                onTelemetryNavigation = {},
                onAppStateNavigation = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun ApplicationDebugScreenPreview() {
    ProtonTheme {
        ApplicationDebugScreen(actions = ApplicationDebugScreen.Actions.Empty)
    }
}
