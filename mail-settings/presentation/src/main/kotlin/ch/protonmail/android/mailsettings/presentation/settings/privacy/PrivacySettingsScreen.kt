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

package ch.protonmail.android.mailsettings.presentation.settings.privacy

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun PrivacySettingsScreen(
    modifier: Modifier,
    onBackClick: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val actions = remember {
        mutableStateOf(
            PrivacySettingsScreen.Actions(
                onBackClick = onBackClick,
                onShowRemoteContent = viewModel::onAutoShowRemoteContentToggled,
                onShowEmbeddedImages = viewModel::onAutoShowEmbeddedImagesToggled,
                onRequestLinkConfirmation = viewModel::onConfirmLinkToggled,
                onPreventScreenshots = viewModel::onPreventScreenshotsToggled,
                onAllowBackgroundSync = viewModel::onAllowBackgroundSyncToggled
            )
        )
    }

    PrivacySettingsScreen(
        modifier = modifier.testTag(PrivacySettingsTestTags.RootItem),
        state = state,
        actions = actions.value
    )
}

@Composable
fun PrivacySettingsScreen(
    modifier: Modifier = Modifier,
    state: PrivacySettingsState,
    actions: PrivacySettingsScreen.Actions
) {
    val snackbarHostState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val updateError = stringResource(id = R.string.mail_settings_privacy_error_updating)

    Scaffold(
        modifier = modifier.testTag(PrivacySettingsTestTags.RootItem),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_privacy),
                onBackClick = actions.onBackClick
            )
        },
        snackbarHost = { DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState) },
        content = { paddingValues ->
            when (state) {
                PrivacySettingsState.Loading -> ProtonCenteredProgress()
                PrivacySettingsState.LoadingError -> PrivacySettingsListError()
                is PrivacySettingsState.WithData -> {
                    PrivacySettingsList(
                        modifier = Modifier.padding(paddingValues),
                        state = state,
                        actions = actions
                    )
                    ConsumableLaunchedEffect(state.updateSettingsError) {
                        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = updateError)
                    }
                }
            }
        }
    )
}

object PrivacySettingsScreen {
    data class Actions(
        val onBackClick: () -> Unit,
        val onShowRemoteContent: (Boolean) -> Unit,
        val onShowEmbeddedImages: (Boolean) -> Unit,
        val onPreventScreenshots: (Boolean) -> Unit,
        val onRequestLinkConfirmation: (Boolean) -> Unit,
        val onAllowBackgroundSync: (Boolean) -> Unit
    )
}

@Preview
@Composable
private fun PrivacySettingsScreenPreview() {
    ProtonTheme {
        PrivacySettingsScreen(
            state = PrivacySettingsState.WithData(
                PrivacySettings(
                    allowBackgroundSync = true,
                    autoShowEmbeddedImages = false,
                    autoShowRemoteContent = false,
                    preventTakingScreenshots = false,
                    requestLinkConfirmation = false
                ),
                updateSettingsError = Effect.empty()
            ),
            actions = PrivacySettingsScreen.Actions(
                onBackClick = {},
                onShowRemoteContent = {},
                onShowEmbeddedImages = {},
                onPreventScreenshots = {},
                onRequestLinkConfirmation = {},
                onAllowBackgroundSync = {}
            )
        )
    }
}

object PrivacySettingsTestTags {

    const val RootItem = "PrivacySettingsScreenRootItem"
}
