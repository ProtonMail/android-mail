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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonSettingsItem
import ch.protonmail.android.design.compose.component.appbar.ProtonMediumTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import me.proton.core.presentation.utils.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppStateScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: AppStateViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.resets.collect { target ->
            context.showToast(context.getString(target.confirmationRes()))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonMediumTopAppBar(
                title = { Text(text = stringResource(R.string.app_debug_screen_app_state_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.presentation_back)
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            AppStateScreen(
                modifier = Modifier.padding(paddingValues),
                actions = AppStateScreen.Actions(
                    onResetOnboarding = { viewModel.resetOnboarding() },
                    onResetFeatureSpotlight = { viewModel.resetFeatureSpotlight() },
                    onResetNotificationPrompt = { viewModel.resetNotificationPrompt() }
                )
            )
        }
    )
}

@Composable
private fun AppStateScreen(modifier: Modifier = Modifier, actions: AppStateScreen.Actions) {
    LazyColumn(modifier = modifier) {
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.app_debug_screen_app_state_reset_onboarding_title),
                hint = stringResource(R.string.app_debug_screen_app_state_reset_onboarding_description),
                onClick = actions.onResetOnboarding
            )
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.app_debug_screen_app_state_reset_spotlight_title),
                hint = stringResource(R.string.app_debug_screen_app_state_reset_spotlight_description),
                onClick = actions.onResetFeatureSpotlight
            )
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.app_debug_screen_app_state_reset_notifications_title),
                hint = stringResource(R.string.app_debug_screen_app_state_reset_notifications_description),
                onClick = actions.onResetNotificationPrompt
            )
        }
    }
}

private fun AppStateViewModel.ResetTarget.confirmationRes() = when (this) {
    AppStateViewModel.ResetTarget.Onboarding -> R.string.app_debug_screen_app_state_reset_onboarding_done
    AppStateViewModel.ResetTarget.FeatureSpotlight -> R.string.app_debug_screen_app_state_reset_spotlight_done
    AppStateViewModel.ResetTarget.NotificationPrompt -> R.string.app_debug_screen_app_state_reset_notifications_done
}

object AppStateScreen {

    data class Actions(
        val onResetOnboarding: () -> Unit,
        val onResetFeatureSpotlight: () -> Unit,
        val onResetNotificationPrompt: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onResetOnboarding = {},
                onResetFeatureSpotlight = {},
                onResetNotificationPrompt = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun AppStateScreenPreview() {
    ProtonTheme {
        AppStateScreen(actions = AppStateScreen.Actions.Empty)
    }
}
