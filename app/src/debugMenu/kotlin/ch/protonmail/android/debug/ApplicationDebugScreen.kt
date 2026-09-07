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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard
import me.proton.core.presentation.utils.showToast

@Composable
internal fun ApplicationDebugScreen(
    modifier: Modifier = Modifier,
    actions: ApplicationDebugScreen.Actions,
    viewModel: ApplicationDebugViewModel = hiltViewModel()
) {
    ApplicationDebugScreen(
        modifier = modifier,
        appVersion = viewModel.appVersion,
        actions = actions
    )
}

@Composable
private fun ApplicationDebugScreen(
    appVersion: String,
    actions: ApplicationDebugScreen.Actions,
    modifier: Modifier = Modifier
) {
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
                trailing = { DebugMenuChevron() },
                onClick = actions.onLogsNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_feature_flags_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_sliders,
                title = R.string.app_debug_screen_feature_flags_subtitle,
                description = R.string.app_debug_screen_feature_flags_description,
                trailing = { DebugMenuChevron() },
                onClick = actions.onFeatureFlagsNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_app_state_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_arrows_rotate,
                title = R.string.app_debug_screen_app_state_subtitle,
                description = R.string.app_debug_screen_app_state_description,
                trailing = { DebugMenuChevron() },
                onClick = actions.onAppStateNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_telemetry_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_chart_line,
                title = R.string.app_debug_screen_telemetry_subtitle,
                description = R.string.app_debug_screen_telemetry_description,
                trailing = { DebugMenuChevron() },
                onClick = actions.onTelemetryNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_database_export_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_storage,
                title = R.string.app_debug_screen_database_export_subtitle,
                description = R.string.app_debug_screen_database_export_description,
                trailing = { DebugMenuChevron() },
                onClick = actions.onDatabaseExportNavigation
            )
        }

        debugMenuSection(title = R.string.app_debug_screen_danger_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_exclamation_triangle_filled,
                title = R.string.app_debug_screen_danger_subtitle,
                description = R.string.app_debug_screen_danger_description,
                tint = ProtonTheme.colors.notificationError,
                trailing = { DebugMenuChevron() },
                onClick = actions.onDangerZoneNavigation
            )
        }

        item { AppVersion(appVersion) }
    }
}

@Composable
private fun AppVersion(appVersion: String) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.copyTextToClipboard(label = context.getString(R.string.app_debug_screen), text = appVersion)
                context.showToast(context.getString(R.string.app_debug_screen_version_copied))
            }
            .padding(ProtonDimens.Spacing.Large),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appVersion,
            color = ProtonTheme.colors.textWeak,
            style = ProtonTheme.typography.bodyMedium,
            maxLines = 1
        )
        Icon(
            modifier = Modifier
                .padding(start = ProtonDimens.Spacing.Standard)
                .size(ProtonDimens.IconSize.Small),
            painter = painterResource(R.drawable.ic_proton_squares),
            contentDescription = null,
            tint = ProtonTheme.colors.iconHint
        )
    }
}

object ApplicationDebugScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onLogsNavigation: () -> Unit,
        val onDangerZoneNavigation: () -> Unit,
        val onDatabaseExportNavigation: () -> Unit,
        val onTelemetryNavigation: () -> Unit,
        val onAppStateNavigation: () -> Unit,
        val onFeatureFlagsNavigation: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onLogsNavigation = {},
                onDangerZoneNavigation = {},
                onDatabaseExportNavigation = {},
                onTelemetryNavigation = {},
                onAppStateNavigation = {},
                onFeatureFlagsNavigation = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun ApplicationDebugScreenPreview() {
    ProtonTheme {
        ApplicationDebugScreen(
            appVersion = "7.12.0-alpha (1) - 0.169.18",
            actions = ApplicationDebugScreen.Actions.Empty
        )
    }
}
