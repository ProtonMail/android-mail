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

package ch.protonmail.android.mailsettings.presentation.webcategorysettings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonErrorMessage
import ch.protonmail.android.design.compose.component.ProtonSettingsDetailsAppBar
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.websettings.SettingWebView
import ch.protonmail.android.mailsettings.presentation.websettings.WebSettingsScreenActions
import ch.protonmail.android.mailsettings.presentation.websettings.WebSettingsState
import ch.protonmail.android.mailsettings.presentation.websettings.model.WebSettingsAction
import me.proton.core.util.kotlin.exhaustive

@Composable
fun WebCategorySettingScreen(
    actions: WebSettingsScreenActions,
    modifier: Modifier = Modifier,
    viewModel: WebCategorySettingsViewModel = hiltViewModel()
) {
    val settingsState = viewModel.state.collectAsStateWithLifecycle(
        WebSettingsState.Loading
    ).value

    WebCategorySettingScreen(
        modifier = modifier,
        state = settingsState,
        actions = WebSettingsScreenActions.Empty.copy(
            onBackClick = {
                viewModel.submit(WebSettingsAction.OnCloseWebSettings)
                actions.onBackClick()
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebCategorySettingScreen(
    modifier: Modifier = Modifier,
    state: WebSettingsState,
    actions: WebSettingsScreenActions
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsDetailsAppBar(
                title = stringResource(id = R.string.mail_settings_app_customization_email_categories),
                onBackClick = actions.onBackClick
            )
        },
        content = { paddingValues ->
            when (state) {
                is WebSettingsState.Data -> SettingWebView(
                    modifier.padding(paddingValues),
                    state = state
                )

                is WebSettingsState.Error -> ProtonErrorMessage(
                    modifier = modifier.padding(paddingValues),
                    errorMessage = state.errorMessage
                )

                WebSettingsState.Loading -> ProtonCenteredProgress(modifier = modifier.padding(paddingValues))

                WebSettingsState.NotLoggedIn ->
                    ProtonErrorMessage(
                        modifier = modifier.padding(paddingValues),
                        errorMessage = stringResource(id = R.string.x_error_not_logged_in)
                    )
            }.exhaustive

        }
    )
}

