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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews

@Composable
internal fun DangerZoneScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: DangerZoneViewModel = hiltViewModel()
) {
    DangerZoneScreen(
        modifier = modifier,
        actions = DangerZoneScreen.Actions(
            onBackClick = onBackClick,
            onNativeCrash = { viewModel.nativeCrashApp() },
            onCrash = { viewModel.crashApp() },
            onWebViewCrash = { viewModel.crashWebViewLike() }
        )
    )
}

@Composable
private fun DangerZoneScreen(modifier: Modifier = Modifier, actions: DangerZoneScreen.Actions) {
    DebugMenuScaffold(
        modifier = modifier,
        title = R.string.app_debug_screen_danger_title,
        onCloseClick = actions.onBackClick
    ) {
        debugMenuSection(title = R.string.app_debug_screen_danger_crash_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_bug,
                title = R.string.app_debug_screen_danger_crash_native_title,
                description = R.string.app_debug_screen_danger_crash_native_description,
                tint = ProtonTheme.colors.notificationError,
                onClick = actions.onNativeCrash
            )
            DebugMenuRow(
                icon = R.drawable.ic_proton_bolt,
                title = R.string.app_debug_screen_danger_crash_title,
                description = R.string.app_debug_screen_danger_crash_description,
                tint = ProtonTheme.colors.notificationError,
                onClick = actions.onCrash
            )
            DebugMenuRow(
                icon = R.drawable.ic_proton_window_image,
                title = R.string.app_debug_screen_danger_crash_webview_title,
                description = R.string.app_debug_screen_danger_crash_webview_description,
                tint = ProtonTheme.colors.notificationError,
                onClick = actions.onWebViewCrash
            )
        }
    }
}

object DangerZoneScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onCrash: () -> Unit,
        val onNativeCrash: () -> Unit,
        val onWebViewCrash: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onCrash = {},
                onNativeCrash = {},
                onWebViewCrash = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun DangerZoneScreenPreview() {
    ProtonTheme {
        DangerZoneScreen(actions = DangerZoneScreen.Actions.Empty)
    }
}
